package com.supplychain.visualization;

import com.supplychain.visualization.VisualizationDtos.NetworkResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/visualization")
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Visualization")
public class VisualizationController {

    private final VisualizationService visualizationService;

    public VisualizationController(VisualizationService visualizationService) {
        this.visualizationService = visualizationService;
    }

    @GetMapping("/network")
    public NetworkResponse network() {
        return visualizationService.network();
    }
}
