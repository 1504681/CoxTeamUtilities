package com.myplugin;

import javax.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import net.runelite.api.Client;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.overlay.OverlayManager;
import com.google.inject.Provides;

@PluginDescriptor(
	name = "My Plugin",
	description = "A RuneLite plugin",
	tags = {"example"}
)
public class MyPlugin extends Plugin
{
	private static final Logger log = LoggerFactory.getLogger(MyPlugin.class);

	@Inject
	private Client client;

	@Inject
	private MyConfig config;

	@Inject
	private OverlayManager overlayManager;

	// IMPORTANT: Must provide config or Guice injection fails with ClassReader error
	@Provides
	MyConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(MyConfig.class);
	}

	@Override
	protected void startUp()
	{
		log.info("My Plugin started!");
		// Register overlays, panels, etc. here
	}

	@Override
	protected void shutDown()
	{
		log.info("My Plugin stopped!");
		// Unregister everything here
	}
}
