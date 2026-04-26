package com.aiagent.admin.service.impl;

import com.aiagent.admin.api.dto.CreateToolRequest;
import com.aiagent.admin.api.dto.ToolResponse;
import com.aiagent.admin.api.dto.UpdateToolRequest;
import com.aiagent.admin.domain.entity.Tool;
import com.aiagent.admin.domain.enums.ToolCategory;
import com.aiagent.admin.domain.enums.ToolType;
import com.aiagent.admin.domain.repository.AgentToolRepository;
import com.aiagent.admin.domain.repository.ToolRepository;
import com.aiagent.admin.service.ToolIdGenerator;
import com.aiagent.admin.service.mapper.ToolMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * ToolService 单元测试
 * <p>
 * 测试工具 CRUD 操作、查询等功能。
 * </p>
 */
@ExtendWith(MockitoExtension.class)
class ToolServiceImplTest {

    @Mock
    private ToolRepository toolRepository;

    @Mock
    private AgentToolRepository agentToolRepository;

    @Mock
    private ToolIdGenerator toolIdGenerator;

    @Mock
    private ToolMapper toolMapper;

    @InjectMocks
    private ToolServiceImpl toolService;

    private Tool builtinTool;
    private Tool customTool;
    private ToolResponse toolResponse;
    private CreateToolRequest createRequest;

    @BeforeEach
    void setUp() {
        // 内置工具
        builtinTool = Tool.builder()
                .id("tool_builtin123")
                .name("calculator")
                .description("Calculator tool")
                .category(ToolCategory.CALCULATION)
                .type(ToolType.BUILTIN)
                .schema("{}")
                .executor("calculatorExecutor")
                .build();

        // 自定义工具
        customTool = Tool.builder()
                .id("tool_custom123")
                .name("custom_api")
                .description("Custom API tool")
                .category(ToolCategory.HTTP)
                .type(ToolType.CUSTOM)
                .schema("{}")
                .executor("https://api.example.com")
                .config("{\"authType\":\"API_KEY\"}")
                .build();

        toolResponse = ToolResponse.builder()
                .id("tool_builtin123")
                .name("calculator")
                .description("Calculator tool")
                .category(ToolCategory.CALCULATION)
                .type(ToolType.BUILTIN)
                .build();

        createRequest = CreateToolRequest.builder()
                .name("custom_api")
                .description("Custom API tool")
                .endpoint("https://api.example.com")
                .authType("API_KEY")
                .build();
    }

    @Test
    @DisplayName("findById should return tool when exists")
    void findById_shouldReturnTool_whenExists() {
        // Given
        when(toolRepository.findById("tool_builtin123")).thenReturn(Optional.of(builtinTool));
        when(toolMapper.toResponse(builtinTool)).thenReturn(toolResponse);

        // When
        Optional<ToolResponse> result = toolService.findById("tool_builtin123");

        // Then
        assertThat(result).isPresent();
        assertThat(result.get().getName()).isEqualTo("calculator");
        verify(toolRepository).findById("tool_builtin123");
    }

    @Test
    @DisplayName("findById should return empty when not exists")
    void findById_shouldReturnEmpty_whenNotExists() {
        // Given
        when(toolRepository.findById("tool_unknown")).thenReturn(Optional.empty());

        // When
        Optional<ToolResponse> result = toolService.findById("tool_unknown");

        // Then
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("findByName should return tool when exists")
    void findByName_shouldReturnTool_whenExists() {
        // Given
        when(toolRepository.findByName("calculator")).thenReturn(Optional.of(builtinTool));
        when(toolMapper.toResponse(builtinTool)).thenReturn(toolResponse);

        // When
        Optional<ToolResponse> result = toolService.findByName("calculator");

        // Then
        assertThat(result).isPresent();
        assertThat(result.get().getName()).isEqualTo("calculator");
    }

    @Test
    @DisplayName("findBuiltinTools should return only builtin tools")
    void findBuiltinTools_shouldReturnBuiltinTools() {
        // Given
        when(toolRepository.findByType(ToolType.BUILTIN)).thenReturn(List.of(builtinTool));
        when(toolMapper.toResponse(builtinTool)).thenReturn(toolResponse);

        // When
        List<ToolResponse> result = toolService.findBuiltinTools();

        // Then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getType()).isEqualTo(ToolType.BUILTIN);
    }

    @Test
    @DisplayName("create should throw exception when name exists")
    void create_shouldThrowException_whenNameExists() {
        // Given
        when(toolRepository.existsByName("custom_api")).thenReturn(true);

        // When & Then
        assertThatThrownBy(() -> toolService.create(createRequest))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Tool name already exists");
    }

    @Test
    @DisplayName("create should create custom tool successfully")
    void create_shouldCreateTool_successfully() {
        // Given
        when(toolRepository.existsByName("custom_api")).thenReturn(false);
        when(toolIdGenerator.generateId()).thenReturn("tool_new123");
        when(toolMapper.toEntity(createRequest)).thenReturn(customTool);
        when(toolRepository.save(any())).thenReturn(customTool);
        ToolResponse response = ToolResponse.builder()
                .id("tool_new123")
                .name("custom_api")
                .type(ToolType.CUSTOM)
                .build();
        when(toolMapper.toResponse(customTool)).thenReturn(response);

        // When
        ToolResponse result = toolService.create(createRequest);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getName()).isEqualTo("custom_api");
        assertThat(result.getType()).isEqualTo(ToolType.CUSTOM);
        verify(toolRepository).save(any());
    }

    @Test
    @DisplayName("update should throw exception when tool not found")
    void update_shouldThrowException_whenNotFound() {
        // Given
        UpdateToolRequest request = UpdateToolRequest.builder()
                .name("updated_name")
                .description("Updated description")
                .build();
        when(toolRepository.findById("tool_unknown")).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> toolService.update("tool_unknown", request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Tool not found");
    }

    @Test
    @DisplayName("update should throw exception for builtin tool")
    void update_shouldThrowException_forBuiltinTool() {
        // Given
        UpdateToolRequest request = UpdateToolRequest.builder()
                .name("updated_name")
                .description("Updated description")
                .build();
        when(toolRepository.findById("tool_builtin123")).thenReturn(Optional.of(builtinTool));

        // When & Then
        assertThatThrownBy(() -> toolService.update("tool_builtin123", request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Only CUSTOM type tools can be updated");
    }

    @Test
    @DisplayName("update should update custom tool successfully")
    void update_shouldUpdateTool_successfully() {
        // Given
        UpdateToolRequest request = UpdateToolRequest.builder()
                .name("updated_api")
                .description("Updated description")
                .endpoint("https://api.example.com/v2")
                .build();
        when(toolRepository.findById("tool_custom123")).thenReturn(Optional.of(customTool));
        when(toolMapper.toResponse(any())).thenReturn(ToolResponse.builder()
                .id("tool_custom123")
                .name("updated_api")
                .build());
        when(toolRepository.save(any())).thenReturn(customTool);

        // When
        ToolResponse result = toolService.update("tool_custom123", request);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getName()).isEqualTo("updated_api");
        verify(toolRepository).save(any());
    }

    @Test
    @DisplayName("delete should throw exception when tool not found")
    void delete_shouldThrowException_whenNotFound() {
        // Given
        when(toolRepository.findById("tool_unknown")).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> toolService.delete("tool_unknown"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Tool not found");
    }

    @Test
    @DisplayName("delete should throw exception for builtin tool")
    void delete_shouldThrowException_forBuiltinTool() {
        // Given
        when(toolRepository.findById("tool_builtin123")).thenReturn(Optional.of(builtinTool));

        // When & Then
        assertThatThrownBy(() -> toolService.delete("tool_builtin123"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Only CUSTOM type tools can be deleted");
    }

    @Test
    @DisplayName("delete should throw exception when tool is bound to agent")
    void delete_shouldThrowException_whenToolIsBound() {
        // Given
        when(toolRepository.findById("tool_custom123")).thenReturn(Optional.of(customTool));
        when(agentToolRepository.findByToolId("tool_custom123")).thenReturn(List.of(
                com.aiagent.admin.domain.entity.AgentTool.builder().build()
        ));

        // When & Then
        assertThatThrownBy(() -> toolService.delete("tool_custom123"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Tool is bound to agents");
    }

    @Test
    @DisplayName("delete should delete custom tool successfully")
    void delete_shouldDeleteTool_successfully() {
        // Given
        when(toolRepository.findById("tool_custom123")).thenReturn(Optional.of(customTool));
        when(agentToolRepository.findByToolId("tool_custom123")).thenReturn(List.of());

        // When
        toolService.delete("tool_custom123");

        // Then
        verify(toolRepository).deleteById("tool_custom123");
    }
}