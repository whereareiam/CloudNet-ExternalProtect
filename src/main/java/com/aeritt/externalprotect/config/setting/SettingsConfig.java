package com.aeritt.externalprotect.config.setting;

import com.aeritt.externalprotect.model.Credentials;
import com.google.inject.Singleton;
import lombok.Getter;
import lombok.ToString;

import java.util.ArrayList;
import java.util.List;

@Singleton
@Getter
@ToString
public class SettingsConfig {
	private boolean proxyProtocol = true;
	private List<Credentials> credentials = new ArrayList<>();
}
