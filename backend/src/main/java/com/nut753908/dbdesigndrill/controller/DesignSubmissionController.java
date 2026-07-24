package com.nut753908.dbdesigndrill.controller;

import com.nut753908.dbdesigndrill.entity.DesignSubmission;
import com.nut753908.dbdesigndrill.entity.Problem;
import com.nut753908.dbdesigndrill.service.DesignSubmissionService;
import com.nut753908.dbdesigndrill.service.ImplementationSubmissionService;
import com.nut753908.dbdesigndrill.service.ProblemService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class DesignSubmissionController {

    private final ProblemService problemService;
    private final DesignSubmissionService designSubmissionService;
    private final ImplementationSubmissionService implementationSubmissionService;

    public DesignSubmissionController(
            ProblemService problemService,
            DesignSubmissionService designSubmissionService,
            ImplementationSubmissionService implementationSubmissionService) {
        this.problemService = problemService;
        this.designSubmissionService = designSubmissionService;
        this.implementationSubmissionService = implementationSubmissionService;
    }

    @PostMapping("/problems/{problemId}/design-submissions")
    public String submit(@PathVariable Long problemId, @RequestParam String ddlText) {
        Problem problem = problemService.findById(problemId);
        DesignSubmission submission = designSubmissionService.submit(problem, ddlText);
        return "redirect:/design-submissions/" + submission.getId();
    }

    @GetMapping("/design-submissions/{id}")
    public String show(@PathVariable Long id, Model model) {
        DesignSubmission submission = designSubmissionService.findById(id);
        model.addAttribute("submission", submission);
        model.addAttribute(
                "implementationSubmissions", implementationSubmissionService.findByDesignSubmissionId(id));
        return "design/review";
    }
}
