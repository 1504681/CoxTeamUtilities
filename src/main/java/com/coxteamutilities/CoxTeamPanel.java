package com.coxteamutilities;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import javax.swing.JCheckBox;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JTextArea;
import javax.swing.JTextField;
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

		/** Switches the doses needed between the team and the solo numbers. */
		void setNeedsTab(boolean solo);

		/** Thanks for the raid: drops every claim and role you have. */
		void finishRaid();

		void renameChest(String key, String name);

		void setChestOrdered(String key, boolean ordered);

		/** Replaces a chest's deposit (true) or withdraw (false) list with the lines of the text. */
		void setChestLines(String key, boolean deposit, String text);

		/** Whether clicking items in a storage or the inventory adds them to the chest's lists. */
		void setMarking(boolean on);

		/** The chest shown in the sidebar, which inventory items are marked for when no storage is open. */
		void selectChest(String key);

		void deleteChest(String key);

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
	private final Map<Potion, JTextField> needFields = new EnumMap<>(Potion.class);
	private final Map<Potion, JComponent> supplyRows = new EnumMap<>(Potion.class);
	private final JLabel teamTab = small("Team", Color.WHITE);
	private final JLabel soloTab = small("Solo", MUTED);
	private NeedUnits units = NeedUnits.DOSES;
	private final JPanel storageGrid = new JPanel(new GridBagLayout());
	private final Stack chestsBody = new Stack(ColorScheme.DARKER_GRAY_COLOR);
	private final JComboBox<String> chestChooser = new JComboBox<>();
	private final JTextField chestName = new JTextField();
	private final JCheckBox chestOrdered = new JCheckBox("Withdraw in this order");
	private final JTextArea chestDeposit = new JTextArea(2, 10);
	private final JTextArea chestWithdraw = new JTextArea(3, 10);
	private final Stack chestSteps = new Stack(null);
	private String selectedChest;
	private final JLabel chestHere = small("", MUTED);
	private final JCheckBox chestMark = new JCheckBox("Mark by clicking");
	private final JPanel chestEditor = new JPanel(new BorderLayout());
	private final List<String> chestKeys = new ArrayList<>();
	private String lastCurrentChest;
	private final JLabel claimCount = small("", GOOD);
	private final JLabel claimsToggle = small("show", MUTED);
	private final JPanel claimsSection = new JPanel(new BorderLayout());
	private final Map<Role, JCheckBox> roleBoxes = new EnumMap<>(Role.class);
	private final Map<Role, JLabel> roleMissing = new EnumMap<>(Role.class);
	private final Stack teamBody = new Stack(ColorScheme.DARKER_GRAY_COLOR);
	private final Stack dropsBody = new Stack(ColorScheme.DARKER_GRAY_COLOR);

	private boolean updating;
	private String teamSignature;
	private String dropsSignature;
	private PanelState lastState;

	CoxTeamPanel(Actions actions, Icons icons)
	{
		this.actions = actions;
		this.icons = icons;

		setLayout(new BorderLayout());
		setBackground(ColorScheme.DARK_GRAY_COLOR);

		Stack content = new Stack(null);
		content.addRow(header("Supplies", needsTabs()), 0);
		content.addRow(buildSupplies(), 4);
		content.addRow(header("Roles", null), 12);
		content.addRow(buildRoles(), 4);
		content.addRow(header("Team", null), 12);
		content.addRow(padded(teamBody), 4);
		content.addRow(header("Chests", chip("Delete", ColorScheme.DARKER_GRAY_COLOR,
			"Forget the chest shown below", () ->
			{
				if (selectedChest != null)
				{
					actions.deleteChest(selectedChest);
				}
			})), 12);
		content.addRow(buildChests(), 4);
		content.addRow(claimsHeader(), 12);
		claimsSection.setOpaque(false);
		claimsSection.add(padded(dropsBody), BorderLayout.CENTER);
		claimsSection.setVisible(false);
		content.addRow(claimsSection, 4);
		JLabel tyfr = chip("TYFR", ColorScheme.DARKER_GRAY_COLOR,
			"Thanks for the raid: drops all your claims and all your roles", actions::finishRaid);
		tyfr.setBorder(new EmptyBorder(6, 6, 6, 6));
		content.addRow(tyfr, 12);
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

			JTextField need = new JTextField("0");
			need.setFont(FontManager.getRunescapeSmallFont());
			need.setHorizontalAlignment(SwingConstants.CENTER);
			need.setMargin(new Insets(1, 1, 1, 1));
			need.setToolTipText(potion.getDisplayName() + " you want to have when you get to Olm");
			Runnable commit = () -> actions.setNeed(potion, units.parse(need.getText()));
			need.addActionListener(e -> commit.run());
			need.addFocusListener(new FocusAdapter()
			{
				@Override
				public void focusLost(FocusEvent e)
				{
					commit.run();
				}
			});
			needFields.put(potion, need);
			JPanel needHolder = new JPanel(new BorderLayout());
			needHolder.setOpaque(false);
			needHolder.setPreferredSize(new Dimension(36, 36));
			needHolder.add(small("need", MUTED), BorderLayout.NORTH);
			needHolder.add(need, BorderLayout.CENTER);

			JPanel row = new JPanel(new BorderLayout(6, 0));
			row.setOpaque(false);
			row.add(icon, BorderLayout.WEST);
			row.add(text, BorderLayout.CENTER);
			row.add(needHolder, BorderLayout.EAST);
			supplyRows.put(potion, row);
			body.addRow(row, first ? 0 : 6);
			first = false;
		}
		storageGrid.setOpaque(false);
		storageGrid.setToolTipText("Inventory + private storage of everyone in the party, the shared storage, and all of it added up");
		body.addRow(storageGrid, 8);
		return body;
	}

	/** Who holds what: a row per party member, one for the shared storage and a total. */
	private void rebuildStorageGrid(PanelState state)
	{
		storageGrid.removeAll();
		List<Potion> potions = new ArrayList<>();
		for (Potion potion : Potion.values())
		{
			if (potion.isSupply() && !potion.isSoloOnly())
			{
				potions.add(potion);
			}
		}
		GridBagConstraints c = new GridBagConstraints();
		c.insets = new Insets(1, 0, 1, 4);
		c.anchor = GridBagConstraints.WEST;
		c.fill = GridBagConstraints.HORIZONTAL;
		c.gridy = 0;
		gridRow(c, "", MUTED, potions, potion -> potion.getShortName(), null);

		int[] total = new int[Potion.values().length];
		List<PanelState.Member> members = state.team;
		if (members.isEmpty())
		{
			PanelState.Member me = new PanelState.Member();
			me.name = "You";
			me.self = true;
			me.status = new MemberStatus(EnumSet.noneOf(Role.class), Collections.emptyList(), Collections.emptyMap(),
				state.inventory, state.privateStorage, null, false);
			members = Collections.singletonList(me);
		}
		for (PanelState.Member member : members)
		{
			c.gridy++;
			Supplies held = member.status == null ? null : member.status.getHeld();
			boolean storageUnknown = member.status != null && member.status.getStored() == null;
			String note = held == null ? "Nothing known: ask them to install CoX Team Utilities from the Plugin Hub"
				: storageUnknown ? "Inventory only, " + (member.self ? "your" : "their")
				+ " private storage hasn't been opened this raid" : null;
			gridRow(c, member.self ? "You" : member.name, held == null ? MUTED : member.self ? GOOD : Color.WHITE, potions,
				potion -> held == null ? "\u00d7" : units.format(held.doses(potion)) + (storageUnknown ? "?" : ""), note);
			if (held != null)
			{
				for (Potion potion : potions)
				{
					total[potion.ordinal()] += held.doses(potion);
				}
			}
		}
		c.gridy++;
		Supplies shared = state.shared();
		gridRow(c, "Shared", Color.WHITE, potions, potion -> shared == null ? "?" : units.format(shared.doses(potion)),
			shared == null ? "Nobody has opened the shared storage this raid" : null);
		if (shared != null)
		{
			for (Potion potion : potions)
			{
				total[potion.ordinal()] += shared.doses(potion);
			}
		}
		c.gridy++;
		gridRow(c, "Total", MUTED, potions, potion -> units.format(total[potion.ordinal()]), null);
		storageGrid.revalidate();
	}

	private void gridRow(GridBagConstraints c, String name, Color color, List<Potion> potions,
		java.util.function.Function<Potion, String> cell, String tooltip)
	{
		c.gridx = 0;
		c.weightx = 1;
		JLabel label = small(name, color);
		label.setToolTipText(tooltip == null && name.length() > MAX_NAME ? name : tooltip);
		// a long name gets the column's share and an ellipsis, not a wider grid
		label.setPreferredSize(new Dimension(60, label.getPreferredSize().height));
		storageGrid.add(label, c);
		c.weightx = 0;
		for (Potion potion : potions)
		{
			c.gridx++;
			JLabel value = small(cell.apply(potion), color);
			value.setHorizontalAlignment(SwingConstants.RIGHT);
			value.setPreferredSize(new Dimension(30, value.getPreferredSize().height));
			value.setToolTipText(tooltip == null ? potion.getDisplayName() : tooltip);
			storageGrid.add(value, c);
		}
	}

	/** A chooser for the chest, its name, and the two lists with what the inventory says about them. */
	private JPanel buildChests()
	{
		chestsBody.setBorder(new EmptyBorder(6, 6, 6, 6));
		chestsBody.addRow(chestHere, 0);
		chestMark.setOpaque(false);
		chestMark.setFont(FontManager.getRunescapeSmallFont());
		chestMark.setForeground(Color.WHITE);
		chestMark.setToolTipText("<html>While on, left-clicking an item in a storage adds it to Take out and one in your inventory to Put in."
			+ "<br>Each click adds one more; Unmark on the right-click menu takes one away. Off again when you TYFR.</html>");
		chestMark.addActionListener(e ->
		{
			if (!updating)
			{
				actions.setMarking(chestMark.isSelected());
			}
		});
		chestsBody.addRow(chestMark, 2);

		chestChooser.setFont(FontManager.getRunescapeSmallFont());
		chestChooser.setPreferredSize(new Dimension(100, 24));
		chestChooser.addActionListener(e ->
		{
			int index = chestChooser.getSelectedIndex();
			if (!updating && index >= 0 && index < chestKeys.size())
			{
				selectedChest = chestKeys.get(index);
				actions.selectChest(selectedChest);
				showChest(lastState);
			}
		});

		chestName.setFont(FontManager.getRunescapeSmallFont());
		chestName.setToolTipText("Your name for this chest");
		commitOn(chestName, () -> actions.renameChest(selectedChest, chestName.getText()));

		chestOrdered.setOpaque(false);
		chestOrdered.setFont(FontManager.getRunescapeSmallFont());
		chestOrdered.setForeground(Color.WHITE);
		chestOrdered.setToolTipText("Take things out top to bottom; the next one is lit up in the storage");
		chestOrdered.addActionListener(e ->
		{
			if (!updating && selectedChest != null)
			{
				actions.setChestOrdered(selectedChest, chestOrdered.isSelected());
			}
		});

		Stack editor = new Stack(null);
		editor.addRow(chestChooser, 0);
		editor.addRow(chestName, 4);
		editor.addRow(chestOrdered, 4);
		editor.addRow(small("Put in", Color.WHITE), 6);
		editor.addRow(listArea(chestDeposit, true), 2);
		editor.addRow(small("Take out", Color.WHITE), 6);
		editor.addRow(listArea(chestWithdraw, false), 2);
		editor.addRow(chestSteps, 6);
		chestEditor.setOpaque(false);
		chestEditor.add(editor, BorderLayout.CENTER);
		chestsBody.addRow(chestEditor, 4);
		return chestsBody;
	}

	private JTextArea listArea(JTextArea area, boolean deposit)
	{
		area.setFont(FontManager.getRunescapeSmallFont());
		area.setLineWrap(true);
		area.setWrapStyleWord(true);
		area.setMargin(new Insets(3, 3, 3, 3));
		area.setToolTipText("<html>One item per line, matched from the start of its name, so 'Xeric's aid' is any dose."
			+ "<br>* and ? are wildcards: '*chinchompa', 'Dragon *'. 'Stinkhorn mushroom x3' for a number"
			+ (deposit ? ", 'everything' to empty the inventory" : "") + ".</html>");
		area.addFocusListener(new FocusAdapter()
		{
			@Override
			public void focusLost(FocusEvent e)
			{
				if (selectedChest != null)
				{
					actions.setChestLines(selectedChest, deposit, area.getText());
				}
			}
		});
		return area;
	}

	private static void commitOn(JTextField field, Runnable commit)
	{
		field.addActionListener(e -> commit.run());
		field.addFocusListener(new FocusAdapter()
		{
			@Override
			public void focusLost(FocusEvent e)
			{
				commit.run();
			}
		});
	}

	private void updateChests(PanelState state)
	{
		List<ChestPlan> plans = state.chests.all();
		if (state.currentChest != null && !state.currentChest.equals(lastCurrentChest) && state.chests.get(state.currentChest) != null)
		{
			selectedChest = state.currentChest;
		}
		lastCurrentChest = state.currentChest;
		if (state.chests.get(selectedChest) == null)
		{
			selectedChest = state.chests.get(state.currentChest) != null ? state.currentChest
				: plans.isEmpty() ? null : plans.get(0).getKey();
		}

		chestKeys.clear();
		chestChooser.removeAllItems();
		for (ChestPlan plan : plans)
		{
			chestKeys.add(plan.getKey());
			chestChooser.addItem(plan.getName().isEmpty() ? plan.getKey() : plan.getName());
		}
		chestEditor.setVisible(!plans.isEmpty());
		chestMark.setSelected(state.marking);
		actions.selectChest(selectedChest);
		if (plans.isEmpty())
		{
			chestHere.setText("<html>Open a storage unit in a raid and it shows up here, with a list of what to put in and take out.</html>");
		}
		else if (state.currentChest == null)
		{
			chestHere.setText("Not at a chest");
		}
		else
		{
			ChestPlan here = state.chests.get(state.currentChest);
			chestHere.setText("At: " + (here == null ? CoxTeamUtilitiesPlugin.chestName(state.currentChest) + " (not set up yet, open it)"
				: here.getName()));
		}
		showChest(state);
	}

	private void showChest(PanelState state)
	{
		ChestPlan plan = state == null ? null : state.chests.get(selectedChest);
		if (plan == null)
		{
			return;
		}
		int index = chestKeys.indexOf(plan.getKey());
		if (chestChooser.getSelectedIndex() != index)
		{
			chestChooser.setSelectedIndex(index);
		}
		setIfIdle(chestName, plan.getName());
		chestOrdered.setSelected(plan.isOrdered());
		setIfIdle(chestDeposit, String.join("\n", plan.getDeposit()));
		setIfIdle(chestWithdraw, String.join("\n", plan.getWithdraw()));

		chestSteps.clear();
		if (plan.getKey().equals(state.currentChest))
		{
			ChestProgress progress = new ChestProgress(plan, state.inventoryNames);
			ChestProgress.Step next = progress.next();
			for (ChestProgress.Step step : progress.deposits)
			{
				chestSteps.addRow(small((step.done ? "\u2713 " : "\u2022 ") + "in: " + step.line.text, step.done ? GOOD : Color.WHITE), 0);
			}
			for (ChestProgress.Step step : progress.withdrawals)
			{
				String prefix = step.done ? "\u2713 " : step == next ? "\u2192 " : "\u2022 ";
				chestSteps.addRow(small(prefix + (plan.isOrdered() ? step.order + ". " : "out: ") + step.line.text,
					step.done ? GOOD : step == next ? WARN : Color.WHITE), 0);
			}
		}
	}

	private static void setIfIdle(javax.swing.text.JTextComponent field, String text)
	{
		if (!field.hasFocus() && !field.getText().equals(text))
		{
			field.setText(text);
		}
	}

	/** Team | Solo, which set of "need" numbers is shown and edited. */
	private JPanel needsTabs()
	{
		JPanel tabs = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
		tabs.setOpaque(false);
		for (JLabel tab : new JLabel[]{teamTab, soloTab})
		{
			tab.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
			tab.addMouseListener(new MouseAdapter()
			{
				@Override
				public void mousePressed(MouseEvent e)
				{
					actions.setNeedsTab(tab == soloTab);
				}
			});
			tabs.add(tab);
		}
		return tabs;
	}

	/** "Claims", how many the party has made, and the fold that starts closed. */
	private JPanel claimsHeader()
	{
		JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
		right.setOpaque(false);
		right.add(claimCount);
		right.add(chip("Reset", ColorScheme.DARKER_GRAY_COLOR, "Put every drop count back to the wiki's",
			actions::resetDropCounts));
		right.add(claimsToggle);
		JPanel header = header("Claims", right);
		header.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
		header.setToolTipText("What each room drops, who takes what, and how it gets passed on");
		MouseAdapter fold = new MouseAdapter()
		{
			@Override
			public void mousePressed(MouseEvent e)
			{
				showClaims(!claimsSection.isVisible());
			}
		};
		header.addMouseListener(fold);
		claimsToggle.addMouseListener(fold);
		claimCount.addMouseListener(fold);
		return header;
	}

	void showClaims(boolean open)
	{
		claimsSection.setVisible(open);
		claimsToggle.setText(open ? "hide" : "show");
		revalidate();
		repaint();
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
			lastState = state;
			updateSupplies(state);
			updateRoles(state);
			updateChests(state);
			String team = state.teamSignature();
			if (!team.equals(teamSignature))
			{
				teamSignature = team;
				rebuildTeam(state);
			}
			int claims = state.claimCount();
			claimCount.setText(claims == 0 ? "" : claims + " claimed");
			claimCount.setToolTipText(claims == 0 ? null : "Claims made by you and your party. Click to see them");
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
		units = state.units;
		for (Potion potion : haveLabels.keySet())
		{
			supplyRows.get(potion).setVisible(state.applies(potion));
			int need = state.need.getOrDefault(potion, 0);
			int have = state.have(potion);
			int shortfall = state.shortfall(potion);

			JLabel label = haveLabels.get(potion);
			if (need == 0)
			{
				label.setText(units.format(have) + " " + units.getWord());
				label.setForeground(Color.WHITE);
			}
			else if (shortfall == 0)
			{
				label.setText(units.format(have) + " / " + units.format(need) + " " + units.getWord());
				label.setForeground(GOOD);
			}
			else
			{
				label.setText(units.format(have) + " / " + units.format(need) + ", " + units.format(shortfall) + " more");
				label.setForeground(BAD);
			}

			String inventory = "inv " + units.format(state.inventory.doses(potion));
			String stored = "private " + (state.privateStorage == null ? "?" : units.format(state.privateStorage.doses(potion)));
			Supplies sharedStorage = state.shared();
			String shared = "shared " + (sharedStorage == null ? "?" : units.format(sharedStorage.doses(potion)));
			String split = potion != Potion.OVERLOAD ? "" : "<br>split overload " + units.format(state.splitHeld()) + " held"
				+ (state.countSplit ? "" : " (not counted)");
			JLabel detail = detailLabels.get(potion);
			detail.setText(inventory + "  " + stored);
			moreLabels.get(potion).setText(shared + (state.countShared ? "" : " (not counted)"));
			detail.setToolTipText("<html>" + inventory + "<br>" + stored + "<br>" + shared + split
				+ "<br><br>? means the storage hasn't been opened this raid</html>");
			label.setToolTipText(detail.getToolTipText());
			moreLabels.get(potion).setToolTipText(detail.getToolTipText());

			JTextField field = needFields.get(potion);
			String value = units.format(need);
			if (!field.hasFocus() && !field.getText().equals(value))
			{
				field.setText(value);
			}
		}
		updateNeeds(state);
		rebuildStorageGrid(state);
	}

	private void updateNeeds(PanelState state)
	{
		teamTab.setForeground(state.solo ? MUTED : Color.WHITE);
		soloTab.setForeground(state.solo ? Color.WHITE : MUTED);
		String same = state.separateSoloNeeds ? "" : "<br>Same numbers for both until 'Separate doses for solo raids' is on in the settings";
		teamTab.setToolTipText("<html>What you need for Olm in a team raid" + same + "</html>");
		soloTab.setToolTipText("<html>What you need for Olm in a solo raid" + same + "</html>");
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
			JLabel name = new JLabel(member.name + (member.iron ? " (iron)" : "") + (member.self ? " (you)" : ""));
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
			boolean ironFirst = false;
			for (PanelState.Holder holder : slot.holders)
			{
				names.add(holder.name);
				ironFirst |= holder.iron && !holder.self;
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
			else if (slot.viewerIron && slot.availableToMe() > 0)
			{
				color = OPEN;
				hint = "Click to take the first sip and drop it for them. Ironmen go first.";
			}
			else if (slot.freeDoses() > 0 && slot.availableToMe() > 0)
			{
				color = OPEN;
				hint = "Click to pick it up after them and take the " + slot.freeDoses() + " left.";
			}
			else
			{
				color = TAKEN;
				hint = slot.viewerIron && ironFirst ? "An ironman has it, you can't pick up what they drop."
					: "All claimed.";
			}
			boolean clickable = slot.me() != null || slot.availableToMe() > 0;
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
			JMenuItem none = new JMenuItem(slot.viewerIron ? "Another ironman has it"
				: "All " + Claim.MAX_DOSES + " doses are claimed");
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
