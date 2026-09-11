package com.gateway.api_gateway.service;

import com.gateway.api_gateway.dto.ApiDtos.RegisterApiRequest;
import com.gateway.api_gateway.dto.ApiDtos.RegisteredApiResponse;
import com.gateway.api_gateway.entity.Policy;
import com.gateway.api_gateway.entity.RegisteredApi;
import com.gateway.api_gateway.entity.User;
import com.gateway.api_gateway.repository.PolicyRepository;
import com.gateway.api_gateway.repository.RegisteredApiRepository;
import com.gateway.api_gateway.repository.UserRepository;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ApiRegistrationService {

    private final RegisteredApiRepository registeredApiRepository;
    private final PolicyRepository policyRepository;
    private final UserRepository userRepository;
    private final UrlValidationService urlValidationService;

    public ApiRegistrationService(
        RegisteredApiRepository registeredApiRepository,
        PolicyRepository policyRepository,
        UserRepository userRepository,
        UrlValidationService urlValidationService
    ) {
        this.registeredApiRepository = registeredApiRepository;
        this.policyRepository = policyRepository;
        this.userRepository = userRepository;
        this.urlValidationService = urlValidationService;
    }

    @Transactional
    public RegisteredApiResponse register(String ownerEmail, RegisterApiRequest request) {
        // Proxy-abuse mitigation happens before anything is persisted.
        urlValidationService.validateOrThrow(request.targetUrl());

        User owner = userRepository.findByEmail(ownerEmail)
            .orElseThrow(() -> new UsernameNotFoundException("User not found: " + ownerEmail));

        RegisteredApi api = new RegisteredApi();
        api.setOwner(owner);
        api.setName(request.name());
        api.setTargetUrl(request.targetUrl());
        registeredApiRepository.save(api);

        Policy policy = new Policy();
        policy.setRegisteredApi(api);
        policy.setAlgorithm(request.algorithm());
        policy.setRequestsPerWindow(request.requestsPerWindow());
        policy.setWindowSeconds(request.windowSeconds());
        policy.setBurstCapacity(request.burstCapacity());
        policyRepository.save(policy);

        return toResponse(api, policy);
    }

    public List<RegisteredApiResponse> listForOwner(String ownerEmail) {
        User owner = userRepository.findByEmail(ownerEmail)
            .orElseThrow(() -> new UsernameNotFoundException("User not found: " + ownerEmail));

        return registeredApiRepository.findByOwner(owner).stream()
            .map(api -> {
                Policy policy = policyRepository.findByRegisteredApi(api)
                    .orElseThrow(() -> new IllegalStateException("Policy missing for API " + api.getId()));
                return toResponse(api, policy);
            })
            .toList();
    }

    private RegisteredApiResponse toResponse(RegisteredApi api, Policy policy) {
        return new RegisteredApiResponse(
            api.getId(),
            api.getName(),
            api.getTargetUrl(),
            api.getGatewayApiKey(),
            api.isActive(),
            policy.getAlgorithm(),
            policy.getRequestsPerWindow(),
            policy.getWindowSeconds()
        );
    }
}
