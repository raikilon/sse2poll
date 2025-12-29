import { Component, signal } from '@angular/core';
import { CurrencyPipe, JsonPipe } from '@angular/common';
import { HttpErrorResponse, httpResource } from '@angular/common/http';
import { withPolling } from '@sse2poll/polling-client/angular';

interface ProductDetails {
  id: string;
  name: string;
  description: string;
  price: number;
}

@Component({
  selector: 'app-root',
  imports: [CurrencyPipe, JsonPipe],
  templateUrl: './app.html',
  styleUrl: './app.css'
})
export class App {
  readonly productId = signal<string | null>(null);

  readonly availableProducts = ['keyboard', 'mouse', 'monitor', 'dock'];

  readonly productResource = httpResource<ProductDetails>(() => {
    const id = this.productId();

    if (!id) {
      return undefined;
    }

    return {
      url: `/api/catalog/products/${encodeURIComponent(id)}`,
      context: withPolling({
        waitMs: 1000,
        pollIntervalMs: 2000
      })
    };
  });

  onProductSelect(event: Event): void {
    const value = (event.target as HTMLSelectElement).value;
    this.productId.set(value || null);
  }

  formatError(err: unknown): string {
    if (err instanceof HttpErrorResponse) {
      if (err.status === 404) {
        return 'Product not found (404).';
      }
      return `Request failed with ${err.status} ${err.statusText || ''}`.trim();
    }
    if (err instanceof Error) {
      return err.message;
    }
    return 'Request failed.';
  }
}
