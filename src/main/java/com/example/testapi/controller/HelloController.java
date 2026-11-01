package com.example.testapi.controller;

import com.example.testapi.model.Greet;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;


@RestController
public class HelloController {

    @GetMapping("/api/hello")
    public Greet greet(){
        return new Greet("Hello World!!");
    }
}
