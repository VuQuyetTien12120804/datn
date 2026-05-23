package com.bookingcare.backend_bookingcare.util;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.Collections;
import java.util.List;

public final class JsonStringListParser {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private JsonStringListParser() {
    }

    public static List<String> parse(String json) {
        if (json == null || json.isBlank()) {
            return Collections.emptyList();
        }
        try {
            return MAPPER.readValue(json, new TypeReference<List<String>>() {
            });
        } catch (Exception e) {
            return List.of(json);
        }
    }
}
