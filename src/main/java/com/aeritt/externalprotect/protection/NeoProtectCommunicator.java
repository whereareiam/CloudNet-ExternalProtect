package com.aeritt.externalprotect.protection;

import com.aeritt.externalprotect.config.setting.SettingsConfig;
import com.aeritt.externalprotect.model.BackendService;
import com.aeritt.externalprotect.model.Credentials;
import com.aeritt.externalprotect.model.Protection;
import com.aeritt.externalprotect.rest.RestHelper;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import eu.cloudnetservice.common.log.Logger;

@Singleton
public class NeoProtectCommunicator extends AbstractCommunicator {
	private final Logger logger;
	private final RestHelper restHelper;
	private final SettingsConfig settingsConfig;

	@Inject
	public NeoProtectCommunicator(Logger logger, RestHelper restHelper, SettingsConfig settingsConfig) {
		this.logger = logger;
		this.restHelper = restHelper;
		this.settingsConfig = settingsConfig;
	}

	@Override
	public boolean testConnection() {
		String token = settingsConfig.getCredentials().stream()
				.filter(c -> c.getProtection().equals(Protection.NEOPROTECT))
				.findFirst()
				.map(Credentials::getToken)
				.orElse(null);

		if (token == null) {
			logger.severe("No token found for NeoProtect");
			return false;
		}

		String requestBody = "{\"host\":\"127.0.0.1\",\"port\":25565}";
		connected = restHelper.sendPostRequest("https://api.neoprotect.net/v2/gameshield/backends/available", "Bearer", token, requestBody);

		return connected;
	}

	@Override
	public void addBackend(BackendService backendService, boolean proxyProtocol) {
		logger.info("Adding backend for " + backendService.getServiceName() + " on NeoProtect");
	}

	@Override
	public void removeBackend(BackendService backendService) {

	}

	@Override
	public void clearBackends() {
	}
}
