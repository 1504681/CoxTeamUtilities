package com.coxteamutilities;

import java.util.Arrays;
import java.util.Collections;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import org.junit.Test;

public class DropPlanTest
{
	@Test
	public void startsFromTheWiki()
	{
		DropPlan plan = new DropPlan();
		assertEquals(2, plan.count(CmRoom.TEKTON, Potion.OVERLOAD));
		assertEquals(0, plan.count(CmRoom.TEKTON, Potion.XERICS_AID));
		assertEquals(1, plan.count(CmRoom.VANGUARDS, Potion.OVERLOAD));
		assertEquals(4, plan.count(CmRoom.VANGUARDS, Potion.XERICS_AID));
		assertEquals(2, plan.count(CmRoom.VASA, Potion.TWISTED));
		assertEquals(2, plan.count(CmRoom.MUTTADILE, Potion.PRAYER_ENHANCE));
		assertEquals(0, plan.count(CmRoom.SHAMANS, Potion.OVERLOAD));
		assertTrue(plan.encode().isEmpty());
		assertFalse(DropPlan.hasDrops(CmRoom.OLM));
	}

	@Test
	public void editsAreClampedAndCounted()
	{
		DropPlan plan = new DropPlan();
		assertTrue(plan.set(CmRoom.VANGUARDS, Potion.OVERLOAD, 3));
		assertFalse(plan.set(CmRoom.VANGUARDS, Potion.OVERLOAD, 3));
		assertTrue(plan.isEdited(CmRoom.VANGUARDS, Potion.OVERLOAD));
		assertEquals(3, plan.count(CmRoom.VANGUARDS, Potion.OVERLOAD));
		plan.set(CmRoom.VANGUARDS, Potion.OVERLOAD, 500);
		assertEquals(DropPlan.MAX_COUNT, plan.count(CmRoom.VANGUARDS, Potion.OVERLOAD));
		assertFalse(plan.set(CmRoom.TEKTON, Potion.OVERLOAD, DropPlan.DEFAULT));
	}

	@Test
	public void editsReachAnotherClient()
	{
		DropPlan mine = new DropPlan();
		DropPlan theirs = new DropPlan();
		mine.set(CmRoom.VANGUARDS, Potion.OVERLOAD, 2);
		mine.set(CmRoom.TEKTON, Potion.OVERLOAD, 3);
		assertTrue(theirs.merge(mine.encode()));
		assertFalse(theirs.merge(mine.encode()));
		assertEquals(2, theirs.count(CmRoom.VANGUARDS, Potion.OVERLOAD));
		assertEquals(3, theirs.count(CmRoom.TEKTON, Potion.OVERLOAD));
	}

	@Test
	public void laterEditWinsWhateverTheOrder()
	{
		DropPlan a = new DropPlan();
		DropPlan b = new DropPlan();
		a.set(CmRoom.VANGUARDS, Potion.OVERLOAD, 2);
		b.merge(a.encode());
		b.set(CmRoom.VANGUARDS, Potion.OVERLOAD, 3);

		DropPlan late = new DropPlan();
		late.merge(b.encode());
		assertFalse(late.merge(a.encode()));
		assertEquals(3, late.count(CmRoom.VANGUARDS, Potion.OVERLOAD));

		a.merge(b.encode());
		assertEquals(3, a.count(CmRoom.VANGUARDS, Potion.OVERLOAD));
	}

	@Test
	public void editsAtTheSameTimeSettleTheSameWayEverywhere()
	{
		DropPlan a = new DropPlan();
		DropPlan b = new DropPlan();
		a.set(CmRoom.TEKTON, Potion.OVERLOAD, 1);
		b.set(CmRoom.TEKTON, Potion.OVERLOAD, 3);
		a.merge(b.encode());
		b.merge(a.encode());
		assertEquals(a.count(CmRoom.TEKTON, Potion.OVERLOAD), b.count(CmRoom.TEKTON, Potion.OVERLOAD));
	}

	@Test
	public void resetTravelsToo()
	{
		DropPlan a = new DropPlan();
		DropPlan b = new DropPlan();
		a.set(CmRoom.VANGUARDS, Potion.OVERLOAD, 3);
		a.set(CmRoom.TEKTON, Potion.OVERLOAD, 4);
		b.merge(a.encode());

		assertTrue(a.reset(true));
		assertEquals(1, a.count(CmRoom.VANGUARDS, Potion.OVERLOAD));
		assertEquals(4, a.count(CmRoom.TEKTON, Potion.OVERLOAD));
		assertFalse(a.reset(true));

		b.merge(a.encode());
		assertEquals(1, b.count(CmRoom.VANGUARDS, Potion.OVERLOAD));

		assertTrue(a.reset(false));
		assertEquals(2, a.count(CmRoom.TEKTON, Potion.OVERLOAD));
		assertFalse(a.isEdited(CmRoom.TEKTON, Potion.OVERLOAD));
	}

	@Test
	public void survivesTheConfigRoundTrip()
	{
		DropPlan plan = new DropPlan();
		plan.set(CmRoom.MUTTADILE, Potion.XERICS_AID, 2);
		DropPlan loaded = new DropPlan();
		loaded.merge(Arrays.asList(String.join(",", plan.encode()).split(",")));
		assertEquals(2, loaded.count(CmRoom.MUTTADILE, Potion.XERICS_AID));
	}

	@Test
	public void junkFromThePartyIsIgnored()
	{
		DropPlan plan = new DropPlan();
		assertFalse(plan.merge(null));
		assertFalse(plan.merge(Arrays.asList(null, "", "TEKTON:OVERLOAD", "TEKTON:OVERLOAD:x:1", "NOWHERE:OVERLOAD:1:1",
			"TEKTON:OVERLOAD:999:1", "TEKTON:OVERLOAD:-7:1", "TEKTON:OVERLOAD:1:0", "TEKTON:OVERLOAD:1:1:1")));
		assertEquals(2, plan.count(CmRoom.TEKTON, Potion.OVERLOAD));
		assertEquals(Collections.emptyList(), plan.encode());
	}

	@Test
	public void slotsParseBackAndRejectJunk()
	{
		Slot slot = new Slot(CmRoom.VASA, Potion.TWISTED, 1);
		assertEquals("VASA:TWISTED:1", slot.key());
		assertEquals(slot, Slot.parse(slot.key()));
		assertNull(Slot.parse("VASA:TWISTED"));
		assertNull(Slot.parse("VASA:TWISTED:-1"));
		assertNull(Slot.parse("VASA:TWISTED:99"));
		assertNull(Slot.parse("VASA:BEER:0"));
		assertNull(Slot.parse(null));
	}
}
