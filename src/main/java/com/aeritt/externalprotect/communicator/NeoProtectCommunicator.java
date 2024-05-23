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
		return getNeoProtectCredentials()
				.map(this::testConnection)
				.orElseGet(() -> {
					logger.severe("No token or serverId found for NeoProtect");
					return false;
				});
	}

	private boolean testConnection(Credentials credentials) {
		Authentication auth = new Authentication(AUTH_TYPE, credentials.getToken());
		String url = String.format(NEO_PROTECT_URL, "gameshields/" + credentials.getServerId() + "/backendGroups");

		HttpResponse<String> response = restHelper.get(url, auth);

		if (response.statusCode() != 200) {
			connected = false;
		} else {
			connected = handleBackendGroupsResponse(response, credentials, auth);
		}

		return connected;
	}

	private boolean handleBackendGroupsResponse(HttpResponse<String> response, Credentials credentials, Authentication auth) {
		boolean groupExists = false;

		try {
			JsonElement element = JsonParser.parseString(response.body());
			if (element.isJsonArray()) {
				JsonArray backendGroups = element.getAsJsonArray();
				groupExists = handleBackendGroups(backendGroups, credentials, auth);
			}
		} catch (Exception e) {
			logger.severe("Error parsing JSON response: " + e.getMessage());
			connected = false;
		}

		if (!groupExists) {
			createGroup(auth, credentials);
		}

		return connected;
	}

	private boolean handleBackendGroups(JsonArray backendGroups, Credentials credentials, Authentication auth) {
		boolean groupExists = false;

		for (JsonElement groupElement : backendGroups) {
			JsonObject group = groupElement.getAsJsonObject();
			String groupName = group.get("name").getAsString();

			if (GROUP_NAME.equals(groupName)) {
				groupExists = true;
			} else if (group.get("useDefault").getAsBoolean()) {
				deleteDefaultBackendGroup(group, credentials, auth);
			}
		}

		return groupExists;
	}

	private void deleteDefaultBackendGroup(JsonObject group, Credentials credentials, Authentication auth) {
		String groupId = group.get("id").getAsString();
		String deleteUrl = String.format(NEO_PROTECT_URL, "gameshields/" + credentials.getServerId() + "/backendGroups/" + groupId);
		HttpResponse<String> deleteResponse = restHelper.delete(deleteUrl, auth);

		if (deleteResponse.statusCode() != 200) {
			logger.severe("Failed to remove default backend group with ID: " + groupId);
		} else {
			logger.info("Successfully removed default backend group with ID: " + groupId);
		}
	}

	private void createGroup(Authentication auth, Credentials credentials) {
		String url = String.format(NEO_PROTECT_URL, "gameshields/" + credentials.getServerId() + "/backendGroups");
		String payload = getCreateGroupPayload();
		HttpResponse<String> response = restHelper.post(url, auth, payload);

		if (response.statusCode() != 200) {
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

	private Optional<Credentials> getNeoProtectCredentials() {
		return settingsConfig.getCredentials().stream()
				.filter(c -> c.getProtection().equals(Protection.NEOPROTECT))
				.findFirst();
	}

	private String getCloudNetGroupId(Credentials credentials, Authentication auth) {
		String url = String.format(NEO_PROTECT_URL, "gameshields/" + credentials.getServerId() + "/backendGroups");
		HttpResponse<String> response = restHelper.get(url, auth);
		return extractGroupId(response.body());
	}

	private String getBackendId(BackendService backendService, Credentials credentials, Authentication auth) {
		String url = String.format(NEO_PROTECT_URL, "gameshields/" + credentials.getServerId() + "/backendGroups");
		HttpResponse<String> response = restHelper.get(url, auth);

		try {
			JsonElement element = JsonParser.parseString(response.body());
			if (element.isJsonArray()) {
				JsonArray backendGroups = element.getAsJsonArray();
				for (JsonElement groupElement : backendGroups) {
					JsonObject group = groupElement.getAsJsonObject();
					if (GROUP_NAME.equals(group.get("name").getAsString())) {
						JsonArray backends = group.getAsJsonArray("backends");
						for (JsonElement backendElement : backends) {
							JsonObject backend = backendElement.getAsJsonObject();
							String host = backend.get("ipv4").getAsString();
							int port = backend.get("port").getAsInt();
							if (backendService.getHost().equals(host) && backendService.getPort() == port) {
								return backend.get("id").getAsString();
							}
						}
					}
				}
			}
		} catch (Exception e) {
			logger.severe("Error parsing JSON response: " + e.getMessage());
		}

		return null;
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

	@Override
	public void addBackend(BackendService backendService, boolean proxyProtocol) {
		logger.info("Adding backend for " + backendService.getServiceName() + " on NeoProtect");

		getNeoProtectCredentials()
				.filter(c -> c.getTasks().contains(backendService.getServiceName().split("-")[0]))
				.ifPresentOrElse(credentials -> addBackend(credentials, backendService),
						() -> logger.severe("No matching credentials found for " + backendService.getServiceName()));
	}

	private void addBackend(Credentials credentials, BackendService backendService) {
		Authentication auth = new Authentication(AUTH_TYPE, credentials.getToken());
		String cloudnetGroupId = getCloudNetGroupId(credentials, auth);

		if (cloudnetGroupId == null) {
			logger.severe("The cloudnet group does not exist, cannot add backend");
			return;
		}

		JsonObject backendData = new JsonObject();
		backendData.addProperty("host", backendService.getHost());
		backendData.addProperty("port", backendService.getPort());

		String url = String.format(NEO_PROTECT_URL, "gameshields/" + credentials.getServerId() + "/backendGroups/" + cloudnetGroupId + "/backends");
		HttpResponse<String> postResponse = restHelper.post(url, auth, backendData.toString());

		if (postResponse.statusCode() != 200) {
			logger.severe("Failed to add backend for " + backendService.getServiceName());
		}
	}

	@Override
	public void removeBackend(BackendService backendService) {
		logger.info("Removing backend for " + backendService.getServiceName() + " from NeoProtect");

		getNeoProtectCredentials()
				.filter(c -> c.getTasks().contains(backendService.getServiceName().split("-")[0]))
				.ifPresentOrElse(credentials -> removeBackend(credentials, backendService),
						() -> logger.severe("No matching credentials found for " + backendService.getServiceName()));
	}

	private void removeBackend(Credentials credentials, BackendService backendService) {
		Authentication auth = new Authentication(AUTH_TYPE, credentials.getToken());
		String cloudnetGroupId = getCloudNetGroupId(credentials, auth);

		if (cloudnetGroupId == null) {
			logger.severe("The cloudnet group does not exist, cannot remove backend");
			return;
		}

		String backendId = getBackendId(backendService, credentials, auth);
		if (backendId == null) {
			logger.severe("No matching backend found for " + backendService.getServiceName());
			return;
		}

		String deleteUrl = String.format(NEO_PROTECT_URL, "gameshields/" + credentials.getServerId() + "/backendGroups/" + cloudnetGroupId + "/backends/" + backendId);
		HttpResponse<String> deleteResponse = restHelper.delete(deleteUrl, auth);

		if (deleteResponse.statusCode() != 200) {
			logger.severe("Failed to remove backend for " + backendService.getServiceName());
		} else {
			logger.info("Successfully removed backend for " + backendService.getServiceName());
		}
	}

	@Override
	public void clearBackends() {
		logger.info("Clearing all backends from NeoProtect");

		getNeoProtectCredentials()
				.ifPresentOrElse(this::clearBackends,
						() -> logger.severe("No credentials found for NeoProtect"));
	}

	private void clearBackends(Credentials credentials) {
		Authentication auth = new Authentication(AUTH_TYPE, credentials.getToken());
		String backendGroupsUrl = String.format(NEO_PROTECT_URL, "gameshields/" + credentials.getServerId() + "/backendGroups");
		HttpResponse<String> backendGroupsResponse = restHelper.get(backendGroupsUrl, auth);

		if (backendGroupsResponse.statusCode() != 200) {
			logger.severe("Failed to retrieve backend groups");
			return;
		}

		try {
			JsonElement element = JsonParser.parseString(backendGroupsResponse.body());
			if (element.isJsonArray()) {
				JsonArray backendGroups = element.getAsJsonArray();
				for (JsonElement groupElement : backendGroups) {
					JsonObject group = groupElement.getAsJsonObject();
					if (GROUP_NAME.equals(group.get("name").getAsString())) {
						clearBackendsInGroup(group, credentials, auth);
					}
				}
			}
		} catch (Exception e) {
			logger.severe("Error parsing JSON response: " + e.getMessage());
		}
	}

	private void clearBackendsInGroup(JsonObject group, Credentials credentials, Authentication auth) {
		String groupId = group.get("id").getAsString();
		JsonArray backends = group.getAsJsonArray("backends");
		for (JsonElement backendElement : backends) {
			JsonObject backend = backendElement.getAsJsonObject();
			String backendId = backend.get("id").getAsString();
			String deleteUrl = String.format(NEO_PROTECT_URL, "gameshields/" + credentials.getServerId() + "/backendGroups/" + groupId + "/backends/" + backendId);
			HttpResponse<String> deleteResponse = restHelper.delete(deleteUrl, auth);

			if (deleteResponse.statusCode() != 200) {
				logger.severe("Failed to remove backend with ID: " + backendId);
			} else {
				logger.info("Successfully removed backend with ID: " + backendId);
			}
		}
	}
}
