package com.coxteamutilities;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

/**
 * A job in one room and what you have to carry to do it. Adding a role is one entry here;
 * the sidebar, reminders and party sync pick it up from the enum.
 */
public enum Role
{
	TIGHTROPE_LURER(CmRoom.TIGHTROPE, "Lurer", "Lure the rangers and magers with a Venator bow or chinchompas",
		Requirement.anyItem("Venator bow or chins", ItemIds.VENATOR_BOW, ItemIds.VENATOR_BOW_ORNAMENT,
			ItemIds.CHINCHOMPA, ItemIds.RED_CHINCHOMPA, ItemIds.BLACK_CHINCHOMPA)),
	TIGHTROPE_TELEGRAB(CmRoom.TIGHTROPE, "Telegrabber", "Telegrab the keystone crystal",
		Requirement.standardSpellbook(),
		Requirement.runes("Law rune", ItemIds.LAW_RUNE, 1)),
	TIGHTROPE_CROSSER(CmRoom.TIGHTROPE, "Crosser", "Cross the tightrope; nothing to carry"),
	MUTTADILE_ZGS(CmRoom.MUTTADILE, "ZGS", "Freeze the muttadile with the Zamorak godsword spec",
		Requirement.anyItem("Zamorak godsword", ItemIds.ZAMORAK_GODSWORD, ItemIds.ZAMORAK_GODSWORD_OR)),
	MUTTADILE_ENTANGLE(CmRoom.MUTTADILE, "Entangler", "Entangle the muttadile",
		Requirement.standardSpellbook(),
		Requirement.runes("Nature runes", ItemIds.NATURE_RUNE, 4));

	private final CmRoom room;
	private final String displayName;
	private final String description;
	private final Requirement[] requirements;

	Role(CmRoom room, String displayName, String description, Requirement... requirements)
	{
		this.room = room;
		this.displayName = displayName;
		this.description = description;
		this.requirements = requirements;
	}

	public CmRoom getRoom()
	{
		return room;
	}

	public String getDisplayName()
	{
		return displayName;
	}

	public String getDescription()
	{
		return description;
	}

	/** "Muttadile ZGS" */
	public String getFullName()
	{
		return room.getDisplayName() + " " + displayName;
	}

	/** Names of the things this role needs that the loadout doesn't have. */
	public List<String> missing(Loadout loadout)
	{
		List<String> missing = new ArrayList<>();
		for (Requirement requirement : requirements)
		{
			if (!requirement.isMet(loadout))
			{
				missing.add(requirement.getName());
			}
		}
		return missing;
	}

	public static List<Role> forRoom(CmRoom room)
	{
		List<Role> roles = new ArrayList<>();
		for (Role role : values())
		{
			if (role.room == room)
			{
				roles.add(role);
			}
		}
		return roles;
	}

	/** Parses the stored/synced form; names this version doesn't know are skipped. */
	public static Set<Role> parse(Collection<String> names)
	{
		Set<Role> roles = EnumSet.noneOf(Role.class);
		if (names == null)
		{
			return roles;
		}
		for (String name : names)
		{
			if (name == null)
			{
				continue;
			}
			try
			{
				roles.add(valueOf(name.trim()));
			}
			catch (IllegalArgumentException e)
			{
				// a role from a newer or older version
			}
		}
		return roles;
	}

	public static List<String> names(Collection<Role> roles)
	{
		if (roles.isEmpty())
		{
			return Collections.emptyList();
		}
		List<String> names = new ArrayList<>();
		for (Role role : roles)
		{
			names.add(role.name());
		}
		return names;
	}
}
