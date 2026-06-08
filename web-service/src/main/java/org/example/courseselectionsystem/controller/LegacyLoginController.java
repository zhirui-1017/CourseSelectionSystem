package org.example.courseselectionsystem.controller;

import java.net.URI;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class LegacyLoginController {

    @GetMapping("/login.html")
    public ResponseEntity<Void> legacyStaticLogin() {
        HttpHeaders headers = new HttpHeaders();
        headers.setLocation(URI.create("/login"));
        return new ResponseEntity<>(headers, HttpStatus.FOUND);
    }
}
