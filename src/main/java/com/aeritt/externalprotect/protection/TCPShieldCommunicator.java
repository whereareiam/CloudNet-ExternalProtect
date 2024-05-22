package com.aeritt.externalprotect.protection;

import com.aeritt.externalprotect.model.BackendService;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import eu.cloudnetservice.common.log.Logger;

@Singleton
public class TCPShieldCommunicator extends AbstractCommunicator {
	private final Logger logger;

	@Inject
	public TCPShieldCommunicator(Logger logger) {
		this.logger = logger;
	}

	@Override
	public boolean testConnection() {
		// TODO: Implement
		return false;
	}

	@Override
	public void addBackend(BackendService backendService, boolean proxyProtocol) {
		logger.info("Adding backend for " + backendService.getServiceName() + " on TCPShield");

		// TODO: Implement
	}

	@Override
	public void removeBackend(BackendService backendService) {
		// TODO: Implement
	}

	@Override
	public void clearBackends() {
		// TODO: Implement
	}
}
