package ch.sse2poll.api.demo;

import ch.sse2poll.core.framework.annotation.PolledGet;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

/**
 * Controller exposing the polled endpoint used by the demo application.
 */
@RestController
@RequestMapping("/api/catalog")
public class CatalogController {

    private final CatalogManager catalogManager;

    public CatalogController(CatalogManager catalogManager) {
        this.catalogManager = catalogManager;
    }

    /**
     * Fetch a catalog item using the {@link PolledGet} orchestration.
     *
     * @param productId item identifier from the in-memory catalog
     * @return catalog item, or {@link HttpStatus#NOT_FOUND} if missing
     */
    @PolledGet
    @GetMapping("/products/{productId}")
    public ProductDetails getProduct(@PathVariable String productId) {
        return catalogManager.findProduct(productId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Unknown product " + productId));
    }
}
