package com.nut753908.dbdesigndrill.controller;

import com.nut753908.dbdesigndrill.entity.Difficulty;
import com.nut753908.dbdesigndrill.entity.Genre;
import com.nut753908.dbdesigndrill.entity.Problem;
import com.nut753908.dbdesigndrill.service.DesignSubmissionService;
import com.nut753908.dbdesigndrill.service.ProblemService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class ProblemController {

    private final ProblemService problemService;
    private final DesignSubmissionService designSubmissionService;

    public ProblemController(ProblemService problemService, DesignSubmissionService designSubmissionService) {
        this.problemService = problemService;
        this.designSubmissionService = designSubmissionService;
    }

    @ModelAttribute("genres")
    public Genre[] genres() {
        return Genre.values();
    }

    @ModelAttribute("difficulties")
    public Difficulty[] difficulties() {
        return Difficulty.values();
    }

    @GetMapping("/problems/new")
    public String newProblem() {
        return "problem/new";
    }

    @PostMapping("/problems")
    public String create(@RequestParam Genre genre, @RequestParam Difficulty difficulty) {
        Problem problem = problemService.generateProblem(genre, difficulty);
        return "redirect:/problems/" + problem.getId();
    }

    @GetMapping("/problems/{id}")
    public String show(@PathVariable Long id, Model model) {
        Problem problem = problemService.findById(id);
        model.addAttribute("problem", problem);
        model.addAttribute("designSubmissions", designSubmissionService.findByProblemId(id));
        return "problem/show";
    }
}
