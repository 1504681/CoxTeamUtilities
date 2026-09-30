package com.coxteamutilities;

/** Challenge Mode rooms in the order the fixed layout visits them. */
public enum CmRoom
{
	TEKTON("Tekton", 1),
	CRABS("Crabs", 1),
	ICE_DEMON("Ice Demon", 1),
	SHAMANS("Shamans", 1),
	VANGUARDS("Vanguards", 2),
	THIEVING("Thieving", 2),
	VESPULA("Vespula", 2),
	TIGHTROPE("Tightrope", 2),
	GUARDIANS("Guardians", 3),
	VASA("Vasa", 3),
	MYSTICS("Mystics", 3),
	MUTTADILE("Muttadile", 3),
	OLM("Olm", 4);

	private final String displayName;
	private final int floor;

	CmRoom(String displayName, int floor)
	{
		this.displayName = displayName;
		this.floor = floor;
	}

	public String getDisplayName()
	{
		return displayName;
	}

	public int getFloor()
	{
		return floor;
	}
}
