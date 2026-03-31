package com.aiagent.admin.prompt.service.mapper;

import com.aiagent.admin.prompt.api.dto.PromptTemplateCreateRequest;
import com.aiagent.admin.prompt.api.dto.PromptTemplateResponse;
import com.aiagent.admin.prompt.domain.entity.PromptTemplate;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import java.util.Arrays;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;

class PromptMapperTest {

    private final PromptMapper mapper = Mappers.getMapper(PromptMapper.class);

    @Test
    void toResponse_WithTagsAndVariables() {
        PromptTemplate entity = PromptTemplate.builder()
                .id("test-id")
                .name("Test Prompt")
                .content("Hello {{name}}!")
                .tags("tag1,tag2,tag3")
                .variables("name,age")
                .build();

        PromptTemplateResponse response = mapper.toResponse(entity);

        assertNotNull(response);
        assertEquals("test-id", response.getId());
        assertEquals("Test Prompt", response.getName());
        assertEquals(3, response.getTags().size());
        assertTrue(response.getTags().contains("tag1"));
        assertTrue(response.getTags().contains("tag2"));
        assertEquals(2, response.getVariables().size());
        assertTrue(response.getVariables().contains("name"));
    }

    @Test
    void toResponse_WithEmptyTags() {
        PromptTemplate entity = PromptTemplate.builder()
                .id("test-id")
                .name("Test Prompt")
                .content("Hello!")
                .tags("")
                .variables("")
                .build();

        PromptTemplateResponse response = mapper.toResponse(entity);

        assertNotNull(response);
        assertTrue(response.getTags().isEmpty());
        assertTrue(response.getVariables().isEmpty());
    }

    @Test
    void toResponse_WithNullTags() {
        PromptTemplate entity = PromptTemplate.builder()
                .id("test-id")
                .name("Test Prompt")
                .content("Hello!")
                .build();

        PromptTemplateResponse response = mapper.toResponse(entity);

        assertNotNull(response);
        assertTrue(response.getTags().isEmpty());
    }

    @Test
    void toEntity_FromCreateRequest() {
        PromptTemplateCreateRequest request = new PromptTemplateCreateRequest();
        request.setName("New Prompt");
        request.setContent("Content {{var}}");
        request.setDescription("Description");
        request.setCategory("category");
        request.setTags(Arrays.asList("tag1", "tag2"));
        request.setCreatedBy("user");

        PromptTemplate entity = mapper.toEntity(request);

        assertNotNull(entity);
        assertEquals("New Prompt", entity.getName());
        assertEquals("Content {{var}}", entity.getContent());
        assertEquals("Description", entity.getDescription());
        assertEquals("category", entity.getCategory());
        assertEquals("tag1,tag2", entity.getTags());
        assertEquals("user", entity.getCreatedBy());
        assertEquals(1, entity.getVersion());
    }

    @Test
    void listToString_WithMultipleTags() {
        String result = mapper.listToString(Arrays.asList("tag1", "tag2", "tag3"));
        assertEquals("tag1,tag2,tag3", result);
    }

    @Test
    void listToString_WithEmptyList() {
        String result = mapper.listToString(Collections.emptyList());
        assertEquals("", result);
    }

    @Test
    void listToString_WithNullList() {
        String result = mapper.listToString(null);
        assertEquals("", result);
    }

    @Test
    void stringToList_WithMultipleValues() {
        java.util.List<String> result = mapper.stringToList("tag1,tag2,tag3");
        assertEquals(3, result.size());
        assertEquals("tag1", result.get(0));
    }

    @Test
    void stringToList_WithEmptyString() {
        java.util.List<String> result = mapper.stringToList("");
        assertTrue(result.isEmpty());
    }

    @Test
    void stringToList_WithNull() {
        java.util.List<String> result = mapper.stringToList(null);
        assertTrue(result.isEmpty());
    }
}
