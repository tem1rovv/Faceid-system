"""
Pydantic schemas for the Face Recognition Python service.
"""

from pydantic import BaseModel, Field
from typing import Optional, List


class ExtractEmbeddingRequest(BaseModel):
    image_base64: str = Field(..., description="Base64-encoded JPEG or PNG image")


class ExtractEmbeddingResponse(BaseModel):
    success: bool
    face_detected: bool
    embedding: Optional[List[float]] = None
    face_count: int = 0
    message: Optional[str] = None


class StoredEmbedding(BaseModel):
    user_id: str
    name: str
    embedding: List[float]


class CompareRequest(BaseModel):
    query_embedding: List[float] = Field(..., description="Embedding to search for")
    stored_embeddings: List[StoredEmbedding] = Field(..., description="Database of embeddings")
    threshold: float = Field(default=0.40, ge=0.0, le=1.0)


class MatchResult(BaseModel):
    user_id: str
    name: str
    similarity: float


class CompareResponse(BaseModel):
    matched: bool
    best_match: Optional[MatchResult] = None
    message: str


class HealthResponse(BaseModel):
    status: str
    model_loaded: bool
    model_name: str
    embedding_dimension: int
