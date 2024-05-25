package com.aeritt.externalprotect.dependency;

import com.saicone.ezlib.Ezlib;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

public class DependencyResolver {
	private final Ezlib ezLib;
	private final List<Dependency> dependencies;

	public DependencyResolver() {
		this.dependencies = new ArrayList<>();

		File file = new File("launcher/libs");
		ezLib = new Ezlib(file);
		ezLib.init();

		addDependencies();
	}

	public void resolveDependencies() {
		dependencies.forEach(d -> {
			if (d.getRepositoryUrl() != null) {
				ezLib.dependency(d.toString()).repository(d.getRepositoryUrl()).parent(d.isParent()).load();
			} else {
				ezLib.dependency(d.toString()).parent(d.isParent()).load();
			}
		});
	}

	public void addDependencies() {
		dependencies.add(new Dependency("aopalliance", "aopalliance", getVersion("aopalliance"), true));
		dependencies.add(new Dependency("com.google.inject", "guice", getVersion("guice"), true));
		dependencies.add(new Dependency("com.google.guava", "guava", getVersion("guava"), true));
		dependencies.add(new Dependency("com.google.code.gson", "gson", getVersion("gson"), true));
		dependencies.add(new Dependency("org.apache.commons", "commons-compress", getVersion("commons-compress"), true));
		dependencies.add(new Dependency("org.yaml", "snakeyaml", getVersion("snakeyaml"), true));
	}

	private String getVersion(String propertyKey) {
		Properties properties = new Properties();
		InputStream inputStream = getClass().getClassLoader().getResourceAsStream("dependencies");

		if (inputStream != null) {
			try {
				properties.load(inputStream);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		return properties.getProperty(propertyKey);
	}
}
