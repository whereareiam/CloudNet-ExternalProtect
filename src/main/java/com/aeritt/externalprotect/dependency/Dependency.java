package com.aeritt.externalprotect.dependency;

import lombok.Getter;

@Getter
public class Dependency {
	private final String groupId;
	private final String artifactId;
	private final String version;
	private final boolean parent;

	private String repositoryUrl;

	public Dependency(String groupId, String artifactId, String version, boolean parent) {
		this.groupId = groupId;
		this.artifactId = artifactId;
		this.version = version;
		this.parent = parent;
	}

	public Dependency repository(String url) {
		this.repositoryUrl = url;
		return this;
	}

	@Override
	public String toString() {
		return groupId + ":" + artifactId + ":" + version;
	}
}