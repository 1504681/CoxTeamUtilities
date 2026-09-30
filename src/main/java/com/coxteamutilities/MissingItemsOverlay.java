package com.coxteamutilities;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.util.Collections;
import java.util.List;
import javax.inject.Inject;
import net.runelite.client.ui.overlay.OverlayPanel;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.components.LineComponent;
import net.runelite.client.ui.overlay.components.TitleComponent;

/** Lists missing role items. Draws nothing when nobody is missing anything. */
class MissingItemsOverlay extends OverlayPanel
{
	private static final Color MISSING = new Color(255, 96, 96);

	static final class Line
	{
		final String who;
		final String what;

		Line(String who, String what)
		{
			this.who = who;
			this.what = what;
		}
	}

	private volatile List<Line> lines = Collections.emptyList();

	@Inject
	MissingItemsOverlay(CoxTeamUtilitiesPlugin plugin)
	{
		super(plugin);
		setPosition(OverlayPosition.TOP_LEFT);
	}

	void setLines(List<Line> lines)
	{
		this.lines = lines;
	}

	@Override
	public Dimension render(Graphics2D graphics)
	{
		List<Line> current = lines;
		if (current.isEmpty())
		{
			return null;
		}
		panelComponent.getChildren().add(TitleComponent.builder()
			.text("Missing for CoX")
			.color(MISSING)
			.build());
		int width = 0;
		for (Line line : current)
		{
			panelComponent.getChildren().add(LineComponent.builder()
				.left(line.who)
				.right(line.what)
				.rightColor(MISSING)
				.build());
			width = Math.max(width, graphics.getFontMetrics().stringWidth(line.who + "  " + line.what));
		}
		panelComponent.setPreferredSize(new Dimension(Math.max(129, width + 10), 0));
		return super.render(graphics);
	}
}
