package com.aeritt.externalprotect;

import com.aeritt.externalprotect.config.ConfigService;
import com.aeritt.externalprotect.config.setting.SettingsConfig;
import com.aeritt.externalprotect.communicator.AbstractCommunicator;
import com.aeritt.externalprotect.communicator.NeoProtectCommunicator;
import com.google.inject.AbstractModule;
import com.google.inject.Singleton;
import com.google.inject.multibindings.Multibinder;
import eu.cloudnetservice.common.log.LogManager;
import eu.cloudnetservice.common.log.Logger;
import eu.cloudnetservice.driver.event.EventManager;
import eu.cloudnetservice.node.command.CommandProvider;

import java.nio.file.Files;
import java.nio.file.Path;

@Singleton
public class ModuleConfig extends AbstractModule {
	private final CommandProvider commandProvider;
	private final EventManager eventManager;

	private final Path dataPath = Path.of("modules/CloudNet-ExternalProtect");

	private final Logger logger = LogManager.logger(Module.class);

	public ModuleConfig(CommandProvider commandProvider, EventManager eventManager) {
		this.commandProvider = commandProvider;
		this.eventManager = eventManager;
	}

	@Override
	protected void configure() {
		bind(Logger.class).toInstance(logger);
		bind(CommandProvider.class).toInstance(commandProvider);
		bind(EventManager.class).toInstance(eventManager);

		Multibinder<AbstractCommunicator> protectionCommunicators = Multibinder.newSetBinder(binder(), AbstractCommunicator.class);
		protectionCommunicators.addBinding().to(NeoProtectCommunicator.class);

		configureWorkingDirectory();
		configureConfig();
	}

	private void configureWorkingDirectory() {
		try {
			Files.createDirectories(dataPath);
		} catch (Exception exception) {
			throw new RuntimeException("Exception while creating data directory", exception);
		}
	}

	private void configureConfig() {
		ConfigService configService = new ConfigService(dataPath);

		bind(SettingsConfig.class).toInstance(configService.registerConfig(SettingsConfig.class, "", "settings.json"));
	}
}
