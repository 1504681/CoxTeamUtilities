package com.coxteamutilities;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import net.runelite.client.party.messages.PartyMemberMessage;

/** Sent to the party when your roles, missing items, claims or carried doses change. */
public class CoxStatusMessage extends PartyMemberMessage
{
	private List<String> roles;
	private List<String> missing;
	private List<String> claims;
	private int[] carried;
	/** Shared storage as this member last saw it, null if they haven't opened it this raid. */
	private int[] shared;
	private boolean iron;

	public CoxStatusMessage()
	{
	}

	public CoxStatusMessage(List<String> roles, List<String> missing, List<String> claims, int[] carried,
		int[] shared, boolean iron)
	{
		this.iron = iron;
		this.roles = roles;
		this.missing = missing;
		this.claims = claims;
		this.carried = carried;
		this.shared = shared;
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

	public int[] getCarried()
	{
		return carried;
	}

	public int[] getShared()
	{
		return shared;
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
			&& Arrays.equals(carried, other.carried)
			&& Arrays.equals(shared, other.shared)
			&& iron == other.iron;
	}
}
