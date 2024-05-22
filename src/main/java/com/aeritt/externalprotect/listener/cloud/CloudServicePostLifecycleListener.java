package com.aeritt.externalprotect.listener.cloud;

import com.aeritt.externalprotect.service.ServiceManager;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import eu.cloudnetservice.driver.event.EventListener;
import eu.cloudnetservice.driver.service.ServiceLifeCycle;
import eu.cloudnetservice.node.event.service.CloudServicePostLifecycleEvent;

@Singleton
public class CloudServicePostLifecycleListener {
	private final ServiceManager serviceManager;

	@Inject
	public CloudServicePostLifecycleListener(ServiceManager serviceManager) {
		this.serviceManager = serviceManager;
	}

	@EventListener
	public void handleCloudServicePostLifecycleEvent(CloudServicePostLifecycleEvent event) {
		String taskName = event.service().serviceId().taskName();

		if (event.newLifeCycle() == ServiceLifeCycle.RUNNING && serviceManager.getTrackedTasks().contains(taskName))
			serviceManager.addService(event.service());
		
		if (event.newLifeCycle() == ServiceLifeCycle.STOPPED && serviceManager.getTrackedTasks().contains(taskName))
			serviceManager.removeService(event.service());

	}
}
