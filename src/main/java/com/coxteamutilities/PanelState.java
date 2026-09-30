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
		boolean iron;
		/** null until the member's plugin has sent something */
		MemberStatus status;
	}

	/** One person's share of a potion. */
	static final class Holder
	{
		final String name;
		final boolean self;
		final boolean iron;
		final Claim claim;

		Holder(String name, boolean self, boolean iron, Claim claim)
		{
			this.name = name;
			this.self = self;
			this.iron = iron;
			this.claim = claim;
		}

		String label()
		{
			return iron ? name + " (iron)" : name;
		}

		/** Where they start sipping. With no rooms picked that's as soon as it drops. */
		private int startsAt()
		{
			List<CmRoom> rooms = claim.getSipRooms();
			return (rooms.isEmpty() ? claim.getSlot().getRoom() : rooms.get(0)).ordinal();
		}
	}

	/**
	 * One dropped potion and the people it is passed along, in the order they hold it.
	 * An ironman can't pick up what someone else has held, so an ironman always holds it first,
	 * outranks everyone else's claim, and there can only be one per potion.
	 */
	static final class SlotView
	{
		Slot slot;
		/** Whether the person looking at the sidebar is an ironman. */
		boolean viewerIron;
		final List<Holder> holders = new ArrayList<>();

		void add(String name, boolean self, Claim claim)
		{
			add(name, self, false, claim);
		}

		void add(String name, boolean self, boolean iron, Claim claim)
		{
			holders.add(new Holder(name, self, iron, claim));
			holders.sort(Comparator.comparing((Holder h) -> !h.iron)
				.thenComparingInt(Holder::startsAt)
				.thenComparingInt(h -> h.claim.getOrder())
				.thenComparing(h -> h.name));
		}

		private int ironmen()
		{
			int ironmen = 0;
			for (Holder holder : holders)
			{
				ironmen += holder.iron ? 1 : 0;
			}
			return ironmen;
		}

		private int dosesOfOthers(boolean ironOnly)
		{
			int doses = 0;
			for (Holder holder : holders)
			{
				if (!holder.self && (holder.iron || !ironOnly))
				{
					doses += holder.claim.getDoses();
				}
			}
			return doses;
		}

		/** Doses of ironmen other than you, which is what everyone who isn't one has to leave alone. */
		int dosesOfOtherIronmen()
		{
			return dosesOfOthers(true);
		}

		/** Doses a click on the box takes. An ironman cutting in front of others takes one sip. */
		int takenOnClick()
		{
			int available = availableToMe();
			return viewerIron && dosesOfOthers(false) > 0 ? Math.min(1, available) : available;
		}

		/** Behind everyone who already has a share. */
		int nextOrder()
		{
			int order = 0;
			for (Holder holder : holders)
			{
				if (!holder.self)
				{
					order = Math.max(order, holder.claim.getOrder());
				}
			}
			return order + 1;
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

		/** More doses claimed than the potion has, or two ironmen on one potion. */
		boolean overclaimed()
		{
			return claimedDoses() > Claim.MAX_DOSES || ironmen() > 1;
		}

		/**
		 * Doses you could hold. Everything the others haven't claimed, and for an ironman everything
		 * unless another ironman is on it, because the rest give way.
		 */
		int availableToMe()
		{
			if (viewerIron)
			{
				return dosesOfOtherIronmen() > 0 ? 0 : Claim.MAX_DOSES;
			}
			return Math.max(0, Claim.MAX_DOSES - dosesOfOthers(false));
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
					shares.add(holder.label() + " " + holder.claim.getDoses());
				}
				lines.add((ironmen() > 1 ? "Ironmen can't pass a potion to each other: " : "Too many doses claimed: ")
					+ String.join(", ", shares));
				return lines;
			}
			for (int i = 0; i < holders.size(); i++)
			{
				Holder holder = holders.get(i);
				int doses = holder.claim.getDoses();
				StringBuilder line = new StringBuilder(holder.label()).append(": ");
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
					line.append(", then drop for ").append(holders.get(i + 1).label());
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
	/** Whether you are an ironman. */
	boolean iron;
	boolean countShared;
	boolean countSplit;

	Supplies inventory = Supplies.EMPTY;
	/** null until the storage has been opened this raid */
	Supplies privateStorage;
	Supplies sharedStorage;
	Supplies claimed = Supplies.EMPTY;
	final Map<Potion, Integer> need = new EnumMap<>(Potion.class);
	/** Whether the numbers are for a solo raid: the raid's size while in one, the tab otherwise. */
	boolean solo;
	boolean separateSoloNeeds;
	NeedUnits units = NeedUnits.DOSES;

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
		if (potion == Potion.OVERLOAD && countSplit)
		{
			have += splitHeld();
		}
		return have;
	}

	/** Claims anyone in the party has made. */
	int claimCount()
	{
		int count = 0;
		for (RoomDrops room : drops)
		{
			for (PotionDrop drop : room.potions)
			{
				for (SlotView slot : drop.slots)
				{
					count += slot.holders.size();
				}
			}
		}
		return count;
	}

	int shortfall(Potion potion)
	{
		return Math.max(0, need.getOrDefault(potion, 0) - have(potion));
	}

	/** Whether the supply row for this potion applies right now. */
	boolean applies(Potion potion)
	{
		return potion.isSupply() && (solo || !potion.isSoloOnly());
	}

	/** "Xeric's aid 8 doses, Stamina 1 potion" for everything short, empty when nothing is. */
	String shortfalls()
	{
		StringBuilder text = new StringBuilder();
		for (Potion potion : Potion.values())
		{
			int shortfall = applies(potion) ? shortfall(potion) : 0;
			if (shortfall > 0)
			{
				text.append(text.length() == 0 ? "" : ", ").append(potion.getDisplayName()).append(' ')
					.append(units.format(shortfall)).append(' ').append(units.getWord());
			}
		}
		return text.toString();
	}

	/** Changes whenever the team section has to be redrawn. */
	String teamSignature()
	{
		StringBuilder sb = new StringBuilder().append(inParty).append(units);
		for (Member member : team)
		{
			sb.append('|').append(member.name).append(member.self).append(member.iron);
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
				sb.append(';').append(drop.potion).append(drop.count).append(drop.edited).append(iron);
				for (SlotView slot : drop.slots)
				{
					sb.append(',');
					for (Holder holder : slot.holders)
					{
						sb.append(holder.self).append(holder.iron).append(holder.name).append(holder.claim).append('/');
					}
				}
			}
		}
		return sb.toString();
	}
}
