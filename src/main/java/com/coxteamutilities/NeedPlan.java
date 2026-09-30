package com.coxteamutilities;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Doses you want to drink at each room. The totals are what the supply rows compare against,
 * and "from this room on" is what has to be in your inventory before you walk in.
 * Stored as "ROOM:POTION:doses,..." with just the rooms that have any.
 */
public final class NeedPlan
{
	public static final int MAX_DOSES = 99;
	/** Stored form of a plan with nothing in it, so it isn't mistaken for "never set". */
	private static final String NONE = "none";

	private final Map<CmRoom, Map<Potion, Integer>> doses = new EnumMap<>(CmRoom.class);

	public NeedPlan()
	{
	}

	/** A sensible first plan: everything at Olm. */
	public static NeedPlan defaults()
	{
		NeedPlan plan = new NeedPlan();
		plan.set(CmRoom.OLM, Potion.OVERLOAD, 4);
		plan.set(CmRoom.OLM, Potion.XERICS_AID, 12);
		plan.set(CmRoom.OLM, Potion.REVITALISATION, 8);
		plan.set(CmRoom.OLM, Potion.PRAYER_ENHANCE, 4);
		return plan;
	}

	/** The defaults for something never stored, otherwise whatever parses. */
	public static NeedPlan parse(String encoded)
	{
		if (encoded == null || encoded.isEmpty())
		{
			return defaults();
		}
		NeedPlan plan = new NeedPlan();
		for (String entry : encoded.split(","))
		{
			String[] parts = entry.split(":");
			if (parts.length != 3)
			{
				continue;
			}
			try
			{
				plan.set(CmRoom.valueOf(parts[0]), Potion.valueOf(parts[1]), Integer.parseInt(parts[2]));
			}
			catch (IllegalArgumentException e)
			{
				// an entry from another version
			}
		}
		return plan;
	}

	public synchronized String encode()
	{
		List<String> entries = new ArrayList<>();
		for (Map.Entry<CmRoom, Map<Potion, Integer>> room : doses.entrySet())
		{
			for (Map.Entry<Potion, Integer> potion : room.getValue().entrySet())
			{
				if (potion.getValue() > 0)
				{
					entries.add(room.getKey().name() + ":" + potion.getKey().name() + ":" + potion.getValue());
				}
			}
		}
		return entries.isEmpty() ? NONE : String.join(",", entries);
	}

	public synchronized NeedPlan copy()
	{
		return parse(encode());
	}

	public synchronized int get(CmRoom room, Potion potion)
	{
		Map<Potion, Integer> room_ = doses.get(room);
		return room_ == null ? 0 : room_.getOrDefault(potion, 0);
	}

	/** @return whether anything changed */
	public synchronized boolean set(CmRoom room, Potion potion, int count)
	{
		count = Math.max(0, Math.min(MAX_DOSES, count));
		if (get(room, potion) == count)
		{
			return false;
		}
		doses.computeIfAbsent(room, r -> new EnumMap<>(Potion.class)).put(potion, count);
		return true;
	}

	public synchronized int total(Potion potion)
	{
		return fromRoomOn(CmRoom.values()[0], potion);
	}

	/** Doses still to drink at this room and every room after it. */
	public synchronized int fromRoomOn(CmRoom from, Potion potion)
	{
		int total = 0;
		for (CmRoom room : CmRoom.values())
		{
			if (room.ordinal() >= from.ordinal())
			{
				total += get(room, potion);
			}
		}
		return total;
	}
}
