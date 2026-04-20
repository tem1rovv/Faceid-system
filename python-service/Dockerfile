# Face Recognition Python Service
FROM python:3.11-slim

WORKDIR /app

# System dependencies for OpenCV and InsightFace
RUN apt-get update && apt-get install -y --no-install-recommends \
    build-essential \
    python3-dev \
    libglib2.0-0 \
    libsm6 \
    libxext6 \
    libxrender-dev \
    libgomp1 \
    libgl1 \
    curl \
    && rm -rf /var/lib/apt/lists/*

# Install Python dependencies
COPY requirements.txt .
RUN pip install --no-cache-dir --upgrade pip && \
    pip install --no-cache-dir -r requirements.txt

COPY . .

# Pre-download InsightFace model at build time so container starts fast
RUN python -c "\
import insightface; \
from insightface.app import FaceAnalysis; \
app = FaceAnalysis(name='buffalo_sc', providers=['CPUExecutionProvider']); \
app.prepare(ctx_id=0, det_size=(640, 640)); \
print('Model downloaded successfully')" || echo "Model will be downloaded on first run"

EXPOSE 8000

CMD ["uvicorn", "main:app", "--host", "0.0.0.0", "--port", "8000", "--workers", "2"]
