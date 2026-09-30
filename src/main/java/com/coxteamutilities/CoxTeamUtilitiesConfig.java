package com.coxteamutilities;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.ConfigSection;
import net.runelite.client.config.Notification;

@ConfigGroup(CoxTeamUtilitiesConfig.GROUP)
public interface CoxTeamUtilitiesConfig extends Config
{
	String GROUP = "coxteamutilities";
	String KEY_ROLES = "roles";
	String KEY_PLAN = "plan";

	@ConfigSection(
		name = "Reminders",
		description = "What happens when a role item is missing",
		position = 0
	)
	String reminderSection = "reminders";

	@ConfigItem(
		keyName = "overlay",
		name = "Missing item overlay",
		description = "List missing role items on screen at the raid lobby and before the raid starts",
		section = reminderSection,
		position = 0
	)
	default boolean overlay()
	{
		return true;
	}

	@ConfigItem(
		keyName = "overlayDuringRaid",
		name = "Keep overlay during the raid",
		description = "Keep listing missing items after the raid has started",
		section = reminderSection,
		position = 1
	)
	default boolean overlayDuringRaid()
	{
		return false;
	}

	@ConfigItem(
		keyName = "includeTeam",
		name = "Include party members",
		description = "Also remind you about items your party members are missing",
		section = reminderSection,
		position = 2
	)
	default boolean includeTeam()
	{
		return true;
	}

	@ConfigItem(
		keyName = "remindSolo",
		name = "Role reminders in solo raids",
		description = "Show the missing-item overlay and chat message in a solo raid too. Off means only in a team",
		section = reminderSection,
		position = 5
	)
	default boolean remindSolo()
	{
		return false;
	}

	@ConfigItem(
		keyName = "chatReminder",
		name = "Chat message on entry",
		description = "Put missing items in the chatbox when you enter the raid. Only you see it.",
		section = reminderSection,
		position = 3
	)
	default boolean chatReminder()
	{
		return true;
	}

	@ConfigItem(
		keyName = "notification",
		name = "Notify on entry",
		description = "Send a notification when you enter the raid with a role item missing",
		section = reminderSection,
		position = 4
	)
	default Notification notification()
	{
		return Notification.OFF;
	}

	@ConfigItem(
		keyName = KEY_ROLES,
		name = "Roles",
		description = "Roles picked in the sidebar",
		hidden = true
	)
	default String roles()
	{
		return "";
	}

	@ConfigItem(
		keyName = KEY_PLAN,
		name = "Drop counts",
		description = "Drop counts edited in the sidebar",
		hidden = true
	)
	default String plan()
	{
		return "";
	}
}
