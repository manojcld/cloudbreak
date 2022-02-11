package com.sequenceiq.datalake.service;

import java.util.Optional;

import javax.inject.Inject;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.ServiceUnavailableException;
import javax.ws.rs.WebApplicationException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.common.exception.BadRequestException;
import com.sequenceiq.cloudbreak.common.exception.WebApplicationExceptionMessageExtractor;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.FreeIpaV1Endpoint;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.AvailabilityType;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.instance.InstanceGroupBase;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.describe.DescribeFreeIpaResponse;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.scale.UpscaleRequest;
import com.sequenceiq.freeipa.api.v1.operation.OperationV1Endpoint;
import com.sequenceiq.freeipa.api.v1.operation.model.OperationStatus;

@Service
public class FreeipaService {

    private static final Logger LOGGER = LoggerFactory.getLogger(FreeipaService.class);

    @Inject
    private FreeIpaV1Endpoint freeIpaV1Endpoint;

    @Inject
    private OperationV1Endpoint operationV1Endpoint;

    @Inject
    private WebApplicationExceptionMessageExtractor webApplicationExceptionMessageExtractor;

    public void checkFreeipaRunning(String envCrn) {
        DescribeFreeIpaResponse freeipa = describe(envCrn);
        if (freeipa != null && freeipa.getAvailabilityStatus() != null) {
            if (!freeipa.getAvailabilityStatus().isAvailable()) {
                String message = "Freeipa should be in Available state but currently is " + freeipa.getStatus().name();
                LOGGER.info(message);
                throw new BadRequestException(message);
            }
        } else {
            String message = "Freeipa availability cannot be determined currently.";
            LOGGER.warn(message);
            throw new ServiceUnavailableException(message);
        }

    }

    public DescribeFreeIpaResponse describe(String envCrn) {
        try {
            return freeIpaV1Endpoint.describe(envCrn);
        } catch (NotFoundException e) {
            LOGGER.error("Could not find freeipa with envCrn: " + envCrn, e);
        }
        return null;
    }

    public long getNodeCount(String envCrn) {
        return describe(envCrn)
                .getInstanceGroups().stream().mapToLong(InstanceGroupBase::getNodeCount).sum();
    }

    public String upscale(String envCrn, AvailabilityType availabilityType) {
        UpscaleRequest upscaleRequest = new UpscaleRequest();
        upscaleRequest.setEnvironmentCrn(envCrn);
        upscaleRequest.setTargetAvailabilityType(availabilityType);
        try {
            return freeIpaV1Endpoint.upscale(upscaleRequest).getOperationId();
        } catch (WebApplicationException e) {
            String errorMessage = webApplicationExceptionMessageExtractor.getErrorMessage(e);
            LOGGER.info("Can not upscale freeIpa {} from datalake: {}", envCrn, errorMessage, e);
            throw new RuntimeException("Cannot start cluster, error happened during operation: " + errorMessage);
        }
    }

    public Optional<OperationStatus> getFreeIpaOperation(String operationId, String acccountId) {
        try {
            return Optional.ofNullable(operationV1Endpoint.getOperationStatus(operationId, acccountId));
        } catch (WebApplicationException e) {
            return Optional.empty();
        }
    }
}
