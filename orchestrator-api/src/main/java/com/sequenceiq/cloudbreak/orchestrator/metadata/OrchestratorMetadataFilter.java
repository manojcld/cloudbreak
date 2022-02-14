package com.sequenceiq.cloudbreak.orchestrator.metadata;

import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.collections4.CollectionUtils;

import com.sequenceiq.cloudbreak.common.orchestration.Node;

public class OrchestratorMetadataFilter {

    private final Set<String> includeHosts;

    private final Set<String> excludeHosts;

    private final Set<String> includeHostGroups;

    private final Set<Node> nodes;

    public OrchestratorMetadataFilter(Builder builder) {
        this.includeHosts = builder.includeHosts;
        this.excludeHosts = builder.excludeHosts;
        this.includeHostGroups = builder.includeHostGroups;
        this.nodes = builder.nodes;
    }

    public Set<Node> apply(OrchestratorMetadata metadata) {
        return CollectionUtils.isNotEmpty(nodes) ? nodes : metadata.getNodes().stream()
                .filter(this::apply)
                .collect(Collectors.toSet());
    }

    public Set<Node> apply(Set<Node> allNodes) {
        return CollectionUtils.isNotEmpty(nodes) ? nodes : allNodes.stream()
                .filter(this::apply)
                .collect(Collectors.toSet());
    }

    public boolean apply(Node node) {
        boolean result = true;
        if (CollectionUtils.isNotEmpty(excludeHosts)) {
            result = !nodeHostFilterMatches(node, excludeHosts);
        }
        if (result && CollectionUtils.isNotEmpty(includeHosts)) {
            result = nodeHostFilterMatches(node, includeHosts);
        }
        if (result && CollectionUtils.isNotEmpty(includeHostGroups)) {
            result = includeHostGroups.contains(node.getHostGroup());
        }
        return result;
    }

    @Override
    public String toString() {
        return "OrchestratorMetadataFilter{" +
                "includeHosts=" + includeHosts +
                ", excludeHosts=" + excludeHosts +
                ", includeHostGroups=" + includeHostGroups +
                ", nodes=" + nodes +
                '}';
    }

    private boolean nodeHostFilterMatches(Node node, Set<String> hosts) {
        return hosts.contains(node.getHostname()) || hosts.contains(node.getPrivateIp()) || hosts.contains(node.getPublicIp());
    }

    public static class Builder {
        private Set<String> includeHosts;

        private Set<String> excludeHosts;

        private Set<String> includeHostGroups;

        private Set<Node> nodes;

        private Builder() {
        }

        public static Builder newBuilder() {
            return new Builder();
        }

        public OrchestratorMetadataFilter build() {
            return new OrchestratorMetadataFilter(this);
        }

        public Builder withNodes(Set<Node> nodes) {
            this.nodes = nodes;
            return this;
        }

        public Builder includeHosts(Set<String> includeHosts) {
            this.includeHosts = includeHosts;
            return this;
        }

        public Builder includeHostGroups(Set<String> includeHostGroups) {
            this.includeHostGroups = includeHostGroups;
            return this;
        }

        public Builder exlcudeHosts(Set<String> excludeHosts) {
            this.excludeHosts = excludeHosts;
            return this;
        }
    }
}
