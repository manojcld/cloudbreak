package com.sequenceiq.cloudbreak.telemetry.upgrade;

import static com.sequenceiq.cloudbreak.telemetry.common.TelemetryCommonConfigView.DESIRED_CDP_LOGGING_AGENT_VERSION;
import static com.sequenceiq.cloudbreak.telemetry.common.TelemetryCommonConfigView.DESIRED_CDP_TELEMETRY_VERSION;
import static com.sequenceiq.cloudbreak.telemetry.orchestrator.TelemetryOrchestratorModule.DATABUS;
import static com.sequenceiq.cloudbreak.telemetry.orchestrator.TelemetryOrchestratorModule.FILECOLLECTOR;
import static com.sequenceiq.cloudbreak.telemetry.orchestrator.TelemetryOrchestratorModule.FLUENT;
import static com.sequenceiq.cloudbreak.telemetry.orchestrator.TelemetryOrchestratorModule.METERING;
import static com.sequenceiq.cloudbreak.telemetry.orchestrator.TelemetryOrchestratorModule.MONITORING;
import static com.sequenceiq.cloudbreak.telemetry.orchestrator.TelemetryOrchestratorModule.NODESTATUS;
import static com.sequenceiq.cloudbreak.telemetry.orchestrator.TelemetryOrchestratorModule.TELEMETRY;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.apache.commons.collections4.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.common.orchestration.Node;
import com.sequenceiq.cloudbreak.orchestrator.exception.CloudbreakOrchestratorFailedException;
import com.sequenceiq.cloudbreak.orchestrator.host.HostOrchestrator;
import com.sequenceiq.cloudbreak.orchestrator.host.TelemetryOrchestrator;
import com.sequenceiq.cloudbreak.orchestrator.model.SaltConfig;
import com.sequenceiq.cloudbreak.orchestrator.model.SaltPillarProperties;
import com.sequenceiq.cloudbreak.telemetry.TelemetryComponentType;
import com.sequenceiq.cloudbreak.telemetry.TelemetryUpgradeConfiguration;
import com.sequenceiq.cloudbreak.orchestrator.metadata.OrchestratorMetadata;
import com.sequenceiq.cloudbreak.orchestrator.metadata.OrchestratorMetadataFilter;
import com.sequenceiq.cloudbreak.orchestrator.metadata.OrchestratorMetadataProvider;
import com.sequenceiq.cloudbreak.telemetry.orchestrator.TelemetryConfigProvider;
import com.sequenceiq.cloudbreak.telemetry.orchestrator.TelemetryOrchestratorModule;
import com.sequenceiq.cloudbreak.util.CompressUtil;

@Service
public class TelemetryUpgradeService {

    private static final Logger LOGGER = LoggerFactory.getLogger(TelemetryUpgradeService.class);

    private static final String EMPTY_VERSION = "";

    @Inject
    private TelemetryUpgradeConfiguration telemetryUpgradeConfiguration;

    @Inject
    private TelemetryConfigProvider telemetryConfigProvider;

    @Inject
    private OrchestratorMetadataProvider orchestratorMetadataProvider;

    @Inject
    private TelemetryOrchestrator telemetryOrchestrator;

    @Inject
    private HostOrchestrator hostOrchestrator;

    @Inject
    private CompressUtil compressUtil;

    public void upgradeTelemetryComponent(Long stackId, TelemetryComponentType componentType, OrchestratorMetadataFilter filter)
            throws CloudbreakOrchestratorFailedException {
        OrchestratorMetadata metadata = orchestratorMetadataProvider.getOrchestratorMetadata(stackId);
        Set<Node> nodes = filter != null ? filter.apply(metadata) : metadata.getNodes();
        if (TelemetryComponentType.METERING.equals(componentType)) {
            telemetryOrchestrator.upgradeMetering(metadata.getGatewayConfigs(), nodes, metadata.getExitCriteriaModel(),
                    telemetryUpgradeConfiguration.getMeteringAgent().getDesiredDate(), null);
        } else if (TelemetryComponentType.CDP_LOGGING_AGENT.equals(componentType)) {
            telemetryOrchestrator.updateTelemetryComponent(metadata.getGatewayConfigs(), nodes, metadata.getExitCriteriaModel(),
                    Map.of("telemetry", Map.of(DESIRED_CDP_LOGGING_AGENT_VERSION, telemetryUpgradeConfiguration.getCdpLoggingAgent().getDesiredVersion(),
                            DESIRED_CDP_TELEMETRY_VERSION, EMPTY_VERSION)));
        } else {
            telemetryOrchestrator.updateTelemetryComponent(metadata.getGatewayConfigs(), nodes, metadata.getExitCriteriaModel(),
                    Map.of("telemetry", Map.of(DESIRED_CDP_LOGGING_AGENT_VERSION, EMPTY_VERSION,
                            DESIRED_CDP_TELEMETRY_VERSION, telemetryUpgradeConfiguration.getCdpTelemetry().getDesiredVersion())));
        }
    }

    public void upgradeTelemetrySaltPillars(Long stackId, Set<TelemetryComponentType> components) throws CloudbreakOrchestratorFailedException {
        Map<String, SaltPillarProperties> pillarPropMap = telemetryConfigProvider.createTelemetryConfigs(stackId, components);
        SaltConfig saltConfig = new SaltConfig(pillarPropMap);
        OrchestratorMetadata metadata = orchestratorMetadataProvider.getOrchestratorMetadata(stackId);
        Set<Node> allNodes = metadata.getNodes();
        Set<Node> unresponsiveNodes = telemetryOrchestrator.collectUnresponsiveNodes(
                metadata.getGatewayConfigs(), allNodes, metadata.getExitCriteriaModel());
        Set<String> unresponsiveHostnames = unresponsiveNodes.stream().map(Node::getHostname).collect(Collectors.toSet());
        Set<Node> availableNodes = allNodes.stream()
                .filter(n -> !unresponsiveHostnames.contains(n.getHostname()))
                .collect(Collectors.toSet());
        if (CollectionUtils.isEmpty(availableNodes)) {
            String message = "Not found any available nodes for stack: " + stackId;
            LOGGER.info(message);
            throw new CloudbreakOrchestratorFailedException(message);
        }
        hostOrchestrator.initSaltConfig(metadata.getStack(), metadata.getGatewayConfigs(), availableNodes, saltConfig, metadata.getExitCriteriaModel());
    }

    public void upgradeTelemetrySaltStates(Long stackId, Set<TelemetryComponentType> components) throws IOException, CloudbreakOrchestratorFailedException {
        List<String> saltStateDefinitions = orchestratorMetadataProvider.getSaltStateDefinitionBaseFolders();
        Set<TelemetryOrchestratorModule> saltStateComponents = getSaltStateComponents(components);
        List<String> filteredSaltComponents = getSaltStateComponentPaths(saltStateComponents);
        byte[] currentSaltState = orchestratorMetadataProvider.getStoredStates(stackId);
        byte[] telemetrySaltStateConfigs = compressUtil.generateCompressedOutputFromFolders(saltStateDefinitions, filteredSaltComponents);
        if (currentSaltState == null) {
            updateSaltStateForComponents(stackId, saltStateComponents, telemetrySaltStateConfigs);
        } else {
            boolean saltStateContentMatches = compressUtil.compareCompressedContent(currentSaltState, telemetrySaltStateConfigs, filteredSaltComponents);
            if (!saltStateContentMatches) {
                updateSaltStateForComponents(stackId, saltStateComponents, telemetrySaltStateConfigs);
                byte[] newFullSaltState = compressUtil.updateCompressedOutputFolders(saltStateDefinitions, filteredSaltComponents, currentSaltState);
                orchestratorMetadataProvider.storeNewState(stackId, newFullSaltState);
            }
        }
    }

    private void updateSaltStateForComponents(Long stackId, Set<TelemetryOrchestratorModule> components, byte[] telemetrySaltStateConfigs)
            throws CloudbreakOrchestratorFailedException {
        List<String> componentNames = components.stream().map(TelemetryOrchestratorModule::getValue).collect(Collectors.toList());
        OrchestratorMetadata metadata = orchestratorMetadataProvider.getOrchestratorMetadata(stackId);
        telemetryOrchestrator.updatePartialSaltDefinition(telemetrySaltStateConfigs, componentNames, metadata.getGatewayConfigs(),
                metadata.getExitCriteriaModel());
    }

    private Set<TelemetryOrchestratorModule> getSaltStateComponents(Set<TelemetryComponentType> components) {
        Set<TelemetryOrchestratorModule> modules = new HashSet<>();
        for (TelemetryComponentType component : components) {
            if (TelemetryComponentType.CDP_TELEMETRY.equals(component)) {
                modules.addAll(Set.of(TELEMETRY, DATABUS, NODESTATUS, FILECOLLECTOR, MONITORING));
            } else if (TelemetryComponentType.CDP_LOGGING_AGENT.equals(component)) {
                modules.addAll(Set.of(TELEMETRY, DATABUS, FLUENT));
            } else if (TelemetryComponentType.METERING.equals(component)) {
                modules.addAll(Set.of(TELEMETRY, DATABUS, METERING));
            }
        }
        return modules;
    }

    private List<String> getSaltStateComponentPaths(Set<TelemetryOrchestratorModule> modules) {
        List<String> result = new ArrayList<>();
        for (TelemetryOrchestratorModule module : modules) {
            result.add(String.format("/salt/%s", module.getValue()));
        }
        return result;
    }
}
