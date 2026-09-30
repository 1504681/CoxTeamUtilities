package com.coxteamutilities;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
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
	private final Map<Slot, Claim> claims;
	/** inventory only */
	private final Supplies carried;
	/** null until they open their private storage */
	private final Supplies stored;
	/** null until someone opens the shared storage */
	private final Supplies shared;
	private final boolean iron;

	public MemberStatus(Set<Role> roles, List<String> missing, Map<Slot, Claim> claims, Supplies carried,
		Supplies stored, Supplies shared, boolean iron)
	{
		this.iron = iron;
		this.roles = roles;
		this.missing = missing;
		this.claims = claims;
		this.carried = carried;
		this.stored = stored;
		this.shared = shared;
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
		Map<Slot, Claim> claims = new LinkedHashMap<>();
		if (message.getClaims() != null)
		{
			for (String encoded : message.getClaims())
			{
				Claim claim = Claim.parse(encoded);
				if (claim != null && claims.size() < MAX_CLAIMS)
				{
					claims.put(claim.getSlot(), claim);
				}
			}
		}
		return new MemberStatus(Role.parse(message.getRoles()), missing, claims, Supplies.of(message.getCarried()),
			message.getStored() == null ? null : Supplies.of(message.getStored()),
			message.getShared() == null ? null : Supplies.of(message.getShared()), message.isIron());
	}

	public Set<Role> getRoles()
	{
		return Collections.unmodifiableSet(roles);
	}

	public List<String> getMissing()
	{
		return Collections.unmodifiableList(missing);
	}

	public Collection<Claim> getClaims()
	{
		return Collections.unmodifiableCollection(claims.values());
	}

	public boolean isIron()
	{
		return iron;
	}

	public Supplies getCarried()
	{
		return carried;
	}

	public Supplies getStored()
	{
		return stored;
	}

	public Supplies getShared()
	{
		return shared;
	}

	/** Inventory plus private storage as far as it is known. */
	public Supplies getHeld()
	{
		return stored == null ? carried : carried.plus(stored);
	}
}
