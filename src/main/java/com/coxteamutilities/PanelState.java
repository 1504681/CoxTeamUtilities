package com.coxteamutilities;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** A snapshot of everything the sidebar shows, built by the plugin and drawn on the Swing thread. */
final class PanelState
{
	static final class Member
	{
		String name;
		boolean self;
		/** null until the member's plugin has sent something */
		MemberStatus status;
	}

	/** One person's share of a potion. */
	static final class Holder
	{
		final String name;
		final boolean self;
		final Claim claim;

		Holder(String name, boolean self, Claim claim)
		{
			this.name = name;
			this.self = self;
			this.claim = claim;
		}

		/** Where they start sipping. With no rooms picked that's as soon as it drops. */
		private int startsAt()
		{
			List<CmRoom> rooms = claim.getSipRooms();
			return (rooms.isEmpty() ? claim.getSlot().getRoom() : rooms.get(0)).ordinal();
		}
	}

	/** One dropped potion and the people it is passed along, in the order they hold it. */
	static final class SlotView
	{
		Slot slot;
		final List<Holder> holders = new ArrayList<>();

		void add(String name, boolean self, Claim claim)
		{
			holders.add(new Holder(name, self, claim));
			holders.sort(Comparator.comparingInt(Holder::startsAt)
				.thenComparingInt(h -> h.claim.getOrder())
				.thenComparing(h -> h.name));
		}

		Holder me()
		{
			for (Holder holder : holders)
			{
				if (holder.self)
				{
					return holder;
				}
			}
			return null;
		}

		int claimedDoses()
		{
			int doses = 0;
			for (Holder holder : holders)
			{
				doses += holder.claim.getDoses();
			}
			return doses;
		}

		int freeDoses()
		{
			return Math.max(0, Claim.MAX_DOSES - claimedDoses());
		}

		/** More doses claimed than the potion has, which happens when two people click at once. */
		boolean overclaimed()
		{
			return claimedDoses() > Claim.MAX_DOSES;
		}

		/** Doses you could hold: everything the others haven't claimed. */
		int availableToMe()
		{
			Holder me = me();
			return Math.max(0, Claim.MAX_DOSES - claimedDoses() + (me == null ? 0 : me.claim.getDoses()));
		}

		/** Whether there is more to say than one name: shared, partly claimed or with sip rooms. */
		boolean planned()
		{
			if (holders.isEmpty())
			{
				return false;
			}
			return holders.size() > 1 || freeDoses() > 0 || !holders.get(0).claim.getSipRooms().isEmpty();
		}

		/** "You: 2 doses (Tekton, Vanguards), then drop for Bob", one line per holder, or one line for a conflict. */
		List<String> planLines()
		{
			List<String> lines = new ArrayList<>();
			if (overclaimed())
			{
				// no hand-off makes sense until someone gives way
				List<String> shares = new ArrayList<>();
				for (Holder holder : holders)
				{
					shares.add(holder.name + " " + holder.claim.getDoses());
				}
				lines.add("Too many doses claimed: " + String.join(", ", shares));
				return lines;
			}
			for (int i = 0; i < holders.size(); i++)
			{
				Holder holder = holders.get(i);
				int doses = holder.claim.getDoses();
				StringBuilder line = new StringBuilder(holder.name).append(": ");
				if (i > 0)
				{
					line.append("pick up, ");
				}
				line.append(doses).append(doses == 1 ? " dose" : " doses");
				if (!holder.claim.getSipRooms().isEmpty())
				{
					List<String> rooms = new ArrayList<>();
					for (CmRoom room : holder.claim.getSipRooms())
					{
						rooms.add(room.getDisplayName());
					}
					line.append(" (").append(String.join(", ", rooms)).append(')');
				}
				if (i + 1 < holders.size())
				{
					line.append(", then drop for ").append(holders.get(i + 1).name);
				}
				else if (freeDoses() > 0)
				{
					line.append(", then drop, ").append(freeDoses()).append(" unclaimed");
				}
				lines.add(line.toString());
			}
			return lines;
		}
	}

	static final class PotionDrop
	{
		Potion potion;
		int count;
		boolean edited;
		final List<SlotView> slots = new ArrayList<>();
	}

	static final class RoomDrops
	{
		CmRoom room;
		final List<PotionDrop> potions = new ArrayList<>();
	}

	boolean inParty;
	boolean countShared;
	boolean countClaimed;
	boolean countSplit;

	Supplies inventory = Supplies.EMPTY;
	/** null until the storage has been opened this raid */
	Supplies privateStorage;
	Supplies sharedStorage;
	Supplies claimed = Supplies.EMPTY;
	final Map<Potion, Integer> need = new EnumMap<>(Potion.class);

	final Set<Role> roles = EnumSet.noneOf(Role.class);
	final Map<Role, List<String>> missing = new EnumMap<>(Role.class);

	final List<Member> team = new ArrayList<>();
	final List<RoomDrops> drops = new ArrayList<>();

	/** Everything within reach: inventory, private storage, and shared storage if that counts. */
	private Supplies held()
	{
		Supplies held = inventory;
		if (privateStorage != null)
		{
			held = held.plus(privateStorage);
		}
		if (countShared && sharedStorage != null)
		{
			held = held.plus(sharedStorage);
		}
		return held;
	}

	/** Overload doses you hold as sets of elder, twisted and kodai. */
	int splitHeld()
	{
		return held().splitOverloadDoses();
	}

	/** Doses that count towards what you need. */
	int have(Potion potion)
	{
		int have = held().doses(potion);
		if (countClaimed)
		{
			have += claimed.doses(potion);
		}
		if (potion == Potion.OVERLOAD && countSplit)
		{
			have += splitHeld();
			if (countClaimed)
			{
				have += claimed.doses(Potion.SPLIT_OVERLOAD);
			}
		}
		return have;
	}

	int shortfall(Potion potion)
	{
		return Math.max(0, need.getOrDefault(potion, 0) - have(potion));
	}

	/** Changes whenever the team section has to be redrawn. */
	String teamSignature()
	{
		StringBuilder sb = new StringBuilder().append(inParty);
		for (Member member : team)
		{
			sb.append('|').append(member.name).append(member.self);
			if (member.status != null)
			{
				sb.append(member.status.getRoles()).append(member.status.getMissing())
					.append(member.status.getCarried());
			}
		}
		return sb.toString();
	}

	/** Changes whenever the drops section has to be redrawn. */
	String dropsSignature()
	{
		StringBuilder sb = new StringBuilder();
		for (RoomDrops room : drops)
		{
			sb.append('|').append(room.room);
			for (PotionDrop drop : room.potions)
			{
				sb.append(';').append(drop.potion).append(drop.count).append(drop.edited);
				for (SlotView slot : drop.slots)
				{
					sb.append(',');
					for (Holder holder : slot.holders)
					{
						sb.append(holder.self).append(holder.name).append(holder.claim).append('/');
					}
				}
			}
		}
		return sb.toString();
	}
}
