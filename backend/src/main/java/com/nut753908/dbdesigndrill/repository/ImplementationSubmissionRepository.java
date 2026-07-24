package com.nut753908.dbdesigndrill.repository;

import com.nut753908.dbdesigndrill.entity.ImplementationSubmission;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ImplementationSubmissionRepository extends JpaRepository<ImplementationSubmission, Long> {

    List<ImplementationSubmission> findByDesignSubmissionIdOrderByCreatedAtDesc(Long designSubmissionId);
}
