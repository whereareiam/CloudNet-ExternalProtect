package com.aeritt.externalprotect.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.util.List;

@Getter
@Setter
@ToString
@AllArgsConstructor
public class Credentials {
	private Protection protection;
	private String serverId;
	private String token;
	private List<String> tasks;
}
