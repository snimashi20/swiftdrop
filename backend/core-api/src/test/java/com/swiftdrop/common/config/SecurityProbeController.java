package com.swiftdrop.common.config;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class SecurityProbeController {

    @GetMapping("/api/v1/protected")
    String protectedRoute() {
        return "ok";
    }
}
