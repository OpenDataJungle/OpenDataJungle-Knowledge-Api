package com.laulem.vectopath.knowledge.api.client.controller;

import com.laulem.vectopath.knowledge.api.business.service.impl.VectorizedResourceService;
import com.laulem.vectopath.knowledge.api.client.dto.SearchRequest;
import com.laulem.vectopath.knowledge.api.client.dto.SearchResponse;
import com.laulem.vectopath.knowledge.api.infra.conf.security.SecurityExpressions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;


@RestController
@RequestMapping("/api/v1/search")
public class SearchController {

    private static final Logger logger = LoggerFactory.getLogger(SearchController.class);

    private final VectorizedResourceService vectorizedResourceService;

    public SearchController(VectorizedResourceService vectorizedResourceService) {
        this.vectorizedResourceService = vectorizedResourceService;
    }

    @PreAuthorize(SecurityExpressions.SEARCH_SEMANTIC)
    @PostMapping("/semantic")
    public List<SearchResponse> searchSemantic(@RequestBody SearchRequest request) {
        logger.info("Semantic search requested");

        return vectorizedResourceService.searchSimilar(request.query(), request.limit(), request.minSimilarity(), request.resourceIds())
                .stream()
                .map(SearchResponse::new)
                .toList();
    }
}
