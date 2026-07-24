package com.nut753908.dbdesigndrill.repository;

import com.nut753908.dbdesigndrill.entity.DesignSubmission;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface DesignSubmissionRepository extends JpaRepository<DesignSubmission, Long> {

    List<DesignSubmission> findByProblemIdOrderByCreatedAtDesc(Long problemId);

    @Query("SELECT ds FROM DesignSubmission ds JOIN FETCH ds.problem WHERE ds.id = :id")
    Optional<DesignSubmission> findByIdWithProblem(@Param("id") Long id);
}
