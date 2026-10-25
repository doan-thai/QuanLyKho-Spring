import React from 'react'
import ReactDOM from 'react-dom/client'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { Toaster } from 'sonner' // 1. Bổ sung import Sonner
import App from './App.jsx'
import './index.css'

const queryClient = new QueryClient()

ReactDOM.createRoot(document.getElementById('root')).render(
  <React.StrictMode>
    <QueryClientProvider client={queryClient}>
      <App />
      
      {/* 2. Đặt Toaster ở ngoài cùng. 
          - richColors: Giúp màu sắc thông báo đậm và đẹp hơn (Xanh cho thành công, Đỏ cho lỗi).
          - position: Góc hiển thị thông báo (góc trên bên phải). 
      */}
      <Toaster richColors position="top-right" />
      
    </QueryClientProvider>
  </React.StrictMode>,
)