package com.aeritt.externalprotect.protection;

import com.aeritt.externalprotect.model.BackendService;
import lombok.Getter;

@Getter
public abstract class AbstractCommunicator {
	protected boolean connected = false;

	public abstract boolean testConnection();

	public abstract void addBackend(BackendService backendService, boolean proxyProtocol);

	public abstract void removeBackend(BackendService backendService);

	public abstract void clearBackends();
}
