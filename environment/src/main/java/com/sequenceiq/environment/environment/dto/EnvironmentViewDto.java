package com.sequenceiq.environment.environment.dto;

import com.sequenceiq.environment.credential.domain.CredentialView;
import com.sequenceiq.environment.proxy.domain.ProxyConfigView;

public class EnvironmentViewDto extends EnvironmentDtoBase {

    private CredentialView credentialView;

    private ProxyConfigView proxyConfig;

    public CredentialView getCredentialView() {
        return credentialView;
    }

    public void setCredentialView(CredentialView credentialView) {
        this.credentialView = credentialView;
    }

    public ProxyConfigView getProxyConfig() {
        return proxyConfig;
    }

    public void setProxyConfig(ProxyConfigView proxyConfig) {
        this.proxyConfig = proxyConfig;
    }

    public static EnvironmentViewDtoBuilder builder() {
        return new EnvironmentViewDtoBuilder();
    }

    public static final class EnvironmentViewDtoBuilder extends EnvironmentDtoBaseBuilder<EnvironmentViewDto, EnvironmentViewDtoBuilder> {

        private CredentialView credentialView;

        private ProxyConfigView proxyConfig;

//        private EnvironmentDtoBaseBuilder<EnvironmentViewDto> baseBuilder;

        public EnvironmentViewDtoBuilder withCredentialView(CredentialView credential) {
            this.credentialView = credential;
            return this;
        }

        public EnvironmentViewDtoBuilder withProxyConfig(ProxyConfigView proxyConfig) {
            this.proxyConfig = proxyConfig;
            return this;
        }

//        public EnvironmentViewDtoBuilder withBaseBuilder(EnvironmentDtoBaseBuilder<EnvironmentViewDto> builder) {
//            this.baseBuilder = builder;
//            return this;
//        }

        @Override
        public EnvironmentViewDto build() {
            EnvironmentViewDto environmentDto = new EnvironmentViewDto();
            environmentDto.setCredentialView(credentialView);
            environmentDto.setProxyConfig(proxyConfig);
            super.build(environmentDto);
//            baseBuilder.build(build(environmentDto));
            return environmentDto;
        }
    }
}
