package com.coxteamutilities;

/** Challenge Mode rooms in the order the fixed layout visits them. */
public enum CmRoom
{
	TEKTON("Tekton", "Tekton", 1),
	CRABS("Crabs", "Crabs", 1),
	ICE_DEMON("Ice Demon", "Ice", 1),
	SHAMANS("Shamans", "Shamans", 1),
	VANGUARDS("Vanguards", "Vangs", 2),
	THIEVING("Thieving", "Thieving", 2),
	VESPULA("Vespula", "Vespula", 2),
	TIGHTROPE("Tightrope", "Rope", 2),
	GUARDIANS("Guardians", "Guards", 3),
	VASA("Vasa", "Vasa", 3),
	MYSTICS("Mystics", "Mystics", 3),
	MUTTADILE("Muttadile", "Mutta", 3),
	OLM("Olm", "Olm", 4);

	private final String displayName;
	private final String shortName;
	private final int floor;

	CmRoom(String displayName, String shortName, int floor)
	{
		this.displayName = displayName;
		this.shortName = shortName;
		this.floor = floor;
	}

	/** Fits next to five number fields in the sidebar. */
	public String getShortName()
	{
		return shortName;
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
