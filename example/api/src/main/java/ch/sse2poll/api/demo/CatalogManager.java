package ch.sse2poll.api.demo;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Optional;

/**
 * Fake in-memory storage that purposely introduces random latency to simulate a slow data store.
 */
@Component
public class CatalogManager {

    private static final Map<String, ProductDetails> CATALOG = Map.of(
            "keyboard", new ProductDetails("keyboard", "Compact Keyboard", "60% mechanical keyboard", new BigDecimal("149.99")),
            "mouse", new ProductDetails("mouse", "Wireless Mouse", "Silent productivity mouse", new BigDecimal("79.99")),
            "monitor", new ProductDetails("monitor", "27\" Monitor", "1440p IPS panel", new BigDecimal("429.00")),
            "dock", new ProductDetails("dock", "USB-C Dock", "Power delivery docking station", new BigDecimal("189.50"))
    );

    public Optional<ProductDetails> findProduct(String productId) {
        simulateSlowQuery();
        return Optional.ofNullable(CATALOG.get(productId));
    }

    private void simulateSlowQuery() {
        long delay = 10000;
        try {
            Thread.sleep(delay);
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while querying fake catalog", ie);
        }
    }
}
