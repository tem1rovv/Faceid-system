"""
Image utilities: base64 decoding, format conversion, preprocessing.
"""

import base64
import logging
from typing import Tuple

import cv2
import numpy as np
from PIL import Image
import io

logger = logging.getLogger(__name__)

MAX_IMAGE_DIM = 1920   # Down-scale if larger (saves memory, speeds detection)


def base64_to_bgr(image_base64: str) -> np.ndarray:
    """
    Decode a base64 string (with or without data-URI prefix) into a BGR numpy
    array suitable for OpenCV / InsightFace.

    Raises:
        ValueError: If the image cannot be decoded.
    """
    # Strip data-URI prefix if present
    if "," in image_base64:
        image_base64 = image_base64.split(",", 1)[1]

    try:
        img_bytes = base64.b64decode(image_base64)
    except Exception as exc:
        raise ValueError(f"Invalid base64 data: {exc}") from exc

    # Decode via Pillow (handles JPEG / PNG / WEBP / BMP / …)
    try:
        pil_img = Image.open(io.BytesIO(img_bytes)).convert("RGB")
    except Exception as exc:
        raise ValueError(f"Cannot decode image: {exc}") from exc

    # Optionally down-scale large images
    pil_img = _maybe_resize(pil_img)

    # Convert RGB → BGR for OpenCV/InsightFace
    bgr = cv2.cvtColor(np.array(pil_img), cv2.COLOR_RGB2BGR)
    return bgr


def _maybe_resize(img: Image.Image) -> Image.Image:
    w, h = img.size
    if w <= MAX_IMAGE_DIM and h <= MAX_IMAGE_DIM:
        return img
    scale = MAX_IMAGE_DIM / max(w, h)
    new_w, new_h = int(w * scale), int(h * scale)
    logger.debug("Resizing image from %dx%d to %dx%d", w, h, new_w, new_h)
    return img.resize((new_w, new_h), Image.LANCZOS)


def cosine_similarity(a: np.ndarray, b: np.ndarray) -> float:
    """
    Cosine similarity between two 1-D float32 vectors.
    Both should already be L2-normalised → result equals dot product.
    """
    a = a / (np.linalg.norm(a) + 1e-10)
    b = b / (np.linalg.norm(b) + 1e-10)
    return float(np.dot(a, b))
