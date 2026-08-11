package com.acr.review.insight.dto;

import java.util.ArrayList;
import java.util.List;
import com.acr.system.domain.SysUserIdentity;

public class InsightTeamIdentitiesResponse
{
    private List<SysUserIdentity> bindings = new ArrayList<>();
    private List<InsightUnboundIdentity> unbound = new ArrayList<>();

    public List<SysUserIdentity> getBindings()
    {
        return bindings;
    }

    public void setBindings(List<SysUserIdentity> bindings)
    {
        this.bindings = bindings;
    }

    public List<InsightUnboundIdentity> getUnbound()
    {
        return unbound;
    }

    public void setUnbound(List<InsightUnboundIdentity> unbound)
    {
        this.unbound = unbound;
    }
}
