import { Component, inject, signal } from '@angular/core';
import { CurrencyPipe, JsonPipe } from '@angular/common';
import { HttpClient, HttpErrorResponse, httpResource } from '@angular/common/http';
import { withPolling } from '@sse2poll/polling-client/angular';

interface ProductDetails {
  id: string;
  name: string;
  description: string;
  price: number;
}

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [CurrencyPipe, JsonPipe],
  templateUrl: './app.html',
  styleUrl: './app.css'
})
export class App {
  private readonly http = inject(HttpClient);

  readonly productId = signal('keyboard');
  private readonly refreshKey = signal(-1);

  readonly availableProducts = ['keyboard', 'mouse', 'monitor', 'dock'];

  readonly productResource = httpResource<ProductDetails | null>(() => {
    const productId = this.productId().trim();
    const refresh = this.refreshKey();

    if (refresh < 0 || !productId) {
      return undefined; // ✅ must be undefined, not null
    }

    return {
      url: `/api/catalog/products/${encodeURIComponent(productId)}`,
      context: withPolling({ waitMs: 1000, pollIntervalMs: 400 })
    };
  });


  fetchProduct(): void {
    this.refreshKey.update(v => v + 1);
  }

  onProductInput(event: Event): void {
    const value = (event.target as HTMLInputElement)?.value ?? '';
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
