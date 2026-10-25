import React from 'react';
import { Navigate, Outlet } from 'react-router-dom';
import useAuthStore from '../../lib/authStore';

const readPersistedRole = () => {
  try {
    const persisted = localStorage.getItem('syncstock-auth-storage');
    if (!persisted) {
      return null;
    }

    const parsed = JSON.parse(persisted);
    return parsed?.state?.role ?? null;
  } catch {
    return null;
  }
};

const ProtectedRoute = ({ allowedRoles = [] }) => {
  const token = localStorage.getItem('access_token');
  const storeRole = useAuthStore((state) => state.role);
  const effectiveRole = storeRole || readPersistedRole();

  if (!token) {
    return <Navigate to="/login" replace />;
  }

  if (allowedRoles.length > 0 && !effectiveRole) {
    return <Navigate to="/login" replace />;
  }

  if (allowedRoles.length > 0 && !allowedRoles.includes(effectiveRole)) {
    return <Navigate to="/dashboard" replace />;
  }

  return <Outlet />;
};

export default ProtectedRoute;