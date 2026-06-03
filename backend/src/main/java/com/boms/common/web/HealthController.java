package com.boms.common.web;

import com.boms.common.result.R;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/health")
public class HealthController {

    @GetMapping
    public R<Map<String, Object>> health() {
        return R.ok(Map.of("status", "UP", "app", "boms-backend", "version", "0.1.0"));
    }
}
