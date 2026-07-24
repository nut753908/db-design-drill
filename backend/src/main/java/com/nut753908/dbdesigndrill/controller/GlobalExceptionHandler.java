package com.nut753908.dbdesigndrill.controller;

import com.nut753908.dbdesigndrill.exception.LambdaInvocationException;
import com.nut753908.dbdesigndrill.exception.ResourceNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;

@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ResourceNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public String handleNotFound(ResourceNotFoundException e, Model model) {
        model.addAttribute("message", e.getMessage());
        return "error";
    }

    @ExceptionHandler(LambdaInvocationException.class)
    @ResponseStatus(HttpStatus.BAD_GATEWAY)
    public String handleLambdaError(LambdaInvocationException e, Model model) {
        model.addAttribute("message", "AI連携中にエラーが発生しました: " + e.getMessage());
        return "error";
    }
}
