"""
Face Recognition Python Service
================================
FastAPI application exposing:
  POST /extract-embedding   — detect face and return 512-dim embedding
  POST /compare             — compare query embedding against a list
  GET  /health              — readiness probe
"""

import logging
import os
import sys
from contextlib import asynccontextmanager

import numpy as np
from fastapi import FastAPI, HTTPException, Request
from fastapi.middleware.cors import CORSMiddleware
from fastapi.responses import JSONResponse

from face_analyzer import face_analyzer
from image_utils import base64_to_bgr, cosine_similarity
from schemas import (
    CompareRequest,
    CompareResponse,
    ExtractEmbeddingRequest,
    ExtractEmbeddingResponse,
    HealthResponse,
    MatchResult,
)

# ── Logging setup ─────────────────────────────────────────────────────────────
logging.basicConfig(
    stream=sys.stdout,
    level=logging.DEBUG,
    format="%(asctime)s [%(levelname)s] %(name)s — %(message)s",
)
logger = logging.getLogger("faceid.python")

SIMILARITY_THRESHOLD = float(os.getenv("SIMILARITY_THRESHOLD", "0.40"))


# ── Lifespan: load model once at startup ─────────────────────────────────────
@asynccontextmanager
async def lifespan(app: FastAPI):
    logger.info("Starting Face Recognition Service …")
    face_analyzer.load()
    logger.info("Face Recognition Service ready ✓")
    yield
    logger.info("Shutting down Face Recognition Service")


# ── App factory ───────────────────────────────────────────────────────────────
app = FastAPI(
    title="FaceID Python Service",
    version="1.0.0",
    description="InsightFace-powered embedding extraction and comparison",
    lifespan=lifespan,
)

app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_methods=["*"],
    allow_headers=["*"],
)


# ── Global exception handler ──────────────────────────────────────────────────
@app.exception_handler(Exception)
async def global_exception_handler(request: Request, exc: Exception):
    logger.error("Unhandled exception at %s: %s", request.url.path, exc, exc_info=True)
    return JSONResponse(
        status_code=500,
        content={"detail": f"Internal server error: {str(exc)}"},
    )


# ── Routes ────────────────────────────────────────────────────────────────────

@app.get("/health", response_model=HealthResponse, tags=["System"])
def health():
    """Readiness probe — returns 200 only when the model is loaded."""
    if not face_analyzer.is_ready:
        raise HTTPException(status_code=503, detail="Model not yet loaded")
    return HealthResponse(
        status="UP",
        model_loaded=True,
        model_name="buffalo_sc (InsightFace)",
        embedding_dimension=512,
    )


@app.post("/extract-embedding", response_model=ExtractEmbeddingResponse, tags=["Face"])
def extract_embedding(request: ExtractEmbeddingRequest):
    """
    Detect the largest face in the image and return its L2-normalised embedding.

    Input:
        image_base64 — Base64 string of JPEG/PNG image

    Output:
        embedding    — List[float] of length 512
        face_detected — bool
        face_count   — number of faces detected in the frame
    """
    if not face_analyzer.is_ready:
        raise HTTPException(status_code=503, detail="Face recognition model is loading, please retry")

    # Decode image
    try:
        img_bgr = base64_to_bgr(request.image_base64)
    except ValueError as exc:
        logger.warning("Image decode error: %s", exc)
        return ExtractEmbeddingResponse(
            success=False,
            face_detected=False,
            message=str(exc),
        )

    # Extract embedding
    try:
        embedding, face_count = face_analyzer.extract_embedding(img_bgr)
    except Exception as exc:
        logger.error("Embedding extraction error: %s", exc)
        raise HTTPException(status_code=500, detail=f"Embedding extraction failed: {exc}")

    if embedding is None:
        logger.info("No face detected in submitted image (face_count=0)")
        return ExtractEmbeddingResponse(
            success=False,
            face_detected=False,
            face_count=0,
            message="No face detected in the image. Please ensure your face is clearly visible.",
        )

    logger.debug("Extracted embedding dim=%d, face_count=%d", len(embedding), face_count)
    return ExtractEmbeddingResponse(
        success=True,
        face_detected=True,
        embedding=embedding.tolist(),
        face_count=face_count,
        message=f"Embedding extracted from {face_count} detected face(s)",
    )


@app.post("/compare", response_model=CompareResponse, tags=["Face"])
def compare(request: CompareRequest):
    """
    Compare a query embedding against a list of stored embeddings using
    cosine similarity.  Returns the best match if similarity ≥ threshold.

    This endpoint is available for direct use.  In the primary flow the
    Spring Boot backend handles comparison locally (more efficient).
    """
    if not request.stored_embeddings:
        return CompareResponse(matched=False, message="No stored embeddings to compare against")

    query = np.array(request.query_embedding, dtype=np.float32)
    query = query / (np.linalg.norm(query) + 1e-10)

    best_score  = -1.0
    best_user_id = None
    best_name   = None

    for stored in request.stored_embeddings:
        stored_vec = np.array(stored.embedding, dtype=np.float32)
        score = cosine_similarity(query, stored_vec)
        logger.debug("User %s (%s) — similarity=%.4f", stored.name, stored.user_id, score)

        if score > best_score:
            best_score   = score
            best_user_id = stored.user_id
            best_name    = stored.name

    threshold = request.threshold or SIMILARITY_THRESHOLD
    matched   = best_score >= threshold

    if matched:
        return CompareResponse(
            matched=True,
            best_match=MatchResult(
                user_id=best_user_id,
                name=best_name,
                similarity=round(best_score, 4),
            ),
            message=f"Match found: {best_name} (similarity={best_score:.4f})",
        )

    return CompareResponse(
        matched=False,
        best_match=MatchResult(
            user_id=best_user_id or "",
            name=best_name or "unknown",
            similarity=round(best_score, 4),
        ) if best_user_id else None,
        message=f"No match found. Best similarity={best_score:.4f} (threshold={threshold})",
    )
