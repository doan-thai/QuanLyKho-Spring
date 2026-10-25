import React, { useState, useRef, useEffect } from "react";
import { Outlet, Link, useLocation, useNavigate } from "react-router-dom";
import { MessageSquare, Send, X, Bot, Loader2 } from "lucide-react"; // Import thêm icon
import useAuthStore from "../../lib/authStore";
import { aiService } from "../../services/aiService"; // Import AI service

export default function DashboardLayout() {
    const location = useLocation();
    const navigate = useNavigate();
    const [isProfileOpen, setIsProfileOpen] = useState(false);
    const profileRef = useRef(null);
    const { logout, role } = useAuthStore((state) => state);

    // --- LOGIC AI CHATBOT MỚI ---
    const [isChatOpen, setIsChatOpen] = useState(false);
    const [chatInput, setChatInput] = useState("");
    const [isAsking, setIsAsking] = useState(false);
    const [chatMessages, setChatMessages] = useState([
        {
            role: "assistant",
            text: "Xin chào! Tôi là SyncStock AI. Tôi có thể giúp gì cho sếp về tồn kho hôm nay?",
        },
    ]);
    const scrollRef = useRef(null);

    // Tự động cuộn xuống tin nhắn mới nhất
    useEffect(() => {
        if (scrollRef.current) {
            scrollRef.current.scrollTop = scrollRef.current.scrollHeight;
        }
    }, [chatMessages, isChatOpen]);

    const handleAskAi = async (event) => {
        event.preventDefault();
        const question = chatInput.trim();
        if (!question) return;

        setChatMessages((prev) => [...prev, { role: "user", text: question }]);
        setChatInput("");
        setIsAsking(true);

        try {
            const response = await aiService.ask(question);
            const answer =
                response?.data?.answer ||
                "Tôi đang gặp chút trục trặc, sếp hỏi lại câu khác nhé!";
            setChatMessages((prev) => [
                ...prev,
                { role: "assistant", text: answer },
            ]);
        } catch {
            setChatMessages((prev) => [
                ...prev,
                { role: "assistant", text: "Lỗi kết nối AI rồi sếp ơi!" },
            ]);
        } finally {
            setIsAsking(false);
        }
    };
    // ----------------------------

    const handleLogout = () => {
        setIsProfileOpen(false);
        logout();
        navigate("/login");
    };

    useEffect(() => {
        const handleClickOutside = (event) => {
            if (
                profileRef.current &&
                !profileRef.current.contains(event.target)
            ) {
                setIsProfileOpen(false);
            }
        };
        document.addEventListener("mousedown", handleClickOutside);
        return () =>
            document.removeEventListener("mousedown", handleClickOutside);
    }, []);

    const menuItems = [
        {
            name: "Tổng quan",
            path: "/dashboard",
            icon: "dashboard",
            roles: ["Admin", "ThuKho", "KeToan"],
        },
        {
            name: "Sản phẩm",
            path: "/products",
            icon: "inventory_2",
            roles: ["Admin", "ThuKho", "KeToan"],
        },
        {
            name: "Nhập kho",
            path: "/stock-in",
            icon: "input",
            roles: ["Admin", "ThuKho"],
        },
        {
            name: "Xuất kho",
            path: "/stock-out",
            icon: "output",
            roles: ["Admin", "ThuKho"],
        },
        {
            name: "Nhà cung cấp",
            path: "/suppliers",
            icon: "local_shipping",
            roles: ["Admin", "ThuKho", "KeToan"],
        },
        {
            name: "Quản lý tài khoản",
            path: "/users",
            icon: "manage_accounts",
            roles: ["Admin"],
        },
    ].filter((item) => item.roles.includes(role || "Admin"));

    return (
        <div className="flex min-h-screen bg-[#f9f9ff]">
            {/* SIDEBAR */}
            <aside className="fixed left-0 top-0 h-full w-64 bg-slate-100/70 backdrop-blur-xl border-r border-slate-200/20 z-40">
                <div className="p-6">
                    <div className="flex items-center gap-3 mb-10">
                        <div className="w-10 h-10 bg-gradient-to-br from-[#003d9b] to-[#0052cc] rounded-xl flex items-center justify-center text-white shadow-lg">
                            <span className="material-symbols-outlined">
                                architecture
                            </span>
                        </div>
                        <div>
                            <h1 className="text-xl font-bold tracking-tighter text-slate-900">
                                SyncStock
                            </h1>
                            <p className="text-[10px] uppercase tracking-widest text-slate-500 font-bold">
                                Điều phối kho
                            </p>
                        </div>
                    </div>
                    <nav className="space-y-1">
                        {menuItems.map((item) => (
                            <Link
                                key={item.path}
                                to={item.path}
                                className={`flex items-center gap-3 px-4 py-3 rounded-r-full font-bold transition-all ${location.pathname === item.path ? "bg-blue-100 text-blue-700" : "text-slate-500 hover:bg-slate-200/50"}`}
                            >
                                <span className="material-symbols-outlined">
                                    {item.icon}
                                </span>
                                <span className="text-sm">{item.name}</span>
                            </Link>
                        ))}
                    </nav>
                </div>
            </aside>

            {/* MAIN CONTENT */}
            <main className="ml-64 flex-1 relative">
                <header className="fixed top-0 right-0 left-64 h-16 flex items-center justify-end px-8 z-30 bg-white/80 backdrop-blur-md border-b border-slate-200/20">
                    <div className="flex items-center gap-4" ref={profileRef}>
                        <div className="relative">
                            <div
                                onClick={() => setIsProfileOpen(!isProfileOpen)}
                                className="flex items-center gap-2 cursor-pointer select-none"
                            >
                                <div className="text-right">
                                    <p className="text-xs font-bold text-slate-900">
                                        Chi nhánh Quận 1
                                    </p>
                                    {/* Thêm một object để map từ value sang tiếng Việt hiển thị */}
                                    <p className="text-[10px] uppercase text-blue-700 font-medium">
                                        {{
                                            Admin: "QUẢN LÝ",
                                            ThuKho: "THỦ KHO",
                                            KeToan: "KẾ TOÁN",
                                        }[role] || "QUẢN LÝ"}
                                    </p>
                                </div>
                                <span
                                    className={`material-symbols-outlined text-slate-400 transition-transform ${isProfileOpen ? "rotate-180" : ""}`}
                                >
                                    expand_more
                                </span>
                            </div>
                            {isProfileOpen && (
                                <div className="absolute right-0 mt-3 w-48 bg-white rounded-xl shadow-xl border border-slate-100 py-2 z-50">
                                    <button
                                        onClick={() => navigate("/profile")}
                                        className="w-full flex items-center gap-3 px-4 py-2 text-sm font-bold text-slate-600 hover:bg-slate-50 text-left"
                                    >
                                        <span className="material-symbols-outlined text-lg">
                                            account_circle
                                        </span>{" "}
                                        Thông tin
                                    </button>
                                    <button
                                        onClick={handleLogout}
                                        className="w-full flex items-center gap-3 px-4 py-2 text-sm font-bold text-red-500 hover:bg-red-50 text-left"
                                    >
                                        <span className="material-symbols-outlined text-lg">
                                            logout
                                        </span>{" "}
                                        Đăng xuất
                                    </button>
                                </div>
                            )}
                        </div>
                    </div>
                </header>

                <div className="pt-24 px-8 pb-12">
                    <Outlet />
                </div>

                {/* --- FLOATING AI CHATBOX (STYLE FACEBOOK) --- */}
                <div className="fixed bottom-6 right-6 z-[9999] flex flex-col items-end gap-4 not-italic">
                    {/* Cửa sổ Chat */}
                    {isChatOpen && (
                        <div className="w-[380px] h-[520px] bg-white rounded-2xl shadow-2xl border border-slate-200 flex flex-col overflow-hidden animate-in slide-in-from-bottom-5">
                            {/* Header Chat */}
                            <div className="bg-[#003d9b] p-4 text-white flex items-center justify-between shadow-lg">
                                <div className="flex items-center gap-3">
                                    <div className="w-10 h-10 bg-white/20 rounded-full flex items-center justify-center">
                                        <Bot size={22} />
                                    </div>
                                    <div>
                                        <p className="font-black text-sm uppercase tracking-wider">
                                            SyncStock AI
                                        </p>
                                        <div className="flex items-center gap-1.5">
                                            <div className="w-2 h-2 bg-green-400 rounded-full animate-pulse"></div>
                                            <p className="text-[10px] font-bold opacity-70">
                                                Đang trực tuyến
                                            </p>
                                        </div>
                                    </div>
                                </div>
                                <button
                                    onClick={() => setIsChatOpen(false)}
                                    className="hover:bg-white/10 p-1.5 rounded-lg transition-colors"
                                >
                                    <X size={20} />
                                </button>
                            </div>

                            {/* Nội dung tin nhắn */}
                            <div
                                ref={scrollRef}
                                className="flex-1 p-4 overflow-y-auto space-y-4 bg-slate-50"
                            >
                                {chatMessages.map((msg, index) => (
                                    <div
                                        key={index}
                                        className={`flex ${msg.role === "user" ? "justify-end" : "justify-start"}`}
                                    >
                                        <div
                                            className={`max-w-[80%] p-3 rounded-2xl text-sm font-bold shadow-sm ${
                                                msg.role === "user"
                                                    ? "bg-[#003d9b] text-white rounded-tr-none"
                                                    : "bg-white text-slate-700 border border-slate-200 rounded-tl-none"
                                            }`}
                                        >
                                            {msg.text}
                                        </div>
                                    </div>
                                ))}
                                {isAsking && (
                                    <div className="flex justify-start">
                                        <div className="bg-white p-3 rounded-2xl border border-slate-200">
                                            <Loader2
                                                size={18}
                                                className="animate-spin text-blue-700"
                                            />
                                        </div>
                                    </div>
                                )}
                            </div>

                            {/* Input nhập liệu */}
                            <form
                                onSubmit={handleAskAi}
                                className="p-4 bg-white border-t border-slate-100 flex gap-2"
                            >
                                <input
                                    value={chatInput}
                                    onChange={(e) =>
                                        setChatInput(e.target.value)
                                    }
                                    placeholder="Hỏi AI về kho hàng..."
                                    className="flex-1 bg-slate-50 border-none rounded-xl px-4 py-3 text-sm font-bold outline-none focus:ring-2 focus:ring-blue-500/20"
                                />
                                <button
                                    disabled={isAsking}
                                    type="submit"
                                    className="bg-[#003d9b] text-white p-3 rounded-xl hover:bg-blue-800 transition-all disabled:opacity-50"
                                >
                                    <Send size={18} />
                                </button>
                            </form>
                        </div>
                    )}

                    {/* Nút bong bóng chat */}
                    <button
                        onClick={() => setIsChatOpen(!isChatOpen)}
                        className={`w-16 h-16 rounded-full shadow-2xl flex items-center justify-center transition-all duration-300 hover:scale-110 active:scale-95 ${
                            isChatOpen
                                ? "bg-slate-800 text-white rotate-90"
                                : "bg-[#003d9b] text-white"
                        }`}
                    >
                        {isChatOpen ? (
                            <X size={28} />
                        ) : (
                            <MessageSquare size={28} />
                        )}
                        {!isChatOpen && (
                            <span className="absolute -top-1 -right-1 flex h-5 w-5">
                                <span className="animate-ping absolute inline-flex h-full w-full rounded-full bg-blue-400 opacity-75"></span>
                                <span className="relative inline-flex rounded-full h-5 w-5 bg-blue-600 border-2 border-white"></span>
                            </span>
                        )}
                    </button>
                </div>
            </main>
        </div>
    );
}
