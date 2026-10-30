import { create } from 'zustand';
import { persist } from 'zustand/middleware';

const useAuthStore = create(
  persist(
    (set) => ({
      token: null,
      role: null,
      username: null,
      user: null,
      isAuthenticated: false,

      // Hành động: Login thành công
      setAuth: (token, role, username = null, user = null) => {
        set({ 
          token, 
          role,
          username,
          user, 
          isAuthenticated: true 
        });
      },

      // Hành động: Đăng xuất
      logout: () => {
        set({ 
          token: null, 
          role: null, 
          username: null,
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