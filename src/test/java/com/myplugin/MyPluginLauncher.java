package com.myplugin;

import net.runelite.client.RuneLite;
import net.runelite.client.externalplugins.ExternalPluginManager;

/**
 * Dev launcher — runs RuneLite with this plugin loaded.
 * Use: ./gradlew run
 * This loads ALL your installed plugin hub plugins too.
 */
public class MyPluginLauncher
{
	public static void main(String[] args) throws Exception
	{
		ExternalPluginManager.loadBuiltin(MyPlugin.class);
		RuneLite.main(args);
	}
}
