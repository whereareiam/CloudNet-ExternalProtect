package com.aeritt.externalprotect.rest;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import eu.cloudnetservice.common.log.Logger;

import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;

@Singleton
public class RestHelper {
	private final Logger logger;

	@Inject
	public RestHelper(Logger logger) {
		this.logger = logger;
	}

	public boolean sendPostRequest(String url, String authType, String token, String requestBody) {
		boolean isConnected = false;

		try {
			URL endpoint = new URI(url).toURL();
			HttpURLConnection conn = (HttpURLConnection) endpoint.openConnection();
			conn.setRequestMethod("POST");
			conn.setRequestProperty("Content-Type", "application/json");
			conn.setRequestProperty("Authorization", authType + " " + token);
			conn.setDoOutput(true);

			OutputStream os = conn.getOutputStream();
			byte[] input = requestBody.getBytes(StandardCharsets.UTF_8);
			os.write(input, 0, input.length);

			int responseCode = conn.getResponseCode();
			isConnected = responseCode == HttpURLConnection.HTTP_OK;
		} catch (IOException ignored) {
		} catch (URISyntaxException e) {
			logger.severe("Invalid URL: " + url);
		}

		return isConnected;
	}
}
