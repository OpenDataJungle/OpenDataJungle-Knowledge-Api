package com.opendatajungle.knowledge.api.client.service.resource.general;

import com.opendatajungle.knowledge.api.client.exception.UnsupportedSourceTypeException;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class GeneralResourceGenerationFactory {

    private final Map<String, GeneralResourceGeneration> resourceGenerationFactory;

    public GeneralResourceGenerationFactory(List<GeneralResourceGeneration> generations) {
        this.resourceGenerationFactory = generations.stream()
                .collect(Collectors.toMap(GeneralResourceGeneration::getSourceType, Function.identity(), (_, replacement) -> replacement));
    }

    public GeneralResourceGeneration getResourceGeneration(String sourceType) {
        GeneralResourceGeneration generation = resourceGenerationFactory.get(sourceType.toUpperCase());
        if (generation == null) {
            throw new UnsupportedSourceTypeException(sourceType);
        }
        return generation;
    }
}
