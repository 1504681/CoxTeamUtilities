package com.coxteamutilities;

import java.util.function.Predicate;

/** One thing a role needs, checked against what the player carries. */
public final class Requirement
{
	private final String name;
	private final Predicate<Loadout> check;

	private Requirement(String name, Predicate<Loadout> check)
	{
		this.name = name;
		this.check = check;
	}

	/** Met by carrying any one of the items. */
	public static Requirement anyItem(String name, int... itemIds)
	{
		return new Requirement(name, loadout -> loadout.quantityOfAny(itemIds) > 0);
	}

	/** Met by carrying at least this many of the rune, loose or in a rune pouch. */
	public static Requirement runes(String name, int itemId, int quantity)
	{
		return new Requirement(name, loadout -> loadout.quantity(itemId) >= quantity);
	}

	public static Requirement standardSpellbook()
	{
		return new Requirement("Standard spellbook", loadout -> loadout.getSpellbook() == Loadout.SPELLBOOK_STANDARD);
	}

	public String getName()
	{
		return name;
	}

	public boolean isMet(Loadout loadout)
	{
		return check.test(loadout);
	}
}
