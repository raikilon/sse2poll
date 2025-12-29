# Angular client (SSE2Poll demo)

This Angular app calls the Spring Boot demo API via the `@sse2poll/polling-client` interceptor to demonstrate the kickoff + polling workflow end to end.

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
   npm install ../../client/sse2poll-polling-client-0.1.2.tgz
   ```

## Run the demo
Start the Spring Boot API (from repo root):
```bash
cd example/api
mvn spring-boot:run 
```

Then run the Angular client (from repo root):
```bash
cd example/angular-client
npm start
```

## What to try
- Use the dropdown to pick a product id (`keyboard`, `mouse`, `monitor`, `dock`) and click **Fetch with polling**.
- The kickoff call returns `202 { jobId }`; the interceptor polls `/api/catalog/products/{id}` with `job` until a `200` payload arrives, then updates the UI.
- Switch products or refresh to watch a new polling cycle start with a fresh job id.
