package com.coxteamutilities;

/**
 * The raid potions. Every potion has 12 consecutive item ids:
 * (-) 1-4 dose, regular 1-4 dose, (+) 1-4 dose.
 * The supply potions get a row in the sidebar totals. Elder, twisted and kodai are only counted
 * as the three parts of a split overload, which is what Vanguards drop one set of.
 */
public enum Potion
{
	OVERLOAD("Overload", "Ovl", 20985, true, true),
	XERICS_AID("Xeric's aid", "Aid", 20973, true, true),
	REVITALISATION("Revitalisation", "Revit", 20949, true, true),
	PRAYER_ENHANCE("Prayer enhance", "Enh", 20961, true, true),
	ELDER("Elder", "Elder", 20913, false, false),
	TWISTED("Twisted", "Twisted", 20925, false, false),
	KODAI("Kodai", "Kodai", 20937, false, false),
	/** One elder, one twisted and one kodai, claimed together. Not an item of its own. */
	SPLIT_OVERLOAD("Split overload", "Split", Item.NONE, false, true);

	private static final class Item
	{
		private static final int NONE = -1;
	}

	public static final int DOSES_PER_POTION = 4;

	private static final int VARIANTS = 12;

	private final String displayName;
	private final String shortName;
	private final int firstItemId;
	private final boolean supply;
	private final boolean claimable;

	Potion(String displayName, String shortName, int firstItemId, boolean supply, boolean claimable)
	{
		this.displayName = displayName;
		this.shortName = shortName;
		this.firstItemId = firstItemId;
		this.supply = supply;
		this.claimable = claimable;
	}

	public String getDisplayName()
	{
		return displayName;
	}

	public String getShortName()
	{
		return shortName;
	}

	/** Whether the sidebar totals and the "needed" settings cover this potion. */
	public boolean isSupply()
	{
		return supply;
	}

	/** Whether rooms drop it as something to claim. */
	public boolean isClaimable()
	{
		return claimable;
	}

	/** Item id of the full (+) potion, used for the sidebar icon. */
	public int getIconItemId()
	{
		return firstItemId + VARIANTS - 1;
	}

	public boolean matches(int itemId)
	{
		return firstItemId != Item.NONE && itemId >= firstItemId && itemId < firstItemId + VARIANTS;
	}

	/** Doses in one item with this id, 0 if it isn't this potion. */
	public int doses(int itemId)
	{
		return matches(itemId) ? (itemId - firstItemId) % DOSES_PER_POTION + 1 : 0;
	}

	public static Potion of(int itemId)
	{
		for (Potion potion : values())
		{
			if (potion.matches(itemId))
			{
				return potion;
			}
		}
		return null;
	}
}
