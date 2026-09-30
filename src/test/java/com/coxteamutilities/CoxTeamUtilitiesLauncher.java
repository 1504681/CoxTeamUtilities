package com.coxteamutilities;

import net.runelite.client.RuneLite;
import net.runelite.client.externalplugins.ExternalPluginManager;

public class CoxTeamUtilitiesLauncher
{
	public static void main(String[] args) throws Exception
	{
		ExternalPluginManager.loadBuiltin(CoxTeamUtilitiesPlugin.class);
		RuneLite.main(args);
	}
}
