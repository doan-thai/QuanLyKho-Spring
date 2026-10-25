import axiosInstance from '../lib/axios';

export const supplierService = {
    // Lấy danh sách
    getAll: async (params = {}) => await axiosInstance.get('/supplier', { params }),
    
    // Thêm mới
    create: async (data) => await axiosInstance.post('/supplier', data),

    // Lấy chi tiết 1 đối tác
    getById: async (id) => await axiosInstance.get(`/supplier/${id}`),

    // Cập nhật
    update: async (id, data) => await axiosInstance.put(`/supplier/${id}`, data),

    // Xóa
    delete: async (id) => await axiosInstance.delete(`/supplier/${id}`),

    // Danh mục động từ dữ liệu nhà cung cấp hiện có
    getCategories: async () => {
        const response = await axiosInstance.get('/supplier');
        const list = response.data || [];
        const categories = Array.from(
            new Set(
                list
                    .map((item) => item?.Address)
                    .filter((value) => typeof value === 'string' && value.trim() !== '')
            )
        );

        return categories;
    },

    getAnalytics: async (params = {}) => await axiosInstance.get('/supplier/analytics', { params })
};