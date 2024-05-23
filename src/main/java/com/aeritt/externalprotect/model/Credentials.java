package com.aeritt.externalprotect.model;

import com.aeritt.externalprotect.config.setting.AddressSettingsConfig;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.util.List;

@Getter
@Setter
@ToString
public class Credentials {
	private Protection protection;
	private String serverId;
	private String token;
	private AddressSettingsConfig address = new AddressSettingsConfig();
	private List<String> tasks;
}
