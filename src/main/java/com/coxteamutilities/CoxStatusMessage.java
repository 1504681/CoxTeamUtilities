package com.coxteamutilities;

import java.util.List;
import java.util.Objects;
import net.runelite.client.party.messages.PartyMemberMessage;

/** Sent to the party when your roles, missing items or claims change. */
public class CoxStatusMessage extends PartyMemberMessage
{
	private List<String> roles;
	private List<String> missing;
	private List<String> claims;
	private boolean iron;

	public CoxStatusMessage()
	{
	}

	public CoxStatusMessage(List<String> roles, List<String> missing, List<String> claims, boolean iron)
	{
		this.iron = iron;
		this.roles = roles;
		this.missing = missing;
		this.claims = claims;
	}

	public List<String> getRoles()
	{
		return roles;
	}

	public List<String> getMissing()
	{
		return missing;
	}

	public List<String> getClaims()
	{
		return claims;
	}

	public boolean isIron()
	{
		return iron;
	}

	public boolean sameContent(CoxStatusMessage other)
	{
		return other != null
			&& Objects.equals(roles, other.roles)
			&& Objects.equals(missing, other.missing)
			&& Objects.equals(claims, other.claims)
			&& iron == other.iron;
	}
}
