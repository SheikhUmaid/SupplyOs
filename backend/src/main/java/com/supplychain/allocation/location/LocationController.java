package com.supplychain.allocation.location;

import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/locations")
@Tag(name = "Locations")
public class LocationController {

    public record CityResponse(String name, String state) {}

    private final LocationService locationService;

    public LocationController(LocationService locationService) {
        this.locationService = locationService;
    }

    @GetMapping("/cities")
    public List<CityResponse> cities() {
        return locationService.list().stream()
                .map(city -> new CityResponse(city.name(), "Karnataka"))
                .toList();
    }
}
