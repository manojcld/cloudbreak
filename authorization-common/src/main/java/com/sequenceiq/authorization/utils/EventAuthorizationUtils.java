package com.sequenceiq.authorization.utils;

import static com.sequenceiq.cloudbreak.util.NullUtil.throwIfNull;

import java.util.Arrays;
import java.util.Collection;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;

import com.sequenceiq.authorization.resource.AlternativeAuthorizationResourceActionProvider;
import com.sequenceiq.authorization.resource.AuthorizationResourceAction;
import com.sequenceiq.authorization.service.CommonPermissionCheckingUtils;
import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;

@Component
public class EventAuthorizationUtils {

    private static final Logger LOGGER = LoggerFactory.getLogger(EventAuthorizationUtils.class);

    private AlternativeAuthorizationResourceActionProvider actionAlternativeProvider;

    private CommonPermissionCheckingUtils permissionCheckingUtils;

    public EventAuthorizationUtils(CommonPermissionCheckingUtils permissionCheckingUtils,
            AlternativeAuthorizationResourceActionProvider actionAlternativeProvider) {
        this.actionAlternativeProvider = actionAlternativeProvider;
        this.permissionCheckingUtils = permissionCheckingUtils;
    }

    public void checkPermissionBasedOnResourceTypeAndCrn(Collection<EventAuthorizationDto> eventAuthorizationDtos) {
        throwIfNull(eventAuthorizationDtos,
                () -> new IllegalArgumentException("The collection of " + EventAuthorizationDto.class.getSimpleName() + "s should not be null!"));
        LOGGER.info("Checking permissions for events: {}",
                String.join(",", eventAuthorizationDtos.stream().map(EventAuthorizationDto::toString).collect(Collectors.toSet())));
        for (EventAuthorizationDto dto : eventAuthorizationDtos) {
            String resourceType = dto.getResourceType();
            Arrays.asList(AuthorizationResourceAction.values()).stream()
                    .filter(action -> isResourceTypeHasDescribeOrGetAction(action, resourceType))
                    .findFirst()
                    .ifPresentOrElse(
                            action -> checkPermissionForResource(action, dto.getResourceCrn()),
                            () -> throwIllegalStateExceptionForResourceType(resourceType)
                    );
        }
    }

    private boolean isResourceTypeHasDescribeOrGetAction(AuthorizationResourceAction action, String resourceType) {
        return (action.isDescribeAction() || action.isGetAction() || action.isAdminAction())
                && action.name().contains(resourceType.toUpperCase());
    }

    private void checkPermissionForResource(AuthorizationResourceAction action, String resourceCrn) {
        boolean specialActionHasPermission = checkSpecialActionHasPermission(action, resourceCrn);
        if (!specialActionHasPermission) {
            checkPermission(action, resourceCrn);
        }
    }

    private boolean checkSpecialActionHasPermission(AuthorizationResourceAction action, String resourceCrn) {
        for (AuthorizationResourceAction alternative : actionAlternativeProvider.getAlternatives(action)) {
            try {
                checkPermission(alternative, resourceCrn);
                return true;
            } catch (AccessDeniedException ade) {
                LOGGER.info(AccessDeniedException.class.getSimpleName() + " has caught during - alternative - permission check for " +
                                "[action: {}, resourceCrn: {}]", action.name(), resourceCrn);
            }
        }
        return false;
    }

    private void checkPermission(AuthorizationResourceAction action, String resourceCrn) {
        permissionCheckingUtils.checkPermissionForUserOnResource(
                action,
                ThreadBasedUserCrnProvider.getUserCrn(),
                resourceCrn);
    }

    private void throwIllegalStateExceptionForResourceType(String resourceType) {
        throw new IllegalStateException("Unable to find AuthZ action for resource: " + resourceType);
    }

}
