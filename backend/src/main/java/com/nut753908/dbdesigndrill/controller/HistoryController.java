package com.nut753908.dbdesigndrill.controller;

import com.nut753908.dbdesigndrill.service.ProblemService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class HistoryController {

    private final ProblemService problemService;

    public HistoryController(ProblemService problemService) {
        this.problemService = problemService;
    }

    @GetMapping("/")
    public String index() {
        return "redirect:/history";
    }

    @GetMapping("/history")
    public String list(Model model) {
        model.addAttribute("problems", problemService.findAll());
        return "history/list";
    }
}
