package com.capstone.jobtracker.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class HomeController {

    @GetMapping("/")              // GET / → /home 리다이렉트
    public String root() {
        return "redirect:/home";
    }
}
