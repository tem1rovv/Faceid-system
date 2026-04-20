import React, { useState, useEffect } from 'react';
import { NavLink, Outlet } from 'react-router-dom';
import './App.css';

export default function App() {
  const [stats, setStats] = useState(null);

  useEffect(() => {
    fetch('/api/stats')
      .then(r => r.json())
      .then(setStats)
      .catch(() => {});
  }, []);

  return (
    <div className="app-shell">
      <header className="app-header">
        <div className="logo">
          <span className="logo-icon">◈</span>
          <span className="logo-text">FACE<em>ID</em></span>
        </div>
        <nav className="app-nav">
          <NavLink
            to="/recognize"
            className={({ isActive }) => isActive ? 'nav-link active' : 'nav-link'}
          >
            Recognize
          </NavLink>
          <NavLink
            to="/register"
            className={({ isActive }) => isActive ? 'nav-link active' : 'nav-link'}
          >
            Enroll
          </NavLink>
        </nav>
        {stats && (
          <div className="header-stats">
            <span className="stat-pill">{stats.enrolledUsers} users</span>
            <span className="stat-pill accent">{stats.successfulRecognitions} matched</span>
          </div>
        )}
      </header>
      <main className="app-main">
        <Outlet />
      </main>
    </div>
  );
}
