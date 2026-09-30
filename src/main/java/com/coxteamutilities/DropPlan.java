package com.coxteamutilities;

import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * How many of each potion every room is expected to drop. Starts from the OSRS Wiki drop tables
 * and takes edits from anyone in the party. Each edit carries a revision one higher than the
 * one it replaces, and the highest revision wins, so everyone ends up with the same numbers
 * whatever order the edits arrive in.
 */
public final class DropPlan
{
	/** Count that means "whatever the wiki says". */
	public static final int DEFAULT = -1;
	public static final int MAX_COUNT = 12;

	private static final Map<CmRoom, Map<Potion, Integer>> WIKI = new EnumMap<>(CmRoom.class);

	static
	{
		// oldschool.runescape.wiki drop tables, all (+)(4). The wiki lists no team size scaling.
		wiki(CmRoom.TEKTON, Potion.OVERLOAD, 2, Potion.REVITALISATION, 1, Potion.PRAYER_ENHANCE, 1);
		// 1/3 per vanguard for an overload, at least 1 and at most 3 for the room
		wiki(CmRoom.VANGUARDS, Potion.OVERLOAD, 1, Potion.XERICS_AID, 4, Potion.REVITALISATION, 2,
			Potion.PRAYER_ENHANCE, 1, Potion.ELDER, 1, Potion.TWISTED, 1, Potion.KODAI, 1);
		wiki(CmRoom.VESPULA, Potion.OVERLOAD, 1, Potion.XERICS_AID, 2, Potion.REVITALISATION, 1,
			Potion.PRAYER_ENHANCE, 1);
		wiki(CmRoom.VASA, Potion.OVERLOAD, 1, Potion.XERICS_AID, 2, Potion.TWISTED, 2);
		wiki(CmRoom.MUTTADILE, Potion.OVERLOAD, 2, Potion.XERICS_AID, 1, Potion.REVITALISATION, 1,
			Potion.PRAYER_ENHANCE, 2);
	}

	private static final class Entry
	{
		private final int count;
		private final long revision;

		private Entry(int count, long revision)
		{
			this.count = count;
			this.revision = revision;
		}
	}

	private final Map<String, Entry> edits = new HashMap<>();

	private static void wiki(CmRoom room, Object... potionCounts)
	{
		Map<Potion, Integer> counts = new EnumMap<>(Potion.class);
		for (int i = 0; i < potionCounts.length; i += 2)
		{
			counts.put((Potion) potionCounts[i], (Integer) potionCounts[i + 1]);
		}
		WIKI.put(room, counts);
	}

	public static int wikiCount(CmRoom room, Potion potion)
	{
		Map<Potion, Integer> counts = WIKI.get(room);
		return counts == null ? 0 : counts.getOrDefault(potion, 0);
	}

	/** Whether the room drops potions at all. */
	public static boolean hasDrops(CmRoom room)
	{
		return WIKI.containsKey(room);
	}

	/** The one drop that is rolled per raid, so its edits don't carry over to the next raid. */
	public static boolean isRandom(CmRoom room, Potion potion)
	{
		return room == CmRoom.VANGUARDS && potion == Potion.OVERLOAD;
	}

	private static String key(CmRoom room, Potion potion)
	{
		return room.name() + ":" + potion.name();
	}

	public synchronized int count(CmRoom room, Potion potion)
	{
		Entry entry = edits.get(key(room, potion));
		return entry == null || entry.count == DEFAULT ? wikiCount(room, potion) : entry.count;
	}

	public synchronized boolean isEdited(CmRoom room, Potion potion)
	{
		Entry entry = edits.get(key(room, potion));
		return entry != null && entry.count != DEFAULT && entry.count != wikiCount(room, potion);
	}

	/**
	 * A local edit.
	 *
	 * @param count 0 to {@link #MAX_COUNT}, or {@link #DEFAULT}
	 * @return whether the plan changed
	 */
	public synchronized boolean set(CmRoom room, Potion potion, int count)
	{
		count = count == DEFAULT ? DEFAULT : Math.max(0, Math.min(MAX_COUNT, count));
		String key = key(room, potion);
		Entry entry = edits.get(key);
		if (entry == null ? count == DEFAULT : entry.count == count)
		{
			return false;
		}
		edits.put(key, new Entry(count, entry == null ? 1 : entry.revision + 1));
		return true;
	}

	/** Puts every edited count back to the wiki's, as an edit so it reaches the party too. */
	public synchronized boolean reset(boolean randomOnly)
	{
		boolean changed = false;
		for (CmRoom room : CmRoom.values())
		{
			for (Potion potion : Potion.values())
			{
				if (!randomOnly || isRandom(room, potion))
				{
					changed |= set(room, potion, DEFAULT);
				}
			}
		}
		return changed;
	}

	/** Forgets every edit without telling anyone. */
	public synchronized void clear()
	{
		edits.clear();
	}

	/** Entries as "ROOM:POTION:count:revision", for the party and for the config. */
	public synchronized List<String> encode()
	{
		List<String> encoded = new ArrayList<>();
		for (Map.Entry<String, Entry> e : edits.entrySet())
		{
			encoded.add(e.getKey() + ":" + e.getValue().count + ":" + e.getValue().revision);
		}
		return encoded;
	}

	/**
	 * Takes edits from someone else, keeping whichever side has the higher revision.
	 *
	 * @return whether the plan changed
	 */
	public synchronized boolean merge(Collection<String> encoded)
	{
		boolean changed = false;
		if (encoded == null)
		{
			return false;
		}
		for (String line : encoded)
		{
			changed |= mergeLine(line);
		}
		return changed;
	}

	private boolean mergeLine(String line)
	{
		if (line == null)
		{
			return false;
		}
		String[] parts = line.split(":");
		if (parts.length != 4)
		{
			return false;
		}
		int count;
		long revision;
		String key;
		try
		{
			key = key(CmRoom.valueOf(parts[0]), Potion.valueOf(parts[1]));
			count = Integer.parseInt(parts[2]);
			revision = Long.parseLong(parts[3]);
		}
		catch (IllegalArgumentException e)
		{
			return false;
		}
		if (count < DEFAULT || count > MAX_COUNT || revision < 1)
		{
			return false;
		}
		Entry entry = edits.get(key);
		// the same revision twice means two people edited at once; the higher count wins on every client
		if (entry != null && (entry.revision > revision || (entry.revision == revision && entry.count >= count)))
		{
			return false;
		}
		edits.put(key, new Entry(count, revision));
		return true;
	}
}
