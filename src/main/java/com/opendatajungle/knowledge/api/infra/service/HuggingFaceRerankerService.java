package com.opendatajungle.knowledge.api.infra.service;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.opendatajungle.knowledge.api.business.model.PartialResource;
import com.opendatajungle.knowledge.api.business.service.RerankerUseCase;
import com.opendatajungle.commons.util.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.Comparator;
import java.util.List;

public class HuggingFaceRerankerService implements RerankerUseCase {
    private static final Logger logger = LoggerFactory.getLogger(HuggingFaceRerankerService.class);

    private static final ParameterizedTypeReference<List<RerankResponse>> RESULT_TYPE = new ParameterizedTypeReference<>() {
    };

    private final RestClient restClient;

    public HuggingFaceRerankerService(RestClient restClient) {
        this.restClient = restClient;
        logger.info("HuggingFace TEI reranker initialized");
    }

    @Override
    public List<PartialResource> rerank(String query, List<PartialResource> candidates, int limit) {
        if (CollectionUtils.isEmpty(candidates) || candidates.size() < limit) {
            return getSortedCandidates(candidates, limit);
        }

        logger.debug("HuggingFace TEI re-ranking {} candidates", candidates.size());

        List<String> texts = candidates.stream().map(PartialResource::getContent).toList();
        try {
            List<RerankResponse> results = restClient
                    .post()
                    .uri("/rerank")
                    .body(new RerankRequest(query, texts, true, false))
                    .retrieve()
                    .body(RESULT_TYPE);

            if (!CollectionUtils.isEmpty(results)) {
                results.forEach(result -> candidates.get(result.index()).setSimilarityScore(result.score()));
            }
        } catch (RestClientException e) {
            logger.error("HuggingFace TEI reranker call failed ({}), returning original order", e.getMessage());
        }

        return getSortedCandidates(candidates, limit);
    }

    private List<PartialResource> getSortedCandidates(final List<PartialResource> candidates, final int limit) {
        return candidates.stream().sorted(Comparator.comparingDouble(PartialResource::getSimilarityScore).reversed()).limit(limit).toList();
    }

    record RerankRequest(String query, List<String> texts, boolean truncate,
                         @JsonProperty("raw_scores") boolean rawScores) {
    }

    record RerankResponse(int index, double score) {
    }
}
