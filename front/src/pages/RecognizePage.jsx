import React, { useState, useEffect, useRef, useCallback } from 'react';
import { useWebcam } from '../hooks/useWebcam';
import { api } from '../utils/api';
import './RecognizePage.css';

const INTERVAL_MS = 1500;

export default function RecognizePage() {
  const { videoRef, active, error: camError, start, stop, capture } = useWebcam();
  const [result,  setResult]  = useState(null);   // RecognizeResponse from backend
  const [running, setRunning] = useState(false);  // whether auto-loop is active
  const [loading, setLoading] = useState(false);
  const [log,     setLog]     = useState([]);     // recent recognitions
  const intervalRef = useRef(null);

  /* ── Core recognition call ────────────────────────────────────────────── */
  const recognize = useCallback(async () => {
    const img = capture();
    if (!img) return;

    setLoading(true);
    try {
      const res = await api.recognize(img);
      setResult(res);
      // Prepend to local log (keep last 10)
      setLog(prev => [
        { ...res, ts: new Date().toLocaleTimeString() },
        ...prev.slice(0, 9),
      ]);
    } catch (err) {
      setResult({ match: false, message: err.message, confidence: 0 });
    } finally {
      setLoading(false);
    }
  }, [capture]);

  /* ── Auto-loop ────────────────────────────────────────────────────────── */
  useEffect(() => {
    if (running) {
      intervalRef.current = setInterval(recognize, INTERVAL_MS);
    } else {
      clearInterval(intervalRef.current);
    }
    return () => clearInterval(intervalRef.current);
  }, [running, recognize]);

  /* ── Controls ─────────────────────────────────────────────────────────── */
  const handleStart = async () => {
    await start();
    setRunning(true);
    setResult(null);
  };

  const handleStop = () => {
    setRunning(false);
    stop();
    setResult(null);
  };

  /* ── Derived display values ───────────────────────────────────────────── */
  const confidence = result ? Math.round((result.confidence || 0) * 100) : 0;
  const matchClass = result ? (result.match ? 'match' : 'no-match') : '';

  return (
    <div className="recognize-page">
      <div className="page-hero">
        <h1>Live Recognition</h1>
        <p>Real-time face identification — frames sent every 1.5 s.</p>
      </div>

      <div className="recognize-layout">

        {/* ── Left: Camera ──────────────────────────────────────────────── */}
        <div className="camera-col">

          <div className={`video-wrapper ${matchClass}`}>
            <video ref={videoRef} muted playsInline className="recog-video" />

            {!active && (
              <div className="video-overlay">
                <button className="btn-start" onClick={handleStart}>
                  ▶ Start Recognition
                </button>
              </div>
            )}

            {/* Targeting corner brackets */}
            <div className="corner tl" />
            <div className="corner tr" />
            <div className="corner bl" />
            <div className="corner br" />

            {loading && <div className="scan-bar" />}

            {result && (
              <div className={`result-badge ${matchClass}`}>
                {result.match ? '✓ MATCH' : '✗ NO MATCH'}
              </div>
            )}

            {running && (
              <div className="live-indicator">
                <span className="live-dot" />
                LIVE
              </div>
            )}
          </div>

          {camError && <p className="error-text">{camError}</p>}

          <div className="cam-controls">
            {!active ? (
              <button className="btn-primary" onClick={handleStart}>
                Start
              </button>
            ) : (
              <button className="btn-stop" onClick={handleStop}>
                Stop
              </button>
            )}

            {active && !running && (
              <button className="btn-secondary" onClick={() => setRunning(true)}>
                Resume
              </button>
            )}
            {active && running && (
              <button className="btn-ghost" onClick={() => setRunning(false)}>
                Pause
              </button>
            )}
            {active && (
              <button className="btn-ghost" onClick={recognize} disabled={loading}>
                {loading ? '…' : 'Snap'}
              </button>
            )}
          </div>

          {/* ── Recent log ──────────────────────────────────────────────── */}
          {log.length > 0 && (
            <div className="recent-log">
              <p className="log-heading">RECENT</p>
              {log.map((entry, i) => (
                <div key={i} className={`log-row ${entry.match ? 'match' : 'no-match'}`}>
                  <span className="log-dot">{entry.match ? '✓' : '✗'}</span>
                  <span className="log-name">
                    {entry.match ? entry.user?.name : 'Unknown'}
                  </span>
                  <span className="log-conf">{Math.round((entry.confidence || 0) * 100)}%</span>
                  <span className="log-time">{entry.ts}</span>
                </div>
              ))}
            </div>
          )}
        </div>

        {/* ── Right: Result Panel ───────────────────────────────────────── */}
        <div className="result-col">

          <div className={`result-panel ${matchClass || 'idle'}`}>

            {/* Idle */}
            {!result && (
              <div className="idle-state">
                <div className="idle-icon">◈</div>
                <p className="idle-title">Awaiting Face</p>
                <p className="idle-sub">
                  Start the camera to begin real-time recognition
                </p>
              </div>
            )}

            {/* Match */}
            {result && result.match && (
              <div className="match-state">
                <div className="match-icon">✓</div>
                <h2 className="match-name">{result.user?.name}</h2>
                <p className="match-label">Identity Confirmed</p>

                <div className="confidence-wrap">
                  <div className="conf-header">
                    <span>Confidence</span>
                    <span className="conf-num">{confidence}%</span>
                  </div>
                  <div className="conf-track">
                    <div
                      className="conf-fill"
                      style={{ width: `${confidence}%` }}
                    />
                  </div>
                </div>

                <div className="detail-grid">
                  <div className="detail-item">
                    <span className="d-label">User ID</span>
                    <span className="d-val mono">
                      {result.user?.id?.slice(0, 8)}…
                    </span>
                  </div>
                  <div className="detail-item">
                    <span className="d-label">Similarity</span>
                    <span className="d-val mono">
                      {result.confidence?.toFixed(4)}
                    </span>
                  </div>
                </div>
              </div>
            )}

            {/* No match */}
            {result && !result.match && (
              <div className="nomatch-state">
                <div className="nomatch-icon">✗</div>
                <h2 className="nomatch-title">Unknown</h2>
                <p className="match-label">Face Not Recognised</p>

                <div className="confidence-wrap">
                  <div className="conf-header">
                    <span>Best similarity</span>
                    <span className="conf-num danger">{confidence}%</span>
                  </div>
                  <div className="conf-track">
                    <div
                      className="conf-fill danger"
                      style={{ width: `${confidence}%` }}
                    />
                  </div>
                </div>

                <p className="hint-msg">{result.message}</p>
              </div>
            )}
          </div>

          {/* ── System info strip ─────────────────────────────────────── */}
          <div className="info-strip">
            <div className="info-item">
              <span className="info-label">MODEL</span>
              <span className="info-val">InsightFace buffalo_sc</span>
            </div>
            <div className="info-item">
              <span className="info-label">EMBEDDING</span>
              <span className="info-val">512-dim ArcFace</span>
            </div>
            <div className="info-item">
              <span className="info-label">METRIC</span>
              <span className="info-val">Cosine Similarity</span>
            </div>
            <div className="info-item">
              <span className="info-label">THRESHOLD</span>
              <span className="info-val">≥ 0.40</span>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}
