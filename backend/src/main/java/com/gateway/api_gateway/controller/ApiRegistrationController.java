package com.gateway.api_gateway.controller;

import com.gateway.api_gateway.dto.ApiDtos.RegisterApiRequest;
import com.gateway.api_gateway.dto.ApiDtos.RegisteredApiResponse;
import com.gateway.api_gateway.service.ApiRegistrationService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/apis")
public class ApiRegistrationController {

    private final ApiRegistrationService apiRegistrationService;

    public ApiRegistrationController(ApiRegistrationService apiRegistrationService) {
        this.apiRegistrationService = apiRegistrationService;
    }

    // Authentication.getName() is the email we put as the JWT subject in JwtAuthFilter.
    @PostMapping
    public ResponseEntity<RegisteredApiResponse> register(
        Authentication authentication,
        @Valid @RequestBody RegisterApiRequest request
    ) {
        return ResponseEntity.ok(apiRegistrationService.register(authentication.getName(), request));
    }

    @GetMapping
    public ResponseEntity<List<RegisteredApiResponse>> listMine(Authentication authentication) {
        return ResponseEntity.ok(apiRegistrationService.listForOwner(authentication.getName()));
    }
}
