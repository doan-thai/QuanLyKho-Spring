import React, { useState } from "react";
import { useQuery } from "@tanstack/react-query";
import { Search, Loader2, ArrowDownLeft, ArrowUpRight } from "lucide-react";
import { stockService } from "../../services/stockService";

// IMPORT Modal và nút In của bạn (Sửa đường dẫn nếu cần)
import Modal from "../../components/ui/Modal"; 
import PrintActionButton from "../../components/print/PrintActionButton"; 

const resolveTransactionType = (item) =>
    String(item?.type || item?.transactionType || "").toUpperCase();

const isImportTransaction = (item) => {
    const transactionType = resolveTransactionType(item);
    return (
        transactionType.includes("IMPORT") ||
        transactionType === "IN" ||
        transactionType.includes("NHAP")
    );
};

export default function StockInHistory() {
    const [activeLogType, setActiveLogType] = useState("ALL");
    const [searchTerm, setSearchTerm] = useState("");
    
    // 1. Thêm state để lưu trữ dòng (giao dịch) đang được click chọn
    const [selectedTransaction, setSelectedTransaction] = useState(null);

    const { data: history = [], isLoading } = useQuery({
        queryKey: ["stock-history"],
        queryFn: async () => {
            try {
                const res = await stockService.getHistory();
                return Array.isArray(res?.data) ? res.data : [];
            } catch {
                return [];
            }
        },
    });

    const filteredLogs = history.filter((item) => {
        const isIn = isImportTransaction(item);
        const itemType = isIn ? "IN" : "OUT";
        const matchTab = activeLogType === "ALL" || itemType === activeLogType;
        const searchLower = searchTerm.toLowerCase();
        const matchSearch =
            String(item.id || "")
                .toLowerCase()
                .includes(searchLower) ||
            String(item.productName || item.name || "")
                .toLowerCase()
                .includes(searchLower);
        return matchTab && matchSearch;
    });

    return (
        <div className="w-full not-italic">
            <div className="mb-8">
                <h2 className="text-4xl font-black tracking-tight text-slate-900 mb-2 uppercase">
                    Nhật ký giao dịch
                </h2>
                <p className="text-slate-500 text-base font-medium">
                    Tra cứu toàn bộ luồng hàng hóa nhập và xuất khỏi hệ thống.
                </p>
            </div>

            <div className="bg-white p-6 md:p-8 rounded-[32px] border border-slate-200 shadow-sm">
                <div className="flex flex-wrap items-center justify-between gap-4 mb-8">
                    <h3 className="text-xl font-black uppercase tracking-tight text-slate-900">
                        Chi tiết vận hành
                    </h3>
                    <div className="inline-flex bg-slate-100 p-1.5 rounded-2xl">
                        <button
                            onClick={() => setActiveLogType("ALL")}
                            className={`px-5 py-2.5 text-xs font-black uppercase rounded-xl transition-all ${activeLogType === "ALL" ? "bg-white text-slate-900 shadow-sm" : "text-slate-500"}`}
                        >
                            Tất cả
                        </button>
                        <button
                            onClick={() => setActiveLogType("IN")}
                            className={`px-5 py-2.5 text-xs font-black uppercase rounded-xl inline-flex items-center gap-2 transition-all ${activeLogType === "IN" ? "bg-white text-blue-700 shadow-sm" : "text-slate-500"}`}
                        >
                            <ArrowDownLeft size={16} strokeWidth={3} /> Nhập
                        </button>
                        <button
                            onClick={() => setActiveLogType("OUT")}
                            className={`px-5 py-2.5 text-xs font-black uppercase rounded-xl inline-flex items-center gap-2 transition-all ${activeLogType === "OUT" ? "bg-white text-orange-700 shadow-sm" : "text-slate-500"}`}
                        >
                            <ArrowUpRight size={16} strokeWidth={3} /> Xuất
                        </button>
                    </div>
                </div>

                <div className="mb-8 relative group">
                    <Search
                        size={22}
                        className="absolute left-5 top-1/2 -translate-y-1/2 text-slate-400 group-focus-within:text-blue-600 transition-colors"
                    />
                    <input
                        value={searchTerm}
                        onChange={(e) => setSearchTerm(e.target.value)}
                        placeholder="Tìm theo mã giao dịch hoặc tên sản phẩm..."
                        className="w-full pl-14 pr-6 h-16 bg-slate-50 border-2 border-transparent focus:border-blue-500/20 focus:bg-white rounded-2xl text-lg font-bold text-slate-900 outline-none transition-all placeholder:text-slate-400"
                    />
                </div>

                <div className="overflow-x-auto">
                    <table className="w-full min-w-[900px] text-left border-separate border-spacing-y-2">
                        <thead>
                            <tr className="text-slate-400 uppercase tracking-[0.2em] text-[11px] font-black">
                                <th className="px-6 py-4">Mã GD</th>
                                <th className="px-6 py-4">Sản phẩm</th>
                                <th className="px-6 py-4 text-center">Loại</th>
                                <th className="px-6 py-4 text-right">
                                    Số lượng
                                </th>
                                <th className="px-6 py-4">Thời gian</th>
                                <th className="px-6 py-4 text-right">
                                    Ghi chú
                                </th>
                            </tr>
                        </thead>
                        <tbody>
                            {isLoading ? (
                                <tr>
                                    <td
                                        colSpan="6"
                                        className="py-20 text-center"
                                    >
                                        <Loader2
                                            className="animate-spin mx-auto text-blue-700"
                                            size={40}
                                        />
                                    </td>
                                </tr>
                            ) : (
                                filteredLogs.map((item, index) => {
                                    const isIn = isImportTransaction(item);
                                    return (
                                        // 2. Thêm onClick và cursor-pointer để click được
                                        <tr
                                            key={index}
                                            onClick={() => setSelectedTransaction(item)}
                                            className="bg-white hover:bg-slate-50 transition-all group cursor-pointer"
                                        >
                                            <td className="px-6 py-5 font-mono font-black text-slate-400 group-hover:text-blue-700">
                                                #{item.id}
                                            </td>
                                            <td className="px-6 py-5 text-lg font-black text-slate-900 uppercase tracking-tighter">
                                                {item.productName || "-"}
                                            </td>
                                            <td className="px-6 py-5 text-center">
                                                <span
                                                    className={`px-4 py-1.5 rounded-xl text-[10px] font-black uppercase tracking-widest ${isIn ? "bg-blue-100 text-blue-700" : "bg-orange-100 text-orange-700"}`}
                                                >
                                                    {isIn ? "Nhập" : "Xuất"}
                                                </span>
                                            </td>
                                            <td className="px-6 py-5 text-right text-xl font-black text-slate-900">
                                                {item.quantity || 0}
                                            </td>
                                            <td className="px-6 py-5 text-sm font-bold text-slate-500">
                                                {new Date(
                                                    item.date || item.transactionDate || Date.now(),
                                                ).toLocaleString("vi-VN")}
                                            </td>
                                            <td className="px-6 py-5 text-right text-sm font-medium text-slate-400 italic">
                                                {item.note || "-"}
                                            </td>
                                        </tr>
                                    );
                                })
                            )}
                        </tbody>
                    </table>
                </div>
            </div>

            {/* 3. Đặt Modal ở cuối trang để hiển thị chi tiết khi click */}
            <Modal
                isOpen={!!selectedTransaction}
                onClose={() => setSelectedTransaction(null)}
                title="Chi tiết giao dịch"
            >
                {selectedTransaction && (
                    <div className="p-4 flex flex-col gap-6">
                        {/* Header của Modal */}
                        <div className="flex justify-between items-start border-b border-slate-100 pb-6">
                            <div>
                                <p className="text-xs text-slate-400 uppercase font-bold tracking-widest mb-1">
                                    Mã phiếu
                                </p>
                                <h3 className="text-3xl font-black text-slate-900 font-mono">
                                    #{selectedTransaction.id}
                                </h3>
                                <p className="text-slate-500 mt-2 text-sm">
                                    {new Date(selectedTransaction.date || Date.now()).toLocaleString("vi-VN")}
                                </p>
                            </div>
                            
                            <div className="flex flex-col items-end gap-3">
                                {/* Trạng thái Nhập/Xuất */}
                                <span className={`px-4 py-1.5 rounded-xl text-[10px] font-black uppercase tracking-widest ${
                                    isImportTransaction(selectedTransaction)
                                    ? "bg-blue-100 text-blue-700" 
                                    : "bg-orange-100 text-orange-700"
                                }`}>
                                    {isImportTransaction(selectedTransaction)
                                     ? "NHẬP KHO" : "XUẤT KHO"}
                                </span>

                                {/* NÚT IN MÀ BẠN CẦN LÀ Ở ĐÂY */}
                                <PrintActionButton 
                                    ticketData={{
                                        code: selectedTransaction.id,
                                        date: new Date(selectedTransaction.date || Date.now()).toLocaleDateString("vi-VN"),
                                        partnerName: selectedTransaction.partnerName || "Không có",
                                        reason: selectedTransaction.note || "Hệ thống ghi nhận",
                                        // Vì bảng lịch sử hiện theo từng dòng SP, ta gói nó vào array items để in
                                        items: [{
                                            sku: selectedTransaction.productId, 
                                            name: selectedTransaction.productName,
                                            quantity: selectedTransaction.quantity
                                        }]
                                    }} 
                                    type={isImportTransaction(selectedTransaction) ? "IN" : "OUT"}
                                    // CSS tùy chỉnh cho cục nút bấm in trong modal
                                    className="bg-slate-100 hover:bg-blue-600 hover:text-white rounded-lg transition-colors p-2"
                                />
                            </div>
                        </div>

                        {/* Nội dung chính trong Modal */}
                        <div className="bg-slate-50 rounded-2xl p-6 space-y-5">
                            <div>
                                <p className="text-xs font-bold text-slate-400 uppercase tracking-wider mb-1">Sản phẩm</p>
                                <p className="text-xl font-black text-slate-900 uppercase">
                                    {selectedTransaction.productName || "-"}
                                </p>
                            </div>
                            <div className="flex justify-between items-center">
                                <div>
                                    <p className="text-xs font-bold text-slate-400 uppercase tracking-wider mb-1">Số lượng</p>
                                    <p className="text-2xl font-black text-slate-900">
                                        {selectedTransaction.quantity || 0}
                                    </p>
                                </div>
                            </div>
                            <div>
                                <p className="text-xs font-bold text-slate-400 uppercase tracking-wider mb-1">Ghi chú</p>
                                <p className="text-base font-medium text-slate-700 italic">
                                    {selectedTransaction.note || "Không có ghi chú thêm."}
                                </p>
                            </div>
                        </div>
                    </div>
                )}
            </Modal>
        </div>
    );
}