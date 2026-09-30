package com.coxteamutilities;

import java.util.ArrayList;
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

	static final class SlotView
	{
		Slot slot;
		boolean mine;
		final List<String> owners = new ArrayList<>();
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

	/** Doses that count towards what you need. */
	int have(Potion potion)
	{
		int have = inventory.doses(potion);
		if (privateStorage != null)
		{
			have += privateStorage.doses(potion);
		}
		if (countShared && sharedStorage != null)
		{
			have += sharedStorage.doses(potion);
		}
		if (countClaimed)
		{
			have += claimed.doses(potion);
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
					sb.append(',').append(slot.mine).append(slot.owners);
				}
			}
		}
		return sb.toString();
	}
}
