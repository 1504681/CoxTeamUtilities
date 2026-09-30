package com.coxteamutilities;

import java.util.Arrays;
import java.util.Collections;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import org.junit.Test;

public class ClaimTest
{
	private static final Slot TEKTON_OVERLOAD = new Slot(CmRoom.TEKTON, Potion.OVERLOAD, 0);
	private static final Slot VANGUARDS_OVERLOAD = new Slot(CmRoom.VANGUARDS, Potion.OVERLOAD, 0);

	private static PanelState.SlotView shared(Claim mine, Claim theirs)
	{
		PanelState.SlotView slot = new PanelState.SlotView();
		slot.slot = TEKTON_OVERLOAD;
		if (theirs != null)
		{
			slot.add("Bob", false, theirs);
		}
		if (mine != null)
		{
			slot.add("You", true, mine);
		}
		return slot;
	}

	@Test
	public void goesOverThePartyAndBack()
	{
		Claim claim = new Claim(TEKTON_OVERLOAD, 2, 1, Arrays.asList(CmRoom.VANGUARDS, CmRoom.TEKTON));
		assertEquals("TEKTON:OVERLOAD:0:2:1:TEKTON+VANGUARDS", claim.encode());
		assertEquals(claim, Claim.parse(claim.encode()));

		Claim whole = Claim.whole(TEKTON_OVERLOAD, 3);
		assertEquals("TEKTON:OVERLOAD:0:4:3", whole.encode());
		assertEquals(whole, Claim.parse(whole.encode()));
	}

	@Test
	public void junkFromThePartyIsIgnored()
	{
		for (String junk : new String[]{null, "", "TEKTON:OVERLOAD:0", "TEKTON:OVERLOAD:0:2", "TEKTON:OVERLOAD:0:0:1",
			"TEKTON:OVERLOAD:0:5:1", "TEKTON:OVERLOAD:0:2:0", "TEKTON:OVERLOAD:0:2:500", "TEKTON:OVERLOAD:0:x:1",
			"TEKTON:OVERLOAD:0:2:1:NOWHERE", "TEKTON:TWISTED:0:2:1", "TEKTON:OVERLOAD:0:2:1:TEKTON:VASA"})
		{
			assertNull(junk, Claim.parse(junk));
		}
	}

	@Test
	public void noMoreRoomsThanDosesAndNoneBeforeTheDrop()
	{
		Claim claim = new Claim(VANGUARDS_OVERLOAD, 2, 1,
			Arrays.asList(CmRoom.TEKTON, CmRoom.OLM, CmRoom.VESPULA, CmRoom.VANGUARDS));
		assertEquals(Arrays.asList(CmRoom.VANGUARDS, CmRoom.VESPULA), claim.getSipRooms());
		assertEquals(Collections.singletonList(CmRoom.VANGUARDS), claim.withDoses(1).getSipRooms());
		assertEquals(Collections.singletonList(CmRoom.VESPULA), claim.withRoom(CmRoom.VANGUARDS, false).getSipRooms());
		assertEquals(4, claim.withDoses(9).getDoses());
		assertEquals(1, claim.withDoses(0).getDoses());
	}

	@Test
	public void theOneWhoSipsFirstHoldsItFirst()
	{
		Claim mine = new Claim(TEKTON_OVERLOAD, 2, 1, Arrays.asList(CmRoom.VESPULA, CmRoom.VASA));
		Claim theirs = new Claim(TEKTON_OVERLOAD, 2, 2, Arrays.asList(CmRoom.TEKTON, CmRoom.VANGUARDS));
		PanelState.SlotView slot = shared(mine, theirs);
		assertEquals("Bob", slot.holders.get(0).name);
		assertEquals(Arrays.asList(
			"Bob: 2 doses (Tekton, Vanguards), then drop for You",
			"You: pick up, 2 doses (Vespula, Vasa)"), slot.planLines());
		assertTrue(slot.planned());
		assertFalse(slot.overclaimed());
		assertEquals(0, slot.freeDoses());
		assertEquals(2, slot.availableToMe());
	}

	@Test
	public void withoutRoomsTheFirstToClaimHoldsItFirst()
	{
		PanelState.SlotView slot = shared(new Claim(TEKTON_OVERLOAD, 1, 2, null), new Claim(TEKTON_OVERLOAD, 2, 1, null));
		assertEquals(Arrays.asList(
			"Bob: 2 doses, then drop for You",
			"You: pick up, 1 dose, then drop, 1 unclaimed"), slot.planLines());
		assertEquals(1, slot.freeDoses());
		assertEquals(2, slot.availableToMe());
	}

	@Test
	public void aPotionSomeoneDropsCanBePickedUp()
	{
		PanelState.SlotView slot = shared(null, new Claim(TEKTON_OVERLOAD, 3, 1, null));
		assertNull(slot.me());
		assertEquals(1, slot.freeDoses());
		assertEquals(1, slot.availableToMe());
		assertTrue(slot.planned());
	}

	@Test
	public void oneOwnerOfAWholePotionNeedsNoPlan()
	{
		assertFalse(shared(Claim.whole(TEKTON_OVERLOAD, 1), null).planned());
		assertFalse(shared(null, null).planned());
		assertTrue(shared(new Claim(TEKTON_OVERLOAD, 4, 1, Collections.singleton(CmRoom.OLM)), null).planned());
	}

	@Test
	public void twoWholeClaimsAreAConflict()
	{
		PanelState.SlotView slot = shared(Claim.whole(TEKTON_OVERLOAD, 1), Claim.whole(TEKTON_OVERLOAD, 1));
		assertTrue(slot.overclaimed());
		assertEquals(Collections.singletonList("Too many doses claimed: Bob 4, You 4"), slot.planLines());
		assertEquals(0, slot.freeDoses());
		assertEquals(0, slot.availableToMe());
	}

	@Test
	public void clickingClaimsWhatIsLeftAndClickingAgainDropsIt()
	{
		ClaimBook book = new ClaimBook();
		assertTrue(book.toggle(TEKTON_OVERLOAD, 4, 1));
		assertEquals(Collections.singletonList(Claim.whole(TEKTON_OVERLOAD, 1)), book.all());
		assertTrue(book.toggle(TEKTON_OVERLOAD, 4, 1));
		assertTrue(book.all().isEmpty());

		// someone sips twice and drops it, you take the rest and stand behind them
		assertTrue(book.toggle(TEKTON_OVERLOAD, 2, 2));
		assertEquals("TEKTON:OVERLOAD:0:2:2", book.all().get(0).encode());
		assertEquals(2, book.all().get(0).getDoses());

		assertFalse(new ClaimBook().toggle(TEKTON_OVERLOAD, 0, 2));
	}

	@Test
	public void dosesStayWithinWhatTheOthersLeft()
	{
		ClaimBook book = new ClaimBook();
		assertFalse(book.setDoses(TEKTON_OVERLOAD, 3, 2, 1));
		assertFalse(book.setDoses(TEKTON_OVERLOAD, 0, 2, 1));
		assertTrue(book.setDoses(TEKTON_OVERLOAD, 2, 2, 1));
		assertTrue(book.setDoses(TEKTON_OVERLOAD, 1, 2, 5));
		// changing the doses keeps your place in line
		assertEquals("TEKTON:OVERLOAD:0:1:1", book.all().get(0).encode());
	}

	@Test
	public void pickingRoomsTakesDosesAsNeeded()
	{
		ClaimBook book = new ClaimBook();
		assertTrue(book.setSipRoom(TEKTON_OVERLOAD, CmRoom.TEKTON, true, 2, 1));
		assertEquals("TEKTON:OVERLOAD:0:1:1:TEKTON", book.all().get(0).encode());
		assertTrue(book.setSipRoom(TEKTON_OVERLOAD, CmRoom.VANGUARDS, true, 2, 1));
		assertEquals("TEKTON:OVERLOAD:0:2:1:TEKTON+VANGUARDS", book.all().get(0).encode());
		// the other two doses are someone else's
		assertFalse(book.setSipRoom(TEKTON_OVERLOAD, CmRoom.VESPULA, true, 2, 1));
		assertEquals(2, book.all().get(0).getDoses());

		assertTrue(book.setSipRoom(TEKTON_OVERLOAD, CmRoom.TEKTON, false, 2, 1));
		assertEquals("TEKTON:OVERLOAD:0:2:1:VANGUARDS", book.all().get(0).encode());
		// a free dose gets the room without taking another
		assertTrue(book.setSipRoom(TEKTON_OVERLOAD, CmRoom.OLM, true, 2, 1));
		assertEquals("TEKTON:OVERLOAD:0:2:1:VANGUARDS+OLM", book.all().get(0).encode());
	}

	@Test
	public void noSippingBeforeItDropsOrWithoutAClaim()
	{
		ClaimBook book = new ClaimBook();
		assertFalse(book.setSipRoom(VANGUARDS_OVERLOAD, CmRoom.TEKTON, true, 4, 1));
		assertFalse(book.setSipRoom(VANGUARDS_OVERLOAD, CmRoom.OLM, false, 4, 1));
		assertFalse(book.setSipRoom(VANGUARDS_OVERLOAD, CmRoom.OLM, true, 0, 1));
		assertTrue(book.all().isEmpty());
	}

	@Test
	public void claimsPastTheDropCountGo()
	{
		ClaimBook book = new ClaimBook();
		DropPlan plan = new DropPlan();
		plan.set(CmRoom.VANGUARDS, Potion.OVERLOAD, 3);
		Slot third = new Slot(CmRoom.VANGUARDS, Potion.OVERLOAD, 2);
		book.toggle(third, 4, 1);
		book.toggle(VANGUARDS_OVERLOAD, 4, 1);
		plan.set(CmRoom.VANGUARDS, Potion.OVERLOAD, 2);
		book.dropPast(plan);
		assertEquals(Collections.singletonList(Claim.whole(VANGUARDS_OVERLOAD, 1)), book.all());
	}

	@Test
	public void anIronmanHoldsItFirstWhateverTheRooms()
	{
		PanelState.SlotView slot = new PanelState.SlotView();
		slot.slot = TEKTON_OVERLOAD;
		slot.add("You", true, new Claim(TEKTON_OVERLOAD, 3, 1, Collections.singletonList(CmRoom.TEKTON)));
		slot.add("Iron Bob", false, true, new Claim(TEKTON_OVERLOAD, 1, 2, Collections.singletonList(CmRoom.VANGUARDS)));
		assertEquals(Arrays.asList(
			"Iron Bob (iron): 1 dose (Vanguards), then drop for You",
			"You: pick up, 3 doses (Tekton)"), slot.planLines());
		assertFalse(slot.overclaimed());
	}

	@Test
	public void anIronmanCanCutInFrontOfEveryoneElse()
	{
		PanelState.SlotView slot = new PanelState.SlotView();
		slot.slot = TEKTON_OVERLOAD;
		slot.viewerIron = true;
		slot.add("Bob", false, Claim.whole(TEKTON_OVERLOAD, 1));
		assertEquals(4, slot.availableToMe());
		// one sip, then it goes back to them
		assertEquals(1, slot.takenOnClick());

		PanelState.SlotView free = new PanelState.SlotView();
		free.slot = TEKTON_OVERLOAD;
		free.viewerIron = true;
		assertEquals(4, free.takenOnClick());
	}

	@Test
	public void twoIronmenCannotShareAPotion()
	{
		PanelState.SlotView slot = new PanelState.SlotView();
		slot.slot = TEKTON_OVERLOAD;
		slot.viewerIron = true;
		slot.add("Iron Bob", false, true, new Claim(TEKTON_OVERLOAD, 1, 1, null));
		assertEquals(0, slot.availableToMe());
		assertEquals(0, slot.takenOnClick());
		assertFalse(new ClaimBook().toggle(TEKTON_OVERLOAD, slot.takenOnClick(), slot.nextOrder()));

		slot.add("You", true, true, new Claim(TEKTON_OVERLOAD, 1, 1, null));
		assertTrue(slot.overclaimed());
		assertEquals(Collections.singletonList(
			"Ironmen can't pass a potion to each other: Iron Bob (iron) 1, You (iron) 1"), slot.planLines());
	}

	@Test
	public void everyoneElseTakesWhatTheIronmanLeaves()
	{
		PanelState.SlotView slot = new PanelState.SlotView();
		slot.slot = TEKTON_OVERLOAD;
		slot.add("Iron Bob", false, true, new Claim(TEKTON_OVERLOAD, 1, 1, null));
		assertEquals(3, slot.availableToMe());
		assertEquals(3, slot.takenOnClick());
		assertEquals(2, slot.nextOrder());
		assertEquals(1, slot.dosesOfOtherIronmen());
	}

	@Test
	public void claimsGiveWayToIronmen()
	{
		ClaimBook book = new ClaimBook();
		Slot second = new Slot(CmRoom.TEKTON, Potion.OVERLOAD, 1);
		Slot aid = new Slot(CmRoom.VESPULA, Potion.XERICS_AID, 0);
		book.setDoses(TEKTON_OVERLOAD, 4, 4, 1);
		book.setSipRoom(TEKTON_OVERLOAD, CmRoom.TEKTON, true, 4, 1);
		book.setSipRoom(TEKTON_OVERLOAD, CmRoom.VASA, true, 4, 1);
		book.toggle(second, 4, 1);
		book.toggle(aid, 2, 1);

		java.util.Map<Slot, Integer> iron = new java.util.HashMap<>();
		iron.put(TEKTON_OVERLOAD, 3);
		iron.put(second, 4);
		iron.put(aid, 2);
		assertTrue(book.yieldTo(iron));
		assertEquals(Arrays.asList("TEKTON:OVERLOAD:0:1:1:TEKTON", "VESPULA:XERICS_AID:0:2:1"),
			Arrays.asList(book.all().get(0).encode(), book.all().get(1).encode()));
		assertEquals(2, book.all().size());
		assertFalse(book.yieldTo(iron));
	}
}
