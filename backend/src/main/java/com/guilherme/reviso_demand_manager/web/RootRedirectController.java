package com.guilherme.reviso_demand_manager.web;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class RootRedirectController {

    private final String frontendBaseUrl;

    public RootRedirectController(@Value("${frontend.base-url:http://localhost:4200}") String frontendBaseUrl) {
        this.frontendBaseUrl = frontendBaseUrl;
    }

    @GetMapping({"/", "/index.html"})
    public ResponseEntity<Void> root() {
        return ResponseEntity.status(HttpStatus.FOUND)
                .header(HttpHeaders.LOCATION, frontendBaseUrl)
                .build();
    }
}
