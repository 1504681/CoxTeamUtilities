package com.coxteamutilities;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Your own claims. Every change is told how many doses of the potion the rest of the party has
 * left you and what place in line a new claim gets, and refuses what doesn't fit.
 * Not thread safe, the plugin guards it.
 */
final class ClaimBook
{
	private final Map<Slot, Claim> claims = new LinkedHashMap<>();

	/** Drops your claim, or claims this many doses. */
	boolean toggle(Slot slot, int doses, int nextOrder)
	{
		if (claims.remove(slot) != null)
		{
			return true;
		}
		if (doses < 1)
		{
			return false;
		}
		claims.put(slot, new Claim(slot, doses, nextOrder, null));
		return true;
	}

	/**
	 * Gives way to ironmen, who have to be the first to hold a potion. Only for those who aren't one.
	 *
	 * @param ironDoses doses ironmen have claimed, per potion
	 * @return whether any of your claims shrank or went
	 */
	boolean yieldTo(Map<Slot, Integer> ironDoses)
	{
		boolean changed = false;
		for (Iterator<Map.Entry<Slot, Claim>> it = claims.entrySet().iterator(); it.hasNext(); )
		{
			Map.Entry<Slot, Claim> e = it.next();
			int left = Claim.MAX_DOSES - ironDoses.getOrDefault(e.getKey(), 0);
			if (left < 1)
			{
				it.remove();
				changed = true;
			}
			else if (e.getValue().getDoses() > left)
			{
				e.setValue(e.getValue().withDoses(left));
				changed = true;
			}
		}
		return changed;
	}

	boolean setDoses(Slot slot, int doses, int available, int nextOrder)
	{
		if (doses < 1 || doses > available)
		{
			return false;
		}
		Claim claim = claims.get(slot);
		claims.put(slot, claim == null ? new Claim(slot, doses, nextOrder, null) : claim.withDoses(doses));
		return true;
	}

	/** Picking a room with every dose already at a room takes one more dose, if there is one. */
	boolean setSipRoom(Slot slot, CmRoom room, boolean sipThere, int available, int nextOrder)
	{
		Claim claim = claims.get(slot);
		if (!sipThere)
		{
			if (claim == null)
			{
				return false;
			}
			claims.put(slot, claim.withRoom(room, false));
			return true;
		}
		if (room.ordinal() < slot.getRoom().ordinal())
		{
			return false;
		}
		if (claim == null)
		{
			if (available < 1)
			{
				return false;
			}
			claim = new Claim(slot, 1, nextOrder, null);
		}
		else if (!claim.getSipRooms().contains(room) && claim.getSipRooms().size() == claim.getDoses())
		{
			if (claim.getDoses() >= available)
			{
				return false;
			}
			claim = claim.withDoses(claim.getDoses() + 1);
		}
		claims.put(slot, claim.withRoom(room, true));
		return true;
	}

	/** A claim on the third overload goes away when the room turns out to drop two. */
	void dropPast(DropPlan plan)
	{
		for (Iterator<Slot> it = claims.keySet().iterator(); it.hasNext(); )
		{
			Slot slot = it.next();
			if (slot.getIndex() >= plan.count(slot.getRoom(), slot.getPotion()))
			{
				it.remove();
			}
		}
	}

	void clear()
	{
		claims.clear();
	}

	List<Claim> all()
	{
		return new ArrayList<>(claims.values());
	}

	Map<Slot, Claim> bySlot()
	{
		return new LinkedHashMap<>(claims);
	}

	/** Doses you've claimed, per potion. */
	Supplies doses()
	{
		int[] doses = new int[Potion.values().length];
		for (Claim claim : claims.values())
		{
			doses[claim.getSlot().getPotion().ordinal()] += claim.getDoses();
		}
		return Supplies.of(doses);
	}
}
