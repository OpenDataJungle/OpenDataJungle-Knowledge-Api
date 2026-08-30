package com.opendatajungle.knowledge.api.docs;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.opendatajungle.knowledge.api.testconfig.OpenApiTestConfiguration;
import com.opendatajungle.knowledge.api.testconfig.TestcontainersConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@WithMockUser
@Import({TestcontainersConfiguration.class, OpenApiTestConfiguration.class})
class OpenApiGenerationIT {
    @Autowired
    private MockMvc mockMvc;

    @Value("${application.title}")
    private String title;

    @Test
    void generateOpenApiFile() throws Exception {
        String rawSpec = mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        ObjectMapper objectMapper = new ObjectMapper();
        JsonNode spec = objectMapper.readTree(rawSpec);
        String pretty = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(spec);

        Files.writeString(Path.of("docs/" + title.replace(" ", "") + "_Openapi.json"), pretty + System.lineSeparator());
    }
}
