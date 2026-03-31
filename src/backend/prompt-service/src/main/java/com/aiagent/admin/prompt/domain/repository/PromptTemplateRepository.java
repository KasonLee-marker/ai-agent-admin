package com.aiagent.admin.prompt.domain.repository;

import com.aiagent.admin.prompt.domain.entity.PromptTemplate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PromptTemplateRepository extends JpaRepository<PromptTemplate, String> {

    @Query("SELECT p FROM PromptTemplate p WHERE " +
           "(:category IS NULL OR p.category = :category) AND " +
           "(:tag IS NULL OR p.tags LIKE %:tag%) AND " +
           "(:keyword IS NULL OR p.name LIKE %:keyword% OR p.description LIKE %:keyword%)")
    Page<PromptTemplate> findByFilters(@Param("category") String category,
                                       @Param("tag") String tag,
                                       @Param("keyword") String keyword,
                                       Pageable pageable);

    List<PromptTemplate> findByCategory(String category);

    boolean existsByName(String name);
}
