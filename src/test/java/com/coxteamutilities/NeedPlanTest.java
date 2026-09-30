package com.coxteamutilities;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import org.junit.Test;

public class NeedPlanTest
{
	@Test
	public void defaultsAreEverythingAtOlm()
	{
		NeedPlan plan = NeedPlan.defaults();
		assertEquals(4, plan.get(CmRoom.OLM, Potion.OVERLOAD));
		assertEquals(12, plan.total(Potion.XERICS_AID));
		assertEquals(0, plan.get(CmRoom.TEKTON, Potion.OVERLOAD));
		assertEquals(plan.encode(), NeedPlan.parse("").encode());
		assertEquals(plan.encode(), NeedPlan.parse(null).encode());
	}

	@Test
	public void addsUpFromARoomOn()
	{
		NeedPlan plan = new NeedPlan();
		plan.set(CmRoom.TEKTON, Potion.OVERLOAD, 1);
		plan.set(CmRoom.VANGUARDS, Potion.OVERLOAD, 1);
		plan.set(CmRoom.MUTTADILE, Potion.OVERLOAD, 1);
		plan.set(CmRoom.OLM, Potion.OVERLOAD, 4);
		assertEquals(7, plan.total(Potion.OVERLOAD));
		assertEquals(6, plan.fromRoomOn(CmRoom.CRABS, Potion.OVERLOAD));
		assertEquals(5, plan.fromRoomOn(CmRoom.GUARDIANS, Potion.OVERLOAD));
		assertEquals(4, plan.fromRoomOn(CmRoom.OLM, Potion.OVERLOAD));
		assertEquals(0, plan.total(Potion.XERICS_AID));
	}

	@Test
	public void emptyIsNotTheSameAsNeverSet()
	{
		NeedPlan plan = NeedPlan.defaults();
		for (Potion potion : Potion.values())
		{
			plan.set(CmRoom.OLM, potion, 0);
		}
		assertEquals("none", plan.encode());
		assertEquals(0, NeedPlan.parse(plan.encode()).total(Potion.OVERLOAD));
	}

	@Test
	public void survivesTheConfigRoundTripAndJunk()
	{
		NeedPlan plan = new NeedPlan();
		plan.set(CmRoom.VESPULA, Potion.REVITALISATION, 3);
		plan.set(CmRoom.OLM, Potion.OVERLOAD, 500);
		NeedPlan loaded = NeedPlan.parse(plan.encode() + ",OLM:BEER:1,NOWHERE:OVERLOAD:1,OLM:OVERLOAD,x");
		assertEquals(3, loaded.get(CmRoom.VESPULA, Potion.REVITALISATION));
		assertEquals(NeedPlan.MAX_DOSES, loaded.get(CmRoom.OLM, Potion.OVERLOAD));
		assertEquals(plan.encode(), loaded.encode());
	}

	@Test
	public void reportsWhetherAnythingChanged()
	{
		NeedPlan plan = new NeedPlan();
		assertFalse(plan.set(CmRoom.OLM, Potion.OVERLOAD, 0));
		assertTrue(plan.set(CmRoom.OLM, Potion.OVERLOAD, 2));
		assertFalse(plan.set(CmRoom.OLM, Potion.OVERLOAD, 2));
		assertFalse(plan.set(CmRoom.OLM, Potion.OVERLOAD, -5) && plan.get(CmRoom.OLM, Potion.OVERLOAD) != 0);
	}
}
