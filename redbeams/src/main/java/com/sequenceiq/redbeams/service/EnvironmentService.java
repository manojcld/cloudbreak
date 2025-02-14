package com.sequenceiq.redbeams.service;

import javax.inject.Inject;

import org.springframework.stereotype.Service;

import com.sequenceiq.authorization.service.EnvironmentPropertyProvider;
import com.sequenceiq.environment.api.v1.environment.endpoint.EnvironmentEndpoint;
import com.sequenceiq.environment.api.v1.environment.model.response.DetailedEnvironmentResponse;

@Service
public class EnvironmentService implements EnvironmentPropertyProvider {

    @Inject
    private EnvironmentEndpoint environmentEndpoint;

    public DetailedEnvironmentResponse getByCrn(String envCrn) {
        return environmentEndpoint.getByCrn(envCrn);
    }
}
