package com.coxteamutilities;

import java.util.HashMap;
import java.util.Map;

/**
 * Everything the player has with them that a role can ask for: inventory, worn items,
 * private storage and the rune pouch, plus the active spellbook.
 */
public final class Loadout
{
	public static final int SPELLBOOK_STANDARD = 0;

	private final Map<Integer, Integer> quantities = new HashMap<>();
	private int spellbook = SPELLBOOK_STANDARD;

	public Loadout add(int itemId, int quantity)
	{
		if (itemId > 0 && quantity > 0)
		{
			quantities.merge(itemId, quantity, Integer::sum);
		}
		return this;
	}

	public Loadout spellbook(int spellbook)
	{
		this.spellbook = spellbook;
		return this;
	}

	public int getSpellbook()
	{
		return spellbook;
	}

	public int quantity(int itemId)
	{
		return quantities.getOrDefault(itemId, 0);
	}

	public int quantityOfAny(int... itemIds)
	{
		int total = 0;
		for (int itemId : itemIds)
		{
			total += quantity(itemId);
		}
		return total;
	}
}
