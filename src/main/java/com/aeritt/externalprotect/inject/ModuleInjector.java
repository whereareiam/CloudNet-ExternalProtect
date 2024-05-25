package com.aeritt.externalprotect.inject;

import com.google.inject.Guice;
import com.google.inject.Injector;
import eu.cloudnetservice.driver.event.EventManager;
import eu.cloudnetservice.node.command.CommandProvider;
import lombok.Getter;

public class ModuleInjector {
	@Getter
	private static Injector injector;

	public ModuleInjector(CommandProvider commandProvider, EventManager eventManager) {
		injector = Guice.createInjector(new ModuleInjectorConfiguration(commandProvider, eventManager));
	}
}
