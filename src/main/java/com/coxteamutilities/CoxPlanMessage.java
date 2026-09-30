package com.coxteamutilities;

import java.util.List;
import net.runelite.client.party.messages.PartyMessage;

/** Drop count edits, sent when you change one and when someone joins the party. */
public class CoxPlanMessage extends PartyMessage
{
	private List<String> edits;

	public CoxPlanMessage()
	{
	}

	public CoxPlanMessage(List<String> edits)
	{
		this.edits = edits;
	}

	public List<String> getEdits()
	{
		return edits;
	}
}
