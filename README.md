# FaceID — Production-Grade Face Recognition System

A Hikvision-style biometric access system built with a clean microservice architecture.

```
┌──────────────────────────────────────────────────────────────────────┐
│                        FaceID Architecture                           │
│                                                                      │
│  Browser           Spring Boot           FastAPI           Postgres  │
│  :3000       ───►  :8080           ───►  :8000        ───► :5432    │
│                    Java 21               Python 3.11                 │
│                    REST API              InsightFace                 │
│                    Business Logic        ArcFace R100                │
│                    DB access             Cosine Similarity           │
└──────────────────────────────────────────────────────────────────────┘
```

---

## Tech Stack

| Layer | Technology |
|---|---|
| Frontend | React 18 + Vite + react-webcam |
| Backend API | Java 21, Spring Boot 3.5, Spring Data JPA |
| Face Recognition | Python 3.11, FastAPI, InsightFace (ArcFace R100) |
| Database | PostgreSQL 16 |
| Orchestration | Docker Compose |

---

## Prerequisites

| Tool | Min Version |
|---|---|
| Docker | 24+ |
| Docker Compose | 2.20+ |

> **No Java, Python, or Node.js needed on your machine** — everything runs inside Docker.

---

## Quick Start

```bash
# 1. Clone / enter the project
cd face-recognition

# 2. Start everything
docker compose up --build

# (First run downloads InsightFace models — takes 3–5 minutes)

# 3. Open the UI
open http://localhost:3000
```

Services will be ready when you see:
```
faceid-backend  | Started FaceIdApplication in X seconds
```

---

## Project Structure

```
face-recognition/
├── docker-compose.yml          # Orchestrates all 4 services
├── Makefile                    # Convenience commands
│
├── backend/                    # Spring Boot 3.5 (Java 21)
│   ├── Dockerfile
│   ├── pom.xml
│   └── src/main/
│       ├── java/com/faceid/
│       │   ├── FaceIdApplication.java
│       │   ├── controller/
│       │   │   ├── UserController.java        # POST /api/users/register, GET, DELETE
│       │   │   ├── RecognitionController.java # POST /api/recognize
│       │   │   └── GlobalExceptionHandler.java
│       │   ├── service/
│       │   │   ├── FaceRecognitionService.java
│       │   │   └── PythonServiceClient.java   # HTTP client for Python service
│       │   ├── repository/
│       │   │   └── UserRepository.java
│       │   ├── model/
│       │   │   └── UserEntity.java            # JPA entity (id, name, embedding[])
│       │   ├── dto/
│       │   │   └── FaceDto.java               # All request/response DTOs
│       │   └── config/
│       │       └── AppConfig.java             # RestTemplate bean
│       └── resources/
│           ├── application.yml
│           └── init.sql                       # PostgreSQL schema
│
├── python-service/             # FastAPI (Python 3.11)
│   ├── Dockerfile
│   ├── requirements.txt
│   ├── main.py                 # FastAPI app, endpoints
│   ├── face_engine.py          # InsightFace/DeepFace wrapper
│   └── models.py               # Pydantic models
│
└── front/                      # React 18 + Vite
    ├── Dockerfile              # Builds & serves via nginx
    ├── nginx.conf
    ├── package.json
    ├── vite.config.js
    └── src/
        ├── App.jsx             # Router + sidebar layout
        ├── api.js              # Axios API client
        ├── hooks/
        │   └── useCapture.js   # Webcam capture hook
        ├── components/
        │   └── CameraView.jsx  # Camera + overlay + scan animation
        └── pages/
            ├── RecognizePage.jsx   # Real-time recognition (1.5s interval)
            ├── RegisterPage.jsx    # Face enrollment
            └── UsersPage.jsx       # User management
```

---

## API Reference

### Spring Boot (`:8080`)

#### Register a new face
```http
POST /api/users/register
Content-Type: application/json

{
  "name": "John Doe",
  "image": "<base64-encoded JPEG/PNG>"
}
```
Response:
```json
{
  "id": "550e8400-e29b-41d4-a716-446655440000",
  "name": "John Doe",
  "createdAt": "2024-01-15T10:30:00",
  "message": "User registered successfully"
}
```

#### Recognize a face
```http
POST /api/recognize
Content-Type: application/json

{
  "image": "<base64-encoded JPEG/PNG>"
}
```
Response (match):
```json
{
  "match": true,
  "user": { "id": "550e...", "name": "John Doe" },
  "confidence": 0.87,
  "message": "Face matched successfully"
}
```
Response (no match):
```json
{
  "match": false,
  "user": null,
  "confidence": 0.31,
  "message": "No match found"
}
```

#### List all users
```http
GET /api/users
```

#### Delete a user
```http
DELETE /api/users/{id}
```

---

### Python Service (`:8000`)

#### Extract embedding
```http
POST /extract-embedding
Content-Type: application/json

{ "image": "<base64>" }
```
Response:
```json
{
  "success": true,
  "embedding": [0.123, -0.456, ...],   // 512 floats, L2-normalized
  "error": null
}
```

#### Compare embeddings
```http
POST /compare
Content-Type: application/json

{
  "embedding": [0.123, ...],
  "stored_embeddings": [
    { "id": "uuid", "name": "John", "embedding": [...] }
  ]
}
```
Response:
```json
{
  "match": true,
  "user_id": "uuid",
  "user_name": "John",
  "confidence": 0.87
}
```

#### Health check
```http
GET /health
→ { "status": "ok", "model_loaded": true }
```

---

## How Face Recognition Works

```
1. ENROLLMENT
   Photo → decode base64 → BGR image
         → InsightFace detect face → crop & align
         → ArcFace R100 → 512-dim embedding
         → L2-normalize → store in PostgreSQL FLOAT8[]

2. RECOGNITION
   Photo → extract embedding (same pipeline)
         → load all DB embeddings
         → cosine_similarity = dot(a_norm, b_norm)
         → find max similarity
         → if similarity ≥ 0.45 → MATCH
         → return { match, user, confidence }

3. COSINE SIMILARITY (normalized vectors)
   sim(a, b) = a·b / (|a| × |b|)
   Range: [-1, 1]  →  1.0 = identical face
   Threshold: 0.45 (InsightFace ArcFace normalized space)
```

---

## Configuration

### Backend (`backend/src/main/resources/application.yml`)

| Key | Default | Description |
|---|---|---|
| `spring.datasource.url` | `jdbc:postgresql://localhost:5432/faceid` | Override with env var |
| `python.service.url` | `http://localhost:8000` | Python service URL |
| `python.service.timeout` | `30000` | HTTP timeout (ms) |

### Environment Variables (docker-compose)

| Variable | Description |
|---|---|
| `SPRING_DATASOURCE_URL` | PostgreSQL JDBC URL |
| `SPRING_DATASOURCE_USERNAME` | DB username |
| `SPRING_DATASOURCE_PASSWORD` | DB password |
| `PYTHON_SERVICE_URL` | Python FastAPI base URL |

---

## Running Locally (Without Docker)

### 1. PostgreSQL
```bash
docker run -d \
  -e POSTGRES_DB=faceid \
  -e POSTGRES_USER=faceid \
  -e POSTGRES_PASSWORD=faceid_secret \
  -p 5432:5432 \
  postgres:16-alpine
```

### 2. Python Service
```bash
cd python-service

# Create virtual environment
python3 -m venv venv
source venv/bin/activate          # Windows: venv\Scripts\activate

# Install dependencies
pip install -r requirements.txt

# Start service (downloads InsightFace models on first run ~300MB)
uvicorn main:app --reload --port 8000
```

### 3. Spring Boot Backend
```bash
cd backend
mvn spring-boot:run
# Runs on http://localhost:8080
```

### 4. React Frontend
```bash
cd front
npm install
npm run dev
# Runs on http://localhost:3000
# /api/* is proxied to :8080 by Vite
```

---

## Step-by-Step Test

### Using the UI

1. Open `http://localhost:3000`
2. Click **Enroll** in the sidebar
3. Position your face in the camera frame
4. Click **CAPTURE PHOTO**
5. Enter your name → click **ENROLL PERSON**
6. You should see: "✓ User registered successfully"
7. Click **Recognize** in the sidebar
8. Click **START RECOGNITION**
9. Your name should appear in green within 1–2 seconds

### Using curl

```bash
# Step 1: Capture a photo and save as test.jpg
# Step 2: Register
B64=$(base64 -w0 test.jpg)   # Linux
# B64=$(base64 test.jpg)     # macOS

curl -X POST http://localhost:8080/api/users/register \
  -H "Content-Type: application/json" \
  -d "{\"name\": \"John Doe\", \"image\": \"$B64\"}"

# Step 3: Recognize
curl -X POST http://localhost:8080/api/recognize \
  -H "Content-Type: application/json" \
  -d "{\"image\": \"$B64\"}"

# Step 4: List users
curl http://localhost:8080/api/users
```

---

## Troubleshooting

### Python service takes a long time to start
Normal on first run — InsightFace downloads the `buffalo_l` model pack (~300MB).
Watch progress: `docker compose logs -f python-service`

### "No face detected" error
- Ensure the face is clearly visible and well-lit
- Avoid extreme angles (> 30° tilt)
- Minimum face size: ~80×80 pixels in the frame

### Spring Boot fails to connect to Python
Check Python service health: `curl http://localhost:8000/health`
Check logs: `docker compose logs python-service`

### Webcam not working in browser
- Must be served over HTTPS **or** `localhost`
- Grant camera permissions when prompted
- Only one browser tab should use the camera at a time

### Database connection issues
```bash
docker exec -it faceid-postgres psql -U faceid -d faceid -c "\dt"
```

---

## Threshold Tuning

The default similarity threshold is `0.45` (in `python-service/main.py`):

| Threshold | Behavior |
|---|---|
| `0.35` | More permissive — higher false-accept rate |
| `0.45` | **Default** — balanced for ArcFace normalized embeddings |
| `0.55` | Stricter — may reject valid matches under bad lighting |

Equivalent to ~0.7 in raw (non-normalized) cosine space.

---

## Security Notes

This is a **demonstration system**. For production use, add:

- HTTPS / TLS termination
- JWT authentication on the API
- Rate limiting on recognition endpoint
- Audit logging
- Input validation on image size (limit to < 5MB)
- Database encryption at rest