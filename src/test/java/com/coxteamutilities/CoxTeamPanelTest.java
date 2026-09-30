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
import java.util.LinkedHashSet;
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
		public void setDropCount(CmRoom room, Potion potion, int count)
		{
		}

		@Override
		public void toggleClaim(Slot slot)
		{
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
		state.countClaimed = true;
		state.inventory = Supplies.of(new int[]{4, 8, 4, 0});
		state.privateStorage = Supplies.of(new int[]{0, 4, 0, 4});
		state.claimed = Supplies.of(new int[]{4, 0, 0, 0});
		state.need.put(Potion.OVERLOAD, 8);
		state.need.put(Potion.XERICS_AID, 16);
		state.need.put(Potion.REVITALISATION, 8);
		state.need.put(Potion.PRAYER_ENHANCE, 4);

		state.roles.addAll(EnumSet.of(Role.MUTTADILE_ZGS, Role.MUTTADILE_ENTANGLE, Role.TIGHTROPE_CHINS));
		state.missing.put(Role.MUTTADILE_ENTANGLE, Arrays.asList("Standard spellbook", "Nature runes"));

		PanelState.Member me = new PanelState.Member();
		me.name = "brizzy";
		me.self = true;
		me.status = new MemberStatus(EnumSet.copyOf(state.roles),
			Arrays.asList("Standard spellbook (Muttadile Entangler)", "Nature runes (Muttadile Entangler)"),
			new LinkedHashSet<>(), Supplies.of(new int[]{4, 12, 4, 4}));
		PanelState.Member bob = new PanelState.Member();
		bob.name = "Zezima the 2nd";
		bob.status = new MemberStatus(EnumSet.of(Role.TIGHTROPE_VENATOR, Role.TIGHTROPE_TELEGRAB),
			Collections.emptyList(), new LinkedHashSet<>(), Supplies.of(new int[]{8, 8, 8, 8}));
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
					if (potion == Potion.OVERLOAD && i == 0)
					{
						slot.mine = true;
						slot.owners.add("You");
					}
					else if (potion == Potion.OVERLOAD && i == 1)
					{
						slot.owners.add("Zezima the 2nd");
					}
					else if (potion == Potion.XERICS_AID && i == 0)
					{
						slot.mine = true;
						slot.owners.addAll(Arrays.asList("You", "Zezima the 2nd"));
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

		File out = new File("build/panel.png");
		out.getParentFile().mkdirs();
		ImageIO.write(drawn[0], "png", out);

		assertEquals(PluginPanel.PANEL_WIDTH, drawn[0].getWidth());
		assertTrue("taller than it should be: " + drawn[0].getHeight(), drawn[0].getHeight() < 2500);
		assertTrue("something sticks out to x=" + rightEdge[0], rightEdge[0] <= PluginPanel.PANEL_WIDTH);
	}
}
