-- FaceID Database Schema
-- PostgreSQL 16

CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- Users table with face embeddings
CREATE TABLE IF NOT EXISTS face_users (
    id          UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    name        VARCHAR(255) NOT NULL,
    embedding   DOUBLE PRECISION[] NOT NULL,
    image_b64   TEXT,
    created_at  TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    updated_at  TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

-- Index on name for quick lookups
CREATE INDEX IF NOT EXISTS idx_face_users_name ON face_users(name);
CREATE INDEX IF NOT EXISTS idx_face_users_created_at ON face_users(created_at DESC);

-- Recognition logs table
CREATE TABLE IF NOT EXISTS recognition_logs (
    id              UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    matched_user_id UUID REFERENCES face_users(id) ON DELETE SET NULL,
    confidence      DOUBLE PRECISION,
    matched         BOOLEAN NOT NULL DEFAULT FALSE,
    image_b64       TEXT,
    recognized_at   TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_recognition_logs_matched_user ON recognition_logs(matched_user_id);
CREATE INDEX IF NOT EXISTS idx_recognition_logs_recognized_at ON recognition_logs(recognized_at DESC);

-- Trigger to update updated_at
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = NOW();
    RETURN NEW;
END;
$$ language 'plpgsql';

CREATE TRIGGER update_face_users_updated_at
    BEFORE UPDATE ON face_users
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();
