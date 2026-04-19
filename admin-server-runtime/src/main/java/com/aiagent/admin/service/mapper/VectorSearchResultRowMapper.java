package com.aiagent.admin.service.mapper;

import com.aiagent.admin.api.dto.VectorSearchResult;
import org.springframework.jdbc.core.RowMapper;

import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * 向量搜索结果 RowMapper
 * <p>
 * 将数据库查询结果映射为 VectorSearchResult 对象，
 * 用于向量检索和 BM25 搜索结果映射。
 * </p>
 *
 * @see VectorSearchResult
 */
public class VectorSearchResultRowMapper implements RowMapper<VectorSearchResult> {

    @Override
    public VectorSearchResult mapRow(ResultSet rs, int rowNum) throws SQLException {
        return VectorSearchResult.builder()
                .chunkId(rs.getString("chunk_id"))
                .documentId(rs.getString("document_id"))
                .score(rs.getDouble("score"))
                .content(rs.getString("content"))
                .build();
    }
}