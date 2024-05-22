package com.aeritt.externalprotect.communicator;

import com.aeritt.externalprotect.config.setting.SettingsConfig;
import com.aeritt.externalprotect.model.BackendService;
import com.aeritt.externalprotect.model.Credentials;
import com.aeritt.externalprotect.model.Protection;
import com.aeritt.externalprotect.model.authentication.Authentication;
import com.aeritt.externalprotect.model.authentication.AuthenticationType;
import com.aeritt.externalprotect.rest.RestHelper;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import eu.cloudnetservice.common.log.Logger;

import java.net.http.HttpResponse;

@Singleton
public class NeoProtectCommunicator extends AbstractCommunicator {
	private final Logger logger;
	private final RestHelper restHelper;
	private final SettingsConfig settingsConfig;

	private final String NEO_PROTECT_URL = "https://api.neoprotect.net/v2/%s";
	private final AuthenticationType AUTH_TYPE = AuthenticationType.BEARER;

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

		String serverId = settingsConfig.getCredentials().stream()
				.filter(c -> c.getProtection().equals(Protection.NEOPROTECT))
				.findFirst()
				.map(Credentials::getServerId)
				.orElse(null);

		if (token == null || serverId == null) {
			logger.severe("No token or serverId found for NeoProtect");
			return false;
		}

		Authentication auth = new Authentication(AUTH_TYPE, token);
		String url = String.format(NEO_PROTECT_URL, "gameshields/" + serverId + "/backendGroups");

		HttpResponse<String> response = restHelper.get(url, auth);

		if (response.statusCode() != 200) return connected = false;
		boolean groupExists = response.body().contains("\"name\":\"cloudnet\"");

		if (!groupExists) {
			String createGroupPayload = "{"
					+ "\"name\":\"cloudnet\","
					+ "\"loadBalanceType\":\"RANDOM\","
					+ "\"proxyProtocol\":" + settingsConfig.isProxyProtocol() + ","
					+ "\"bedrock\":false,"
					+ "\"useDefault\":true,"
					+ "\"regionId\":4,"
					+ "\"nodeGroupIds\":[]"
					+ "}";

			HttpResponse<String> createResponse = restHelper.post(url, auth, createGroupPayload);

			if (createResponse.statusCode() != 200) connected = false;
		}

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
