package com.aeritt.externalprotect.communicator;

import com.aeritt.externalprotect.model.BackendService;
import lombok.Getter;

@Getter
public abstract class AbstractCommunicator {
	protected boolean connected = true;

	public abstract boolean testConnection();

	public abstract void addBackend(BackendService backendService, boolean proxyProtocol);

	public abstract void removeBackend(BackendService backendService);

	public abstract void clearBackends();
}
