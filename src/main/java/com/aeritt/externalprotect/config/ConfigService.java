package com.aeritt.externalprotect.config;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

@Singleton
public class ConfigService {
	private final Path dataPath;
	private final Map<Class<?>, String[]> registeredConfigs = new HashMap<>();

	@Inject
	public ConfigService(@Named("dataPath") Path dataPath) {
		this.dataPath = dataPath;
	}

	public <T> T registerConfig(Class<T> configClass, String path, String fileName) {
		if (registeredConfigs.containsKey(configClass) || path == null || fileName == null) return null;

		ConfigLoader<T> configLoader = new ConfigLoader<>(dataPath);
		configLoader.load(configClass, path, fileName);
		T object = configLoader.getConfig();

		registeredConfigs.put(configClass, new String[]{path, fileName});

		return object;
	}
}

