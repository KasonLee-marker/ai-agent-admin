package com.aiagent.admin.domain.repository;

import com.aiagent.admin.domain.entity.PromptVersion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Prompt 版本数据访问接口
 * <p>
 * 提供 Prompt 版本实体的 CRUD 操作和自定义查询方法，支持按模板 ID 查询版本历史，
 * 以及查询最新版本。
 * </p>
 *
 * @see PromptVersion
 */
@Repository
public interface PromptVersionRepository extends JpaRepository<PromptVersion, String> {

    /**
     * 查询模板的所有版本（按版本号倒序）
     *
     * @param promptId 模板 ID
     * @return 版本列表
     */
    List<PromptVersion> findByPromptIdOrderByVersionDesc(String promptId);

    /**
     * 查询模板的指定版本
     *
     * @param promptId 模板 ID
     * @param version  版本号
     * @return 版本 Optional
     */
    Optional<PromptVersion> findByPromptIdAndVersion(String promptId, Integer version);

    /**
     * 查询模板的最新版本
     * <p>
     * 用于获取模板当前生效的内容。
     * </p>
     *
     * @param promptId 模板 ID
     * @return 最新版本 Optional
     */
    Optional<PromptVersion> findTopByPromptIdOrderByVersionDesc(String promptId);
}
