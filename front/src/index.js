import React from 'react';
import ReactDOM from 'react-dom/client';
import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom';
import App from './App';
import RegisterPage from './pages/RegisterPage';
import RecognizePage from './pages/RecognizePage';
import './index.css';

const root = ReactDOM.createRoot(document.getElementById('root'));
root.render(
  <BrowserRouter>
    <Routes>
      <Route path="/" element={<App />}>
        <Route index element={<Navigate to="/recognize" replace />} />
        <Route path="register" element={<RegisterPage />} />
        <Route path="recognize" element={<RecognizePage />} />
      </Route>
    </Routes>
  </BrowserRouter>
);
