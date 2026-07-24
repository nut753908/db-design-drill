package com.nut753908.dbdesigndrill.service;

import com.nut753908.dbdesigndrill.dto.ReviewImplementationRequest;
import com.nut753908.dbdesigndrill.dto.ReviewImplementationResponse;
import com.nut753908.dbdesigndrill.entity.DesignSubmission;
import com.nut753908.dbdesigndrill.entity.ImplementationSubmission;
import com.nut753908.dbdesigndrill.exception.ResourceNotFoundException;
import com.nut753908.dbdesigndrill.repository.ImplementationSubmissionRepository;
import java.util.List;
import org.springframework.stereotype.Service;

/** JPA実装コードの提出・AIレビューを担うサービス */
@Service
public class ImplementationSubmissionService {

    private final ImplementationSubmissionRepository implementationSubmissionRepository;
    private final LambdaInvoker lambdaInvoker;

    public ImplementationSubmissionService(
            ImplementationSubmissionRepository implementationSubmissionRepository, LambdaInvoker lambdaInvoker) {
        this.implementationSubmissionRepository = implementationSubmissionRepository;
        this.lambdaInvoker = lambdaInvoker;
    }

    public ImplementationSubmission submit(DesignSubmission designSubmission, String codeText) {
        ReviewImplementationRequest request = new ReviewImplementationRequest(
                designSubmission.getProblem().getRequirementText(), designSubmission.getDdlText(), codeText);
        ReviewImplementationResponse response =
                lambdaInvoker.invoke(request, ReviewImplementationResponse.class);

        ImplementationSubmission submission =
                new ImplementationSubmission(designSubmission, codeText, response.reviewComment());
        return implementationSubmissionRepository.save(submission);
    }

    public ImplementationSubmission findById(Long id) {
        return implementationSubmissionRepository
                .findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("実装提出が見つかりません: id=" + id));
    }

    public List<ImplementationSubmission> findByDesignSubmissionId(Long designSubmissionId) {
        return implementationSubmissionRepository.findByDesignSubmissionIdOrderByCreatedAtDesc(designSubmissionId);
    }
}
