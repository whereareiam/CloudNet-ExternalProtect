package com.aeritt.externalprotect;

import com.aeritt.externalprotect.listener.ListenerRegistrar;
import com.aeritt.externalprotect.service.ServiceManager;
import com.google.inject.Guice;
import com.google.inject.Injector;
import eu.cloudnetservice.driver.event.EventManager;
import eu.cloudnetservice.driver.module.ModuleLifeCycle;
import eu.cloudnetservice.driver.module.ModuleTask;
import eu.cloudnetservice.driver.module.driver.DriverModule;
import eu.cloudnetservice.node.command.CommandProvider;

public final class Module extends DriverModule {
	private Injector injector;

	@ModuleTask(lifecycle = ModuleLifeCycle.LOADED)
	public void onLoad(CommandProvider commandProvider, EventManager eventManager) {
		injector = Guice.createInjector(new ModuleConfig(commandProvider, eventManager));
	}

	@ModuleTask(lifecycle = ModuleLifeCycle.STARTED)
	public void onStart() {
		injector.getInstance(ListenerRegistrar.class).registerListeners();
		injector.getInstance(ServiceManager.class).init();
	}
}
