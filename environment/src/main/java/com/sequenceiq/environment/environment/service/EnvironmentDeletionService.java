package com.sequenceiq.environment.environment.service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import javax.ws.rs.BadRequestException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.google.common.annotations.VisibleForTesting;
import com.sequenceiq.cloudbreak.logger.MDCBuilder;
import com.sequenceiq.environment.environment.domain.EnvironmentView;
import com.sequenceiq.environment.environment.dto.EnvironmentDtoConverter;
import com.sequenceiq.environment.environment.dto.EnvironmentViewDto;
import com.sequenceiq.environment.environment.flow.EnvironmentReactorFlowManager;
import com.sequenceiq.environment.environment.sync.EnvironmentJobService;
import com.sequenceiq.environment.exception.ExperienceOperationFailedException;

@Service
public class EnvironmentDeletionService {

    private static final Logger LOGGER = LoggerFactory.getLogger(EnvironmentDeletionService.class);

    private final EnvironmentViewService environmentViewService;

    private final EnvironmentDtoConverter environmentDtoConverter;

    private final EnvironmentReactorFlowManager reactorFlowManager;

    private final EnvironmentResourceDeletionService environmentResourceDeletionService;

    private final EnvironmentJobService environmentJobService;

    public EnvironmentDeletionService(EnvironmentViewService environmentViewService,
            EnvironmentJobService environmentJobService,
            EnvironmentDtoConverter environmentDtoConverter,
            EnvironmentReactorFlowManager reactorFlowManager,
            EnvironmentResourceDeletionService environmentResourceDeletionService) {
        this.environmentResourceDeletionService = environmentResourceDeletionService;
        this.environmentDtoConverter = environmentDtoConverter;
        this.environmentJobService = environmentJobService;
        this.environmentViewService = environmentViewService;
        this.reactorFlowManager = reactorFlowManager;
    }

    public EnvironmentViewDto deleteByNameAndAccountId(String environmentName, String accountId, String actualUserCrn,
            boolean cascading, boolean forced) {
        EnvironmentView environment = environmentViewService.getByNameAndAccountId(environmentName, accountId);
        MDCBuilder.buildMdcContext(environment);
        LOGGER.debug(String.format("Deleting environment [name: %s]", environment.getName()));
        delete(environment, actualUserCrn, cascading, forced);
        return environmentDtoConverter.environmentViewToViewDto(environment);
    }

    public EnvironmentViewDto deleteByCrnAndAccountId(String crn, String accountId, String actualUserCrn,
            boolean cascading, boolean forced) {
        EnvironmentView environment = environmentViewService.getByCrnAndAccountId(crn, accountId);
        MDCBuilder.buildMdcContext(environment);
        LOGGER.debug(String.format("Deleting  environment [name: %s]", environment.getName()));
        delete(environment, actualUserCrn, cascading, forced);
        return environmentDtoConverter.environmentViewToViewDto(environment);
    }

    public EnvironmentView delete(EnvironmentView environment, String userCrn,
            boolean cascading, boolean forced) {
        MDCBuilder.buildMdcContext(environment);
        validateDeletion(environment, cascading);
        updateEnvironmentDeletionType(environment, forced);
        LOGGER.debug("Deleting environment with name: {}", environment.getName());
        environmentJobService.unschedule(environment.getId());
        if (cascading) {
            reactorFlowManager.triggerCascadingDeleteFlow(environment, userCrn, forced);
        } else {
            checkIsEnvironmentDeletable(environment);
            reactorFlowManager.triggerDeleteFlow(environment, userCrn, forced);
        }
        return environment;
    }

    private void updateEnvironmentDeletionType(EnvironmentView environment, boolean forced) {
        environmentViewService.editDeletionType(environment, forced);
    }

    public List<EnvironmentViewDto> deleteMultipleByNames(Set<String> environmentNames, String accountId, String actualUserCrn,
            boolean cascading, boolean forced) {
        Collection<EnvironmentView> environmentViews = environmentViewService.findByNamesInAccount(environmentNames, accountId);
        return deleteMultiple(actualUserCrn, cascading, forced, environmentViews);
    }

    public List<EnvironmentViewDto> deleteMultipleByCrns(Set<String> crns, String accountId, String actualUserCrn,
            boolean cascading, boolean forced) {
        Collection<EnvironmentView> environmentViews = environmentViewService.findByResourceCrnsInAccount(crns, accountId);
        return deleteMultiple(actualUserCrn, cascading, forced, environmentViews);
    }

    private List<EnvironmentViewDto> deleteMultiple(String actualUserCrn, boolean cascading, boolean forced, Collection<EnvironmentView> environments) {
        return new ArrayList<>(environments).stream()
                .map(environment -> {
                    LOGGER.debug(String.format("Starting to archive environment [name: %s, CRN: %s]", environment.getName(), environment.getResourceCrn()));
                    delete(environment, actualUserCrn, cascading, forced);
                    return environmentDtoConverter.environmentViewToViewDto(environment);
                })
                .collect(Collectors.toList());
    }

    @VisibleForTesting
    void checkIsEnvironmentDeletable(EnvironmentView env) {
        LOGGER.info("Checking if environment [name: {}] is deletable", env.getName());

        Set<String> distroXClusterNames = environmentResourceDeletionService.getAttachedDistroXClusterNames(env);
        if (!distroXClusterNames.isEmpty()) {
            throw new BadRequestException(String.format("The following Data Hub cluster(s) must be terminated before Environment deletion [%s]",
                    String.join(", ", distroXClusterNames)));
        }

        int amountOfConnectedExperiences = 0;
        try {
            amountOfConnectedExperiences = environmentResourceDeletionService.getConnectedExperienceAmount(env);
        } catch (IllegalStateException | IllegalArgumentException | ExperienceOperationFailedException re) {
            LOGGER.info("Something has occurred during checking the connected experiences!", re);
            throw new IllegalStateException("Unable to access all experience to check whether the environment have any connected one(s)!");
        }
        if (amountOfConnectedExperiences == 1) {
            throw new BadRequestException("The given environment [" + env.getName() + "] has 1 connected experience. " +
                    "This must be terminated before Environment deletion.");
        } else if (amountOfConnectedExperiences > 1) {
            throw new BadRequestException("The given environment [" + env.getName() + "] has " + amountOfConnectedExperiences +
                    " connected experiences. " + "These must be terminated before Environment deletion.");
        }

        Set<String> datalakes = environmentResourceDeletionService.getAttachedSdxClusterCrns(env);
        // if someone use create the clusters via internal cluster API, in this case the SDX service does not know about these clusters,
        // so we need to check against legacy DL API from Core service
        if (datalakes.isEmpty()) {
            datalakes = environmentResourceDeletionService.getDatalakeClusterNames(env);
        }
        if (!datalakes.isEmpty()) {
            throw new BadRequestException(String.format("The following Data Lake cluster(s) must be terminated before Environment deletion [%s]",
                    String.join(", ", datalakes)));
        }
    }

    void validateDeletion(EnvironmentView environmentView, boolean cascading) {
        if (!cascading) {
            List<String> childEnvNames =
                    environmentViewService.findNameWithAccountIdAndParentEnvIdAndArchivedIsFalse(environmentView.getAccountId(), environmentView.getId());
            if (!childEnvNames.isEmpty()) {
                throw new BadRequestException(String.format("The following Environment(s) must be deleted before Environment deletion [%s]",
                        String.join(", ", childEnvNames)));
            }
        }
    }

}
