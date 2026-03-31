package com.aiagent.admin.prompt.service;

import com.aiagent.admin.prompt.api.dto.*;
import com.aiagent.admin.prompt.domain.entity.PromptTemplate;
import com.aiagent.admin.prompt.domain.entity.PromptVersion;
import com.aiagent.admin.prompt.domain.repository.PromptTemplateRepository;
import com.aiagent.admin.prompt.domain.repository.PromptVersionRepository;
import com.aiagent.admin.prompt.service.impl.PromptServiceImpl;
import com.aiagent.admin.prompt.service.mapper.PromptMapper;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.*;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PromptServiceImplTest {

    @Mock
    private PromptTemplateRepository promptTemplateRepository;

    @Mock
    private PromptVersionRepository promptVersionRepository;

    @Mock
    private PromptMapper promptMapper;

    @InjectMocks
    private PromptServiceImpl promptService;

    private PromptTemplate testTemplate;
    private PromptTemplateResponse testResponse;

    @BeforeEach
    void setUp() {
        testTemplate = PromptTemplate.builder()
                .id("test-id-123")
                .name("Test Prompt")
                .content("Hello {{name}}, welcome to {{place}}!")
                .description("Test description")
                .category("test-category")
                .tags("tag1,tag2")
                .version(1)
                .variables("name,place")
                .createdBy("test-user")
                .build();

        testResponse = new PromptTemplateResponse();
        testResponse.setId("test-id-123");
        testResponse.setName("Test Prompt");
        testResponse.setContent("Hello {{name}}, welcome to {{place}}!");
        testResponse.setVariables(Arrays.asList("name", "place"));
    }

    @Test
    void createPrompt_Success() {
        PromptTemplateCreateRequest request = new PromptTemplateCreateRequest();
        request.setName("Test Prompt");
        request.setContent("Hello {{name}}!");
        request.setDescription("Test");
        request.setCategory("test");
        request.setTags(Arrays.asList("tag1"));
        request.setCreatedBy("user");

        PromptTemplate entity = PromptTemplate.builder()
                .name("Test Prompt")
                .content("Hello {{name}}!")
                .build();

        when(promptTemplateRepository.existsByName("Test Prompt")).thenReturn(false);
        when(promptMapper.toEntity(request)).thenReturn(entity);
        when(promptTemplateRepository.save(any(PromptTemplate.class))).thenReturn(testTemplate);
        when(promptVersionRepository.save(any(PromptVersion.class))).thenReturn(mock(PromptVersion.class));
        when(promptMapper.toResponse(testTemplate)).thenReturn(testResponse);

        PromptTemplateResponse result = promptService.createPrompt(request);

        assertNotNull(result);
        assertEquals("test-id-123", result.getId());
        verify(promptTemplateRepository).save(any(PromptTemplate.class));
        verify(promptVersionRepository).save(any(PromptVersion.class));
    }

    @Test
    void createPrompt_DuplicateName_ThrowsException() {
        PromptTemplateCreateRequest request = new PromptTemplateCreateRequest();
        request.setName("Existing Prompt");
        request.setContent("Content");

        when(promptTemplateRepository.existsByName("Existing Prompt")).thenReturn(true);

        assertThrows(IllegalArgumentException.class, () -> promptService.createPrompt(request));
    }

    @Test
    void getPrompt_Success() {
        when(promptTemplateRepository.findById("test-id-123")).thenReturn(Optional.of(testTemplate));
        when(promptMapper.toResponse(testTemplate)).thenReturn(testResponse);

        PromptTemplateResponse result = promptService.getPrompt("test-id-123");

        assertNotNull(result);
        assertEquals("test-id-123", result.getId());
    }

    @Test
    void getPrompt_NotFound_ThrowsException() {
        when(promptTemplateRepository.findById("non-existent")).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class, () -> promptService.getPrompt("non-existent"));
    }

    @Test
    void updatePrompt_Success() {
        PromptTemplateUpdateRequest request = new PromptTemplateUpdateRequest();
        request.setName("Updated Prompt");
        request.setContent("Updated {{content}}!");
        request.setChangeLog("Updated version");

        PromptTemplate updatedTemplate = PromptTemplate.builder()
                .id("test-id-123")
                .name("Updated Prompt")
                .content("Updated {{content}}!")
                .version(2)
                .build();

        when(promptTemplateRepository.findById("test-id-123")).thenReturn(Optional.of(testTemplate));
        when(promptTemplateRepository.existsByName("Updated Prompt")).thenReturn(false);
        when(promptVersionRepository.save(any(PromptVersion.class))).thenReturn(mock(PromptVersion.class));
        when(promptTemplateRepository.save(any(PromptTemplate.class))).thenReturn(updatedTemplate);
        when(promptMapper.toResponse(updatedTemplate)).thenReturn(testResponse);

        PromptTemplateResponse result = promptService.updatePrompt("test-id-123", request);

        assertNotNull(result);
        verify(promptVersionRepository).save(any(PromptVersion.class));
        verify(promptTemplateRepository).save(any(PromptTemplate.class));
    }

    @Test
    void deletePrompt_Success() {
        when(promptTemplateRepository.existsById("test-id-123")).thenReturn(true);
        when(promptVersionRepository.findByPromptIdOrderByVersionDesc("test-id-123"))
                .thenReturn(Collections.emptyList());

        promptService.deletePrompt("test-id-123");

        verify(promptTemplateRepository).deleteById("test-id-123");
    }

    @Test
    void listPrompts_Success() {
        Pageable pageable = PageRequest.of(0, 20, Sort.by("updatedAt").descending());
        Page<PromptTemplate> page = new PageImpl<>(Collections.singletonList(testTemplate), pageable, 1);

        when(promptTemplateRepository.findByFilters(null, null, null, pageable)).thenReturn(page);
        when(promptMapper.toResponse(testTemplate)).thenReturn(testResponse);

        PageResponse<PromptTemplateResponse> result = promptService.listPrompts(null, null, null, pageable);

        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
    }

    @Test
    void getPromptVersions_Success() {
        PromptVersion version = PromptVersion.builder()
                .id("version-id")
                .promptId("test-id-123")
                .version(1)
                .content("Content")
                .build();

        when(promptTemplateRepository.existsById("test-id-123")).thenReturn(true);
        when(promptVersionRepository.findByPromptIdOrderByVersionDesc("test-id-123"))
                .thenReturn(Collections.singletonList(version));

        List<PromptVersionResponse> result = promptService.getPromptVersions("test-id-123");

        assertNotNull(result);
        assertEquals(1, result.size());
    }

    @Test
    void rollbackPrompt_Success() {
        RollbackRequest request = new RollbackRequest();
        request.setVersion(1);
        request.setChangeLog("Rollback to v1");

        PromptVersion targetVersion = PromptVersion.builder()
                .promptId("test-id-123")
                .version(1)
                .content("Old content {{var}}")
                .build();

        PromptTemplate updatedTemplate = PromptTemplate.builder()
                .id("test-id-123")
                .version(2)
                .content("Old content {{var}}")
                .build();

        when(promptTemplateRepository.findById("test-id-123")).thenReturn(Optional.of(testTemplate));
        when(promptVersionRepository.findByPromptIdAndVersion("test-id-123", 1)).thenReturn(Optional.of(targetVersion));
        when(promptVersionRepository.save(any(PromptVersion.class))).thenReturn(mock(PromptVersion.class));
        when(promptTemplateRepository.save(any(PromptTemplate.class))).thenReturn(updatedTemplate);
        when(promptMapper.toResponse(updatedTemplate)).thenReturn(testResponse);

        PromptTemplateResponse result = promptService.rollbackPrompt("test-id-123", request);

        assertNotNull(result);
        verify(promptVersionRepository).save(any(PromptVersion.class));
    }

    @Test
    void extractVariables_Success() {
        String content = "Hello {{name}}, your age is {{age}}. Welcome to {{place}}!";
        List<String> variables = promptService.extractVariables(content);

        assertEquals(3, variables.size());
        assertTrue(variables.contains("name"));
        assertTrue(variables.contains("age"));
        assertTrue(variables.contains("place"));
    }

    @Test
    void extractVariables_NoVariables_ReturnsEmptyList() {
        String content = "Hello world, no variables here.";
        List<String> variables = promptService.extractVariables(content);

        assertTrue(variables.isEmpty());
    }

    @Test
    void extractVariables_DuplicateVariables_ReturnsUnique() {
        String content = "Hello {{name}}, {{name}} is your name.";
        List<String> variables = promptService.extractVariables(content);

        assertEquals(1, variables.size());
        assertEquals("name", variables.get(0));
    }
}