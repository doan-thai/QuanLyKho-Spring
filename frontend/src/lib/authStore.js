import { create } from 'zustand';
import { persist } from 'zustand/middleware';

const useAuthStore = create(
  persist(
    (set) => ({
      token: null,
      role: null,
      user: null,
      isAuthenticated: false,

      // Hành động: Login thành công
      setAuth: (token, role, user = null) => {
        // 1. LƯU RIÊNG TOKEN CHO AXIOS ĐỌC
        localStorage.setItem('access_token', token); 
        
        // 2. Cập nhật state cho giao diện
        set({ 
          token, 
          role, 
          user, 
          isAuthenticated: true 
        });
      },

      // Hành động: Đăng xuất
      logout: () => {
        // Xóa token riêng của Axios
        localStorage.removeItem('access_token'); 
        
        // Xóa state giao diện
        set({ 
          token: null, 
          role: null, 
          user: null, 
          isAuthenticated: false 
        });
      },
    }),
    {
      name: 'syncstock-auth-storage', 
    }
  )
);

export default useAuthStore;