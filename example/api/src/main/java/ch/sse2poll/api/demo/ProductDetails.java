package ch.sse2poll.api.demo;

import java.math.BigDecimal;

public record ProductDetails(String id, String name, String description, BigDecimal price) {
}
