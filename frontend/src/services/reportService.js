import axiosInstance from '../lib/axios'; // Đổi đường dẫn trỏ tới file api.js của sếp

const csvEscape = (value) => {
    const raw = String(value ?? '');
    if (!raw.includes(',') && !raw.includes('"') && !raw.includes('\n') && !raw.includes('\r')) {
        return raw;
    }
    return `"${raw.replace(/"/g, '""')}"`;
};

const buildCsv = (headers, rows) => {
    const head = headers.join(',');
    const lines = rows.map((row) => row.map((cell) => csvEscape(cell)).join(','));
    return [head, ...lines].join('\n');
};

const resolveTxDate = (item) => {
    const raw = item?.date || item?.transactionDate;
    if (!raw) return null;
    const parsed = new Date(raw);
    return Number.isNaN(parsed.getTime()) ? null : parsed;
};

const inDateRange = (item, from, to) => {
    const d = resolveTxDate(item);
    if (!d) return false;

    if (from) {
        const fromDate = new Date(`${from}T00:00:00`);
        if (d < fromDate) return false;
    }

    if (to) {
        const toDate = new Date(`${to}T23:59:59`);
        if (d > toDate) return false;
    }

    return true;
};

const toCsvBlob = (content) => new Blob([content], { type: 'text/csv;charset=utf-8' });

export const reportService = {
    // Xem báo cáo tồn kho
    getStockReport: async () => {
        return await axiosInstance.get('/report/stock');
    },

    // Lịch sử nhập xuất (Truyền ?from=...&to=...)
    getTransactions: async (params) => {
        try {
            return await axiosInstance.get('/report/transactions', { params });
        } catch (error) {
            // Fallback khi Report Service chưa có route lịch sử giao dịch.
            if (error?.response?.status === 404) {
                const limit = Number(params?.limit) || 200;
                return await axiosInstance.get('/inventory/history', { params: { limit } });
            }
            throw error;
        }
    },

    getBusinessReport: async (params) => {
        return await axiosInstance.get('/report/business', { params });
    },

    // Tải file Excel
    exportExcel: async (params) => {
        return await axiosInstance.get('/report/export/excel', {
            params,
            responseType: 'blob' // Bắt buộc phải có cái này để tải file .xlsx không bị lỗi font
        });
    },

    // Fallback xuất CSV khi Report Service chưa có endpoint export
    exportFallbackCsv: async (params = {}) => {
        const reportType = String(params.reportType || 'inventory').toLowerCase();

        if (reportType === 'inventory') {
            const stockRes = await axiosInstance.get('/inventory/stock');
            const stockRows = Array.isArray(stockRes?.data) ? stockRes.data : [];

            const csv = buildCsv(
                ['ProductId', 'ProductName', 'Category', 'Stock', 'MinStock', 'IsLowStock'],
                stockRows.map((item) => [
                    item.id,
                    item.name,
                    item.category,
                    item.stock,
                    item.minStock,
                    item.isLowStock ? 'Yes' : 'No'
                ])
            );

            return {
                blob: toCsvBlob(csv),
                extension: 'csv'
            };
        }

        const txRes = await axiosInstance.get('/inventory/history', {
            params: { limit: 500 }
        });
        const txRows = Array.isArray(txRes?.data) ? txRes.data : [];

        const filtered = txRows.filter((item) => {
            if (!inDateRange(item, params.from, params.to)) {
                return false;
            }

            const txType = String(item.type || item.transactionType || '').toUpperCase();
            if (reportType === 'inward') {
                return txType.includes('IMPORT') || txType.includes('IN') || txType.includes('NHAP');
            }
            if (reportType === 'outward') {
                return txType.includes('EXPORT') || txType.includes('OUT') || txType.includes('XUAT');
            }
            return true;
        });

        const csv = buildCsv(
            ['TransactionId', 'ProductId', 'ProductName', 'Type', 'Quantity', 'Date', 'Note'],
            filtered.map((item) => {
                const txDate = resolveTxDate(item);
                return [
                    item.id,
                    item.productId,
                    item.productName,
                    item.type || item.transactionType,
                    item.quantity,
                    txDate ? txDate.toISOString() : '',
                    item.note
                ];
            })
        );

        return {
            blob: toCsvBlob(csv),
            extension: 'csv'
        };
    }
};