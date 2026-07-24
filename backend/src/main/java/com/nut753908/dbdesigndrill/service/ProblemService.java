package com.nut753908.dbdesigndrill.service;

import com.nut753908.dbdesigndrill.dto.GenerateProblemRequest;
import com.nut753908.dbdesigndrill.dto.GenerateProblemResponse;
import com.nut753908.dbdesigndrill.entity.Difficulty;
import com.nut753908.dbdesigndrill.entity.Genre;
import com.nut753908.dbdesigndrill.entity.Problem;
import com.nut753908.dbdesigndrill.exception.ResourceNotFoundException;
import com.nut753908.dbdesigndrill.repository.ProblemRepository;
import java.util.List;
import org.springframework.stereotype.Service;

/** お題の生成・取得を担うサービス */
@Service
public class ProblemService {

    private final ProblemRepository problemRepository;
    private final LambdaInvoker lambdaInvoker;

    public ProblemService(ProblemRepository problemRepository, LambdaInvoker lambdaInvoker) {
        this.problemRepository = problemRepository;
        this.lambdaInvoker = lambdaInvoker;
    }

    public Problem generateProblem(Genre genre, Difficulty difficulty) {
        GenerateProblemRequest request = new GenerateProblemRequest(genre.getLabel(), difficulty.getLabel());
        GenerateProblemResponse response = lambdaInvoker.invoke(request, GenerateProblemResponse.class);

        Problem problem = new Problem(genre, difficulty, response.requirementText());
        return problemRepository.save(problem);
    }

    public Problem findById(Long id) {
        return problemRepository
                .findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("お題が見つかりません: id=" + id));
    }

    public List<Problem> findAll() {
        return problemRepository.findAll();
    }
}
