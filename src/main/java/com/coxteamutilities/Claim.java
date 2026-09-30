package com.coxteamutilities;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

/**
 * Someone's share of one dropped potion: how many doses, at which rooms they sip them, and their
 * place in the line of people the potion is passed along. Synced as
 * "ROOM:POTION:index:doses:order" with ":ROOM+ROOM" after it when sip rooms are picked.
 */
public final class Claim
{
	public static final int MAX_DOSES = Potion.DOSES_PER_POTION;
	static final int MAX_ORDER = 99;

	private final Slot slot;
	private final int doses;
	private final int order;
	private final List<CmRoom> sipRooms;

	/**
	 * @param order    place in line when two holders start at the same room, lower goes first
	 * @param sipRooms rooms before the one that drops the potion are left out, and no more rooms
	 *                 are kept than there are doses
	 */
	public Claim(Slot slot, int doses, int order, Collection<CmRoom> sipRooms)
	{
		this.slot = slot;
		this.doses = Math.max(1, Math.min(MAX_DOSES, doses));
		this.order = Math.max(1, Math.min(MAX_ORDER, order));

		Set<CmRoom> sorted = EnumSet.noneOf(CmRoom.class);
		if (sipRooms != null)
		{
			sorted.addAll(sipRooms);
		}
		List<CmRoom> rooms = new ArrayList<>();
		for (CmRoom room : sorted)
		{
			if (room.ordinal() >= slot.getRoom().ordinal() && rooms.size() < this.doses)
			{
				rooms.add(room);
			}
		}
		this.sipRooms = Collections.unmodifiableList(rooms);
	}

	/** All four doses, no rooms picked. */
	public static Claim whole(Slot slot, int order)
	{
		return new Claim(slot, MAX_DOSES, order, null);
	}

	/** @return the claim, or null if this version can't read it */
	public static Claim parse(String encoded)
	{
		if (encoded == null)
		{
			return null;
		}
		String[] parts = encoded.split(":");
		if (parts.length < 5 || parts.length > 6)
		{
			return null;
		}
		Slot slot = Slot.parse(parts[0] + ":" + parts[1] + ":" + parts[2]);
		if (slot == null)
		{
			return null;
		}
		try
		{
			int doses = Integer.parseInt(parts[3]);
			int order = Integer.parseInt(parts[4]);
			if (doses < 1 || doses > MAX_DOSES || order < 1 || order > MAX_ORDER)
			{
				return null;
			}
			List<CmRoom> rooms = new ArrayList<>();
			if (parts.length == 6)
			{
				for (String room : parts[5].split("\\+"))
				{
					rooms.add(CmRoom.valueOf(room));
				}
			}
			return new Claim(slot, doses, order, rooms);
		}
		catch (IllegalArgumentException e)
		{
			return null;
		}
	}

	public String encode()
	{
		StringBuilder sb = new StringBuilder(slot.key()).append(':').append(doses).append(':').append(order);
		for (int i = 0; i < sipRooms.size(); i++)
		{
			sb.append(i == 0 ? ':' : '+').append(sipRooms.get(i).name());
		}
		return sb.toString();
	}

	public Slot getSlot()
	{
		return slot;
	}

	public int getDoses()
	{
		return doses;
	}

	public int getOrder()
	{
		return order;
	}

	public List<CmRoom> getSipRooms()
	{
		return sipRooms;
	}

	/** Fewer doses than rooms drops the latest rooms. */
	public Claim withDoses(int doses)
	{
		return new Claim(slot, doses, order, sipRooms);
	}

	public Claim withRoom(CmRoom room, boolean sipThere)
	{
		Set<CmRoom> rooms = EnumSet.noneOf(CmRoom.class);
		rooms.addAll(sipRooms);
		if (sipThere)
		{
			rooms.add(room);
		}
		else
		{
			rooms.remove(room);
		}
		return new Claim(slot, doses, order, rooms);
	}

	@Override
	public boolean equals(Object o)
	{
		return o instanceof Claim && encode().equals(((Claim) o).encode());
	}

	@Override
	public int hashCode()
	{
		return encode().hashCode();
	}

	@Override
	public String toString()
	{
		return encode();
	}
}
