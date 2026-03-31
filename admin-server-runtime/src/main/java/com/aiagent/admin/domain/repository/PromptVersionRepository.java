package com.aiagent.admin.domain.repository;

import com.aiagent.admin.domain.entity.PromptVersion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PromptVersionRepository extends JpaRepository<PromptVersion, String> {

    List<PromptVersion> findByPromptIdOrderByVersionDesc(String promptId);

    Optional<PromptVersion> findByPromptIdAndVersion(String promptId, Integer version);

    Optional<PromptVersion> findTopByPromptIdOrderByVersionDesc(String promptId);
}
