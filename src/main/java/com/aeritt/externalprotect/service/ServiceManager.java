package com.aeritt.externalprotect.service;

import com.aeritt.externalprotect.communicator.AbstractCommunicator;
import com.aeritt.externalprotect.communicator.NeoProtectCommunicator;
import com.aeritt.externalprotect.communicator.TCPShieldCommunicator;
import com.aeritt.externalprotect.config.setting.SettingsConfig;
import com.aeritt.externalprotect.model.BackendService;
import com.aeritt.externalprotect.model.Credentials;
import com.aeritt.externalprotect.model.Protection;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import eu.cloudnetservice.common.log.Logger;
import eu.cloudnetservice.node.service.CloudService;
import lombok.Getter;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Singleton
public class ServiceManager {
	private final Logger logger;
	private final SettingsConfig settingsConfig;

	@Getter
	private final List<String> trackedTasks;
	private final Set<AbstractCommunicator> communicators;

	private final Map<Protection, List<BackendService>> backendServices = Map.of(
			Protection.NEOPROTECT, new ArrayList<>(),
			Protection.TCPSHIELD, new ArrayList<>()
	);

	@Inject
	public ServiceManager(Logger logger, SettingsConfig settingsConfig, Set<AbstractCommunicator> communicators) {
		this.logger = logger;
		this.settingsConfig = settingsConfig;
		this.trackedTasks = settingsConfig.getCredentials().stream().map(Credentials::getTasks).flatMap(List::stream).distinct().toList();
		this.communicators = communicators;
	}

	public void init() {
		communicators.forEach(c -> {
			boolean successful = c.testConnection();
			if (!successful) logger.warning("Failed to connect to " + c.getClass().getSimpleName());
			else logger.info("Connected to " + c.getClass().getSimpleName());
		});

		communicators.forEach(AbstractCommunicator::clearBackends);
	}

	public synchronized void addService(CloudService cloudService) {
		Credentials credentials = settingsConfig.getCredentials().stream()
				.filter(c -> c.getTasks().contains(cloudService.serviceId().taskName()))
				.findFirst().orElse(null);

		if (credentials == null) return;

		BackendService backendService = createBackendService(cloudService, credentials);
		AbstractCommunicator communicator = getCommunicator(credentials.getProtection());
		if (!communicator.isConnected()) return;

		communicator.addBackend(backendService, settingsConfig.isProxyProtocol());
		backendServices.get(credentials.getProtection()).add(backendService);
	}

	public synchronized void removeService(CloudService cloudService) {
		Credentials credentials = settingsConfig.getCredentials().stream()
				.filter(c -> c.getTasks().contains(cloudService.serviceId().taskName()))
				.findFirst().orElse(null);

		if (credentials == null) return;

		BackendService backendService = getBackendService(cloudService, credentials.getProtection());
		if (backendService == null) return;

		AbstractCommunicator communicator = getCommunicator(credentials.getProtection());
		if (!communicator.isConnected()) return;

		communicator.removeBackend(backendService);
		backendServices.get(credentials.getProtection()).remove(backendService);
	}

	private BackendService createBackendService(CloudService cloudService, Credentials credentials) {
		String serviceName = cloudService.serviceId().name();
		String host = credentials.getAddress().isUseCustomAddress() ? credentials.getAddress().getCustomAddress() : cloudService.serviceInfo().address().host();
		int port = cloudService.serviceInfo().address().port();

		return new BackendService(serviceName, host, port);
	}

	private BackendService getBackendService(CloudService cloudService, Protection protectionType) {
		return backendServices.get(protectionType).stream()
				.filter(b -> b.getServiceName().equals(cloudService.serviceId().name()))
				.findFirst().orElse(null);
	}

	private AbstractCommunicator getCommunicator(Protection protectionType) {
		return switch (protectionType) {
			case NEOPROTECT -> communicators.stream()
					.filter(c -> c instanceof NeoProtectCommunicator)
					.findFirst().orElse(null);
			case TCPSHIELD -> communicators.stream()
					.filter(c -> c instanceof TCPShieldCommunicator)
					.findFirst().orElse(null);
		};
	}

	public synchronized void clearBackends() {
		communicators.forEach(AbstractCommunicator::clearBackends);
		backendServices.values().forEach(List::clear);
	}
}
