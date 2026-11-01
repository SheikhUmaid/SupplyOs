package com.supplychain.allocation.location;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import tools.jackson.databind.json.JsonMapper;

/** The fixed list of deliverable cities. It supplies customer coordinates without an external API. */
@Service
public class LocationService {

    public record City(String name, BigDecimal latitude, BigDecimal longitude) {}

    private final List<City> cities;

    public LocationService(JsonMapper jsonMapper) {
        try (InputStream json = new ClassPathResource("data/karnataka-cities.json").getInputStream()) {
            this.cities = Arrays.stream(jsonMapper.readValue(json, City[].class))
                    .sorted(Comparator.comparing(City::name))
                    .toList();
        } catch (IOException ex) {
            throw new UncheckedIOException("Could not read the city list", ex);
        }
    }

    public List<City> list() {
        return cities;
    }

    public Optional<City> find(String name) {
        String wanted = name.trim().toLowerCase(Locale.ROOT);
        return cities.stream().filter(city -> city.name().toLowerCase(Locale.ROOT).equals(wanted)).findFirst();
    }
}
