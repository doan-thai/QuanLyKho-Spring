import React, { useRef, useState } from "react";
import { useNavigate } from "react-router-dom";
import { useQuery } from "@tanstack/react-query";
import PageWrapper from "../../components/layout/PageWrapper";
import {
    Activity,
    BarChart3,
    ArrowDownLeft,
    ArrowUpRight,
    Loader2,
    Bot,
    Send,
} from "lucide-react";
import {
    BarChart,
    Bar,
    XAxis,
    YAxis,
    CartesianGrid,
    Tooltip,
    Legend,
    ResponsiveContainer,
} from "recharts";
import { stockService } from "../../services/stockService";
import Modal from "../../components/ui/Modal";
import { aiService } from "../../services/aiService";

// IMPORT THÊM ĐỂ LẤY ROLE
import useAuthStore from "../../lib/authStore";

// IMPORT COMPONENT BÁO CÁO VÀ NHẬT KÝ ĐỂ GỘP VÀO ĐÁY TRANG
import Reports from "./Reports";
import StockInHistory from "../transactions/StockInHistory";

export default function Dashboard() {
    const navigate = useNavigate();
    const { role } = useAuthStore(); // LẤY ROLE TỪ ĐÂY
    const reportSectionRef = useRef(null);
    const historySectionRef = useRef(null);

    const canViewReports = ["Admin", "KeToan"].includes(role);
    const canViewHistory = ["Admin", "ThuKho", "KeToan"].includes(role);

    const scrollToSection = (sectionRef) => {
        sectionRef?.current?.scrollIntoView({
            behavior: "smooth",
            block: "start",
        });
    };

    // Dữ liệu mặc định khi chưa có report API
    const zeroData = {
        totalStock: 0,
        alertsCount: 0,
        shippingCount: 0,
        monthlyIn: 0,
        monthlyOut: 0,
        inventory: [],
        chartData: [
            { day: "Th 2", in: 0, out: 0 },
            { day: "Th 3", in: 0, out: 0 },
            { day: "Th 4", in: 0, out: 0 },
            { day: "Th 5", in: 0, out: 0 },
            { day: "Th 6", in: 0, out: 0 },
            { day: "Th 7", in: 0, out: 0 },
            { day: "CN", in: 0, out: 0 },
        ],
        recentActivities: [],
    };

    const { data: stats, isLoading } = useQuery({
        queryKey: ["dashboard-stats"],
        retry: false,
        queryFn: async () => {
            const stockRes = await stockService.getInventory();
            const inventory = Array.isArray(stockRes?.data)
                ? stockRes.data
                : [];

            let history = [];
            try {
                const historyRes = await stockService.getHistory();
                history = Array.isArray(historyRes?.data)
                    ? historyRes.data
                    : [];
            } catch {
                history = [];
            }

            const lowStockItems = inventory.filter(
                (item) => item.isLowStock,
            ).length;
            const totalStock = inventory.reduce(
                (sum, item) => sum + Number(item.stock || 0),
                0,
            );

            const now = new Date();
            const monthlyIn = history
                .filter((tx) => {
                    const d = new Date(
                        tx.date || tx.transactionDate || Date.now(),
                    );
                    const txType = String(
                        tx.type || tx.transactionType || "",
                    ).toUpperCase();
                    return (
                        d.getMonth() === now.getMonth() &&
                        d.getFullYear() === now.getFullYear() &&
                        (txType.includes("IMPORT") ||
                            txType.includes("IN") ||
                            txType.includes("NHAP"))
                    );
                })
                .reduce((sum, tx) => sum + Number(tx.quantity || 0), 0);

            const monthlyOut = history
                .filter((tx) => {
                    const d = new Date(
                        tx.date || tx.transactionDate || Date.now(),
                    );
                    const txType = String(
                        tx.type || tx.transactionType || "",
                    ).toUpperCase();
                    return (
                        d.getMonth() === now.getMonth() &&
                        d.getFullYear() === now.getFullYear() &&
                        (txType.includes("EXPORT") ||
                            txType.includes("OUT") ||
                            txType.includes("XUAT"))
                    );
                })
                .reduce((sum, tx) => sum + Number(tx.quantity || 0), 0);

            const shippingCount = history.filter((tx) => {
                const txType = String(
                    tx.type || tx.transactionType || "",
                ).toUpperCase();
                return (
                    txType.includes("EXPORT") ||
                    txType.includes("OUT") ||
                    txType.includes("XUAT")
                );
            }).length;

            const daysLabel = [
                "CN",
                "Th 2",
                "Th 3",
                "Th 4",
                "Th 5",
                "Th 6",
                "Th 7",
            ];
            const chartData = Array.from({ length: 7 }, (_, i) => {
                const d = new Date();
                d.setDate(d.getDate() - (6 - i));
                return {
                    dateStr: d.toISOString().split("T")[0],
                    day: daysLabel[d.getDay()],
                    in: 0,
                    out: 0,
                };
            });

            history.forEach((trans) => {
                const rawDate = trans.date || trans.transactionDate;
                if (!rawDate) {
                    return;
                }

                const dateStr = new Date(rawDate).toISOString().split("T")[0];
                const slot = chartData.find((d) => d.dateStr === dateStr);
                if (!slot) {
                    return;
                }

                const txType = String(
                    trans.type || trans.transactionType || "",
                ).toUpperCase();
                const quantity = Number(trans.quantity || 0);

                if (
                    txType.includes("IMPORT") ||
                    txType.includes("IN") ||
                    txType.includes("NHAP")
                ) {
                    slot.in += quantity;
                } else if (
                    txType.includes("EXPORT") ||
                    txType.includes("OUT") ||
                    txType.includes("XUAT")
                ) {
                    slot.out += quantity;
                }
            });

            return {
                ...zeroData,
                inventory,
                totalStock,
                alertsCount: lowStockItems,
                shippingCount,
                monthlyIn,
                monthlyOut,
                chartData,
                recentActivities: history.slice(0, 5).map((item, index) => {
                    const txType = String(
                        item.type || item.transactionType || "",
                    ).toUpperCase();
                    const isImport =
                        txType.includes("IMPORT") ||
                        txType.includes("IN") ||
                        txType.includes("NHAP");
                    return {
                        id: item.id || `${txType}-${index}`,
                        type: isImport ? "in" : "out",
                        title: `${isImport ? "Nhập kho" : "Xuất kho"} ${item.productName || item.name || ""}`.trim(),
                        user: "Hệ thống",
                        code: `SL: ${item.quantity || 0}`,
                    };
                }),
            };
        },
    });

    if (isLoading)
        return (
            <div className="h-screen flex items-center justify-center">
                <Loader2 className="animate-spin text-blue-700" size={48} />
            </div>
        );

    return (
        <PageWrapper className="w-full px-4 md:px-10">
            <div className="mb-10">
                <div className="flex items-center justify-between gap-4 flex-wrap mb-3">
                    <h2 className="text-2xl md:text-3xl font-black tracking-tight text-[#091c35] uppercase">
                        Trung tâm điều phối
                    </h2>
                </div>
                <p className="text-[#434654] text-sm md:text-base font-medium">
                    Hệ thống đang vận hành ổn định trên toàn chi nhánh.
                </p>
            </div>

            <div className="mb-10">
                <div className="bg-[#003d9b] text-white p-10 rounded-xl shadow-xl relative overflow-hidden group">
                    <div className="relative z-10">
                        <p className="text-xs md:text-sm font-bold uppercase tracking-[0.2em] opacity-70 mb-4">
                            Tổng tồn kho hiện tại
                        </p>
                        <h3 className="text-4xl md:text-5xl font-black tracking-tighter mb-8">
                            {stats?.totalStock?.toLocaleString() || "0"}{" "}
                            <span className="text-lg md:text-xl opacity-60 font-medium uppercase">
                                Sản phẩm
                            </span>
                        </h3>
                        <div className="flex flex-wrap gap-4 md:gap-6">
                            <div className="bg-white/10 backdrop-blur-md px-6 py-3 rounded-xl border border-white/10">
                                <p className="text-xs md:text-sm opacity-70 uppercase font-bold mb-1">
                                    Nhập tháng này
                                </p>
                                <p className="text-xl md:text-2xl font-black">
                                    +{stats?.monthlyIn}
                                </p>
                            </div>
                            <div className="bg-white/10 backdrop-blur-md px-6 py-3 rounded-xl border border-white/10">
                                <p className="text-xs md:text-sm opacity-70 uppercase font-bold mb-1">
                                    Xuất tháng này
                                </p>
                                <p className="text-xl md:text-2xl font-black">
                                    -{stats?.monthlyOut}
                                </p>
                            </div>
                        </div>
                    </div>
                    <Activity className="absolute -right-12 -bottom-12 text-[280px] w-96 h-96 opacity-10 rotate-12" />
                </div>
            </div>

            <div className="grid grid-cols-12 gap-8 mb-10">
                <div className="col-span-12 lg:col-span-8">
                    <div className="bg-white p-8 rounded-xl border border-slate-200 shadow-sm h-full flex flex-col">
                        <h3 className="text-base md:text-lg font-black uppercase tracking-widest text-slate-500 flex items-center gap-2 mb-6">
                            <BarChart3 size={20} /> Lưu lượng hàng hóa (7 ngày
                            qua)
                        </h3>
                        <div className="flex-1 w-full min-h-[300px]">
                            <ResponsiveContainer width="100%" height="100%">
                                <BarChart
                                    data={stats?.chartData || []}
                                    barGap={8}
                                >
                                    <CartesianGrid
                                        strokeDasharray="3 3"
                                        vertical={false}
                                        stroke="#e2e8f0"
                                    />
                                    <XAxis
                                        dataKey="day"
                                        axisLine={false}
                                        tickLine={false}
                                        tick={{ fontSize: 14 }}
                                    />
                                    <YAxis
                                        axisLine={false}
                                        tickLine={false}
                                        tick={{ fontSize: 14 }}
                                    />
                                    <Tooltip
                                        contentStyle={{
                                            borderRadius: "12px",
                                            border: "none",
                                            shadow: "none",
                                            fontSize: "14px",
                                        }}
                                    />
                                    <Legend
                                        iconType="circle"
                                        wrapperStyle={{ fontSize: "14px" }}
                                    />
                                    <Bar
                                        dataKey="in"
                                        name="Nhập kho"
                                        fill="#003d9b"
                                        radius={[6, 6, 0, 0]}
                                    />
                                    <Bar
                                        dataKey="out"
                                        name="Xuất kho"
                                        fill="#f97316"
                                        radius={[6, 6, 0, 0]}
                                    />
                                </BarChart>
                            </ResponsiveContainer>
                        </div>
                    </div>
                </div>

                <div className="col-span-12 lg:col-span-4">
                    <div className="bg-white p-8 rounded-xl border border-slate-200 shadow-sm flex flex-col h-full">
                        <h3 className="text-base md:text-lg font-black uppercase tracking-widest text-slate-500 mb-8">
                            Hoạt động gần đây
                        </h3>
                        <div className="space-y-8 flex-1">
                            {stats?.recentActivities?.length > 0 ? (
                                stats.recentActivities.map((item) => (
                                    <div
                                        key={item.id}
                                        className="flex items-start gap-4"
                                    >
                                        <div
                                            className={`w-12 h-12 rounded-xl flex items-center justify-center text-white shrink-0 ${
                                                item.type === "in"
                                                    ? "bg-[#003d9b]"
                                                    : "bg-[#f97316]"
                                            }`}
                                        >
                                            {item.type === "in" ? (
                                                <ArrowDownLeft size={24} />
                                            ) : (
                                                <ArrowUpRight size={24} />
                                            )}
                                        </div>
                                        <div>
                                            <p className="text-sm md:text-base font-black text-[#091c35] leading-tight mb-1">
                                                {item.title}
                                            </p>
                                            <p className="text-xs md:text-sm text-slate-500 font-bold">
                                                {item.user} đã xử lý {item.code}
                                            </p>
                                        </div>
                                    </div>
                                ))
                            ) : (
                                <p className="text-xs font-bold text-slate-400 uppercase text-center py-10">
                                    Chưa có hoạt động
                                </p>
                            )}
                        </div>

                        <div className="mt-10 flex flex-col gap-3">
                            {canViewReports && (
                                <button
                                    onClick={() => scrollToSection(reportSectionRef)}
                                    className="w-full py-4 text-sm md:text-base font-black text-[#003d9b] bg-[#dae2ff] hover:bg-[#c4d2ff] rounded-xl transition-all uppercase tracking-widest"
                                >
                                    Xem báo cáo
                                </button>
                            )}

                            {canViewHistory && (
                                <button
                                    onClick={() => scrollToSection(historySectionRef)}
                                    className="w-full py-4 text-sm md:text-base font-black text-[#003d9b] bg-[#dae2ff] hover:bg-[#c4d2ff] rounded-xl transition-all uppercase tracking-widest"
                                >
                                    Xem nhật ký
                                </button>
                            )}
                        </div>
                    </div>
                </div>
            </div>

            <div className="bg-white p-8 rounded-xl border border-slate-200 shadow-sm">
                <h3 className="text-base md:text-lg font-black uppercase tracking-widest text-slate-500 mb-6">
                    Tồn kho hiện tại
                </h3>
                <div className="overflow-x-auto">
                    <table className="w-full text-left border-collapse min-w-[640px]">
                        <thead>
                            <tr className="border-b border-slate-200">
                                <th className="py-3 text-[11px] font-black text-slate-500 uppercase tracking-widest">
                                    Sản phẩm
                                </th>
                                <th className="py-3 text-[11px] font-black text-slate-500 uppercase tracking-widest">
                                    Danh mục
                                </th>
                                <th className="py-3 text-[11px] font-black text-slate-500 uppercase tracking-widest text-right">
                                    Tồn
                                </th>
                                <th className="py-3 text-[11px] font-black text-slate-500 uppercase tracking-widest text-right">
                                    Mức tối thiểu
                                </th>
                            </tr>
                        </thead>
                        <tbody>
                            {stats?.inventory?.length > 0 ? (
                                stats.inventory.slice(0, 10).map((item) => (
                                    <tr
                                        key={item.id}
                                        className={`border-b border-slate-100 ${item.isLowStock ? "bg-red-50/70" : ""}`}
                                    >
                                        <td className="py-3 font-bold text-slate-800">
                                            {item.name}
                                        </td>
                                        <td className="py-3 text-slate-600">
                                            {item.category}
                                        </td>
                                        <td
                                            className={`py-3 text-right font-black ${item.isLowStock ? "text-red-700" : "text-slate-800"}`}
                                        >
                                            {item.stock}
                                        </td>
                                        <td className="py-3 text-right text-slate-600">
                                            {item.minStock}
                                        </td>
                                    </tr>
                                ))
                            ) : (
                                <tr>
                                    <td
                                        colSpan="4"
                                        className="py-8 text-center text-slate-400 font-bold uppercase text-xs tracking-widest"
                                    >
                                        Chưa có dữ liệu tồn kho
                                    </td>
                                </tr>
                            )}
                        </tbody>
                    </table>
                </div>
            </div>

            {/* ========================================================= */}
            {/* KHU VỰC GỘP: HIỂN THỊ DƯỚI ĐÁY THEO ROLE */}
            {/* ========================================================= */}

            {/* TRUNG TÂM BÁO CÁO: Hiện cho QUẢN LÝ và KẾ TOÁN */}
            {canViewReports && (
                <div ref={reportSectionRef} className="mt-16 pt-10 border-t-4 border-slate-100">
                    <Reports />
                </div>
            )}

            {/* NHẬT KÝ GIAO DỊCH: Hiện cho QUẢN LÝ và THỦ KHO */}
            {canViewHistory && (
                <div ref={historySectionRef} className="mt-16 pt-10 border-t-4 border-slate-100">
                    <StockInHistory />
                </div>
            )}

            {/* ========================================================= */}
        </PageWrapper>
    );
}
