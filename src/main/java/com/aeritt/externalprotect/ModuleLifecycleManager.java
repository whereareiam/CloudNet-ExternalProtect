package com.aeritt.externalprotect;

import com.aeritt.externalprotect.inject.ModuleInjector;
import com.aeritt.externalprotect.listener.ListenerRegistrar;
import com.aeritt.externalprotect.service.ServiceManager;
import com.google.inject.Injector;
import com.google.inject.Singleton;

@Singleton
public class ModuleLifecycleManager {
	private static final Injector injector = ModuleInjector.getInjector();

	public static void onStart() {
		injector.getInstance(ListenerRegistrar.class).registerListeners();
		injector.getInstance(ServiceManager.class).init();
	}

	public static void onStop() {
		injector.getInstance(ServiceManager.class).clearBackends();
	}

	public static void onReload() {
		injector.getInstance(ServiceManager.class).clearBackends();
	}
}
