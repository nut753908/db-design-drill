package com.nut753908.dbdesigndrill.controller;

import com.nut753908.dbdesigndrill.entity.DesignSubmission;
import com.nut753908.dbdesigndrill.entity.ImplementationSubmission;
import com.nut753908.dbdesigndrill.service.DesignSubmissionService;
import com.nut753908.dbdesigndrill.service.ImplementationSubmissionService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class ImplementationSubmissionController {

    private final DesignSubmissionService designSubmissionService;
    private final ImplementationSubmissionService implementationSubmissionService;

    public ImplementationSubmissionController(
            DesignSubmissionService designSubmissionService,
            ImplementationSubmissionService implementationSubmissionService) {
        this.designSubmissionService = designSubmissionService;
        this.implementationSubmissionService = implementationSubmissionService;
    }

    @PostMapping("/design-submissions/{designSubmissionId}/implementation-submissions")
    public String submit(@PathVariable Long designSubmissionId, @RequestParam String codeText) {
        DesignSubmission designSubmission = designSubmissionService.findByIdWithProblem(designSubmissionId);
        ImplementationSubmission submission = implementationSubmissionService.submit(designSubmission, codeText);
        return "redirect:/implementation-submissions/" + submission.getId();
    }

    @GetMapping("/implementation-submissions/{id}")
    public String show(@PathVariable Long id, Model model) {
        ImplementationSubmission submission = implementationSubmissionService.findById(id);
        model.addAttribute("submission", submission);
        return "implementation/review";
    }
}
