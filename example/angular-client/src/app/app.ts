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
  readonly productId = signal('keyboard');
  private readonly hasFetched = signal(false);

  readonly availableProducts = ['keyboard', 'mouse', 'monitor', 'dock'];

  readonly productResource = httpResource<ProductDetails | null>(() => {
    const productId = this.productId().trim();

    if (!this.hasFetched() || !productId) {
      return undefined;
    }

    return {
      url: `/api/catalog/products/${encodeURIComponent(productId)}`,
      context: withPolling({ waitMs: 1000, pollIntervalMs: 2000 })
    };
  });


  fetchProduct(): void {
    this.hasFetched.set(true);
    this.productResource.reload();
  }

  onProductSelect(event: Event): void {
    const value = (event.target as HTMLSelectElement)?.value ?? '';
    this.productId.set(value);
  }

  formatError(err: unknown): string {
    if (err instanceof HttpErrorResponse) {
      if (err.status === 404) {
        return 'Product not found (404). Try keyboard/mouse/monitor/dock.';
      }
      if (err.status > 0) {
        return `Request failed with ${err.status} ${err.statusText || ''}`.trim();
      }
      return err.message ?? 'Request failed.';
    }
    if (err instanceof Error) {
      return err.message;
    }
    return 'Request failed.';
  }
}
