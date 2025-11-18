package ch.sse2poll.api.demo;

import java.math.BigDecimal;

/**
 * DTO returned from the fake catalog.
 *
 * @param id          product identifier
 * @param name        human friendly name
 * @param description short description
 * @param price       retail price
 */
public record ProductDetails(String id, String name, String description, BigDecimal price) {
}
