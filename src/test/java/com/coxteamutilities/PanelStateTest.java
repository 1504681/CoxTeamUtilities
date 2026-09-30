package com.coxteamutilities;

import java.util.Arrays;
import java.util.Collections;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;
import org.junit.Test;

public class PanelStateTest
{
	@Test
	public void partyMessagesAreCapped()
	{
		String[] missing = new String[200];
		String[] claims = new String[200];
		Arrays.fill(missing, String.join("", Collections.nCopies(500, "x")));
		for (int i = 0; i < claims.length; i++)
		{
			claims[i] = "TEKTON:OVERLOAD:" + i + ":2:1:TEKTON+VASA";
		}
		MemberStatus status = MemberStatus.from(new CoxStatusMessage(
			Arrays.asList("MUTTADILE_ZGS", "junk"), Arrays.asList(missing), Arrays.asList(claims), true));
		assertTrue(status.isIron());
		assertEquals(Collections.singleton(Role.MUTTADILE_ZGS), status.getRoles());
		assertEquals(MemberStatus.MAX_MISSING, status.getMissing().size());
		assertEquals(MemberStatus.MAX_TEXT, status.getMissing().get(0).length());
		assertEquals(DropPlan.MAX_COUNT, status.getClaims().size());
	}

	@Test
	public void emptyPartyMessageIsFine()
	{
		MemberStatus status = MemberStatus.from(new CoxStatusMessage());
		assertTrue(status.getRoles().isEmpty());
		assertTrue(status.getMissing().isEmpty());
		assertTrue(status.getClaims().isEmpty());
	}

	@Test
	public void signaturesNoticeClaims()
	{
		PanelState before = dropsWithOwner(null);
		PanelState after = dropsWithOwner("Bob");
		assertEquals(before.dropsSignature(), dropsWithOwner(null).dropsSignature());
		assertNotEquals(before.dropsSignature(), after.dropsSignature());
	}

	@Test
	public void signaturesNoticeDosesAndRooms()
	{
		PanelState whole = dropsWithOwner("Bob");
		PanelState half = dropsWithOwner(null);
		PanelState.SlotView slot = half.drops.get(0).potions.get(0).slots.get(0);
		slot.add("Bob", false, new Claim(slot.slot, 2, 1, null));
		PanelState rooms = dropsWithOwner(null);
		slot = rooms.drops.get(0).potions.get(0).slots.get(0);
		slot.add("Bob", false, new Claim(slot.slot, 2, 1, Collections.singleton(CmRoom.VASA)));

		assertNotEquals(whole.dropsSignature(), half.dropsSignature());
		assertNotEquals(half.dropsSignature(), rooms.dropsSignature());
	}

	private static PanelState dropsWithOwner(String owner)
	{
		PanelState state = new PanelState();
		PanelState.RoomDrops room = new PanelState.RoomDrops();
		room.room = CmRoom.TEKTON;
		PanelState.PotionDrop drop = new PanelState.PotionDrop();
		drop.potion = Potion.OVERLOAD;
		drop.count = 1;
		PanelState.SlotView slot = new PanelState.SlotView();
		slot.slot = new Slot(CmRoom.TEKTON, Potion.OVERLOAD, 0);
		if (owner != null)
		{
			slot.add(owner, false, Claim.whole(slot.slot, 1));
		}
		drop.slots.add(slot);
		room.potions.add(drop);
		state.drops.add(room);
		return state;
	}
}
