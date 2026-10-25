import axiosInstance from '../lib/axios';

export const authService = {
    register: async (data) => await axiosInstance.post('/auth/register', data),
    login: async (data) => await axiosInstance.post('/auth/login', data),
    getUsers: async (params = {}) => await axiosInstance.get('/auth/users', { params }),
    createUser: async (data) => await axiosInstance.post('/auth/users', data),
    updateUserRole: async (id, data) => await axiosInstance.put(`/auth/users/${id}/role`, data),
    updateUserStatus: async (id, data) => await axiosInstance.patch(`/auth/users/${id}/status`, data),
    deleteUser: async (id) => await axiosInstance.delete(`/auth/users/${id}`)
};