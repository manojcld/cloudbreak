{%- from 'telemetry/settings.sls' import telemetry with context %}
{% if salt['pillar.get']('cloudera-manager:paywall_username') %}
  {% set paywall_username = salt['pillar.get']('cloudera-manager:paywall_username') %}
  {% set paywall_password = salt['pillar.get']('cloudera-manager:paywall_password') %}
  {% set curl_cmd = 'curl --max-time 30 -s -k -f -u $PAYWALL_USERNAME:$PAYWALL_PASSWORD ' + telemetry.repoGpgKey %}
{% else %}
  {% set paywall_username = None %}
  {% set paywall_password = None %}
  {% set curl_cmd = 'curl --max-time 30 -s -k -f ' + telemetry.repoGpgKey %}
{% endif %}
/etc/yum.repos.d/cdp-infra-tools.repo:
  file.managed:
    - source: salt://telemetry/template/cdp-infra-tools.repo.j2
    - template: jinja
    - mode: 640
    - context:
         repoName: "{{ telemetry.repoName }}"
         repoBaseUrl: "{{ telemetry.repoBaseUrl }}"
         repoGpgKey: "{{ telemetry.repoGpgKey }}"
         repoGpgCheck: {{ telemetry.repoGpgCheck }}

/opt/salt/scripts/cdp-telemetry-deployer.sh:
    file.managed:
        - source: salt://telemetry/scripts/cdp-telemetry-deployer.sh
        - template: jinja
        - mode: '0750'{% if telemetry.desiredCdpTelemetryVersion or telemetry.desiredCdpLoggingAgentVersion %}
upgrade_cdp_infra_tools_components:
    cmd.run:
        - names:{% if telemetry.desiredCdpTelemetryVersion %}
          - '/opt/salt/scripts/cdp-telemetry-deployer.sh upgrade -c cdp-telemetry -v {{ telemetry.desiredCdpTelemetryVersion }};exit 0'{% endif %}{% if telemetry.desiredCdpLoggingAgentVersion %}
          - '/opt/salt/scripts/cdp-telemetry-deployer.sh upgrade -c cdp-logging-agent -v {{ telemetry.desiredCdpLoggingAgentVersion }};exit 0'{% endif %}
        - onlyif: "{{ curl_cmd }} > /dev/null"{% if telemetry.proxyUrl or paywall_username %}
        - env:{% if telemetry.proxyUrl %}
          - https_proxy: {{ telemetry.proxyUrl }}{% if telemetry.noProxyHosts %}
          - no_proxy: {{ telemetry.noProxyHosts }}{% endif %}{% if paywall_username %}
          - PAYWALL_USERNAME: {{ paywall_username }}
          - PAYWALL_PASSWORD: {{ paywall_password }}{% endif %}{% endif %}{% endif %}{% endif %}