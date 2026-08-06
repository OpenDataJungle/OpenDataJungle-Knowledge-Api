package com.laulem.vectopath.knowledge.api.infra.conf;

import com.laulem.vectopath.knowledge.api.business.repository.FolderRepository;
import com.laulem.vectopath.knowledge.api.business.repository.ResourceRepository;
import com.laulem.vectopath.knowledge.api.business.repository.VectorStoreRepository;
import com.laulem.vectopath.knowledge.api.business.service.AuthenticationUseCase;
import com.laulem.vectopath.knowledge.api.business.service.FolderUseCase;
import com.laulem.vectopath.knowledge.api.business.service.ReferentialUseCase;
import com.laulem.vectopath.knowledge.api.business.service.RerankerUseCase;
import com.laulem.vectopath.knowledge.api.business.service.ResourceUseCase;
import com.laulem.vectopath.knowledge.api.business.service.impl.FolderService;
import com.laulem.vectopath.knowledge.api.business.service.impl.ResourceService;
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
    public FolderUseCase folderService(FolderRepository folderRepository, AuthenticationUseCase authenticationUseCase,
                                       ReferentialUseCase referentialUseCase) {
        return new FolderService(folderRepository, authenticationUseCase, referentialUseCase);
    }

    @Bean
    public ResourceUseCase resourceService(ResourceRepository resourceRepository,
                                           VectorStoreRepository vectorStoreRepository,
                                           FolderUseCase folderUseCase,
                                           AuthenticationUseCase authenticationUseCase,
                                           ReferentialUseCase referentialUseCase) {
        return new ResourceService(resourceRepository, vectorStoreRepository, folderUseCase, authenticationUseCase, referentialUseCase);
    }
}

