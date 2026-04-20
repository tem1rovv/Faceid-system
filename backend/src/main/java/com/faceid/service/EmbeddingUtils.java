package com.faceid.service;

/**
 * Utility class for cosine similarity computation on face embeddings.
 * InsightFace embeddings are L2-normalized, so dot product = cosine similarity.
 */
public final class EmbeddingUtils {

    private EmbeddingUtils() {}

    /**
     * Computes cosine similarity between two embedding vectors.
     * Both vectors should be L2-normalized (InsightFace guarantees this).
     * Result is in [-1, 1]; higher means more similar.
     */
    public static double cosineSimilarity(double[] a, double[] b) {
        if (a == null || b == null) throw new IllegalArgumentException("Embeddings must not be null");
        if (a.length != b.length) {
            throw new IllegalArgumentException(
                    "Embedding dimensions must match: " + a.length + " vs " + b.length);
        }

        double dot    = 0.0;
        double normA  = 0.0;
        double normB  = 0.0;

        for (int i = 0; i < a.length; i++) {
            dot   += a[i] * b[i];
            normA += a[i] * a[i];
            normB += b[i] * b[i];
        }

        double denom = Math.sqrt(normA) * Math.sqrt(normB);
        if (denom < 1e-10) return 0.0;   // degenerate case

        return dot / denom;
    }

    /**
     * L2-normalise an embedding vector in place and returns it.
     * InsightFace already normalises, but we do it again as a safety net.
     */
    public static double[] l2Normalize(double[] v) {
        double norm = 0.0;
        for (double x : v) norm += x * x;
        norm = Math.sqrt(norm);
        if (norm < 1e-10) return v;
        double[] result = new double[v.length];
        for (int i = 0; i < v.length; i++) result[i] = v[i] / norm;
        return result;
    }
}
