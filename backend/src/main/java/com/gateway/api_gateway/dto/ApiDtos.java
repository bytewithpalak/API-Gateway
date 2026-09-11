package com.gateway.api_gateway.dto;

import com.gateway.api_gateway.entity.Policy;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class ApiDtos {

    public record RegisterApiRequest(
        @NotBlank String name,
        @NotBlank String targetUrl,
        @NotNull Policy.Algorithm algorithm,
        @Min(1) int requestsPerWindow,
        @Min(1) int windowSeconds,
        Integer burstCapacity // optional, only used for TOKEN_BUCKET
    ) {}

    public record RegisteredApiResponse(
        Long id,
        String name,
        String targetUrl,
        String gatewayApiKey,
        boolean active,
        Policy.Algorithm algorithm,
        int requestsPerWindow,
        int windowSeconds
    ) {}
}
