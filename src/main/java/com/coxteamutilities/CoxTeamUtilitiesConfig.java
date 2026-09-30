package com.coxteamutilities;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.ConfigSection;
import net.runelite.client.config.Notification;
import net.runelite.client.config.Range;

@ConfigGroup(CoxTeamUtilitiesConfig.GROUP)
public interface CoxTeamUtilitiesConfig extends Config
{
	String GROUP = "coxteamutilities";
	String KEY_ROLES = "roles";
	String KEY_PLAN = "plan";

	@ConfigSection(
		name = "Doses needed",
		description = "How many doses you want to have by Olm. Also editable in the sidebar.",
		position = 0
	)
	String needSection = "need";

	@ConfigSection(
		name = "Reminders",
		description = "What happens when a role item is missing",
		position = 1
	)
	String reminderSection = "reminders";

	@Range(max = 99)
	@ConfigItem(
		keyName = "needOverload",
		name = "Overload",
		description = "Overload doses you want. 0 hides the shortfall.",
		section = needSection,
		position = 0
	)
	default int needOverload()
	{
		return 4;
	}

	@Range(max = 99)
	@ConfigItem(
		keyName = "needXericsAid",
		name = "Xeric's aid",
		description = "Xeric's aid doses you want. 0 hides the shortfall.",
		section = needSection,
		position = 1
	)
	default int needXericsAid()
	{
		return 12;
	}

	@Range(max = 99)
	@ConfigItem(
		keyName = "needRevitalisation",
		name = "Revitalisation",
		description = "Revitalisation doses you want. 0 hides the shortfall.",
		section = needSection,
		position = 2
	)
	default int needRevitalisation()
	{
		return 8;
	}

	@Range(max = 99)
	@ConfigItem(
		keyName = "needPrayerEnhance",
		name = "Prayer enhance",
		description = "Prayer enhance doses you want. 0 hides the shortfall.",
		section = needSection,
		position = 3
	)
	default int needPrayerEnhance()
	{
		return 4;
	}

	@ConfigItem(
		keyName = "countClaimed",
		name = "Count claimed drops",
		description = "Count the drops you've claimed towards the doses you need",
		section = needSection,
		position = 4
	)
	default boolean countClaimed()
	{
		return true;
	}

	@ConfigItem(
		keyName = "countShared",
		name = "Count shared storage",
		description = "Count what's in shared storage towards the doses you need",
		section = needSection,
		position = 5
	)
	default boolean countShared()
	{
		return false;
	}

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
