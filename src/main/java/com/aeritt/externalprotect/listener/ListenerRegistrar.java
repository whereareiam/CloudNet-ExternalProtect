package com.aeritt.externalprotect.listener;

import com.aeritt.externalprotect.listener.cloud.CloudServicePostLifecycleListener;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Singleton;
import eu.cloudnetservice.driver.event.EventManager;

import java.util.List;

@Singleton
public class ListenerRegistrar {
	private final Injector injector;
	private final EventManager eventManager;

	@Inject
	public ListenerRegistrar(Injector injector, EventManager eventManager) {
		this.injector = injector;
		this.eventManager = eventManager;
	}

	public void registerListeners() {
		List<Object> listeners = List.of(
				injector.getInstance(CloudServicePostLifecycleListener.class)
		);

		listeners.forEach(eventManager::registerListener);
	}
}
