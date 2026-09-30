package com.coxteamutilities;

/**
 * The raid potions. Every potion has 12 consecutive item ids:
 * (-) 1-4 dose, regular 1-4 dose, (+) 1-4 dose.
 * The supply potions get a row in the sidebar totals; all of them can be claimed as drops.
 */
public enum Potion
{
	OVERLOAD("Overload", "Ovl", 20985, true),
	XERICS_AID("Xeric's aid", "Aid", 20973, true),
	REVITALISATION("Revitalisation", "Revit", 20949, true),
	PRAYER_ENHANCE("Prayer enhance", "Enh", 20961, true),
	ELDER("Elder", "Elder", 20913, false),
	TWISTED("Twisted", "Twisted", 20925, false),
	KODAI("Kodai", "Kodai", 20937, false);

	public static final int DOSES_PER_POTION = 4;

	private static final int VARIANTS = 12;

	private final String displayName;
	private final String shortName;
	private final int firstItemId;
	private final boolean supply;

	Potion(String displayName, String shortName, int firstItemId, boolean supply)
	{
		this.displayName = displayName;
		this.shortName = shortName;
		this.firstItemId = firstItemId;
		this.supply = supply;
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

	/** Item id of the full (+) potion, used for the sidebar icon. */
	public int getIconItemId()
	{
		return firstItemId + VARIANTS - 1;
	}

	public boolean matches(int itemId)
	{
		return itemId >= firstItemId && itemId < firstItemId + VARIANTS;
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
