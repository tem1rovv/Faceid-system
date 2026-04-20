"""
FaceAnalyzer: singleton wrapper around InsightFace FaceAnalysis.

InsightFace buffalo_sc model:
  - Detection:   RetinaFace (scrfd_500m)
  - Recognition: ArcFace    (w600k_r50)
  - Embedding:   512-dim, L2-normalized
"""

import logging
import os
import threading
from typing import Optional, Tuple

import numpy as np
from insightface.app import FaceAnalysis

logger = logging.getLogger(__name__)

MODEL_NAME        = os.getenv("MODEL_NAME", "buffalo_sc")
DET_SIZE          = (640, 640)
CTX_ID            = 0          # 0 = CPU; use GPU id if available
PROVIDERS         = ["CPUExecutionProvider"]


class FaceAnalyzer:
    """Thread-safe singleton for InsightFace model."""

    _instance: Optional["FaceAnalyzer"] = None
    _lock = threading.Lock()

    def __new__(cls) -> "FaceAnalyzer":
        with cls._lock:
            if cls._instance is None:
                obj = super().__new__(cls)
                obj._app = None
                obj._ready = False
                cls._instance = obj
        return cls._instance

    def load(self) -> None:
        """Load InsightFace model. Safe to call multiple times."""
        if self._ready:
            return
        logger.info("Loading InsightFace model '%s' …", MODEL_NAME)
        try:
            self._app = FaceAnalysis(
                name=MODEL_NAME,
                providers=PROVIDERS,
            )
            self._app.prepare(ctx_id=CTX_ID, det_size=DET_SIZE)
            self._ready = True
            logger.info("InsightFace model loaded — embedding dim=512")
        except Exception as exc:
            logger.error("Failed to load InsightFace model: %s", exc)
            raise RuntimeError(f"Model load failed: {exc}") from exc

    @property
    def is_ready(self) -> bool:
        return self._ready

    # ── Public API ────────────────────────────────────────────────────────────

    def get_faces(self, img_bgr: np.ndarray) -> list:
        """
        Run detection + recognition on a BGR image.
        Returns list of insightface Face objects (each has .normed_embedding).
        """
        if not self._ready:
            raise RuntimeError("Model not loaded")
        return self._app.get(img_bgr)

    def extract_embedding(self, img_bgr: np.ndarray) -> Tuple[Optional[np.ndarray], int]:
        """
        Extract the embedding of the LARGEST detected face.

        Returns:
            (embedding, face_count) where embedding is None if no face found.
        """
        faces = self.get_faces(img_bgr)
        if not faces:
            return None, 0

        # Pick the face with the largest bounding-box area
        best = max(faces, key=lambda f: _bbox_area(f.bbox))
        embedding = np.array(best.normed_embedding, dtype=np.float32)

        # Extra safety: L2-normalise
        norm = np.linalg.norm(embedding)
        if norm > 1e-10:
            embedding = embedding / norm

        return embedding, len(faces)


def _bbox_area(bbox) -> float:
    """Compute bounding-box area from [x1,y1,x2,y2]."""
    x1, y1, x2, y2 = bbox
    return max(0.0, x2 - x1) * max(0.0, y2 - y1)


# Module-level singleton
face_analyzer = FaceAnalyzer()
