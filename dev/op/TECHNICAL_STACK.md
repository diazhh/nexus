# TECHNICAL STACK - PF & PO Modules

**Proyecto**: Nexus Production Facilities & Optimization
**Versión**: 1.0
**Fecha**: 2026-02-03

---

## 📋 Stack Overview

El stack tecnológico está basado en la arquitectura existente de **Nexus (ThingsBoard 4.3.0 Extended)** para mantener consistencia y aprovechar la infraestructura actual.

```
┌─────────────────────────────────────────────────────────────────┐
│                       TECHNOLOGY STACK                          │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│  FRONTEND                                                       │
│  ┌──────────────────────────────────────────────────────────┐  │
│  │  Angular 18.2.13 + TypeScript 5.5.4                      │  │
│  │  ├─ Angular Material 18.2.14                             │  │
│  │  ├─ NgRx (State Management)                              │  │
│  │  ├─ RxJS 7.8 (Reactive Programming)                      │  │
│  │  ├─ ECharts 5.4 (Visualizations)                         │  │
│  │  ├─ Leaflet 1.9 (Maps)                                   │  │
│  │  └─ Tailwind CSS 3.3 (Styling)                           │  │
│  └──────────────────────────────────────────────────────────┘  │
│                                                                 │
│  BACKEND                                                        │
│  ┌──────────────────────────────────────────────────────────┐  │
│  │  Java 17 + Spring Boot 3.4.10                            │  │
│  │  ├─ Spring Data JPA                                      │  │
│  │  ├─ Spring Security 6                                    │  │
│  │  ├─ Spring WebFlux (Reactive)                            │  │
│  │  ├─ Spring Cloud (Microservices)                         │  │
│  │  └─ Lombok (Boilerplate reduction)                       │  │
│  └──────────────────────────────────────────────────────────┘  │
│                                                                 │
│  DATA LAYER                                                     │
│  ┌──────────────────────────────────────────────────────────┐  │
│  │  PostgreSQL 14+ (Relational)                             │  │
│  │  TimescaleDB 2.11+ (Time-Series)                         │  │
│  │  Redis 7.0 (Cache)                                       │  │
│  │  RocksDB (State Store)                                   │  │
│  └──────────────────────────────────────────────────────────┘  │
│                                                                 │
│  MESSAGING & STREAMING                                          │
│  ┌──────────────────────────────────────────────────────────┐  │
│  │  Apache Kafka 3.3                                        │  │
│  │  Kafka Streams (Stream Processing)                       │  │
│  │  Kafka Connect (Integration)                             │  │
│  └──────────────────────────────────────────────────────────┘  │
│                                                                 │
│  IoT PROTOCOLS                                                  │
│  ┌──────────────────────────────────────────────────────────┐  │
│  │  MQTT (Eclipse Paho)                                     │  │
│  │  OPC-UA (Eclipse Milo)                                   │  │
│  │  Modbus (Modbus4j)                                       │  │
│  │  HTTP/REST                                               │  │
│  └──────────────────────────────────────────────────────────┘  │
│                                                                 │
│  MACHINE LEARNING                                               │
│  ┌──────────────────────────────────────────────────────────┐  │
│  │  Python 3.11                                             │  │
│  │  ├─ TensorFlow 2.15 / PyTorch 2.1                       │  │
│  │  ├─ scikit-learn 1.4                                    │  │
│  │  ├─ pandas 2.1, numpy 1.26                              │  │
│  │  ├─ Flask 3.0 (ML API)                                  │  │
│  │  └─ MLflow (Model Management)                            │  │
│  └──────────────────────────────────────────────────────────┘  │
│                                                                 │
│  DEVOPS & INFRASTRUCTURE                                        │
│  ┌──────────────────────────────────────────────────────────┐  │
│  │  Docker 24.0 + Docker Compose                           │  │
│  │  Kubernetes 1.28 (Orchestration)                         │  │
│  │  GitHub Actions / Jenkins (CI/CD)                        │  │
│  │  Terraform (Infrastructure as Code)                      │  │
│  │  Grafana + Prometheus (Monitoring)                       │  │
│  └──────────────────────────────────────────────────────────┘  │
│                                                                 │
└─────────────────────────────────────────────────────────────────┘
```

---

## 🎨 Frontend Stack

### Core Framework
```json
{
  "framework": "Angular 18.2.13",
  "language": "TypeScript 5.5.4",
  "build_tool": "Angular CLI + Webpack",
  "package_manager": "npm 10.2"
}
```

### UI Libraries

#### Angular Material 18.2.14
**Uso**: Componentes UI base
```typescript
import { MatButtonModule } from '@angular/material/button';
import { MatTableModule } from '@angular/material/table';
import { MatDialogModule } from '@angular/material/dialog';
```

**Componentes Principales**:
- Buttons, Inputs, Selects
- Tables con sorting/pagination
- Dialogs modales
- Snackbars (notificaciones)
- Menus y toolbars

#### ECharts 5.4.3
**Uso**: Gráficas y visualizaciones de datos
```typescript
import * as echarts from 'echarts';
```

**Tipos de gráficas**:
- Line charts (tendencias de telemetría)
- Bar charts (producción por pozo)
- Pie charts (distribución de sistemas de levantamiento)
- Gauge charts (Health Score)
- Scatter plots (eficiencia vs. producción)

#### Leaflet 1.9.4
**Uso**: Mapas geográficos
```typescript
import * as L from 'leaflet';
```

**Features**:
- Mapa de campo con pozos
- Markers con código de color por estado
- Clustering de pozos
- Heatmaps de producción
- Líneas de recolección

#### NgRx (State Management)
**Uso**: Gestión de estado global
```typescript
import { Store } from '@ngrx/store';
import { createAction, createReducer } from '@ngrx/store';
```

**Estado Gestionado**:
- Estado de pozos (wellState)
- Alarmas activas (alarmState)
- Recomendaciones (recommendationState)
- Configuración de usuario (userPreferencesState)

### Build & Dev Tools

```json
{
  "webpack": "5.89",
  "postcss": "8.4",
  "tailwindcss": "3.3",
  "prettier": "3.0",
  "eslint": "8.50",
  "jest": "29.7"
}
```

---

## ⚙️ Backend Stack

### Core Framework

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-parent</artifactId>
    <version>3.4.10</version>
</dependency>
```

**Java Version**: 17 (LTS)

### Spring Modules

#### Spring Data JPA
**Uso**: Persistencia de entidades
```java
@Entity
@Table(name = "pf_well")
public class PfWell extends BaseEntity {
    @ManyToOne
    @JoinColumn(name = "wellpad_id")
    private PfWellpad wellpad;
}
```

#### Spring Security
**Uso**: Autenticación y autorización
```java
@PreAuthorize("hasAuthority('TENANT_ADMIN')")
public PfWell createWell(PfWell well) { }
```

#### Spring WebFlux (Reactive)
**Uso**: APIs reactivas para telemetría en tiempo real
```java
@GetMapping(value = "/telemetry/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
public Flux<TelemetryData> streamTelemetry(@PathParam String wellId) {
    return telemetryService.getRealtimeStream(wellId);
}
```

### Key Dependencies

```xml
<!-- Lombok -->
<dependency>
    <groupId>org.projectlombok</groupId>
    <artifactId>lombok</artifactId>
    <version>1.18.30</version>
</dependency>

<!-- Validation -->
<dependency>
    <groupId>jakarta.validation</groupId>
    <artifactId>jakarta.validation-api</artifactId>
    <version>3.0.2</version>
</dependency>

<!-- Kafka -->
<dependency>
    <groupId>org.springframework.kafka</groupId>
    <artifactId>spring-kafka</artifactId>
    <version>3.1.0</version>
</dependency>

<!-- JSON Processing -->
<dependency>
    <groupId>com.fasterxml.jackson.core</groupId>
    <artifactId>jackson-databind</artifactId>
    <version>2.15.3</version>
</dependency>
```

---

## 🗄️ Data Layer

### PostgreSQL 14+
**Uso**: Base de datos relacional principal

**Schema**:
```sql
CREATE SCHEMA pf;
CREATE SCHEMA po;

-- Example table
CREATE TABLE pf.well (
    id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL,
    name VARCHAR(255) NOT NULL,
    wellpad_id UUID,
    status VARCHAR(50),
    created_time BIGINT NOT NULL,
    CONSTRAINT fk_wellpad FOREIGN KEY (wellpad_id) REFERENCES pf.wellpad(id)
);

CREATE INDEX idx_well_tenant ON pf.well(tenant_id);
CREATE INDEX idx_well_wellpad ON pf.well(wellpad_id);
```

**Connection Pool**: HikariCP
```yaml
spring:
  datasource:
    hikari:
      maximum-pool-size: 20
      minimum-idle: 5
      connection-timeout: 30000
```

### TimescaleDB 2.11+
**Uso**: Series temporales (telemetría)

**Hypertables**:
```sql
CREATE TABLE pf.telemetry (
    time TIMESTAMPTZ NOT NULL,
    entity_id UUID NOT NULL,
    key VARCHAR(255) NOT NULL,
    value_numeric DOUBLE PRECISION,
    value_string TEXT,
    value_boolean BOOLEAN
);

SELECT create_hypertable('pf.telemetry', 'time',
    chunk_time_interval => INTERVAL '1 day',
    if_not_exists => TRUE
);

-- Compression for old data
ALTER TABLE pf.telemetry SET (
    timescaledb.compress,
    timescaledb.compress_segmentby = 'entity_id, key'
);

SELECT add_compression_policy('pf.telemetry', INTERVAL '7 days');
```

**Retention Policy**:
```sql
-- Keep raw data for 30 days
SELECT add_retention_policy('pf.telemetry', INTERVAL '30 days');
```

**Continuous Aggregates** (para performance):
```sql
-- 1-minute aggregates
CREATE MATERIALIZED VIEW pf.telemetry_1min
WITH (timescaledb.continuous) AS
SELECT
    time_bucket('1 minute', time) AS bucket,
    entity_id,
    key,
    AVG(value_numeric) AS avg_value,
    MAX(value_numeric) AS max_value,
    MIN(value_numeric) AS min_value
FROM pf.telemetry
GROUP BY bucket, entity_id, key;

SELECT add_continuous_aggregate_policy('pf.telemetry_1min',
    start_offset => INTERVAL '1 hour',
    end_offset => INTERVAL '1 minute',
    schedule_interval => INTERVAL '1 minute');
```

### Redis 7.0
**Uso**: Cache de datos frecuentes

**Data Structures**:
```
# Current well status
well:{wellId}:status → JSON string
TTL: 5 minutes

# Latest telemetry
well:{wellId}:telemetry:latest → Hash
TTL: 1 minute

# Active alarms
alarms:active → Sorted Set (by timestamp)

# Session data
session:{sessionId} → Hash
TTL: 30 minutes
```

**Configuration**:
```yaml
spring:
  redis:
    host: localhost
    port: 6379
    timeout: 2000ms
    jedis:
      pool:
        max-active: 8
        max-idle: 8
        min-idle: 0
```

### RocksDB
**Uso**: State store para Kafka Streams

**Purpose**: Mantener estado de procesamiento de streams
- Windowed aggregations
- Join state
- Deduplication state

---

## 📨 Messaging & Streaming

### Apache Kafka 3.3

**Topics**:
```
pf.telemetry.raw        - Telemetría cruda desde MQTT
pf.telemetry.validated  - Telemetría validada
pf.alarms               - Alarmas generadas
pf.events               - Eventos operacionales
po.recommendations      - Recomendaciones generadas
po.setpoint-changes     - Cambios de setpoint
```

**Configuration**:
```yaml
spring:
  kafka:
    bootstrap-servers: localhost:9092
    consumer:
      group-id: nexus-pf-consumer
      auto-offset-reset: earliest
      key-deserializer: org.apache.kafka.common.serialization.StringDeserializer
      value-deserializer: org.springframework.kafka.support.serializer.JsonDeserializer
    producer:
      key-serializer: org.apache.kafka.common.serialization.StringSerializer
      value-serializer: org.springframework.kafka.support.serializer.JsonSerializer
      acks: all
      retries: 3
```

**Kafka Streams**:
```java
@Bean
public KStream<String, TelemetryData> telemetryStream(
    StreamsBuilder builder) {

    return builder.stream("pf.telemetry.raw")
        .filter((key, value) -> value.getQuality() > 0.9)
        .mapValues(this::enrichWithMetadata)
        .to("pf.telemetry.validated");
}
```

---

## 🌐 IoT Protocols

### MQTT (Eclipse Paho)
**Uso**: Comunicación con devices de campo

```xml
<dependency>
    <groupId>org.eclipse.paho</groupId>
    <artifactId>org.eclipse.paho.client.mqttv3</artifactId>
    <version>1.2.5</version>
</dependency>
```

**Topic Structure**:
```
v1/devices/{deviceId}/telemetry    - Device → Platform
v1/devices/{deviceId}/attributes   - Device → Platform
v1/devices/{deviceId}/rpc/request  - Platform → Device
v1/devices/{deviceId}/rpc/response - Device → Platform
```

**QoS**: QoS 1 (at least once delivery)

### OPC-UA (Eclipse Milo)
**Uso**: Integración con PLCs y SCADA

```xml
<dependency>
    <groupId>org.eclipse.milo</groupId>
    <artifactId>sdk-client</artifactId>
    <version>0.6.11</version>
</dependency>
```

**Endpoints**:
```
opc.tcp://scada-server:4840/nexus/pf
```

**Node IDs**:
```
ns=2;s=Well.ABC123.Frequency
ns=2;s=Well.ABC123.Current
ns=2;s=Well.ABC123.Temperature
```

### Modbus (Modbus4j)
**Uso**: RTUs y PLCs legacy

```xml
<dependency>
    <groupId>com.infiniteautomation</groupId>
    <artifactId>modbus4j</artifactId>
    <version>3.0.6</version>
</dependency>
```

**Supported**:
- Modbus TCP
- Modbus RTU (via serial)
- Function codes: 03 (Read Holding Registers), 06 (Write Single Register)

---

## 🤖 Machine Learning Stack

### Python 3.11

**ML Service** (Microservicio separado):
```
ml-service/
├── models/
│   ├── esp_failure_predictor.py
│   ├── anomaly_detector.py
│   └── production_forecaster.py
├── training/
│   ├── train_esp_model.py
│   └── evaluate_model.py
├── api/
│   └── app.py (Flask REST API)
└── requirements.txt
```

### Key Libraries

```txt
# requirements.txt
tensorflow==2.15.0
torch==2.1.0
scikit-learn==1.4.0
pandas==2.1.4
numpy==1.26.3
scipy==1.11.4
flask==3.0.0
mlflow==2.9.2
joblib==1.3.2
```

### TensorFlow / PyTorch
**Uso**: Deep learning para predicción de fallas

**Arquitectura típica (LSTM para ESP failure)**:
```python
import tensorflow as tf
from tensorflow.keras import layers

model = tf.keras.Sequential([
    layers.LSTM(64, return_sequences=True, input_shape=(timesteps, features)),
    layers.Dropout(0.2),
    layers.LSTM(32, return_sequences=False),
    layers.Dropout(0.2),
    layers.Dense(16, activation='relu'),
    layers.Dense(1, activation='sigmoid')  # Binary: fail / no fail
])

model.compile(
    optimizer='adam',
    loss='binary_crossentropy',
    metrics=['accuracy', 'precision', 'recall']
)
```

### scikit-learn
**Uso**: Modelos tradicionales ML

**Anomaly Detection (Isolation Forest)**:
```python
from sklearn.ensemble import IsolationForest

model = IsolationForest(
    n_estimators=100,
    contamination=0.05,  # 5% expected anomalies
    random_state=42
)

model.fit(X_train)
predictions = model.predict(X_test)  # -1 = anomaly, 1 = normal
```

### MLflow
**Uso**: Model management y tracking

```python
import mlflow
import mlflow.tensorflow

with mlflow.start_run():
    mlflow.log_param("lstm_units", 64)
    mlflow.log_param("dropout_rate", 0.2)

    # Training
    history = model.fit(X_train, y_train, epochs=50)

    # Log metrics
    mlflow.log_metric("accuracy", history.history['accuracy'][-1])
    mlflow.log_metric("val_accuracy", history.history['val_accuracy'][-1])

    # Save model
    mlflow.tensorflow.log_model(model, "esp_failure_model")
```

### Flask API
**Uso**: Exponer modelos ML como REST API

```python
from flask import Flask, request, jsonify
import mlflow.tensorflow

app = Flask(__name__)

# Load model
model = mlflow.tensorflow.load_model("models:/esp_failure_model/production")

@app.route('/predict/esp-failure', methods=['POST'])
def predict_esp_failure():
    data = request.json
    features = extract_features(data['telemetry'])

    prediction = model.predict(features)
    probability = float(prediction[0][0])

    return jsonify({
        'well_id': data['well_id'],
        'failure_probability': probability,
        'days_to_failure': estimate_days(probability),
        'confidence': calculate_confidence(features)
    })

if __name__ == '__main__':
    app.run(host='0.0.0.0', port=5000)
```

---

## 🐳 DevOps & Infrastructure

### Docker

**Dockerfile (Backend)**:
```dockerfile
FROM eclipse-temurin:17-jre-alpine

WORKDIR /app

COPY target/nexus-pf-*.jar app.jar

ENV JAVA_OPTS="-Xms512m -Xmx2g -XX:+UseG1GC"

EXPOSE 8080

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
```

**Dockerfile (ML Service)**:
```dockerfile
FROM python:3.11-slim

WORKDIR /app

COPY requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt

COPY . .

EXPOSE 5000

CMD ["python", "api/app.py"]
```

**Docker Compose** (Development):
```yaml
version: '3.8'

services:
  postgres:
    image: timescale/timescaledb:latest-pg14
    ports:
      - "5432:5432"
    environment:
      POSTGRES_DB: nexus
      POSTGRES_USER: nexus
      POSTGRES_PASSWORD: nexus
    volumes:
      - postgres_data:/var/lib/postgresql/data

  redis:
    image: redis:7-alpine
    ports:
      - "6379:6379"

  kafka:
    image: confluentinc/cp-kafka:7.5.0
    ports:
      - "9092:9092"
    environment:
      KAFKA_BROKER_ID: 1
      KAFKA_ZOOKEEPER_CONNECT: zookeeper:2181
      KAFKA_ADVERTISED_LISTENERS: PLAINTEXT://kafka:9092

  zookeeper:
    image: confluentinc/cp-zookeeper:7.5.0
    environment:
      ZOOKEEPER_CLIENT_PORT: 2181

  nexus-backend:
    build: ./backend
    ports:
      - "8080:8080"
    depends_on:
      - postgres
      - redis
      - kafka
    environment:
      SPRING_DATASOURCE_URL: jdbc:postgresql://postgres:5432/nexus
      SPRING_REDIS_HOST: redis
      SPRING_KAFKA_BOOTSTRAP_SERVERS: kafka:9092

  ml-service:
    build: ./ml-service
    ports:
      - "5000:5000"
    volumes:
      - ml_models:/app/models

volumes:
  postgres_data:
  ml_models:
```

### Kubernetes

**Deployment** (Backend):
```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: nexus-pf-backend
  namespace: nexus
spec:
  replicas: 3
  selector:
    matchLabels:
      app: nexus-pf-backend
  template:
    metadata:
      labels:
        app: nexus-pf-backend
    spec:
      containers:
      - name: backend
        image: nexus-registry/pf-backend:latest
        ports:
        - containerPort: 8080
        env:
        - name: SPRING_PROFILES_ACTIVE
          value: "production"
        - name: SPRING_DATASOURCE_URL
          valueFrom:
            secretKeyRef:
              name: nexus-secrets
              key: database-url
        resources:
          requests:
            memory: "1Gi"
            cpu: "500m"
          limits:
            memory: "2Gi"
            cpu: "1000m"
        livenessProbe:
          httpGet:
            path: /actuator/health
            port: 8080
          initialDelaySeconds: 60
          periodSeconds: 10
        readinessProbe:
          httpGet:
            path: /actuator/health/readiness
            port: 8080
          initialDelaySeconds: 30
          periodSeconds: 5
```

**Service**:
```yaml
apiVersion: v1
kind: Service
metadata:
  name: nexus-pf-backend-svc
  namespace: nexus
spec:
  type: LoadBalancer
  selector:
    app: nexus-pf-backend
  ports:
  - protocol: TCP
    port: 80
    targetPort: 8080
```

### CI/CD (GitHub Actions)

```yaml
name: CI/CD Pipeline

on:
  push:
    branches: [ main, develop ]
  pull_request:
    branches: [ main ]

jobs:
  build:
    runs-on: ubuntu-latest

    steps:
    - uses: actions/checkout@v3

    - name: Set up JDK 17
      uses: actions/setup-java@v3
      with:
        java-version: '17'
        distribution: 'temurin'

    - name: Build with Maven
      run: mvn clean install -DskipTests

    - name: Run Unit Tests
      run: mvn test

    - name: Run Integration Tests
      run: mvn verify -Pintegration-tests

    - name: SonarQube Scan
      run: mvn sonar:sonar -Dsonar.projectKey=nexus-pf

    - name: Build Docker Image
      run: docker build -t nexus/pf-backend:${{ github.sha }} .

    - name: Push to Registry
      run: docker push nexus/pf-backend:${{ github.sha }}

    - name: Deploy to Dev
      if: github.ref == 'refs/heads/develop'
      run: kubectl set image deployment/nexus-pf-backend backend=nexus/pf-backend:${{ github.sha }} -n nexus-dev
```

### Monitoring (Grafana + Prometheus)

**Spring Boot Actuator** (metrics exposure):
```yaml
management:
  endpoints:
    web:
      exposure:
        include: health,metrics,prometheus
  metrics:
    export:
      prometheus:
        enabled: true
```

**Prometheus Scrape Config**:
```yaml
scrape_configs:
  - job_name: 'nexus-pf-backend'
    metrics_path: '/actuator/prometheus'
    static_configs:
      - targets: ['nexus-pf-backend-svc:8080']
```

**Grafana Dashboards**:
- JVM metrics (heap, CPU, threads)
- HTTP request metrics (rate, latency, errors)
- Database connection pool
- Kafka consumer lag
- Custom business metrics (pozos monitoreados, alarmas activas)

---

## 🔧 Development Tools

### IDEs
- **IntelliJ IDEA Ultimate 2023.3** (Backend)
- **WebStorm 2023.3** (Frontend)
- **PyCharm Professional 2023.3** (ML Service)

### Code Quality
- **SonarQube 10.3** (Static analysis)
- **Checkstyle** (Java style)
- **ESLint + Prettier** (TypeScript style)
- **Black** (Python formatter)

### Testing
- **JUnit 5** (Unit tests Java)
- **Mockito** (Mocking)
- **TestContainers** (Integration tests)
- **Jest** (Unit tests TypeScript)
- **Cypress** (E2E tests)
- **pytest** (Python tests)

### API Documentation
- **Swagger / OpenAPI 3.0**
- **Springdoc OpenAPI**

```java
@Configuration
public class OpenApiConfig {
    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
            .info(new Info()
                .title("Nexus PF/PO API")
                .version("1.0")
                .description("Production Facilities & Optimization APIs"));
    }
}
```

Acceso: `http://localhost:8080/swagger-ui.html`

---

## 📊 Technology Decision Matrix

| Decisión | Opciones Consideradas | Opción Elegida | Justificación |
|----------|----------------------|----------------|---------------|
| **Backend Language** | Java, Python, Go | Java 17 | Consistencia con ThingsBoard, ecosystem maduro |
| **Frontend Framework** | Angular, React, Vue | Angular 18 | Ya usado en Nexus, TypeScript nativo |
| **Database** | PostgreSQL, MySQL, Oracle | PostgreSQL 14 | Open source, JSON support, extensible |
| **Time-Series DB** | InfluxDB, TimescaleDB, Cassandra | TimescaleDB | Compatible con PostgreSQL, SQL queries |
| **Message Queue** | Kafka, RabbitMQ, Redis Streams | Kafka | Ya usado en ThingsBoard, stream processing |
| **Cache** | Redis, Memcached | Redis | Data structures ricas, pub/sub |
| **ML Framework** | TensorFlow, PyTorch, scikit-learn | TensorFlow + scikit-learn | TF para DL, sklearn para traditional ML |
| **Containerization** | Docker, Podman | Docker | Estándar de industria, ecosistema amplio |
| **Orchestration** | Kubernetes, Docker Swarm | Kubernetes | Estándar para producción, cloud-agnostic |
| **CI/CD** | GitHub Actions, Jenkins, GitLab CI | GitHub Actions | Integración nativa con GitHub, fácil setup |

---

## 🔐 Security Considerations

### Authentication & Authorization
- **JWT** tokens (siguiendo estándar ThingsBoard)
- **Role-Based Access Control (RBAC)**
- **Multi-tenant isolation** a nivel de base de datos

### Data Encryption
- **TLS 1.3** para todas las comunicaciones
- **Encryption at rest** para base de datos (PostgreSQL native)
- **Secrets management** con Kubernetes Secrets o Vault

### API Security
- **Rate limiting** (100 requests/min por usuario)
- **Input validation** con Jakarta Validation
- **SQL injection prevention** con prepared statements
- **XSS prevention** con sanitización de inputs

---

## 📦 Third-Party Services

| Servicio | Provider | Uso | Costo Estimado |
|----------|----------|-----|----------------|
| **Cloud Hosting** | AWS / Azure / GCP | Compute, Storage | $5K-8K/mes |
| **Monitoring** | DataDog / New Relic | APM, Logs | $2K/mes |
| **Error Tracking** | Sentry | Error monitoring | $500/mes |
| **Email Service** | SendGrid | Notificaciones | $200/mes |
| **SMS Service** | Twilio | Alertas SMS | $500/mes |

---

## 🚀 Performance Targets

| Métrica | Target | Medición |
|---------|--------|----------|
| **API Latency (p95)** | < 200ms | Prometheus |
| **Telemetry Processing** | < 1 seg | Kafka lag |
| **Database Query Time** | < 100ms (p95) | pg_stat_statements |
| **Frontend Page Load** | < 2 seg | Lighthouse |
| **Concurrent Users** | 1000+ | Load testing (JMeter) |
| **Throughput** | 10K req/sec | Gatling |

---

## 📚 Referencias

- [Spring Boot Documentation](https://docs.spring.io/spring-boot/docs/current/reference/html/)
- [Angular Documentation](https://angular.io/docs)
- [TimescaleDB Best Practices](https://docs.timescale.com/timescaledb/latest/how-to-guides/)
- [Apache Kafka Documentation](https://kafka.apache.org/documentation/)
- [TensorFlow Guide](https://www.tensorflow.org/guide)
- [Kubernetes Documentation](https://kubernetes.io/docs/home/)

---

**Última Actualización**: 2026-02-03
**Mantenedor**: Tech Lead Backend / Architect
