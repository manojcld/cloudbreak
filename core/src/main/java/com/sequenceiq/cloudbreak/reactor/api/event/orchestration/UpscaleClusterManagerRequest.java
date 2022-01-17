package com.sequenceiq.cloudbreak.reactor.api.event.orchestration;

import java.util.Map;

import com.sequenceiq.cloudbreak.reactor.api.event.resource.AbstractClusterScaleRequest;

public class UpscaleClusterManagerRequest extends AbstractClusterScaleRequest {

    private final Map<String, Integer> hostGroupWithAdjustment;

    private final boolean primaryGatewayChanged;

    public UpscaleClusterManagerRequest(Long stackId, Map<String, Integer> hostGroupWithAdjustment, boolean primaryGatewayChanged) {
        super(stackId, hostGroupWithAdjustment.keySet());
        this.hostGroupWithAdjustment = hostGroupWithAdjustment;
        this.primaryGatewayChanged = primaryGatewayChanged;
    }

    public Map<String, Integer> getHostGroupWithAdjustment() {
        return hostGroupWithAdjustment;
    }

    public boolean isPrimaryGatewayChanged() {
        return primaryGatewayChanged;
    }
}
