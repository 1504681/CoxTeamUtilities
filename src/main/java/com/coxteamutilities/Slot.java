package com.coxteamutilities;

/** One potion of one room's drop, the unit people claim. Synced as "ROOM:POTION:index". */
public final class Slot
{
	private final CmRoom room;
	private final Potion potion;
	private final int index;

	public Slot(CmRoom room, Potion potion, int index)
	{
		this.room = room;
		this.potion = potion;
		this.index = index;
	}

	/** @return the slot, or null if the key isn't one this version understands */
	public static Slot parse(String key)
	{
		if (key == null)
		{
			return null;
		}
		String[] parts = key.split(":");
		if (parts.length != 3)
		{
			return null;
		}
		try
		{
			int index = Integer.parseInt(parts[2]);
			if (index < 0 || index >= DropPlan.MAX_COUNT)
			{
				return null;
			}
			return new Slot(CmRoom.valueOf(parts[0]), Potion.valueOf(parts[1]), index);
		}
		catch (IllegalArgumentException e)
		{
			return null;
		}
	}

	public CmRoom getRoom()
	{
		return room;
	}

	public Potion getPotion()
	{
		return potion;
	}

	public int getIndex()
	{
		return index;
	}

	public String key()
	{
		return room.name() + ":" + potion.name() + ":" + index;
	}

	@Override
	public boolean equals(Object o)
	{
		return o instanceof Slot && key().equals(((Slot) o).key());
	}

	@Override
	public int hashCode()
	{
		return key().hashCode();
	}

	@Override
	public String toString()
	{
		return key();
	}
}
