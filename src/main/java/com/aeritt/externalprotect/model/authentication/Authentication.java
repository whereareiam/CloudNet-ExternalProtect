package com.aeritt.externalprotect.model.authentication;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.ToString;

@Getter
@ToString
@AllArgsConstructor
public class Authentication {
	private final AuthenticationType type;
	private final String token;
}
