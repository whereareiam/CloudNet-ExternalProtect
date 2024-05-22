package com.aeritt.externalprotect.rest;

import com.aeritt.externalprotect.model.authentication.Authentication;
import com.aeritt.externalprotect.model.authentication.AuthenticationType;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import eu.cloudnetservice.common.log.Logger;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

@Singleton
public class RestHelper {
	private final Logger logger;
	private final HttpClient httpClient;

	@Inject
	public RestHelper(Logger logger) {
		this.logger = logger;
		this.httpClient = HttpClient.newHttpClient();
	}

	public HttpResponse<String> get(String url, Authentication authentication) {
		HttpRequest request = HttpRequest.newBuilder()
				.uri(URI.create(url))
				.header("Authorization", authentication.getType() + " " + authentication.getToken())
				.header("Content-Type", "application/json")
				.GET()
				.build();

		return sendRequest(request);
	}

	public HttpResponse<String> post(String url, Authentication authentication, String payload) {
		HttpRequest request = HttpRequest.newBuilder()
				.uri(URI.create(url))
				.header("Authorization", authentication.getType() + " " + authentication.getToken())
				.header("Content-Type", "application/json")
				.POST(HttpRequest.BodyPublishers.ofString(payload))
				.build();

		return sendRequest(request);
	}

	public HttpResponse<String> delete(String url, Authentication authentication) {
		HttpRequest request = HttpRequest.newBuilder()
				.uri(URI.create(url))
				.header("Authorization", authentication.getType() + " " + authentication.getToken())
				.header("Content-Type", "application/json")
				.DELETE()
				.build();

		return sendRequest(request);
	}

	private HttpResponse<String> sendRequest(HttpRequest request) {
		HttpResponse<String> response;
		try {
			response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
		} catch (IOException | InterruptedException e) {
			logger.severe("An error occurred while sending a request: " + e.getMessage());
			response = null;
		}

		return response;
	}
}
