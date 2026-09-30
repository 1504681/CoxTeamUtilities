package com.myplugin;

import com.google.inject.Guice;
import com.google.inject.testing.fieldbinder.Bind;
import com.google.inject.testing.fieldbinder.BoundFieldModule;
import javax.inject.Inject;
import net.runelite.api.Client;
import net.runelite.client.ui.overlay.OverlayManager;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class MyPluginTest
{
	@Inject
	private MyPlugin plugin;

	@Mock
	@Bind
	private Client client;

	@Mock
	@Bind
	private MyConfig config;

	@Mock
	@Bind
	private OverlayManager overlayManager;

	@Before
	public void before()
	{
		Guice.createInjector(BoundFieldModule.of(this)).injectMembers(this);
	}

	@Test
	public void testStartUp() throws Exception
	{
		plugin.startUp();
	}

	@Test
	public void testShutDown() throws Exception
	{
		plugin.shutDown();
	}
}
