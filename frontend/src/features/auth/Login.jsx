import React, { useState } from "react";
import { useNavigate } from "react-router-dom";
import { toast } from "sonner"; 
import { Box, User, Lock, Eye, EyeOff, ArrowRight, Package } from "lucide-react";
import api from "../../lib/axios";
import useAuthStore from "../../lib/authStore"; // Import store đã tạo

export default function Login() {
  // 1. Khởi tạo State và Store
  const [username, setUsername] = useState("");
  const [password, setPassword] = useState("");
  const [showPassword, setShowPassword] = useState(false);
  const [isLoading, setIsLoading] = useState(false);

  const navigate = useNavigate();
  
  // Lấy hàm setAuth từ Zustand Store để sử dụng
  const setAuth = useAuthStore((state) => state.setAuth);

  // 2. Hàm xử lý Đăng nhập
  const handleLogin = async (e) => {
    e.preventDefault(); 

    if (!username || !password) {
      toast.error("Vui lòng nhập đầy đủ Tên đăng nhập và Mật khẩu!");
      return;
    }

    setIsLoading(true);

    try {
      // Gọi API thật từ Backend
      const response = await api.post("/auth/login", {
        username: username,
        password: password,
      });

      // Lấy token và role từ response data
      const { token, role } = response.data;

      if (token) {
        // --- THAY ĐỔI QUAN TRỌNG Ở ĐÂY ---
        // Thay vì localStorage.setItem thủ công, ta dùng Zustand
        // Nó sẽ tự động lo việc lưu trữ và đồng bộ toàn hệ thống
        setAuth(token, role); 

        toast.success("Đăng nhập thành công! Hệ thống đã sẵn sàng.");
        
        // Chuyển hướng
        setTimeout(() => {
          navigate("/dashboard");
        }, 800);
      }
    } catch (error) {
      console.error("Lỗi đăng nhập:", error);
      if (error.response && error.response.status === 401) {
        toast.error("Tên đăng nhập hoặc mật khẩu không chính xác!");
      } else {
        toast.error("Lỗi kết nối đến máy chủ. Vui lòng thử lại!");
      }
    } finally {
      setIsLoading(false);
    }
  };

  return (
    <div className="min-h-screen flex items-center justify-center p-6 relative overflow-hidden bg-[#f9f9ff] not-italic">
      {/* Background Decor */}
      <div className="absolute top-0 left-0 w-full h-full -z-10 opacity-40">
        <div className="absolute top-[-10%] left-[-10%] w-[40%] h-[40%] bg-blue-200 rounded-full blur-[120px]"></div>
        <div className="absolute bottom-[-10%] right-[-10%] w-[50%] h-[50%] bg-indigo-200 rounded-full blur-[150px]"></div>
      </div>

      <div className="w-full max-w-[1200px] grid lg:grid-cols-2 rounded-xl overflow-hidden shadow-[0_12px_32px_-4px_rgba(9,28,53,0.08)] bg-white">
        {/* CỘT TRÁI: Branding */}
        <div className="hidden lg:flex flex-col justify-between p-12 bg-[#003d9b] relative overflow-hidden">
          <div className="relative z-10">
            <div className="flex items-center gap-2 mb-12">
              <div className="w-10 h-10 bg-white rounded flex items-center justify-center">
                <Box className="text-[#003d9b]" size={24} strokeWidth={2.5} />
              </div>
              <span className="text-white text-2xl font-black tracking-tighter uppercase">SyncStock</span>
            </div>
            <h1 className="text-white text-5xl font-black leading-tight tracking-tight mb-6 uppercase">
              Chuẩn xác trong từng <br /> luồng hàng.
            </h1>
            <p className="text-white/80 text-lg max-w-md font-medium">
              Trung tâm điều phối cho quản trị kho hiện đại. Theo dõi, phân tích và tối ưu toàn bộ luồng vận hành.
            </p>
          </div>

          <div className="relative z-10 flex gap-8">
            <div>
              <div className="text-white text-3xl font-black uppercase">99.9%</div>
              <div className="text-white/60 text-xs uppercase tracking-widest font-bold mt-1">Độ ổn định hệ thống</div>
            </div>
            <div>
              <div className="text-white text-3xl font-black uppercase">2.4M</div>
              <div className="text-white/60 text-xs uppercase tracking-widest font-bold mt-1">Mặt hàng đã xử lý</div>
            </div>
          </div>

          <div className="absolute top-1/2 right-0 translate-x-1/4 -translate-y-1/2 opacity-10">
            <Package size={400} strokeWidth={1} className="text-white" />
          </div>
        </div>

        {/* CỘT PHẢI: Form */}
        <div className="p-8 md:p-16 flex flex-col justify-center bg-white">
          <div className="max-w-sm mx-auto w-full">
            <div className="lg:hidden flex items-center gap-2 mb-12">
              <Box className="text-[#003d9b]" size={32} strokeWidth={2.5} />
              <span className="text-slate-900 text-xl font-black tracking-tighter uppercase">SyncStock</span>
            </div>

            <div className="mb-10">
              <h2 className="text-slate-900 text-3xl font-black tracking-tight mb-2 uppercase">Chào mừng trở lại</h2>
              <p className="text-slate-500 text-sm font-medium">Nhập thông tin đăng nhập để truy cập trung tâm điều phối kho.</p>
            </div>

            <form className="space-y-6" onSubmit={handleLogin}>
              <div>
                <label className="block text-[10px] font-black uppercase tracking-widest text-slate-400 mb-2 ml-1" htmlFor="username">
                  Tên đăng nhập
                </label>
                <div className="relative group">
                  <div className="absolute inset-y-0 left-0 pl-4 flex items-center pointer-events-none">
                    <User className="text-slate-300 group-focus-within:text-[#003d9b] transition-colors" size={18} />
                  </div>
                  <input
                    className="w-full bg-slate-50 border-none border-b border-slate-200 focus:ring-0 focus:border-[#003d9b] transition-all rounded-md pl-12 py-4 text-slate-900 font-bold outline-none"
                    id="username"
                    placeholder="Nhập tên tài khoản..."
                    type="text"
                    value={username}
                    onChange={(e) => setUsername(e.target.value)}
                  />
                </div>
              </div>

              <div>
                <label className="block text-[10px] font-black uppercase tracking-widest text-slate-400 mb-2 ml-1" htmlFor="password">
                  Mật khẩu
                </label>
                <div className="relative group">
                  <div className="absolute inset-y-0 left-0 pl-4 flex items-center pointer-events-none">
                    <Lock className="text-slate-300 group-focus-within:text-[#003d9b] transition-colors" size={18} />
                  </div>
                  <input
                    className="w-full bg-slate-50 border-none border-b border-slate-200 focus:ring-0 focus:border-[#003d9b] transition-all rounded-md pl-12 pr-12 py-4 text-slate-900 font-bold outline-none"
                    id="password"
                    placeholder="••••••••"
                    type={showPassword ? "text" : "password"}
                    value={password}
                    onChange={(e) => setPassword(e.target.value)}
                  />
                  <button 
                    className="absolute inset-y-0 right-0 pr-4 flex items-center text-slate-400 hover:text-slate-700 transition-colors" 
                    type="button"
                    onClick={() => setShowPassword(!showPassword)}
                  >
                    {showPassword ? <EyeOff size={18} /> : <Eye size={18} />}
                  </button>
                </div>
              </div>

              <div className="flex items-center justify-between py-2">
                <label className="flex items-center cursor-pointer group">
                  <input className="peer h-4 w-4 rounded border-slate-300 text-[#003d9b] focus:ring-[#003d9b]/20 transition-all cursor-pointer" type="checkbox" />
                  <span className="ml-3 text-xs text-slate-500 font-bold uppercase tracking-widest group-hover:text-slate-800 transition-colors">Ghi nhớ</span>
                </label>
              </div>

              <button
                disabled={isLoading}
                className={`w-full text-white font-black py-5 rounded-xl shadow-xl transition-all flex items-center justify-center gap-3 uppercase tracking-[0.1em] text-xs ${
                  isLoading ? "bg-slate-400 cursor-not-allowed shadow-none" : "bg-[#003d9b] shadow-blue-900/20 hover:scale-[1.02] active:scale-95 hover:bg-blue-800"
                }`}
                type="submit"
              >
                <span>{isLoading ? "Đang xử lý..." : "Đăng nhập hệ thống"}</span>
                {!isLoading && <ArrowRight size={18} />}
              </button>
            </form>

            <div className="mt-12 pt-8 border-t border-slate-100 text-center">
              <p className="text-slate-300 text-[10px] uppercase font-black tracking-[0.2em]">Hạ tầng doanh nghiệp an toàn</p>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}

// import React from "react";
// import { useNavigate } from "react-router-dom";
// import useAuthStore from "../../lib/authStore"; // Import store của sếp vào

// export default function Login() {
//     const navigate = useNavigate();

//     // Lấy hàm setAuth từ Zustand
//     const setAuth = useAuthStore((state) => state.setAuth);

//     // HÀM "HACK" ĐĂNG NHẬP NHANH
//     const handleQuickLogin = (role) => {
//         // Truyền token giả, role sếp muốn test, và thông tin user ảo
//         setAuth("fake-token-12345", role, { name: "Người dùng Test" });

//         // Đá về trang chủ, App.jsx sẽ tự động điều hướng theo Role
//         navigate("/");
//     };

//     return (
//         <div className="flex flex-col items-center justify-center h-screen bg-slate-50">
//             {/* Form Login cũ của sếp cứ giữ nguyên ở trên này... */}

//             <div className="mt-10 p-6 bg-white rounded-xl shadow-sm border border-slate-200">
//                 <p className="text-sm font-black text-slate-500 uppercase mb-4 text-center">
//                     Khu vực Test Role nhanh (Dành cho Dev)
//                 </p>
//                 <div className="flex gap-4">
//                     <button
//                         onClick={() => handleQuickLogin("Admin")}
//                         className="px-4 py-2 bg-slate-900 text-white rounded-lg font-bold text-sm"
//                     >
//                         Vào vai ADMIN
//                     </button>
//                     <button
//                         onClick={() => handleQuickLogin("ThuKho")}
//                         className="px-4 py-2 bg-blue-600 text-white rounded-lg font-bold text-sm"
//                     >
//                         Vào vai THỦ KHO
//                     </button>
//                     <button
//                         onClick={() => handleQuickLogin("KeToan")}
//                         className="px-4 py-2 bg-orange-600 text-white rounded-lg font-bold text-sm"
//                     >
//                         Vào vai KẾ TOÁN
//                     </button>
//                 </div>
//             </div>
//         </div>
//     );
// }
