package com.aeritt.externalprotect.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.ToString;

@Getter
@ToString
@AllArgsConstructor
public class BackendService {
	private final String serviceName;
	private final String host;
	private final int port;
}
