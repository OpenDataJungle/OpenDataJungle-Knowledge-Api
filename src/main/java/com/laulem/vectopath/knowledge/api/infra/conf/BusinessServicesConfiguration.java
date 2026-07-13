package com.laulem.vectopath.knowledge.api.infra.conf;

import com.laulem.vectopath.knowledge.api.business.model.SecurityConfig;
import com.laulem.vectopath.knowledge.api.business.repository.ResourceRepository;
import com.laulem.vectopath.knowledge.api.business.repository.VectorStoreRepository;
import com.laulem.vectopath.knowledge.api.business.service.AuthenticationUseCase;
import com.laulem.vectopath.knowledge.api.business.service.RerankerUseCase;
import com.laulem.vectopath.knowledge.api.business.service.ResourceUseCase;
import com.laulem.vectopath.knowledge.api.business.service.RoleValidationUseCase;
import com.laulem.vectopath.knowledge.api.business.service.impl.ResourceService;
import com.laulem.vectopath.knowledge.api.business.service.impl.RoleValidationService;
import com.laulem.vectopath.knowledge.api.business.service.impl.VectorizedResourceService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class BusinessServicesConfiguration {

    @Bean
    public VectorizedResourceService vectorizedResourceService(VectorStoreRepository vectorStoreRepository,
                                                               AuthenticationUseCase authenticationUseCase,
                                                               RerankerUseCase rerankerUseCase) {
        return new VectorizedResourceService(vectorStoreRepository, authenticationUseCase, rerankerUseCase);
    }

    @Bean
    public RoleValidationUseCase roleValidationService(AuthenticationUseCase authenticationUseCase,
                                                       SecurityConfig securityConfig) {
        return new RoleValidationService(authenticationUseCase, securityConfig);
    }

    @Bean
    public ResourceUseCase resourceService(ResourceRepository resourceRepository,
                                           VectorizedResourceService vectorizedResourceService,
                                           VectorStoreRepository vectorStoreRepository,
                                           RoleValidationUseCase roleValidationUseCase) {
        return new ResourceService(resourceRepository, vectorizedResourceService, vectorStoreRepository, roleValidationUseCase);
    }
}

