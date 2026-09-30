package com.coxteamutilities;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/** What one party member last told us about themselves. */
public final class MemberStatus
{
	/** Caps on what we keep from a party message, which anyone in the party can craft. */
	static final int MAX_MISSING = 16;
	static final int MAX_CLAIMS = 64;
	static final int MAX_TEXT = 60;

	private final Set<Role> roles;
	private final List<String> missing;
	private final Set<Slot> claims;
	private final Supplies carried;

	public MemberStatus(Set<Role> roles, List<String> missing, Set<Slot> claims, Supplies carried)
	{
		this.roles = roles;
		this.missing = missing;
		this.claims = claims;
		this.carried = carried;
	}

	public static MemberStatus from(CoxStatusMessage message)
	{
		List<String> missing = new ArrayList<>();
		if (message.getMissing() != null)
		{
			for (String text : message.getMissing())
			{
				if (text != null && !text.isEmpty() && missing.size() < MAX_MISSING)
				{
					missing.add(text.length() > MAX_TEXT ? text.substring(0, MAX_TEXT) : text);
				}
			}
		}
		Set<Slot> claims = new LinkedHashSet<>();
		if (message.getClaims() != null)
		{
			for (String key : message.getClaims())
			{
				Slot slot = Slot.parse(key);
				if (slot != null && claims.size() < MAX_CLAIMS)
				{
					claims.add(slot);
				}
			}
		}
		return new MemberStatus(Role.parse(message.getRoles()), missing, claims, Supplies.of(message.getCarried()));
	}

	public Set<Role> getRoles()
	{
		return Collections.unmodifiableSet(roles);
	}

	public List<String> getMissing()
	{
		return Collections.unmodifiableList(missing);
	}

	public Set<Slot> getClaims()
	{
		return Collections.unmodifiableSet(claims);
	}

	public Supplies getCarried()
	{
		return carried;
	}
}
