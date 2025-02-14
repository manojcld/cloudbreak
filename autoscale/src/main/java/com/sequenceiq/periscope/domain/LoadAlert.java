package com.sequenceiq.periscope.domain;

import static com.sequenceiq.periscope.common.AlertConstants.PARAMETERS;

import java.util.HashMap;
import java.util.Map;

import javax.persistence.Column;
import javax.persistence.Convert;
import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import javax.persistence.ManyToOne;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;

import com.sequenceiq.periscope.api.model.AlertType;
import com.sequenceiq.periscope.converter.db.LoadAlertConfigAttributeConverter;

@Entity
@DiscriminatorValue("LOAD")
@NamedQueries({
        @NamedQuery(name = "LoadAlert.findByCluster", query = "SELECT c FROM LoadAlert c WHERE c.cluster.id= :clusterId AND c.id= :alertId"),
        @NamedQuery(name = "LoadAlert.findAllByCluster", query = "SELECT c FROM LoadAlert c WHERE c.cluster.id= :clusterId")
})
public class LoadAlert extends BaseAlert {

    @ManyToOne
    private Cluster cluster;

    @Convert(converter = LoadAlertConfigAttributeConverter.class)
    @Column(name = "load_alert_config")
    private LoadAlertConfiguration loadAlertConfiguration;

    @Override
    public Cluster getCluster() {
        return cluster;
    }

    public void setCluster(Cluster cluster) {
        this.cluster = cluster;
    }

    public LoadAlertConfiguration getLoadAlertConfiguration() {
        return loadAlertConfiguration;
    }

    public void setLoadAlertConfiguration(LoadAlertConfiguration loadAlertConfiguration) {
        this.loadAlertConfiguration = loadAlertConfiguration;
    }

    public AlertType getAlertType() {
        return AlertType.LOAD;
    }

    public Map<String, String> getTelemetryParameters() {
        Map<String, String> parameters = new HashMap<>();
        parameters.put(PARAMETERS, loadAlertConfiguration.toString());
        return parameters;
    }
}
