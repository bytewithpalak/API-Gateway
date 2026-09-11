package com.gateway.api_gateway.service;

import org.springframework.stereotype.Service;

import java.net.InetAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.UnknownHostException;

// Direct implementation of the proposal's "Proxy abuse" risk mitigation:
// "permit forwarding only to targets registered by an authenticated owner,
// and validate target URLs on registration."
//
// This blocks the classic SSRF trick of registering a "target API" that's
// actually localhost, a private-network address, or a cloud metadata
// endpoint, which would turn the gateway into an open proxy into internal
// infrastructure.
@Service
public class UrlValidationService {

    public void validateOrThrow(String rawUrl) {
        URI uri;
        try {
            uri = new URI(rawUrl);
        } catch (URISyntaxException e) {
            throw new IllegalArgumentException("Malformed target URL: " + rawUrl);
        }

        if (uri.getScheme() == null || !(uri.getScheme().equals("http") || uri.getScheme().equals("https"))) {
            throw new IllegalArgumentException("Target URL must use http or https");
        }

        String host = uri.getHost();
        if (host == null || host.isBlank()) {
            throw new IllegalArgumentException("Target URL must include a host");
        }

        InetAddress resolved;
        try {
            resolved = InetAddress.getByName(host);
        } catch (UnknownHostException e) {
            throw new IllegalArgumentException("Could not resolve target host: " + host);
        }

        if (resolved.isLoopbackAddress()
                || resolved.isLinkLocalAddress()
                || resolved.isSiteLocalAddress()   // covers RFC1918 private ranges
                || resolved.isAnyLocalAddress()
                || resolved.isMulticastAddress()) {
            throw new IllegalArgumentException(
                "Target URL resolves to a non-public address and cannot be registered");
        }

        // Explicitly block the common cloud metadata IP as an extra guard,
        // since it isn't always caught by the private-range checks above.
        if (resolved.getHostAddress().equals("169.254.169.254")) {
            throw new IllegalArgumentException("Target URL resolves to a metadata endpoint and cannot be registered");
        }
    }
}
