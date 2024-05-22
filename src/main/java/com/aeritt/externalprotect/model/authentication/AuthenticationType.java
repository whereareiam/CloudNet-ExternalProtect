package com.aeritt.externalprotect.model.authentication;

public enum AuthenticationType {
	X_API_KEY("X-API-KEY"),
	BEARER("Bearer");

	AuthenticationType(String authType) {
	}
}
