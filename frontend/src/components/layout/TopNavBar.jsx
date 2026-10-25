import React, { useState, useRef, useEffect } from "react";
import { NavLink, useNavigate } from "react-router-dom";
import useAuthStore from "../../lib/authStore"; // Sếp nhớ import store vào nhé!

export default function TopNavBar() {
    const [isOpen, setIsOpen] = useState(false);
    const menuRef = useRef(null); // Để bắt sự kiện click ra ngoài
    
    const navigate = useNavigate();
    const logout = useAuthStore((state) => state.logout); // Lấy hàm logout

    const handleLogout = () => {
        setIsOpen(false);
        logout(); // Xóa sạch token thật trong máy
        navigate("/login"); 
    };

    // Tự động đóng menu khi bấm ra ngoài vùng dropdown
    useEffect(() => {
        const handleClickOutside = (event) => {
            if (menuRef.current && !menuRef.current.contains(event.target)) {
                setIsOpen(false);
            }
        };
        document.addEventListener("mousedown", handleClickOutside);
        return () => document.removeEventListener("mousedown", handleClickOutside);
    }, []);

    return (
        /* Nâng z-index lên z-50 để chắc chắn nằm trên cùng */
        <header className="fixed top-0 right-0 left-64 h-16 flex items-center justify-between px-8 z-50 bg-white/80 backdrop-blur-md shadow-sm border-b border-slate-100">
            <div className="flex items-center gap-8">
                <nav className="hidden md:flex gap-6">
                    <NavLink to="/dashboard" className="text-slate-600 hover:text-blue-700 text-sm font-bold transition-colors">Phân tích</NavLink>
                    <NavLink
                        to="/reports"
                        className={({ isActive }) =>
                            `text-sm font-bold transition-colors ${isActive ? "text-blue-700 border-b-2 border-blue-700 pb-1" : "text-slate-600"}`
                        }
                    >
                        Báo cáo & Nhật ký
                    </NavLink>
                </nav>
            </div>

            <div className="flex items-center gap-4" ref={menuRef}>
                <button className="w-10 h-10 flex items-center justify-center text-slate-600 hover:bg-slate-100 rounded-full relative">
                        <span className="material-symbols-outlined">notifications</span>
                    <span className="absolute top-2.5 right-2.5 w-2 h-2 bg-red-500 rounded-full border-2 border-white"></span>
                </button>
                
                <div className="h-8 w-[1px] bg-slate-200 mx-2"></div>

                <div className="relative"> 
                    <div 
                        onClick={() => setIsOpen(!isOpen)} 
                        className="flex items-center gap-2 cursor-pointer active:opacity-70 transition-opacity select-none group"
                    >
                        <div className="text-right">
                            <p className="text-xs font-black text-slate-900 group-hover:text-blue-700 transition-colors">Chi nhánh Quận 1</p>
                            <p className="text-[10px] text-blue-600 font-bold uppercase tracking-wider">Kho chính</p>
                        </div>
                        <span className={`material-symbols-outlined text-slate-400 transition-transform duration-300 ${isOpen ? 'rotate-180' : ''}`}>
                            expand_more
                        </span>
                    </div>

                    {/* MENU DROPDOWN */}
                    {isOpen && (
                        <div className="absolute right-0 mt-3 w-52 bg-white rounded-xl shadow-2xl border border-slate-100 py-2 z-[60] animate-in fade-in slide-in-from-top-2">
                            <button 
                                onClick={() => {
                                    setIsOpen(false);
                                    navigate("/profile"); 
                                }}
                                className="w-full flex items-center gap-3 px-4 py-3 text-sm font-bold text-slate-600 hover:bg-slate-50 hover:text-blue-700 transition-colors text-left"
                            >
                                <span className="material-symbols-outlined text-xl">account_circle</span>
                                Thông tin cá nhân
                            </button>

                            <div className="h-px bg-slate-100 my-1 mx-4"></div>

                            <button 
                                onClick={handleLogout} 
                                className="w-full flex items-center gap-3 px-4 py-3 text-sm font-bold text-red-600 hover:bg-red-50 transition-colors text-left"
                            >
                                <span className="material-symbols-outlined text-xl">logout</span>
                                Đăng xuất hệ thống
                            </button>
                        </div>
                    )}
                </div>
            </div>
        </header>
    );
}