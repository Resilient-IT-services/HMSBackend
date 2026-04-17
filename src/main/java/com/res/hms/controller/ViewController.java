package com.res.hms.controller;

import org.springframework.web.bind.annotation.GetMapping;

public class ViewController {
    @GetMapping("/login")
    public String loginPage(){
        return "login";
    }
}
