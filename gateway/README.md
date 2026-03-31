# Go API Gateway (8090)

A lightweight but powerful reverse HTTP proxy implemented using the `Gin` framework to shield the internal Spring Boot API.

## Features
- **Frontend Reverse Proxy**: Serves the root route (`/`) and routes generic URLs straight back to the Java server's `static/index.html`.
- **API Forwarding**: Transparently proxies all requests beginning with `/api/v1/*` precisely to the Spring Boot instance operating on `:8080`.
- **Cross-Origin Configuration**: Uses `middleware.CORS()` to manage external headers safely.
- **Traffic Throttling (Rate Limiting)**: Applies `RateLimit()` guardrails configured dynamically (e.g., 20 requests per second) per user/IP.

## Execution
Run the compiled Go instance independent of Spring Boot.

```bash
cd gateway
go run main.go
```
