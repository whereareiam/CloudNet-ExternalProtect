package com.aeritt.externalprotect.model.authentication;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum AuthenticationType {
	X_API_KEY("X-API-KEY"),
	BEARER("Bearer");

	private final String authType;
}
