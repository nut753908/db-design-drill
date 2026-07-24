package com.nut753908.dbdesigndrill.service;

import com.nut753908.dbdesigndrill.dto.ReviewDesignRequest;
import com.nut753908.dbdesigndrill.dto.ReviewDesignResponse;
import com.nut753908.dbdesigndrill.entity.DesignSubmission;
import com.nut753908.dbdesigndrill.entity.Problem;
import com.nut753908.dbdesigndrill.exception.ResourceNotFoundException;
import com.nut753908.dbdesigndrill.repository.DesignSubmissionRepository;
import java.util.List;
import org.springframework.stereotype.Service;

/** DDL(テーブル設計)の提出・AIレビューを担うサービス */
@Service
public class DesignSubmissionService {

    private final DesignSubmissionRepository designSubmissionRepository;
    private final LambdaInvoker lambdaInvoker;

    public DesignSubmissionService(
            DesignSubmissionRepository designSubmissionRepository, LambdaInvoker lambdaInvoker) {
        this.designSubmissionRepository = designSubmissionRepository;
        this.lambdaInvoker = lambdaInvoker;
    }

    public DesignSubmission submit(Problem problem, String ddlText) {
        ReviewDesignRequest request = new ReviewDesignRequest(problem.getRequirementText(), ddlText);
        ReviewDesignResponse response = lambdaInvoker.invoke(request, ReviewDesignResponse.class);

        DesignSubmission submission =
                new DesignSubmission(problem, ddlText, response.reviewComment(), response.modelAnswer());
        return designSubmissionRepository.save(submission);
    }

    public DesignSubmission findById(Long id) {
        return designSubmissionRepository
                .findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("設計提出が見つかりません: id=" + id));
    }

    public DesignSubmission findByIdWithProblem(Long id) {
        return designSubmissionRepository
                .findByIdWithProblem(id)
                .orElseThrow(() -> new ResourceNotFoundException("設計提出が見つかりません: id=" + id));
    }

    public List<DesignSubmission> findByProblemId(Long problemId) {
        return designSubmissionRepository.findByProblemIdOrderByCreatedAtDesc(problemId);
    }
}
