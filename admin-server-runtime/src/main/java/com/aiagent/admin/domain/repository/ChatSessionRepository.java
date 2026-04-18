package com.aiagent.admin.domain.repository;

import com.aiagent.admin.domain.entity.ChatSession;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 聊天会话数据访问接口
 * <p>
 * 提供聊天会话实体的 CRUD 操作和自定义查询方法，支持按创建人、关键词等条件查询。
 * </p>
 *
 * @see ChatSession
 */
@Repository
public interface ChatSessionRepository extends JpaRepository<ChatSession, String> {

    /**
     * 查询用户的所有会话（按更新时间倒序）
     *
     * @param createdBy 创建人
     * @return 会话列表
     */
    List<ChatSession> findByCreatedByOrderByUpdatedAtDesc(String createdBy);

    /**
     * 分页查询用户的会话（按更新时间倒序）
     *
     * @param createdBy 创建人
     * @param pageable  分页参数
     * @return 会话分页列表
     */
    Page<ChatSession> findByCreatedByOrderByUpdatedAtDesc(String createdBy, Pageable pageable);

    /**
     * 分页搜索用户的会话（按标题关键词，按更新时间倒序）
     *
     * @param createdBy 创建人
     * @param keyword   搜索关键词（可为空）
     * @param pageable  分页参数
     * @return 会话分页列表
     */
    @Query("SELECT cs FROM ChatSession cs WHERE cs.createdBy = :createdBy AND " +
           "(:keyword IS NULL OR LOWER(cs.title) LIKE LOWER(CONCAT('%', :keyword, '%'))) " +
           "ORDER BY cs.updatedAt DESC")
    Page<ChatSession> findByCreatedByAndKeyword(@Param("createdBy") String createdBy,
                                                @Param("keyword") String keyword,
                                                Pageable pageable);

    /**
     * 查询指定 ID 和创建人的会话
     * <p>
     * 用于验证会话所有权。
     * </p>
     *
     * @param id        会话 ID
     * @param createdBy 创建人
     * @return 会话 Optional
     */
    Optional<ChatSession> findByIdAndCreatedBy(String id, String createdBy);

    /**
     * 检查指定 ID 和创建人的会话是否存在
     *
     * @param id        会话 ID
     * @param createdBy 创建人
     * @return 是否存在
     */
    boolean existsByIdAndCreatedBy(String id, String createdBy);

    /**
     * 统计用户的活跃会话数量
     *
     * @param createdBy 创建人
     * @return 活跃会话数量
     */
    @Query("SELECT COUNT(cs) FROM ChatSession cs WHERE cs.createdBy = :createdBy AND cs.isActive = true")
    long countActiveByCreatedBy(@Param("createdBy") String createdBy);
}
