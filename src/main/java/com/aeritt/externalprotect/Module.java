package com.aeritt.externalprotect;

import com.aeritt.externalprotect.dependency.DependencyResolver;
import com.aeritt.externalprotect.inject.ModuleInjector;
import eu.cloudnetservice.driver.event.EventManager;
import eu.cloudnetservice.driver.module.ModuleLifeCycle;
import eu.cloudnetservice.driver.module.ModuleTask;
import eu.cloudnetservice.driver.module.driver.DriverModule;
import eu.cloudnetservice.node.command.CommandProvider;

public final class Module extends DriverModule {

	@ModuleTask(lifecycle = ModuleLifeCycle.LOADED)
	public void onLoad(CommandProvider commandProvider, EventManager eventManager) {
		DependencyResolver dependencyResolver = new DependencyResolver();
		dependencyResolver.resolveDependencies();

		new ModuleInjector(commandProvider, eventManager);
	}

	@ModuleTask(lifecycle = ModuleLifeCycle.STARTED)
	public void onStart() {
		ModuleLifecycleManager.onStart();
	}

	@ModuleTask(lifecycle = ModuleLifeCycle.STOPPED)
	public void onStop() {
		ModuleLifecycleManager.onStop();
	}

	@ModuleTask(lifecycle = ModuleLifeCycle.RELOADING)
	public void onReload() {
		ModuleLifecycleManager.onReload();
	}
}