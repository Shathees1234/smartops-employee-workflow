import React, { createContext, useContext, useState, useEffect } from 'react';
import { BrowserRouter, Routes, Route, Navigate, NavLink, useNavigate } from 'react-router-dom';
import ReactDOM from 'react-dom/client';
import axios from 'axios';
import { Toaster, toast } from 'react-hot-toast';
import './styles.css';
import {
  LoginPage, Dashboard, Sidebar,
  EmployeeList, RegisterEmployee,
  ApplyLeave, MyLeaves, AllLeaves, TeamLeaves, PendingApprovals
} from './Pages';

// ── Axios instance ─────────────────────────────────────────────────────────────

export const API = axios.create({ baseURL: 'http://localhost:8080/api' });

API.interceptors.request.use(cfg => {
  const token = localStorage.getItem('token');
  if (token) cfg.headers.Authorization = `Bearer ${token}`;
  return cfg;
});

API.interceptors.response.use(r => r, err => {
  if (err.response?.status === 401) {
    localStorage.clear();
    window.location.href = '/login';
  }
  return Promise.reject(err);
});

// ── API helpers ────────────────────────────────────────────────────────────────

export const api = {
  login:          d  => API.post('/auth/login', d),
  register:       d  => API.post('/auth/register', d),
  getEmployees:   () => API.get('/employees'),
  updateEmployee: (id,d) => API.put(`/employees/${id}`, d),
  deleteEmployee: id => API.delete(`/employees/${id}`),
  getDepartments: () => API.get('/employees/departments'),
  applyLeave:     d  => API.post('/leaves/apply', d),
  myLeaves:       () => API.get('/leaves/my'),
  allLeaves:      () => API.get('/leaves/all'),
  teamLeaves:     () => API.get('/leaves/team'),
  pendingLeaves:  () => API.get('/leaves/pending'),
  reviewLeave:    (id,d) => API.put(`/leaves/${id}/review`, d),
  cancelLeave:    id => API.put(`/leaves/${id}/cancel`),
  stats:          () => API.get('/dashboard/stats'),
};

// ── Auth Context ───────────────────────────────────────────────────────────────

const AuthContext = createContext(null);

export const useAuth = () => useContext(AuthContext);

const AuthProvider = ({ children }) => {
  const [user, setUser] = useState(() => {
    const u = localStorage.getItem('user');
    return u ? JSON.parse(u) : null;
  });

  const login = async (email, password) => {
    const res = await api.login({ email, password });
    const { token, ...userData } = res.data;
    localStorage.setItem('token', token);
    localStorage.setItem('user', JSON.stringify(userData));
    setUser(userData);
    return userData;
  };

  const logout = () => {
    localStorage.clear();
    setUser(null);
  };

  const isAdmin   = () => user?.roles?.includes('ROLE_ADMIN');
  const isManager = () => user?.roles?.includes('ROLE_MANAGER');

  return (
    <AuthContext.Provider value={{ user, login, logout, isAdmin, isManager }}>
      {children}
    </AuthContext.Provider>
  );
};

// ── Private Route ──────────────────────────────────────────────────────────────

const PrivateRoute = ({ children, role }) => {
  const { user, isAdmin, isManager } = useAuth();
  if (!user) return <Navigate to="/login" replace />;
  if (role === 'ADMIN'   && !isAdmin())            return <Navigate to="/dashboard" replace />;
  if (role === 'MANAGER' && !isAdmin() && !isManager()) return <Navigate to="/dashboard" replace />;
  return children;
};

// ── App ────────────────────────────────────────────────────────────────────────

const App = () => (
  <AuthProvider>
    <BrowserRouter>
      <Toaster position="top-right" toastOptions={{ duration: 3000 }} />
      <Routes>
        <Route path="/login" element={<LoginPage />} />
        <Route path="/*" element={
          <PrivateRoute>
            <div className="app-layout">
              <Sidebar />
              <main className="main-content">
                <Routes>
                  <Route path="/dashboard"          element={<Dashboard />} />
                  <Route path="/leaves/apply"        element={<ApplyLeave />} />
                  <Route path="/leaves/my"           element={<MyLeaves />} />
                  <Route path="/leaves/team"         element={<PrivateRoute role="MANAGER"><TeamLeaves /></PrivateRoute>} />
                  <Route path="/leaves/pending"      element={<PrivateRoute role="MANAGER"><PendingApprovals /></PrivateRoute>} />
                  <Route path="/leaves/all"          element={<PrivateRoute role="ADMIN"><AllLeaves /></PrivateRoute>} />
                  <Route path="/employees"           element={<PrivateRoute role="ADMIN"><EmployeeList /></PrivateRoute>} />
                  <Route path="/employees/register"  element={<PrivateRoute role="ADMIN"><RegisterEmployee /></PrivateRoute>} />
                  <Route path="*"                    element={<Navigate to="/dashboard" replace />} />
                </Routes>
              </main>
            </div>
          </PrivateRoute>
        } />
        <Route path="/" element={<Navigate to="/dashboard" replace />} />
      </Routes>
    </BrowserRouter>
  </AuthProvider>
);

export default App;

// ── Mount ──────────────────────────────────────────────────────────────────────

ReactDOM.createRoot(document.getElementById('root')).render(<App />);
