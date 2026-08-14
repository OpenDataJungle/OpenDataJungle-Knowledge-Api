package com.opendatajungle.knowledge.api.infra.conf;

import com.opendatajungle.knowledge.api.business.repository.FolderRepository;
import com.opendatajungle.knowledge.api.business.repository.ResourceRepository;
import com.opendatajungle.knowledge.api.business.repository.VectorStoreRepository;
import com.opendatajungle.commons.business.service.AuthenticationUseCase;
import com.opendatajungle.knowledge.api.business.service.FolderUseCase;
import com.opendatajungle.knowledge.api.business.service.ReferenceDataUseCase;
import com.opendatajungle.knowledge.api.business.service.RerankerUseCase;
import com.opendatajungle.knowledge.api.business.service.ResourceUseCase;
import com.opendatajungle.knowledge.api.business.service.impl.FolderService;
import com.opendatajungle.knowledge.api.business.service.impl.ResourceService;
import com.opendatajungle.knowledge.api.business.service.impl.VectorizedResourceService;
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
                                       ReferenceDataUseCase referenceDataUseCase) {
        return new FolderService(folderRepository, authenticationUseCase, referenceDataUseCase);
    }

    @Bean
    public ResourceUseCase resourceService(ResourceRepository resourceRepository,
                                           VectorStoreRepository vectorStoreRepository,
                                           FolderUseCase folderUseCase,
                                           AuthenticationUseCase authenticationUseCase,
                                           ReferenceDataUseCase referenceDataUseCase) {
        return new ResourceService(resourceRepository, vectorStoreRepository, folderUseCase, authenticationUseCase, referenceDataUseCase);
    }
}

