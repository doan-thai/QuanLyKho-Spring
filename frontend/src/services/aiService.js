import axiosInstance from '../lib/axios';

export const aiService = {
    ask: async (message) => {
        return await axiosInstance.post('/chat/ask', { message });
    },

    summary: async () => {
        return await axiosInstance.post('/chat/summary');
    }
};
