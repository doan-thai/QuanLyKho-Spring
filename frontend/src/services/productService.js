import axiosInstance from '../lib/axios';

export const productService = {
    // Lấy sản phẩm theo API phân trang: /api/inventory/product
    getAll: async (params = {}) => {
        return await axiosInstance.get('/inventory/product', { params });
    },
    
    // Thêm sản phẩm mới
    create: async (data) => {
        return await axiosInstance.post('/inventory/product', data);
    },

    // Cập nhật sản phẩm
    update: async (id, data) => {
        return await axiosInstance.put(`/inventory/product/${id}`, data);
    },

    // Xóa sản phẩm
    delete: async (id) => {
        return await axiosInstance.delete(`/inventory/product/${id}`);
    },
};