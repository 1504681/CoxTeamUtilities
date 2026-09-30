package com.coxteamutilities;

import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import org.junit.Test;

public class RoleTest
{
	private static final int ARCEUUS = 3;

	@Test
	public void zgsNeedsTheSword()
	{
		assertEquals(Collections.singletonList("Zamorak godsword"), Role.MUTTADILE_ZGS.missing(new Loadout()));
		assertTrue(Role.MUTTADILE_ZGS.missing(new Loadout().add(ItemIds.ZAMORAK_GODSWORD, 1)).isEmpty());
		assertTrue(Role.MUTTADILE_ZGS.missing(new Loadout().add(ItemIds.ZAMORAK_GODSWORD_OR, 1)).isEmpty());
	}

	@Test
	public void anyChinchompaOrVenatorBowLures()
	{
		for (int id : new int[]{10033, 10034, 11959, 27610, 30434})
		{
			assertTrue(Role.TIGHTROPE_LURER.missing(new Loadout().add(id, 1)).isEmpty());
		}
		assertFalse(Role.TIGHTROPE_LURER.missing(new Loadout()).isEmpty());
	}

	@Test
	public void unchargedVenatorBowDoesNotCount()
	{
		assertFalse(Role.TIGHTROPE_LURER.missing(new Loadout().add(27612, 1)).isEmpty());
	}

	@Test
	public void crossingNeedsNothing()
	{
		assertTrue(Role.TIGHTROPE_CROSSER.missing(new Loadout()).isEmpty());
	}

	@Test
	public void entangleNeedsFourNaturesOnStandard()
	{
		assertEquals(Collections.singletonList("Nature runes"),
			Role.MUTTADILE_ENTANGLE.missing(new Loadout().add(ItemIds.NATURE_RUNE, 3)));
		assertTrue(Role.MUTTADILE_ENTANGLE.missing(new Loadout().add(ItemIds.NATURE_RUNE, 4)).isEmpty());
		assertEquals(Collections.singletonList("Standard spellbook"),
			Role.MUTTADILE_ENTANGLE.missing(new Loadout().add(ItemIds.NATURE_RUNE, 400).spellbook(ARCEUUS)));
	}

	@Test
	public void telegrabNeedsALaw()
	{
		assertEquals(Arrays.asList("Standard spellbook", "Law rune"),
			Role.TIGHTROPE_TELEGRAB.missing(new Loadout().spellbook(ARCEUUS)));
		assertTrue(Role.TIGHTROPE_TELEGRAB.missing(new Loadout().add(ItemIds.LAW_RUNE, 1)).isEmpty());
	}

	@Test
	public void onlySelectedRolesAreChecked()
	{
		Map<Role, List<String>> missing = CoxTeamUtilitiesPlugin.missingFor(
			EnumSet.of(Role.MUTTADILE_ZGS, Role.TIGHTROPE_LURER), new Loadout().add(ItemIds.BLACK_CHINCHOMPA, 1));
		assertEquals(Collections.singleton(Role.MUTTADILE_ZGS), missing.keySet());
	}

	@Test
	public void unknownRoleNamesAreSkipped()
	{
		assertEquals(EnumSet.of(Role.MUTTADILE_ZGS),
			Role.parse(Arrays.asList("MUTTADILE_ZGS", "OLM_SOMETHING_NEW", "", null)));
		assertEquals(Arrays.asList("TIGHTROPE_LURER", "MUTTADILE_ZGS"),
			Role.names(EnumSet.of(Role.MUTTADILE_ZGS, Role.TIGHTROPE_LURER)));
	}

	@Test
	public void rolesSitInRoomsOfTheFixedLayout()
	{
		assertEquals(3, Role.forRoom(CmRoom.TIGHTROPE).size());
		assertEquals(2, Role.forRoom(CmRoom.MUTTADILE).size());
		assertTrue(CmRoom.TIGHTROPE.ordinal() < CmRoom.MUTTADILE.ordinal());
		assertEquals("Muttadile ZGS", Role.MUTTADILE_ZGS.getFullName());
	}
}
