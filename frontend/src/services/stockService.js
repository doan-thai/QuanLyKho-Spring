import axiosInstance from '../lib/axios';

export const stockService = {
    // 1. Xem tồn kho hiện tại - Endpoint: /api/inventory/stock
    getInventory: async () => {
        return await axiosInstance.get('/inventory/stock');
    },

    // 2. Tạo phiếu nhập kho - Endpoint: /api/inventory/import
    createInwardTicket: async (ticketData) => {
        // Payload: { supplierId, note, items: [{productId, quantity, price}] }
        return await axiosInstance.post('/inventory/import', ticketData);
    },

    // 3. Tạo phiếu xuất kho - Endpoint: /api/inventory/export
    createOutwardTicket: async (ticketData) => {
        // Payload: { note, items: [{productId, quantity, price}] }
        return await axiosInstance.post('/inventory/export', ticketData);
    },

    // 4. Lấy lịch sử giao dịch từ Inventory Service
    getHistory: async (params) => {
        return await axiosInstance.get('/inventory/history', { params });
    }
};