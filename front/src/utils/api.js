const BASE = '/api';

async function post(path, body) {
  const res = await fetch(BASE + path, {
    method:  'POST',
    headers: { 'Content-Type': 'application/json' },
    body:    JSON.stringify(body),
  });
  const data = await res.json();
  if (!res.ok) throw new Error(data.message || `HTTP ${res.status}`);
  return data;
}

async function get(path) {
  const res = await fetch(BASE + path);
  if (!res.ok) throw new Error(`HTTP ${res.status}`);
  return res.json();
}

async function del(path) {
  const res = await fetch(BASE + path, { method: 'DELETE' });
  if (!res.ok) throw new Error(`HTTP ${res.status}`);
}

export const api = {
  register:   (name, imageBase64) => post('/users/register', { name, imageBase64 }),
  recognize:  (imageBase64)       => post('/recognize',      { imageBase64 }),
  listUsers:  ()                  => get('/users'),
  deleteUser: (id)                => del(`/users/${id}`),
  stats:      ()                  => get('/stats'),
};
