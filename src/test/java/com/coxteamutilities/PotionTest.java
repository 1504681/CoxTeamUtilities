package com.coxteamutilities;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import org.junit.Test;

public class PotionTest
{
	@Test
	public void raidPotionsHaveTwelveVariants()
	{
		assertEquals(1, Potion.OVERLOAD.doses(20985));
		assertEquals(4, Potion.OVERLOAD.doses(20988));
		assertEquals(4, Potion.OVERLOAD.doses(20996));
		assertEquals(20996, Potion.OVERLOAD.getIconItemId());
		assertEquals(0, Potion.OVERLOAD.doses(20997));
		assertEquals(Potion.XERICS_AID, Potion.of(20973));
		assertNull(Potion.of(4151));
	}

	@Test
	public void staminaOnlyCountsSolo()
	{
		assertEquals(4, Potion.STAMINA.doses(12625));
		assertEquals(1, Potion.STAMINA.doses(12631));
		assertEquals(0, Potion.STAMINA.doses(12626));
		assertEquals(12625, Potion.STAMINA.getIconItemId());
		assertTrue(Potion.STAMINA.isSoloOnly());
		assertFalse(Potion.STAMINA.isClaimable());
	}
}
