package com.sequenceiq.authorization.resource;

import static com.sequenceiq.authorization.resource.AuthorizationResourceAction.ADMIN_FREEIPA;
import static com.sequenceiq.authorization.resource.AuthorizationResourceAction.REGISTER_DATABASE;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Component;

@Component
public class AlternativeAuthorizationResourceActionProvider {

    private static final Map<AuthorizationResourceAction, List<AuthorizationResourceAction>> ALTERNATIVES;

    static {
        ALTERNATIVES = Map.of(
                ADMIN_FREEIPA, List.of(REGISTER_DATABASE)
        );
    }

    public List<AuthorizationResourceAction> getAlternatives(AuthorizationResourceAction action) {
        if (action != null && ALTERNATIVES.containsKey(action)) {
            return ALTERNATIVES.get(action);
        }
        return Collections.emptyList();
    }

}
