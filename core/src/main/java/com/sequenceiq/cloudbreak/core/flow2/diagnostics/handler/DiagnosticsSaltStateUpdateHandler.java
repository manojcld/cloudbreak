package com.sequenceiq.cloudbreak.core.flow2.diagnostics.handler;

import static com.sequenceiq.cloudbreak.core.flow2.diagnostics.event.DiagnosticsCollectionHandlerSelectors.SALT_STATE_UPDATE_DIAGNOSTICS_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.diagnostics.event.DiagnosticsCollectionStateSelectors.START_DIAGNOSTICS_PREFLIGHT_CHECK_EVENT;

import java.util.Set;

import javax.inject.Inject;

import org.springframework.stereotype.Component;

import com.cloudera.thunderhead.service.common.usage.UsageProto;
import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.core.flow2.diagnostics.event.DiagnosticsCollectionEvent;
import com.sequenceiq.cloudbreak.core.flow2.diagnostics.event.DiagnosticsCollectionFailureEvent;
import com.sequenceiq.cloudbreak.telemetry.TelemetryComponentType;
import com.sequenceiq.cloudbreak.telemetry.upgrade.TelemetryUpgradeService;
import com.sequenceiq.common.model.diagnostics.DiagnosticParameters;
import com.sequenceiq.flow.reactor.api.handler.ExceptionCatcherEventHandler;
import com.sequenceiq.flow.reactor.api.handler.HandlerEvent;

import reactor.bus.Event;

@Component
public class DiagnosticsSaltStateUpdateHandler extends ExceptionCatcherEventHandler<DiagnosticsCollectionEvent> {

    @Inject
    private TelemetryUpgradeService telemetryUpgradeService;

    @Override
    protected Selectable doAccept(HandlerEvent<DiagnosticsCollectionEvent> event) throws Exception {
        DiagnosticsCollectionEvent data = event.getData();
        Long resourceId = data.getResourceId();
        String resourceCrn = data.getResourceCrn();
        DiagnosticParameters parameters = data.getParameters();
        telemetryUpgradeService.upgradeTelemetrySaltStates(resourceId, Set.of(TelemetryComponentType.CDP_TELEMETRY));
        return DiagnosticsCollectionEvent.builder()
                .withResourceCrn(resourceCrn)
                .withResourceId(resourceId)
                .withSelector(START_DIAGNOSTICS_PREFLIGHT_CHECK_EVENT.selector())
                .withParameters(parameters)
                .build();
    }

    @Override
    protected Selectable defaultFailureEvent(Long resourceId, Exception e, Event<DiagnosticsCollectionEvent> event) {
        return new DiagnosticsCollectionFailureEvent(resourceId, e, event.getData().getResourceCrn(), event.getData().getParameters(),
                UsageProto.CDPVMDiagnosticsFailureType.Value.UNSET.name());
    }

    @Override
    public String selector() {
        return SALT_STATE_UPDATE_DIAGNOSTICS_EVENT.selector();
    }

}
