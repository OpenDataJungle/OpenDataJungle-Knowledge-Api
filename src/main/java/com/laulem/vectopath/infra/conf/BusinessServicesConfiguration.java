package com.laulem.vectopath.infra.conf;

import com.laulem.vectopath.business.model.SecurityConfig;
import com.laulem.vectopath.business.repository.ResourceRepository;
import com.laulem.vectopath.business.repository.VectorStoreRepository;
import com.laulem.vectopath.business.service.AuthenticationService;
import com.laulem.vectopath.business.service.RerankerService;
import com.laulem.vectopath.business.service.ResourceService;
import com.laulem.vectopath.business.service.RoleValidationService;
import com.laulem.vectopath.business.service.impl.ResourceServiceImpl;
import com.laulem.vectopath.business.service.impl.RoleValidationServiceImpl;
import com.laulem.vectopath.business.service.impl.VectorizedResourceService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class BusinessServicesConfiguration {

    @Bean
    public VectorizedResourceService vectorizedResourceService(VectorStoreRepository vectorStoreRepository,
                                                               AuthenticationService authenticationService,
                                                               RerankerService rerankerService) {
        return new VectorizedResourceService(vectorStoreRepository, authenticationService, rerankerService);
    }

    @Bean
    public RoleValidationService roleValidationService(AuthenticationService authenticationService,
                                                       SecurityConfig securityConfig) {
        return new RoleValidationServiceImpl(authenticationService, securityConfig);
    }

    @Bean
    public ResourceService resourceService(ResourceRepository resourceRepository,
                                           VectorizedResourceService vectorizedResourceService,
                                           VectorStoreRepository vectorStoreRepository,
                                           RoleValidationService roleValidationService) {
        return new ResourceServiceImpl(resourceRepository, vectorizedResourceService, vectorStoreRepository, roleValidationService);
    }
}

