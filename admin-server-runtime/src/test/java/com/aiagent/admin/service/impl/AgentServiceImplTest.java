package com.aiagent.admin.service.impl;

import com.aiagent.admin.api.dto.*;
import com.aiagent.admin.domain.entity.Agent;
import com.aiagent.admin.domain.entity.AgentTool;
import com.aiagent.admin.domain.entity.Tool;
import com.aiagent.admin.domain.enums.AgentStatus;
import com.aiagent.admin.domain.enums.ToolCategory;
import com.aiagent.admin.domain.enums.ToolType;
import com.aiagent.admin.domain.repository.AgentRepository;
import com.aiagent.admin.domain.repository.AgentToolRepository;
import com.aiagent.admin.domain.repository.ToolRepository;
import com.aiagent.admin.service.AgentIdGenerator;
import com.aiagent.admin.service.AgentToolIdGenerator;
import com.aiagent.admin.service.ModelConfigService;
import com.aiagent.admin.service.mapper.AgentMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * AgentService 单元测试
 * <p>
 * 测试 Agent CRUD 操作、工具绑定、状态管理等功能。
 * </p>
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AgentServiceImplTest {

    @Mock
    private AgentRepository agentRepository;

    @Mock
    private AgentToolRepository agentToolRepository;

    @Mock
    private ToolRepository toolRepository;

    @Mock
    private AgentIdGenerator agentIdGenerator;

    @Mock
    private AgentToolIdGenerator agentToolIdGenerator;

    @Mock
    private AgentMapper agentMapper;

    @Mock
    private ModelConfigService modelConfigService;

    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private AgentServiceImpl agentService;

    private Agent testAgent;
    private Tool testTool;
    private AgentTool testAgentTool;
    private CreateAgentRequest createRequest;

    @BeforeEach
    void setUp() {
        // 初始化测试数据
        testAgent = Agent.builder()
                .id("agt_test123")
                .name("Test Agent")
                .description("Test description")
                .modelId("mdl_test123")
                .systemPrompt("You are a helpful assistant")
                .config("{\"temperature\": 0.7, \"maxTokens\": 4096}")
                .status(AgentStatus.DRAFT)
                .build();

        testTool = Tool.builder()
                .id("tool_test123")
                .name("calculator")
                .description("Calculator tool")
                .category(ToolCategory.CALCULATION)
                .type(ToolType.BUILTIN)
                .schema("{}")
                .executor("calculatorExecutor")
                .build();

        testAgentTool = AgentTool.builder()
                .id("at_test123")
                .agentId("agt_test123")
                .toolId("tool_test123")
                .enabled(true)
                .config("{}")
                .build();

        createRequest = CreateAgentRequest.builder()
                .name("Test Agent")
                .description("Test description")
                .modelId("mdl_test123")
                .systemPrompt("You are a helpful assistant")
                .config(AgentConfigRequest.builder()
                        .temperature(0.7)
                        .maxTokens(4096)
                        .build())
                .build();
    }

    @Test
    @DisplayName("findById should return agent when exists")
    void findById_shouldReturnAgent_whenExists() {
        // Given
        when(agentRepository.findById("agt_test123")).thenReturn(Optional.of(testAgent));
        when(agentToolRepository.findByAgentId("agt_test123")).thenReturn(List.of());
        AgentResponse response = AgentResponse.builder()
                .id("agt_test123")
                .name("Test Agent")
                .status(AgentStatus.DRAFT)
                .build();
        when(agentMapper.toResponseWithTools(any(), any(), any())).thenReturn(response);

        // When
        Optional<AgentResponse> result = agentService.findById("agt_test123");

        // Then
        assertThat(result).isPresent();
        assertThat(result.get().getName()).isEqualTo("Test Agent");
        verify(agentRepository).findById("agt_test123");
    }

    @Test
    @DisplayName("findById should return empty when not exists")
    void findById_shouldReturnEmpty_whenNotExists() {
        // Given
        when(agentRepository.findById("agt_unknown")).thenReturn(Optional.empty());

        // When
        Optional<AgentResponse> result = agentService.findById("agt_unknown");

        // Then
        assertThat(result).isEmpty();
        verify(agentRepository).findById("agt_unknown");
    }

    @Test
    @DisplayName("create should throw exception when name already exists")
    void create_shouldThrowException_whenNameExists() {
        // Given
        when(agentRepository.existsByName("Test Agent")).thenReturn(true);

        // When & Then
        assertThatThrownBy(() -> agentService.create(createRequest))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Agent name already exists");
    }

    @Test
    @DisplayName("create should throw exception when model not found")
    void create_shouldThrowException_whenModelNotFound() {
        // Given
        when(agentRepository.existsByName("Test Agent")).thenReturn(false);
        when(modelConfigService.findEntityById("mdl_test123")).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> agentService.create(createRequest))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Model not found");
    }

    @Test
    @DisplayName("create should create agent successfully")
    void create_shouldCreateAgent_successfully() {
        // Given
        when(agentRepository.existsByName("Test Agent")).thenReturn(false);
        when(modelConfigService.findEntityById("mdl_test123")).thenReturn(Optional.of(
                com.aiagent.admin.domain.entity.ModelConfig.builder().id("mdl_test123").name("Test Model").build()
        ));
        when(modelConfigService.findById("mdl_test123")).thenReturn(Optional.of(
                createModelResponse("Test Model")
        ));
        when(agentIdGenerator.generateId()).thenReturn("agt_new123");
        when(agentMapper.toEntity(any())).thenReturn(testAgent);
        when(agentRepository.save(any())).thenReturn(testAgent);
        when(agentToolRepository.findByAgentId(any())).thenReturn(List.of());
        AgentResponse response = AgentResponse.builder()
                .id("agt_new123")
                .name("Test Agent")
                .modelName("Test Model")
                .status(AgentStatus.DRAFT)
                .build();
        when(agentMapper.toResponseWithTools(any(), any(), any())).thenReturn(response);

        // When
        AgentResponse result = agentService.create(createRequest);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getName()).isEqualTo("Test Agent");
        assertThat(result.getModelName()).isEqualTo("Test Model");
        verify(agentRepository).save(any());
    }

    @Test
    @DisplayName("delete should delete agent and tool bindings")
    void delete_shouldDeleteAgentAndBindings() {
        // Given
        when(agentRepository.findById("agt_test123")).thenReturn(Optional.of(testAgent));

        // When
        agentService.delete("agt_test123");

        // Then
        verify(agentToolRepository).deleteByAgentId("agt_test123");
        verify(agentRepository).deleteById("agt_test123");
    }

    @Test
    @DisplayName("delete should throw exception when agent not found")
    void delete_shouldThrowException_whenNotFound() {
        // Given
        when(agentRepository.findById("agt_unknown")).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> agentService.delete("agt_unknown"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Agent not found");
    }

    @Test
    @DisplayName("updateStatus should update status successfully")
    void updateStatus_shouldUpdateStatus_successfully() {
        // Given
        when(agentRepository.findById("agt_test123")).thenReturn(Optional.of(testAgent));
        when(agentRepository.save(any())).thenReturn(testAgent);
        when(agentToolRepository.findByAgentId(any())).thenReturn(List.of());
        when(modelConfigService.findById(any())).thenReturn(Optional.of(createModelResponse("Test")));
        when(agentMapper.toResponseWithTools(any(), any(), any())).thenReturn(
                AgentResponse.builder().status(AgentStatus.PUBLISHED).build()
        );

        // When
        AgentResponse result = agentService.updateStatus("agt_test123", AgentStatus.PUBLISHED);

        // Then
        assertThat(result.getStatus()).isEqualTo(AgentStatus.PUBLISHED);
        verify(agentRepository).save(any());
    }

    @Test
    @DisplayName("bindTool should bind tool successfully")
    void bindTool_shouldBindTool_successfully() {
        // Given
        ToolBindingRequest request = ToolBindingRequest.builder()
                .toolId("tool_test123")
                .enabled(true)
                .build();

        when(agentRepository.findById("agt_test123")).thenReturn(Optional.of(testAgent));
        when(toolRepository.findById("tool_test123")).thenReturn(Optional.of(testTool));
        when(agentToolRepository.existsByAgentIdAndToolId("agt_test123", "tool_test123")).thenReturn(false);
        when(agentToolIdGenerator.generateId()).thenReturn("at_new123");
        when(agentToolRepository.save(any())).thenReturn(testAgentTool);
        when(agentMapper.toToolBindingResponse(any(), any())).thenReturn(
                ToolBindingResponse.builder()
                        .id("at_new123")
                        .toolId("tool_test123")
                        .toolName("calculator")
                        .enabled(true)
                        .build()
        );

        // When
        ToolBindingResponse result = agentService.bindTool("agt_test123", request);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getToolName()).isEqualTo("calculator");
        verify(agentToolRepository).save(any());
    }

    @Test
    @DisplayName("bindTool should throw exception when already bound")
    void bindTool_shouldThrowException_whenAlreadyBound() {
        // Given
        ToolBindingRequest request = ToolBindingRequest.builder()
                .toolId("tool_test123")
                .enabled(true)
                .build();

        when(agentRepository.findById("agt_test123")).thenReturn(Optional.of(testAgent));
        when(toolRepository.findById("tool_test123")).thenReturn(Optional.of(testTool));
        when(agentToolRepository.existsByAgentIdAndToolId("agt_test123", "tool_test123")).thenReturn(true);

        // When & Then
        assertThatThrownBy(() -> agentService.bindTool("agt_test123", request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Tool already bound");
    }

    @Test
    @DisplayName("unbindTool should unbind tool successfully")
    void unbindTool_shouldUnbindTool_successfully() {
        // Given
        when(agentToolRepository.existsByAgentIdAndToolId("agt_test123", "tool_test123")).thenReturn(true);

        // When
        agentService.unbindTool("agt_test123", "tool_test123");

        // Then
        verify(agentToolRepository).deleteByAgentIdAndToolId("agt_test123", "tool_test123");
    }

    @Test
    @DisplayName("unbindTool should throw exception when binding not found")
    void unbindTool_shouldThrowException_whenBindingNotFound() {
        // Given
        when(agentToolRepository.existsByAgentIdAndToolId("agt_test123", "tool_test123")).thenReturn(false);

        // When & Then
        assertThatThrownBy(() -> agentService.unbindTool("agt_test123", "tool_test123"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Tool binding not found");
    }

    /**
     * 创建 ModelResponse 辅助方法
     */
    private ModelResponse createModelResponse(String name) {
        ModelResponse response = new ModelResponse();
        response.setName(name);
        return response;
    }
}