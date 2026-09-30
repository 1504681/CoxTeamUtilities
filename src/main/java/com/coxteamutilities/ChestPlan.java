package com.coxteamutilities;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * What to do at one storage unit: things to put in and things to take out, in order.
 * A line is an item name, matched from its start so "Xeric's aid" covers every dose,
 * with an optional count like "Stinkhorn mushroom x3". "everything" deposits it all.
 */
public final class ChestPlan
{
	public static final String EVERYTHING = "everything";
	public static final int MAX_LINES = 40;
	public static final int MAX_LINE = 60;

	/** One line of a list, parsed. */
	public static final class Line
	{
		public final String text;
		public final String name;
		public final int count;
		public final boolean everything;

		Line(String text)
		{
			this.text = text;
			String lower = text.toLowerCase(Locale.ROOT);
			everything = lower.equals(EVERYTHING) || lower.equals("*") || lower.equals("all");
			int count = 1;
			String name = text;
			int x = lower.lastIndexOf(" x");
			if (x > 0)
			{
				try
				{
					count = Math.max(1, Integer.parseInt(lower.substring(x + 2).trim()));
					name = text.substring(0, x).trim();
				}
				catch (NumberFormatException e)
				{
					// "x" was part of the name
				}
			}
			this.name = name;
			this.count = count;
		}

		/** Whether an item with this name is what the line asks for. */
		public boolean matches(String itemName)
		{
			return everything || (itemName != null
				&& itemName.toLowerCase(Locale.ROOT).startsWith(name.toLowerCase(Locale.ROOT)));
		}

		@Override
		public String toString()
		{
			return text;
		}
	}

	private String key;
	private String name;
	private List<String> deposit = new ArrayList<>();
	private List<String> withdraw = new ArrayList<>();
	private boolean ordered;

	public ChestPlan()
	{
	}

	public ChestPlan(String key, String name)
	{
		this.key = key;
		this.name = name;
	}

	public String getKey()
	{
		return key;
	}

	public String getName()
	{
		return name == null ? "" : name;
	}

	public void setName(String name)
	{
		this.name = name == null ? "" : name.trim();
	}

	public boolean isOrdered()
	{
		return ordered;
	}

	public void setOrdered(boolean ordered)
	{
		this.ordered = ordered;
	}

	public List<String> getDeposit()
	{
		return deposit == null ? deposit = new ArrayList<>() : deposit;
	}

	public List<String> getWithdraw()
	{
		return withdraw == null ? withdraw = new ArrayList<>() : withdraw;
	}

	/** Replaces a list with the non-empty lines of some typed text. */
	public static List<String> lines(String text)
	{
		List<String> lines = new ArrayList<>();
		if (text == null)
		{
			return lines;
		}
		for (String line : text.split("\n"))
		{
			line = line.trim();
			if (!line.isEmpty() && lines.size() < MAX_LINES)
			{
				lines.add(line.length() > MAX_LINE ? line.substring(0, MAX_LINE) : line);
			}
		}
		return lines;
	}

	public static List<Line> parse(List<String> lines)
	{
		List<Line> parsed = new ArrayList<>();
		for (String line : lines)
		{
			parsed.add(new Line(line));
		}
		return parsed;
	}

	/**
	 * Lines for an inventory in slot order, as "Name" or "Name xN" for a run of the same thing.
	 * Dose and charge suffixes are dropped so a line matches any of them.
	 */
	public static List<String> fromInventory(List<String> itemNames)
	{
		List<String> lines = new ArrayList<>();
		String last = null;
		int run = 0;
		for (String itemName : itemNames)
		{
			if (itemName == null)
			{
				continue;
			}
			String name = baseName(itemName);
			if (name.equals(last))
			{
				run++;
			}
			else
			{
				flush(lines, last, run);
				last = name;
				run = 1;
			}
		}
		flush(lines, last, run);
		return lines;
	}

	private static void flush(List<String> lines, String name, int run)
	{
		if (name != null && lines.size() < MAX_LINES)
		{
			lines.add(run > 1 ? name + " x" + run : name);
		}
	}

	/** "Xeric's aid(4)" and "Toxic blowpipe (charged)" become "Xeric's aid" and "Toxic blowpipe". */
	static String baseName(String itemName)
	{
		int paren = itemName.indexOf('(');
		return paren > 0 ? itemName.substring(0, paren).trim() : itemName.trim();
	}
}
