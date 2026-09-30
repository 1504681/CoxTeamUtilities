package com.coxteamutilities;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import javax.swing.JCheckBox;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingConstants;
import javax.swing.border.EmptyBorder;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.FontManager;
import net.runelite.client.ui.PluginPanel;

class CoxTeamPanel extends PluginPanel
{
	interface Actions
	{
		void setRole(Role role, boolean selected);

		void setNeed(Potion potion, int doses);

		void setDropCount(CmRoom room, Potion potion, int count);

		/** Claims what's left of the potion, or drops your claim if you have one. */
		void toggleClaim(Slot slot);

		void setClaimDoses(Slot slot, int doses);

		/** @return false if there's no dose left for one more room */
		boolean setSipRoom(Slot slot, CmRoom room, boolean sipThere);

		void resetDropCounts();
	}

	interface Icons
	{
		void set(JLabel label, int itemId);
	}

	private static final Color GOOD = new Color(110, 200, 110);
	private static final Color BAD = new Color(255, 96, 96);
	private static final Color WARN = new Color(255, 170, 60);
	private static final Color MUTED = new Color(160, 160, 160);
	private static final Color MINE = new Color(40, 90, 50);
	private static final Color TAKEN = new Color(70, 60, 40);
	private static final Color CONFLICT = new Color(120, 70, 20);
	private static final Color OPEN = new Color(40, 70, 100);

	private static final int SLOTS_PER_ROW = 3;
	private static final int MAX_NAME = 12;

	/** A column of rows that each take the full width. */
	private static final class Stack extends JPanel
	{
		private int rows;

		private Stack(Color background)
		{
			setLayout(new GridBagLayout());
			if (background == null)
			{
				setOpaque(false);
			}
			else
			{
				setBackground(background);
			}
		}

		private void addRow(Component component, int gapAbove)
		{
			GridBagConstraints c = new GridBagConstraints();
			c.gridx = 0;
			c.gridy = rows++;
			c.weightx = 1;
			c.fill = GridBagConstraints.HORIZONTAL;
			c.anchor = GridBagConstraints.NORTHWEST;
			c.insets = new Insets(gapAbove, 0, 0, 0);
			add(component, c);
		}

		private void clear()
		{
			removeAll();
			rows = 0;
		}
	}

	private final Actions actions;
	private final Icons icons;

	private final Map<Potion, JLabel> haveLabels = new EnumMap<>(Potion.class);
	private final Map<Potion, JLabel> detailLabels = new EnumMap<>(Potion.class);
	private final Map<Potion, JLabel> moreLabels = new EnumMap<>(Potion.class);
	private final Map<Potion, JSpinner> needSpinners = new EnumMap<>(Potion.class);
	private final Map<Role, JCheckBox> roleBoxes = new EnumMap<>(Role.class);
	private final Map<Role, JLabel> roleMissing = new EnumMap<>(Role.class);
	private final Stack teamBody = new Stack(ColorScheme.DARKER_GRAY_COLOR);
	private final Stack dropsBody = new Stack(ColorScheme.DARKER_GRAY_COLOR);

	private boolean updating;
	private String teamSignature;
	private String dropsSignature;

	CoxTeamPanel(Actions actions, Icons icons)
	{
		this.actions = actions;
		this.icons = icons;

		setLayout(new BorderLayout());
		setBackground(ColorScheme.DARK_GRAY_COLOR);

		Stack content = new Stack(null);
		content.addRow(header("Supplies", null), 0);
		content.addRow(buildSupplies(), 4);
		content.addRow(header("Roles", null), 12);
		content.addRow(buildRoles(), 4);
		content.addRow(header("Team", null), 12);
		content.addRow(padded(teamBody), 4);
		content.addRow(header("Drops", chip("Reset", ColorScheme.DARKER_GRAY_COLOR,
			"Put every drop count back to the wiki's", actions::resetDropCounts)), 12);
		content.addRow(padded(dropsBody), 4);
		add(content, BorderLayout.NORTH);

		update(new PanelState());
	}

	private static JPanel padded(JPanel body)
	{
		body.setBorder(new EmptyBorder(6, 6, 6, 6));
		return body;
	}

	private static JPanel header(String title, Component right)
	{
		JPanel header = new JPanel(new BorderLayout());
		header.setOpaque(false);
		JLabel label = new JLabel(title);
		label.setFont(FontManager.getRunescapeBoldFont());
		label.setForeground(ColorScheme.BRAND_ORANGE);
		header.add(label, BorderLayout.WEST);
		if (right != null)
		{
			header.add(right, BorderLayout.EAST);
		}
		return header;
	}

	private static JLabel small(String text, Color color)
	{
		JLabel label = new JLabel(text);
		label.setFont(FontManager.getRunescapeSmallFont());
		label.setForeground(color);
		return label;
	}

	/** Small label that wraps instead of running off the side of the panel. */
	private static JLabel wrapped(String text, Color color)
	{
		// Swing sizes html px about a third larger than screen pixels
		return small("<html><body style='width:140px'>" + escape(text) + "</body></html>", color);
	}

	private static String escape(String text)
	{
		return text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
	}

	private static JLabel chip(String text, Color background, String tooltip, Runnable onClick)
	{
		JLabel chip = new JLabel(text, SwingConstants.CENTER);
		chip.setFont(FontManager.getRunescapeSmallFont());
		chip.setForeground(Color.WHITE);
		chip.setOpaque(true);
		chip.setBackground(background);
		chip.setBorder(new EmptyBorder(3, 6, 3, 6));
		chip.setToolTipText(tooltip);
		if (onClick != null)
		{
			chip.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
			chip.addMouseListener(new MouseAdapter()
			{
				@Override
				public void mousePressed(MouseEvent e)
				{
					if (e.getButton() == MouseEvent.BUTTON1)
					{
						onClick.run();
					}
				}
			});
		}
		return chip;
	}

	private JPanel buildSupplies()
	{
		Stack body = new Stack(ColorScheme.DARKER_GRAY_COLOR);
		body.setBorder(new EmptyBorder(6, 6, 6, 6));
		boolean first = true;
		for (Potion potion : Potion.values())
		{
			if (!potion.isSupply())
			{
				continue;
			}

			JLabel icon = new JLabel();
			icon.setPreferredSize(new Dimension(36, 32));
			icons.set(icon, potion.getIconItemId());
			icon.setToolTipText(potion.getDisplayName());

			JLabel have = new JLabel();
			have.setFont(FontManager.getRunescapeSmallFont());
			JLabel detail = small("", MUTED);
			JLabel more = small("", MUTED);
			haveLabels.put(potion, have);
			detailLabels.put(potion, detail);
			moreLabels.put(potion, more);

			JPanel text = new JPanel(new GridLayout(4, 1));
			text.setOpaque(false);
			JLabel name = small(potion.getDisplayName(), Color.WHITE);
			text.add(name);
			text.add(have);
			text.add(detail);
			text.add(more);

			JSpinner need = new JSpinner(new SpinnerNumberModel(0, 0, 99, 1));
			need.setPreferredSize(new Dimension(48, 24));
			need.setToolTipText("Doses of " + potion.getDisplayName() + " you want to have by Olm");
			need.addChangeListener(e ->
			{
				if (!updating)
				{
					actions.setNeed(potion, (Integer) need.getValue());
				}
			});
			needSpinners.put(potion, need);
			JPanel needHolder = new JPanel(new BorderLayout());
			needHolder.setOpaque(false);
			needHolder.add(small("need", MUTED), BorderLayout.NORTH);
			needHolder.add(need, BorderLayout.CENTER);

			JPanel row = new JPanel(new BorderLayout(6, 0));
			row.setOpaque(false);
			row.add(icon, BorderLayout.WEST);
			row.add(text, BorderLayout.CENTER);
			row.add(needHolder, BorderLayout.EAST);
			body.addRow(row, first ? 0 : 6);
			first = false;
		}
		return body;
	}

	private JPanel buildRoles()
	{
		Stack body = new Stack(ColorScheme.DARKER_GRAY_COLOR);
		body.setBorder(new EmptyBorder(6, 6, 6, 6));
		boolean first = true;
		for (CmRoom room : CmRoom.values())
		{
			List<Role> roles = Role.forRoom(room);
			if (roles.isEmpty())
			{
				continue;
			}
			JLabel title = new JLabel(room.getDisplayName());
			title.setFont(FontManager.getRunescapeSmallFont());
			title.setForeground(Color.WHITE);
			body.addRow(title, first ? 0 : 8);
			first = false;
			for (Role role : roles)
			{
				JCheckBox box = new JCheckBox(role.getDisplayName());
				box.setOpaque(false);
				box.setFont(FontManager.getRunescapeSmallFont());
				box.setToolTipText(role.getDescription());
				box.addActionListener(e ->
				{
					if (!updating)
					{
						actions.setRole(role, box.isSelected());
					}
				});
				roleBoxes.put(role, box);
				body.addRow(box, 0);

				JLabel missing = wrapped("", BAD);
				missing.setBorder(new EmptyBorder(0, 22, 2, 0));
				missing.setVisible(false);
				roleMissing.put(role, missing);
				body.addRow(missing, 0);
			}
		}
		return body;
	}

	/** Must run on the Swing thread. */
	void update(PanelState state)
	{
		updating = true;
		try
		{
			updateSupplies(state);
			updateRoles(state);
			String team = state.teamSignature();
			if (!team.equals(teamSignature))
			{
				teamSignature = team;
				rebuildTeam(state);
			}
			String drops = state.dropsSignature();
			if (!drops.equals(dropsSignature))
			{
				dropsSignature = drops;
				rebuildDrops(state);
			}
		}
		finally
		{
			updating = false;
		}
		revalidate();
		repaint();
	}

	private void updateSupplies(PanelState state)
	{
		for (Potion potion : haveLabels.keySet())
		{
			int need = state.need.getOrDefault(potion, 0);
			int have = state.have(potion);
			int shortfall = state.shortfall(potion);

			JLabel label = haveLabels.get(potion);
			if (need == 0)
			{
				label.setText(have + " doses");
				label.setForeground(Color.WHITE);
			}
			else if (shortfall == 0)
			{
				label.setText(have + " / " + need + " doses");
				label.setForeground(GOOD);
			}
			else
			{
				label.setText(have + " / " + need + ", short " + shortfall);
				label.setForeground(BAD);
			}

			String inventory = "inv " + state.inventory.doses(potion);
			String stored = "private " + (state.privateStorage == null ? "?" : state.privateStorage.doses(potion));
			String shared = "shared " + (state.sharedStorage == null ? "?" : state.sharedStorage.doses(potion));
			String claimed = "claimed " + state.claimed.doses(potion);
			String split = potion != Potion.OVERLOAD ? "" : "<br>split overload " + state.splitHeld() + " held, "
				+ state.claimed.doses(Potion.SPLIT_OVERLOAD) + " claimed" + (state.countSplit ? "" : " (not counted)");
			JLabel detail = detailLabels.get(potion);
			detail.setText(inventory + "  " + stored);
			moreLabels.get(potion).setText(shared + "  " + claimed);
			detail.setToolTipText("<html>" + inventory + "<br>" + stored + "<br>" + shared + "<br>" + claimed + split
				+ "<br><br>? means the storage hasn't been opened this raid</html>");
			label.setToolTipText(detail.getToolTipText());
			moreLabels.get(potion).setToolTipText(detail.getToolTipText());

			JSpinner spinner = needSpinners.get(potion);
			if (!Integer.valueOf(need).equals(spinner.getValue()))
			{
				spinner.setValue(need);
			}
		}
	}

	private void updateRoles(PanelState state)
	{
		for (Map.Entry<Role, JCheckBox> e : roleBoxes.entrySet())
		{
			Role role = e.getKey();
			boolean selected = state.roles.contains(role);
			if (e.getValue().isSelected() != selected)
			{
				e.getValue().setSelected(selected);
			}
			List<String> missing = selected ? state.missing.get(role) : null;
			JLabel label = roleMissing.get(role);
			if (missing == null || missing.isEmpty())
			{
				label.setVisible(false);
				e.getValue().setForeground(selected ? GOOD : Color.WHITE);
			}
			else
			{
				label.setText("<html><body style='width:120px'>Missing: " + escape(String.join(", ", missing))
					+ "</body></html>");
				label.setVisible(true);
				e.getValue().setForeground(BAD);
			}
		}
	}

	private void rebuildTeam(PanelState state)
	{
		teamBody.clear();
		if (!state.inParty)
		{
			teamBody.addRow(wrapped("Join a party with the Party plugin to see everyone's roles, items and claims.",
				MUTED), 0);
			return;
		}
		boolean first = true;
		for (PanelState.Member member : state.team)
		{
			MemberStatus status = member.status;
			JLabel name = new JLabel(member.name + (member.self ? " (you)" : ""));
			name.setFont(FontManager.getRunescapeSmallFont());
			teamBody.addRow(name, first ? 0 : 8);
			first = false;

			if (status == null)
			{
				name.setForeground(MUTED);
				teamBody.addRow(small("Plugin not installed or not logged in", MUTED), 0);
				continue;
			}
			name.setForeground(status.getMissing().isEmpty() ? GOOD : BAD);

			List<String> roles = new ArrayList<>();
			for (Role role : status.getRoles())
			{
				roles.add(role.getFullName());
			}
			teamBody.addRow(wrapped(roles.isEmpty() ? "No roles" : String.join(", ", roles),
				roles.isEmpty() ? MUTED : Color.WHITE), 0);
			for (String missing : status.getMissing())
			{
				teamBody.addRow(wrapped("Missing " + missing, BAD), 0);
			}

			StringBuilder carried = new StringBuilder();
			for (Potion potion : Potion.values())
			{
				if (potion.isSupply())
				{
					carried.append(carried.length() == 0 ? "" : "  ")
						.append(potion.getShortName()).append(' ').append(status.getCarried().doses(potion));
				}
			}
			JLabel doses = small(carried.toString(), MUTED);
			doses.setToolTipText("Doses in their inventory and private storage");
			teamBody.addRow(doses, 0);
		}
	}

	private void rebuildDrops(PanelState state)
	{
		dropsBody.clear();
		boolean first = true;
		for (PanelState.RoomDrops room : state.drops)
		{
			JLabel title = new JLabel(room.room.getDisplayName());
			title.setFont(FontManager.getRunescapeSmallFont());
			title.setForeground(ColorScheme.BRAND_ORANGE);
			title.setToolTipText("Right click to add a potion the wiki doesn't list for this room");
			title.setComponentPopupMenu(addPotionMenu(room));
			dropsBody.addRow(title, first ? 0 : 10);
			first = false;

			for (PanelState.PotionDrop drop : room.potions)
			{
				dropsBody.addRow(dropHeader(room.room, drop), 3);
				if (!drop.slots.isEmpty())
				{
					dropsBody.addRow(slotGrid(drop), 2);
				}
				for (int i = 0; i < drop.slots.size(); i++)
				{
					PanelState.SlotView slot = drop.slots.get(i);
					if (slot.planned())
					{
						addPlan(slot, i + 1);
					}
				}
			}
		}
	}

	private JPopupMenu addPotionMenu(PanelState.RoomDrops room)
	{
		JPopupMenu menu = new JPopupMenu();
		for (Potion potion : Potion.values())
		{
			boolean listed = false;
			for (PanelState.PotionDrop drop : room.potions)
			{
				listed |= drop.potion == potion;
			}
			if (!listed && potion.isClaimable())
			{
				JMenuItem item = new JMenuItem("Add " + potion.getDisplayName());
				item.addActionListener(e -> actions.setDropCount(room.room, potion, 1));
				menu.add(item);
			}
		}
		return menu;
	}

	private JPanel dropHeader(CmRoom room, PanelState.PotionDrop drop)
	{
		JLabel name = small(drop.potion.getDisplayName(), Color.WHITE);

		JLabel count = small(String.valueOf(drop.count), drop.edited ? WARN : Color.WHITE);
		count.setHorizontalAlignment(SwingConstants.CENTER);
		count.setPreferredSize(new Dimension(20, 16));
		count.setToolTipText(drop.edited
			? "Edited, the wiki says " + DropPlan.wikiCount(room, drop.potion)
			: "From the wiki drop table");

		JPanel adjust = new JPanel(new BorderLayout(2, 0));
		adjust.setOpaque(false);
		adjust.add(chip("-", ColorScheme.DARK_GRAY_COLOR, "One fewer dropped",
			() -> actions.setDropCount(room, drop.potion, drop.count - 1)), BorderLayout.WEST);
		adjust.add(count, BorderLayout.CENTER);
		adjust.add(chip("+", ColorScheme.DARK_GRAY_COLOR, "One more dropped",
			() -> actions.setDropCount(room, drop.potion, drop.count + 1)), BorderLayout.EAST);

		JPanel header = new JPanel(new BorderLayout());
		header.setOpaque(false);
		header.add(name, BorderLayout.WEST);
		header.add(adjust, BorderLayout.EAST);
		return header;
	}

	private JPanel slotGrid(PanelState.PotionDrop drop)
	{
		JPanel grid = new JPanel(new GridLayout(0, SLOTS_PER_ROW, 2, 2));
		grid.setOpaque(false);
		for (int i = 0; i < drop.slots.size(); i++)
		{
			grid.add(slotChip(drop.slots.get(i), i + 1));
		}
		// keep a short last row the same width as the others
		for (int i = drop.slots.size(); i < SLOTS_PER_ROW; i++)
		{
			JPanel filler = new JPanel();
			filler.setOpaque(false);
			grid.add(filler);
		}
		return grid;
	}

	/** Who holds the potion when, under the row of chips. */
	private void addPlan(PanelState.SlotView slot, int number)
	{
		List<String> lines = slot.planLines();
		for (int i = 0; i < lines.size(); i++)
		{
			JLabel line = wrapped((i == 0 ? "#" + number + " " : "") + lines.get(i),
				slot.overclaimed() ? WARN : slot.holders.get(i).self ? GOOD : Color.WHITE);
			line.setBorder(new EmptyBorder(0, i == 0 ? 0 : 10, 0, 0));
			dropsBody.addRow(line, i == 0 ? 3 : 0);
		}
	}

	private JLabel slotChip(PanelState.SlotView slot, int number)
	{
		Runnable click = () -> actions.toggleClaim(slot.slot);
		String menuHint = "Right click for doses and sip rooms.";
		JLabel chip;
		if (slot.holders.isEmpty())
		{
			chip = chip("free", ColorScheme.DARK_GRAY_COLOR, "Click to claim all of it. " + menuHint, click);
		}
		else
		{
			List<String> names = new ArrayList<>();
			for (PanelState.Holder holder : slot.holders)
			{
				names.add(holder.name);
			}
			String text = shorten((slot.planned() ? "#" + number + " " : "") + String.join("/", names));

			List<String> plan = new ArrayList<>();
			for (String line : slot.planLines())
			{
				plan.add(escape(line));
			}
			String hint;
			Color color;
			if (slot.overclaimed())
			{
				color = CONFLICT;
				hint = "More than " + Claim.MAX_DOSES + " doses claimed.";
			}
			else if (slot.me() != null)
			{
				color = MINE;
				hint = "Click to drop your claim.";
			}
			else if (slot.freeDoses() > 0)
			{
				color = OPEN;
				hint = "Click to pick it up after them and take the " + slot.freeDoses() + " left.";
			}
			else
			{
				color = TAKEN;
				hint = "All claimed.";
			}
			boolean clickable = slot.me() != null || slot.freeDoses() > 0;
			chip = chip(text, color, "<html>" + String.join("<br>", plan) + "<br><br>" + hint
				+ (clickable ? " " + menuHint : "") + "</html>", clickable ? click : null);
		}
		chip.setComponentPopupMenu(claimMenu(slot));
		return chip;
	}

	private JPopupMenu claimMenu(PanelState.SlotView slot)
	{
		JPopupMenu menu = new JPopupMenu();
		PanelState.Holder me = slot.me();
		int available = slot.availableToMe();
		if (available == 0)
		{
			JMenuItem none = new JMenuItem("All " + Claim.MAX_DOSES + " doses are claimed");
			none.setEnabled(false);
			menu.add(none);
			return menu;
		}

		for (int doses = 1; doses <= available; doses++)
		{
			int take = doses;
			JCheckBoxMenuItem item = new JCheckBoxMenuItem("Take " + doses + (doses == 1 ? " dose" : " doses"),
				me != null && me.claim.getDoses() == doses);
			item.addActionListener(e -> actions.setClaimDoses(slot.slot, take));
			menu.add(item);
		}

		JMenu sip = new JMenu("Sip at");
		for (CmRoom room : CmRoom.values())
		{
			if (room.ordinal() < slot.slot.getRoom().ordinal())
			{
				continue;
			}
			JCheckBoxMenuItem item = new JCheckBoxMenuItem(room.getDisplayName(),
				me != null && me.claim.getSipRooms().contains(room));
			// several rooms in one go
			item.putClientProperty("CheckBoxMenuItem.doNotCloseOnMouseClick", Boolean.TRUE);
			item.addActionListener(e ->
			{
				if (!actions.setSipRoom(slot.slot, room, item.isSelected()))
				{
					item.setSelected(!item.isSelected());
				}
			});
			sip.add(item);
		}
		menu.add(sip);

		if (me != null)
		{
			menu.addSeparator();
			JMenuItem drop = new JMenuItem("Drop my claim");
			drop.addActionListener(e -> actions.toggleClaim(slot.slot));
			menu.add(drop);
		}
		return menu;
	}

	private static String shorten(String name)
	{
		return name.length() > MAX_NAME ? name.substring(0, MAX_NAME - 1) + "." : name;
	}
}
