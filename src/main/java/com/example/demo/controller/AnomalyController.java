package com.example.demo.controller;

import com.example.demo.entity.Anomaly;
import com.example.demo.repository.AnomalyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/anomalies")
@RequiredArgsConstructor
public class AnomalyController {

    private final AnomalyRepository anomalyRepository;

    @GetMapping
    public List<Anomaly> getAnomalies() {
        return anomalyRepository.findTop100ByOrderByCreatedAtDesc();
    }
}
