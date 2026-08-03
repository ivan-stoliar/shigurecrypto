# Shigure Crypto - Real-Time Arbitrage & Anomaly Detection Engine

A high-performance, event-driven trading system built to ingest, normalize, and analyze real-time cryptocurrency trade data across multiple exchanges (Binance, Coinbase). It detects cross-exchange price divergences and highlights arbitrage opportunities while mitigating alert fatigue.

## 🚀 Key Features

* **Real-Time Event Streaming**: Ingests raw websocket streams into **Apache Kafka** (KRaft mode) to decouple ingestion from downstream processing.
* **CQRS-lite Architecture**: Separates the high-volume write path (PostgreSQL Archiver) from the high-speed read path (Redis Cache) to optimize database performance.
* **Stateful Anomaly Detection**: A dedicated stream processor maintains in-memory rolling windows to detect cross-exchange price divergence (e.g., > 0.05% difference).
* **Alert Fatigue Mitigation**: Implements a 60-second cooldown window for anomalies to prevent downstream system crashes during sustained divergence events.
* **Idempotent Processing**: Ensures data integrity using database-level `UNIQUE` constraints (Flyway) and Kafka manual offset commits (`AckMode.MANUAL`), gracefully handling duplicate deliveries (`DataIntegrityViolationException`).
* **Resilient Error Handling**: Uses Spring Kafka's `@RetryableTopic` and Dead Letter Topics (DLT) for non-blocking retry mechanisms.

## 🛠️ Tech Stack

* **Backend**: Java 21, Spring Boot 3.4
* **Messaging**: Apache Kafka (KRaft), Spring Kafka
* **Database**: PostgreSQL (System of Record), Spring Data JPA
* **Caching**: Redis (Upstash/Local) for O(1) read performance
* **Migrations**: Flyway
* **Testing**: JUnit 5, Mockito, Awaitility, Embedded Kafka, H2 In-Memory DB
* **Infrastructure**: Docker, Docker Compose

## 🏗️ Architecture Flow

1. **Ingestion**: Exchange WebSockets push trades -> `trades.raw` Kafka topic.
2. **Archiving (Write Path)**: `TradeConsumer` reads `trades.raw` and persists robust, idempotent records to **PostgreSQL**.
3. **Stream Processing**: `AnomalyDetectionConsumer` reads `trades.raw`, calculates divergence, and emits events to `anomalies.detected` topic (with a cooldown).
4. **Caching (Read Path)**: `AnomalyCacheConsumer` reads `anomalies.detected` and updates **Redis**.
5. **REST API**: Client requests to `/api/anomalies` fetch instantly from the Redis cache.

## ⚙️ How to Run Locally

### 1. Start Infrastructure (Kafka, Postgres, Redis)
Ensure you have Docker installed and running, then spin up the required infrastructure:
```bash
docker-compose up -d
```

### 2. Run the Spring Boot Application
```bash
# On Windows
.\mvnw.cmd spring-boot:run

# On Mac/Linux
./mvnw spring-boot:run
```

### 3. Run the Test Suite
The project includes a robust suite of unit and integration tests (including an `@EmbeddedKafka` integration test for retry logic).
```bash
.\mvnw.cmd clean test
```

## 📈 Future Roadmap
- [ ] Implement CI/CD pipeline using GitHub Actions
- [ ] Containerize the Spring Boot application using a `Dockerfile`
- [ ] Integrate Prometheus and Grafana for system observability and metrics