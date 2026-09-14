package com.deanwagman.lumenmarsh.venueops.hello;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class HelloController {
    
    @GetMapping("/hello")
    public HelloResponse hello() {
        return new HelloResponse(
                        "Lumen Marsh systems online",
            "venueops-api",
            "operational"
        );
    }
}