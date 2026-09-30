package com.coxteamutilities;

import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import javax.imageio.ImageIO;
import javax.swing.ImageIcon;
import javax.swing.SwingUtilities;
import net.runelite.client.ui.PluginPanel;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import org.junit.Test;

/** Draws the sidebar off screen with a busy raid in it. The picture lands in build/panel.png. */
public class CoxTeamPanelTest
{
	private static final CoxTeamPanel.Actions NO_ACTIONS = new CoxTeamPanel.Actions()
	{
		@Override
		public void setRole(Role role, boolean selected)
		{
		}

		@Override
		public void setNeed(Potion potion, int doses)
		{
		}

		@Override
		public void setNeedsTab(boolean solo)
		{
		}

		@Override
		public void finishRaid()
		{
		}

		@Override
		public void renameChest(String key, String name)
		{
		}

		@Override
		public void setChestOrdered(String key, boolean ordered)
		{
		}

		@Override
		public void setChestLines(String key, boolean deposit, String text)
		{
		}

		@Override
		public void useLastVisit(String key)
		{
		}

		@Override
		public void setRecordVisits(boolean record)
		{
		}

		@Override
		public void deleteChest(String key)
		{
		}

		@Override
		public void setDropCount(CmRoom room, Potion potion, int count)
		{
		}

		@Override
		public void toggleClaim(Slot slot)
		{
		}

		@Override
		public void setClaimDoses(Slot slot, int doses)
		{
		}

		@Override
		public boolean setSipRoom(Slot slot, CmRoom room, boolean sipThere)
		{
			return true;
		}

		@Override
		public void resetDropCounts()
		{
		}
	};

	private static PanelState busyRaid()
	{
		PanelState state = new PanelState();
		state.inParty = true;
		state.inventory = Supplies.of(new int[]{4, 8, 4, 0});
		state.privateStorage = Supplies.of(new int[]{0, 4, 0, 4});
		state.claimed = Supplies.of(new int[]{4, 0, 0, 0});
		state.solo = true;
		state.units = NeedUnits.POTIONS;
		Needs needs = Needs.soloDefaults();
		for (Potion potion : Potion.values())
		{
			state.need.put(potion, state.applies(potion) ? needs.get(potion) : 0);
		}
		ChestPlan ice = state.chests.getOrCreate("RAIDS_ICE_DEMON#1", "Ice Demon");
		ice.getDeposit().add("Elder maul");
		ChestPlan farm = state.chests.getOrCreate("RAIDS_FARMING#1", "Farming 1");
		farm.getDeposit().add("everything");
		farm.getWithdraw().addAll(Arrays.asList("Xeric's aid x2", "Stinkhorn mushroom x3", "Noxifer"));
		farm.setOrdered(true);
		state.currentChest = "RAIDS_FARMING#1";
		ChestPlan.Visit visit = new ChestPlan.Visit();
		visit.moved("Elder maul", 1);
		visit.moved("Xeric's aid(4)", -1);
		visit.moved("Xeric's aid(4)", -1);
		state.visits.put("RAIDS_FARMING#1", visit);
		state.inventoryNames = Arrays.asList("Xeric's aid(4)", "Xeric's aid(3)", "Stinkhorn mushroom");
		state.roles.addAll(EnumSet.of(Role.MUTTADILE_ZGS, Role.MUTTADILE_ENTANGLE, Role.TIGHTROPE_LURER));
		state.missing.put(Role.MUTTADILE_ENTANGLE, Arrays.asList("Standard spellbook", "Nature runes"));

		PanelState.Member me = new PanelState.Member();
		me.name = "brizzy";
		me.self = true;
		me.status = new MemberStatus(EnumSet.copyOf(state.roles),
			Arrays.asList("Standard spellbook (Muttadile Entangler)", "Nature runes (Muttadile Entangler)"),
			new LinkedHashMap<>(), Supplies.of(new int[]{4, 12, 4, 4}), null, null, false);
		PanelState.Member bob = new PanelState.Member();
		bob.name = "Zezima the 2nd";
		bob.status = new MemberStatus(EnumSet.of(Role.TIGHTROPE_LURER, Role.TIGHTROPE_TELEGRAB),
			Collections.emptyList(), new LinkedHashMap<>(), Supplies.of(new int[]{4, 8, 4, 4}),
			Supplies.of(new int[]{4, 0, 4, 4}), Supplies.of(new int[]{4, 16, 0, 0}), true);
		bob.iron = true;
		PanelState.Member quiet = new PanelState.Member();
		quiet.name = "No Plugin";
		state.team.addAll(Arrays.asList(me, bob, quiet));

		DropPlan plan = new DropPlan();
		plan.set(CmRoom.VANGUARDS, Potion.OVERLOAD, 3);
		for (CmRoom room : CmRoom.values())
		{
			if (!DropPlan.hasDrops(room))
			{
				continue;
			}
			PanelState.RoomDrops drops = new PanelState.RoomDrops();
			drops.room = room;
			for (Potion potion : Potion.values())
			{
				int count = plan.count(room, potion);
				if (count == 0)
				{
					continue;
				}
				PanelState.PotionDrop drop = new PanelState.PotionDrop();
				drop.potion = potion;
				drop.count = count;
				drop.edited = plan.isEdited(room, potion);
				for (int i = 0; i < count; i++)
				{
					PanelState.SlotView slot = new PanelState.SlotView();
					slot.slot = new Slot(room, potion, i);
					if (potion == Potion.OVERLOAD && i == 0 && room == CmRoom.TEKTON)
					{
						// shared: the ironman sips first and drops it, you pick it up
						slot.add("You", true, new Claim(slot.slot, 3, 1, Arrays.asList(CmRoom.ICE_DEMON, CmRoom.VANGUARDS)));
						slot.add("Zezima the 2nd", false, true,
							new Claim(slot.slot, 1, 2, Collections.singletonList(CmRoom.TEKTON)));
					}
					else if (potion == Potion.OVERLOAD && i == 0)
					{
						slot.add("You", true, Claim.whole(slot.slot, 1));
					}
					else if (potion == Potion.OVERLOAD && i == 1)
					{
						// half claimed, the rest is up for grabs
						slot.add("Zezima the 2nd", false, new Claim(slot.slot, 2, 1, null));
					}
					else if (potion == Potion.XERICS_AID && i == 0)
					{
						// two people clicked at once
						slot.add("You", true, Claim.whole(slot.slot, 1));
						slot.add("Zezima the 2nd", false, Claim.whole(slot.slot, 1));
					}
					drop.slots.add(slot);
				}
				drops.potions.add(drop);
			}
			state.drops.add(drops);
		}
		return state;
	}

	private static void layOut(Component component)
	{
		component.doLayout();
		if (component instanceof Container)
		{
			for (Component child : ((Container) component).getComponents())
			{
				layOut(child);
			}
		}
	}

	private static int deepestRightEdge(Component component, int offset)
	{
		int edge = component.isVisible() ? offset + component.getX() + component.getWidth() : 0;
		if (component instanceof Container && component.isVisible())
		{
			for (Component child : ((Container) component).getComponents())
			{
				edge = Math.max(edge, deepestRightEdge(child, offset + component.getX()));
			}
		}
		return edge;
	}

	@Test
	public void drawsABusyRaidWithinTheSidebarWidth() throws Exception
	{
		draw(false, "build/panel.png");
	}

	@Test
	public void drawsTheClaimsWithinTheSidebarWidth() throws Exception
	{
		draw(true, "build/panel-claims.png");
	}

	private static void draw(boolean claimsOpen, String file) throws Exception
	{
		BufferedImage[] drawn = new BufferedImage[1];
		int[] rightEdge = new int[1];
		SwingUtilities.invokeAndWait(() ->
		{
			CoxTeamPanel panel = new CoxTeamPanel(NO_ACTIONS, (label, itemId) ->
			{
				BufferedImage square = new BufferedImage(36, 32, BufferedImage.TYPE_INT_ARGB);
				Graphics2D g = square.createGraphics();
				g.setColor(new Color(itemId * 9973 % 0xffffff));
				g.fillRoundRect(10, 4, 16, 24, 6, 6);
				g.dispose();
				label.setIcon(new ImageIcon(square));
			});
			panel.update(busyRaid());
			panel.showClaims(claimsOpen);

			// twice, html labels only know their height once they have a width
			for (int pass = 0; pass < 2; pass++)
			{
				Dimension size = panel.getPreferredSize();
				panel.setSize(PluginPanel.PANEL_WIDTH, size.height);
				layOut(panel);
			}

			BufferedImage image = new BufferedImage(panel.getWidth(), panel.getHeight(), BufferedImage.TYPE_INT_RGB);
			Graphics2D g = image.createGraphics();
			panel.paint(g);
			g.dispose();
			drawn[0] = image;
			rightEdge[0] = deepestRightEdge(panel, -panel.getX());
		});

		File out = new File(file);
		out.getParentFile().mkdirs();
		ImageIO.write(drawn[0], "png", out);

		assertEquals(PluginPanel.PANEL_WIDTH, drawn[0].getWidth());
		assertTrue("taller than it should be: " + drawn[0].getHeight(), drawn[0].getHeight() < 2500);
		assertTrue("something sticks out to x=" + rightEdge[0], rightEdge[0] <= PluginPanel.PANEL_WIDTH);
	}
}
