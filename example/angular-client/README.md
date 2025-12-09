# Angular client (SSE2Poll demo)

This Angular app calls the Spring Boot demo API via the `@sse2poll/polling-client` library.

## Prerequisites
From the repo root:
1. Build and pack the polling client once so the local dependency can be installed:
   ```bash
   cd client
   npm install
   npm run build
   npm pack    # produces sse2poll-polling-client-0.1.2.tgz
   ```
2. Install this Angular app with the packed package:
   ```bash
   cd ../example/angular-client
    npm install ../client/sse2poll-polling-client-0.1.2.tgz
   ```

## Run the demo
Start the Spring Boot API (from repo root):
```bash
cd example/api
./mvnw spring-boot:run   # or mvnw.cmd on Windows
```

Then run the Angular client (from repo root):
```bash
cd example/angular-client
npm start
# dev server runs at http://localhost:4200 with /api proxied to http://localhost:8080
```

On the page, choose a product id (`keyboard`, `mouse`, `monitor`, `dock`) and click **Fetch with polling**.
The first response is a `202 {jobId}`; the interceptor keeps polling `/api/catalog/products/{id}` until the
final `200` payload arrives.
