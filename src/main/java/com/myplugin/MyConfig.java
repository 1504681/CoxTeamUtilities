package com.myplugin;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;

@ConfigGroup("myplugin")
public interface MyConfig extends Config
{
	@ConfigItem(
		keyName = "exampleOption",
		name = "Example Option",
		description = "An example config option",
		position = 0
	)
	default boolean exampleOption()
	{
		return true;
	}
}
