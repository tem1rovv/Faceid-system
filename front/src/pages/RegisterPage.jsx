import React, { useState, useEffect } from 'react';
import { useWebcam } from '../hooks/useWebcam';
import { api } from '../utils/api';
import './RegisterPage.css';

export default function RegisterPage() {
  const { videoRef, active, error: camError, start, stop, capture } = useWebcam();
  const [name,    setName]    = useState('');
  const [status,  setStatus]  = useState(null);   // { type: 'success'|'error', msg }
  const [loading, setLoading] = useState(false);
  const [users,   setUsers]   = useState([]);
  const [preview, setPreview] = useState(null);

  const loadUsers = () =>
    api.listUsers().then(setUsers).catch(() => {});

  useEffect(() => { loadUsers(); }, []);

  const handleCapture = () => {
    const img = capture();
    if (img) setPreview(img);
    else setStatus({ type: 'error', msg: 'Camera not active. Please start the camera first.' });
  };

  const handleRegister = async () => {
    if (!name.trim()) {
      setStatus({ type: 'error', msg: 'Please enter a name.' });
      return;
    }
    const imageBase64 = preview || capture();
    if (!imageBase64) {
      setStatus({ type: 'error', msg: 'No image captured. Start the camera and capture first.' });
      return;
    }

    setLoading(true);
    setStatus(null);
    try {
      const res = await api.register(name.trim(), imageBase64);
      setStatus({ type: 'success', msg: `✓ ${res.name} enrolled successfully!` });
      setName('');
      setPreview(null);
      await loadUsers();
    } catch (err) {
      setStatus({ type: 'error', msg: err.message });
    } finally {
      setLoading(false);
    }
  };

  const handleDelete = async (id) => {
    try {
      await api.deleteUser(id);
      await loadUsers();
    } catch (err) {
      setStatus({ type: 'error', msg: `Delete failed: ${err.message}` });
    }
  };

  return (
    <div className="register-page">
      <div className="page-hero">
        <h1>Enroll User</h1>
        <p>Capture a clear face photo and assign a name to register a new identity.</p>
      </div>

      <div className="register-layout">

        {/* ── Camera Panel ─────────────────────────────────────────────── */}
        <div className="panel camera-panel">
          <div className="panel-header">
            <span className="panel-tag">CAMERA</span>
            <div className={`dot ${active ? 'active' : ''}`} />
          </div>

          <div className="video-frame">
            <video ref={videoRef} muted playsInline className="webcam-video" />

            {!active && !preview && (
              <div className="video-overlay">
                <button className="btn-primary" onClick={start}>
                  ▶ Start Camera
                </button>
              </div>
            )}

            {preview && (
              <img src={preview} alt="Captured face" className="capture-preview" />
            )}

            <div className="scan-line" />
          </div>

          {camError && <p className="error-msg">{camError}</p>}

          <div className="camera-actions">
            {!active ? (
              <button className="btn-secondary" onClick={start}>
                Start Camera
              </button>
            ) : (
              <button className="btn-ghost" onClick={stop}>
                Stop
              </button>
            )}
            <button className="btn-secondary" onClick={handleCapture} disabled={!active}>
              📸 Capture
            </button>
            {preview && (
              <button className="btn-ghost" onClick={() => setPreview(null)}>
                Clear
              </button>
            )}
          </div>
        </div>

        {/* ── Enrollment Form Panel ─────────────────────────────────────── */}
        <div className="panel form-panel">
          <div className="panel-header">
            <span className="panel-tag">ENROLLMENT</span>
          </div>

          <div className="form-body">
            <label className="field-label">FULL NAME</label>
            <input
              className="text-input"
              type="text"
              placeholder="Enter name…"
              value={name}
              onChange={e => setName(e.target.value)}
              onKeyDown={e => e.key === 'Enter' && handleRegister()}
              maxLength={255}
            />

            {status && (
              <div className={`status-box ${status.type}`}>
                {status.msg}
              </div>
            )}

            <button
              className="btn-primary full"
              onClick={handleRegister}
              disabled={loading || !name.trim()}
            >
              {loading ? (
                <span className="spinner">Enrolling…</span>
              ) : (
                'Enroll User'
              )}
            </button>

            {preview && (
              <div className="preview-thumb-row">
                <span className="field-label">CAPTURED IMAGE</span>
                <img src={preview} alt="Preview" className="preview-thumb" />
              </div>
            )}
          </div>

          {/* ── Enrolled Users List ─────────────────────────────────────── */}
          <div className="enrolled-section">
            <p className="list-heading">
              ENROLLED IDENTITIES
              <span className="count-badge">{users.length}</span>
            </p>

            {users.length === 0 && (
              <p className="empty-msg">No users enrolled yet.</p>
            )}

            <div className="enrolled-list">
              {users.map(u => (
                <div key={u.id} className="user-row">
                  {u.imageB64 ? (
                    <img src={u.imageB64} alt={u.name} className="user-thumb" />
                  ) : (
                    <div className="user-thumb placeholder">
                      {u.name.charAt(0).toUpperCase()}
                    </div>
                  )}
                  <div className="user-info">
                    <span className="user-name">{u.name}</span>
                    <span className="user-date">
                      {new Date(u.createdAt).toLocaleDateString()}
                    </span>
                  </div>
                  <button
                    className="btn-danger-sm"
                    onClick={() => handleDelete(u.id)}
                    title="Remove user"
                  >
                    ×
                  </button>
                </div>
              ))}
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}
