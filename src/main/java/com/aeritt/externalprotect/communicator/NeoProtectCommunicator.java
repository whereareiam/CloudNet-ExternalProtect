package com.aeritt.externalprotect.communicator;

import com.aeritt.externalprotect.config.setting.SettingsConfig;
import com.aeritt.externalprotect.model.BackendService;
import com.aeritt.externalprotect.model.Credentials;
import com.aeritt.externalprotect.model.Protection;
import com.aeritt.externalprotect.model.authentication.Authentication;
import com.aeritt.externalprotect.model.authentication.AuthenticationType;
import com.aeritt.externalprotect.rest.RestHelper;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import eu.cloudnetservice.common.log.Logger;

import java.net.http.HttpResponse;
import java.util.List;
import java.util.Optional;

@Singleton
public class NeoProtectCommunicator extends AbstractCommunicator {
	private final Logger logger;
	private final RestHelper restHelper;
	private final SettingsConfig settingsConfig;

	private final String NEO_PROTECT_URL = "https://api.neoprotect.net/v2/%s";
	private final AuthenticationType AUTH_TYPE = AuthenticationType.BEARER;
	private final String GROUP_NAME = "cloudnet";

	@Inject
	public NeoProtectCommunicator(Logger logger, RestHelper restHelper, SettingsConfig settingsConfig) {
		this.logger = logger;
		this.restHelper = restHelper;
		this.settingsConfig = settingsConfig;
	}

	@Override
	public boolean testConnection() {
		Optional<Credentials> credentials = getNeoProtectCredentials();

		if (credentials.isEmpty()) {
			logger.severe("No token or serverId found for NeoProtect");
			return false;
		}

		Authentication auth = new Authentication(AUTH_TYPE, credentials.get().getToken());
		String url = String.format(NEO_PROTECT_URL, "gameshields/" + credentials.get().getServerId() + "/backendGroups");

		HttpResponse<String> response = restHelper.get(url, auth);

		if (response.statusCode() != 200) {
			return connected = false;
		}

		boolean groupExists = response.body().contains("\"name\":\"" + GROUP_NAME + "\"");

		if (!groupExists) {
			createGroup(auth, url);
		}

		return connected;
	}

	private void createGroup(Authentication auth, String url) {
		String createGroupPayload = getCreateGroupPayload();
		HttpResponse<String> createResponse = restHelper.post(url, auth, createGroupPayload);

		if (createResponse.statusCode() != 200) {
			connected = false;
		}
	}

	private String getCreateGroupPayload() {
		JsonObject createGroupPayload = new JsonObject();
		createGroupPayload.addProperty("name", GROUP_NAME);
		createGroupPayload.addProperty("loadBalanceType", "RANDOM");
		createGroupPayload.addProperty("proxyProtocol", settingsConfig.isProxyProtocol());
		createGroupPayload.addProperty("bedrock", false);
		createGroupPayload.addProperty("useDefault", true);
		createGroupPayload.addProperty("regionId", 4);
		createGroupPayload.add("nodeGroupIds", new JsonArray());

		return createGroupPayload.toString();
	}

	@Override
	public void addBackend(BackendService backendService, boolean proxyProtocol) {
		logger.info("Adding backend for " + backendService.getServiceName() + " on NeoProtect");

		Optional<Credentials> credentials = getNeoProtectCredentials()
				.filter(c -> c.getTasks().contains(backendService.getServiceName().split("-")[0]));

		if (credentials.isEmpty()) {
			logger.severe("No matching credentials found for " + backendService.getServiceName());
			return;
		}

		Authentication auth = new Authentication(AUTH_TYPE, credentials.get().getToken());

		String cloudnetGroupId = getCloudnetGroupId(credentials.get(), auth);
		if (cloudnetGroupId == null) {
			logger.severe("The cloudnet group does not exist, cannot add backend");
			return;
		}

		JsonObject backendData = new JsonObject();
		backendData.addProperty("host", backendService.getHost());
		backendData.addProperty("port", backendService.getPort());

		String postUrl = String.format(NEO_PROTECT_URL, "gameshields/" + credentials.get().getServerId() + "/backendGroups/" + cloudnetGroupId + "/backends");
		HttpResponse<String> postResponse = restHelper.post(postUrl, auth, backendData.toString());

		if (postResponse.statusCode() != 200) {
			logger.severe("Failed to add backend for " + backendService.getServiceName());
		}
	}

	private Optional<Credentials> getNeoProtectCredentials() {
		return settingsConfig.getCredentials().stream()
				.filter(c -> c.getProtection().equals(Protection.NEOPROTECT))
				.findFirst();
	}

	private String getCloudnetGroupId(Credentials credentials, Authentication auth) {
		String url = String.format(NEO_PROTECT_URL, "gameshields/" + credentials.getServerId() + "/backendGroups");
		HttpResponse<String> response = restHelper.get(url, auth);
		return extractGroupId(response.body());
	}

	@Override
	public void removeBackend(BackendService backendService) {
		logger.info("Removing backend for " + backendService.getServiceName() + " from NeoProtect");

		Optional<Credentials> credentials = getNeoProtectCredentials();
		if (credentials.isEmpty()) {
			logger.severe("No credentials found for NeoProtect");
			return;
		}

		Authentication auth = new Authentication(AUTH_TYPE, credentials.get().getToken());

		String cloudnetGroupId = getCloudnetGroupId(credentials.get(), auth);
		if (cloudnetGroupId == null) {
			logger.severe("The cloudnet group does not exist, cannot remove backend");
			return;
		}

		String backendsUrl = String.format(NEO_PROTECT_URL, "gameshields/" + credentials.get().getServerId() + "/backendGroups/" + cloudnetGroupId + "/backends");
		HttpResponse<String> backendsResponse = restHelper.get(backendsUrl, auth);

		if (backendsResponse.statusCode() != 200) {
			logger.severe("Failed to retrieve backends for the cloudnet group");
			return;
		}

		String backendId = extractBackendId(backendsResponse.body(), backendService);

		if (backendId == null) {
			logger.info("Backend not found for " + backendService.getServiceName());
			return;
		}

		String deleteUrl = String.format(NEO_PROTECT_URL, "gameshields/" + credentials.get().getServerId() + "/backendGroups/" + cloudnetGroupId + "/backends/" + backendId);
		HttpResponse<String> deleteResponse = restHelper.delete(deleteUrl, auth);

		if (deleteResponse.statusCode() != 200) logger.severe("Failed to remove backend for " + backendService.getServiceName());
	}

	private String extractBackendId(String responseBody, BackendService backendService) {
		try {
			JsonElement element = JsonParser.parseString(responseBody);
			if (element.isJsonArray()) {
				JsonArray backends = element.getAsJsonArray();
				for (JsonElement backendElement : backends) {
					JsonObject backend = backendElement.getAsJsonObject();
					String host = backend.get("ipv4").getAsString();
					int port = backend.get("port").getAsInt();
					if (backendService.getHost().equals(host) && backendService.getPort() == port) {
						return backend.get("id").getAsString();
					}
				}
			}
		} catch (Exception e) {
			logger.severe("Error parsing JSON response: " + e.getMessage());
		}
		return null;
	}

	@Override
	public void clearBackends() {
		String serverId = settingsConfig.getCredentials().stream()
				.filter(c -> c.getProtection().equals(Protection.NEOPROTECT))
				.findFirst()
				.map(Credentials::getServerId)
				.orElse(null);

		if (serverId == null) {
			logger.severe("No serverId found for NeoProtect");
			return;
		}

		String token = settingsConfig.getCredentials().stream()
				.filter(c -> c.getProtection().equals(Protection.NEOPROTECT))
				.findFirst()
				.map(Credentials::getToken)
				.orElse(null);
		Authentication auth = new Authentication(AUTH_TYPE, token);

		String url = String.format(NEO_PROTECT_URL, "gameshields/" + serverId + "/backendGroups");

		HttpResponse<String> response = restHelper.get(url, auth);

		if (response.statusCode() != 200) {
			logger.severe("Failed to retrieve backend groups");
			return;
		}

		String cloudnetGroupId = extractGroupId(response.body());

		if (cloudnetGroupId == null) {
			logger.info("The "+ GROUP_NAME + " group does not exist, no backends to clear");
			return;
		}

		String deleteUrl = String.format(NEO_PROTECT_URL, "gameshields/" + serverId + "/backendGroups/" + cloudnetGroupId);

		HttpResponse<String> deleteResponse = restHelper.delete(deleteUrl, auth);

		if (deleteResponse.statusCode() != 200) logger.severe("Failed to clear the cloudnet group backends");
	}

	private String extractGroupId(String responseBody) {
		try {
			JsonElement element = JsonParser.parseString(responseBody);

			if (element.isJsonArray()) {
				JsonArray groups = element.getAsJsonArray();

				for (JsonElement groupElement : groups) {
					JsonObject group = groupElement.getAsJsonObject();

					if (GROUP_NAME.equals(group.get("name").getAsString())) {
						return group.get("id").getAsString();
					}
				}
			}
		} catch (Exception e) {
			logger.severe("Error parsing JSON response: " + e.getMessage());
		}

		return null;
	}
}
