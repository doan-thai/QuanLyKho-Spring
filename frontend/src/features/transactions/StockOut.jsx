import React, { useEffect, useState } from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { toast } from "sonner";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import Modal from "../../components/ui/Modal";
import AddStockOutItemForm from "./AddStockOutItemForm";
import { stockService } from "../../services/stockService";
import { productService } from "../../services/productService";
import PrintTicketModal from "./PrintTicketModal";

const generateTicketCode = (prefix) => {
    const now = new Date();
    const datePart = `${now.getFullYear()}${String(now.getMonth() + 1).padStart(2, "0")}${String(now.getDate()).padStart(2, "0")}`;
    const timePart = `${String(now.getHours()).padStart(2, "0")}${String(now.getMinutes()).padStart(2, "0")}${String(now.getSeconds()).padStart(2, "0")}`;
    const randomPart = Math.floor(100 + Math.random() * 900);
    return `${prefix}-${datePart}${timePart}-${randomPart}`;
};

const getTodayDateString = () => {
    const now = new Date();
    const localNow = new Date(now.getTime() - now.getTimezoneOffset() * 60000);
    return localNow.toISOString().split("T")[0];
};

export default function StockOut() {
    const queryClient = useQueryClient();
    const DRAFT_KEY = "stock-out-draft-v1";

    const [items, setItems] = useState([]);
    const [isAddModalOpen, setIsAddModalOpen] = useState(false);
    const [customer, setCustomer] = useState("");
    const [exportDate, setExportDate] = useState(() => getTodayDateString());
    const [note, setNote] = useState("");
    const [ticketCode, setTicketCode] = useState(() => generateTicketCode("PX"));
    const [printData, setPrintData] = useState(null);

    useEffect(() => {
        const rawDraft = localStorage.getItem(DRAFT_KEY);
        if (!rawDraft) return;

        try {
            const draft = JSON.parse(rawDraft);
            setItems(Array.isArray(draft.items) ? draft.items : []);
            setCustomer(draft.customer || "");
            setExportDate(getTodayDateString());
            setNote(draft.note || "");
            setTicketCode(draft.ticketCode || generateTicketCode("PX"));
        } catch {
            localStorage.removeItem(DRAFT_KEY);
            setExportDate(getTodayDateString());
        }
    }, []);

    // Đã gộp và chỉ giữ 1 bản duy nhất ở đây
    const handleSaveDraft = () => {
        const draft = { items, customer, exportDate, note, ticketCode };
        localStorage.setItem(DRAFT_KEY, JSON.stringify(draft));
        toast.success("Đã lưu bản nháp phiếu xuất");
    };

    const handleCancelDraft = () => {
        const accepted = window.confirm(
            "Bạn có chắc muốn hủy thao tác và xóa bản nháp hiện tại?",
        );
        if (!accepted) return;

        setItems([]);
        setCustomer("");
        setExportDate(getTodayDateString());
        setNote("");
        setTicketCode(generateTicketCode("PX"));
        localStorage.removeItem(DRAFT_KEY);
        toast.info("Đã hủy thao tác xuất kho");
    };

    const { data: products = [], isLoading: isProductsLoading } = useQuery({
        queryKey: ["products-for-stock-out"],
        queryFn: async () => {
            const res = await productService.getAll({
                pageNumber: 1,
                pageSize: 100,
            });
            return res?.data?.data || [];
        },
    });

    const handleAddItemToList = (data) => {
        const existingItemIndex = items.findIndex(
            (item) => item.productId === data.productId,
        );

        if (existingItemIndex >= 0) {
            const newItems = [...items];
            const newQty = newItems[existingItemIndex].quantity + data.quantity;

            if (newQty > data.currentStock) {
                toast.warning(
                    "Tổng số lượng xuất không được vượt quá tồn kho hiện tại",
                );
                return;
            }
            newItems[existingItemIndex].quantity = newQty;
            newItems[existingItemIndex].exportPrice = data.exportPrice;
            setItems(newItems);
        } else {
            setItems([...items, { ...data, tempId: Date.now() }]);
        }
        setIsAddModalOpen(false);
    };

    const updateQuantity = (id, newQty) => {
        setItems(
            items.map((item) => {
                if (item.tempId === id) {
                    const validQty = Math.max(
                        1,
                        Math.min(item.currentStock, newQty),
                    );
                    return { ...item, quantity: validQty };
                }
                return item;
            }),
        );
    };

    const removeItem = (id) => {
        setItems(items.filter((item) => item.tempId !== id));
    };

    const updateExportPrice = (id, newPrice) => {
        setItems(
            items.map((item) => {
                if (item.tempId === id) {
                    return { ...item, exportPrice: Math.max(0, newPrice) };
                }
                return item;
            }),
        );
    };

    const totalInvoice = items.reduce(
        (sum, item) => sum + (Number(item.quantity) || 0) * (Number(item.exportPrice) || 0),
        0,
    );

    const createTicketMutation = useMutation({
        mutationFn: (newTicket) => stockService.createOutwardTicket(newTicket),
        onSuccess: (_, variables) => {
            toast.success("Lưu phiếu xuất kho thành công");

            setPrintData({
                code: variables?.code || ticketCode,
                date: exportDate,
                partnerName: customer,
                note,
                items: [...items], // copy danh sách sản phẩm
            });

            setItems([]);
            setCustomer("");
            setExportDate(getTodayDateString());
            setNote("");
            setTicketCode(generateTicketCode("PX"));
            localStorage.removeItem(DRAFT_KEY);
            queryClient.invalidateQueries({ queryKey: ["dashboard-stats"] });
            queryClient.invalidateQueries({ queryKey: ["stock-history"] });
        },
        onError: (error) => {
            const message =
                error?.response?.data || "Có lỗi xảy ra khi lưu phiếu xuất";
            toast.error(String(message));
        },
    });

    const handleSubmit = () => {
        if (items.length === 0) {
            toast.warning("Phiếu xuất cần ít nhất một sản phẩm");
            return;
        }

        const codeForSubmit = ticketCode.trim() || generateTicketCode("PX");
        if (!ticketCode.trim()) {
            setTicketCode(codeForSubmit);
        }

        const formattedItems = items.map((item) => ({
            productId: item.productId,
            quantity: item.quantity,
            price: Math.max(0, Number(item.exportPrice) || 0),
        }));

        // Gọi mutation 1 lần duy nhất với đầy đủ dữ liệu
        createTicketMutation.mutate({
            code: codeForSubmit,
            note,
            items: formattedItems,
        });
    };

    return (
        <div className="max-w-7xl mx-auto">
            <div className="mb-10">
                <h2 className="text-4xl font-black tracking-tight text-slate-900 mb-3 uppercase">
                    Xuất kho hàng hóa
                </h2>
                <p className="text-slate-500 text-base max-w-lg font-medium">
                    Quản lý quy trình xuất hàng đi và đảm bảo lưu lượng hàng hóa
                    rời kho chính xác.
                </p>
            </div>

            <div className="grid grid-cols-12 gap-8">
                <div className="col-span-12 lg:col-span-4 flex flex-col gap-6">
                    <section className="bg-surface-container-lowest p-8 rounded-xl shadow-sm border border-outline-variant/10 relative overflow-hidden">
                        <div className="absolute top-0 left-0 w-1.5 h-full bg-primary"></div>
                        <h3 className="text-sm font-bold uppercase tracking-[0.1em] text-primary mb-8">
                            Thông tin chung
                        </h3>

                        <div className="space-y-6">
                            <div className="space-y-2.5">
                                <Label
                                    htmlFor="ma-xuat"
                                    className="text-xs font-bold uppercase tracking-wider text-outline"
                                >
                                    Mã đơn xuất
                                </Label>
                                <Input
                                    id="ma-xuat"
                                    placeholder="Mã phiếu được tạo tự động"
                                    value={ticketCode}
                                    readOnly
                                    className="bg-surface-container-low border-outline-variant/30 focus-visible:ring-primary font-mono text-base h-12 shadow-sm"
                                />
                            </div>
                            <div className="space-y-2.5">
                                <Label
                                    htmlFor="khach-hang"
                                    className="text-xs font-bold uppercase tracking-wider text-outline"
                                >
                                    Khách hàng / Dự án
                                </Label>
                                <Input
                                    id="khach-hang"
                                    placeholder="Nhập tên đối tác..."
                                    value={customer}
                                    onChange={(e) =>
                                        setCustomer(e.target.value)
                                    }
                                    className="bg-surface-container-low border-outline-variant/30 focus-visible:ring-primary text-base h-12 shadow-sm"
                                />
                            </div>
                            <div className="space-y-2.5">
                                <Label
                                    htmlFor="ngay-xuat"
                                    className="text-xs font-bold uppercase tracking-wider text-outline"
                                >
                                    Ngày xuất
                                </Label>
                                <Input
                                    id="ngay-xuat"
                                    type="date"
                                    value={exportDate}
                                    readOnly
                                    disabled
                                    className="bg-slate-100 border-outline-variant/30 text-base h-12 shadow-sm block cursor-not-allowed"
                                />
                            </div>
                            <div className="space-y-2.5">
                                <Label
                                    htmlFor="ly-do"
                                    className="text-xs font-bold uppercase tracking-wider text-outline"
                                >
                                    Ghi chú
                                </Label>
                                <textarea
                                    id="ly-do"
                                    value={note}
                                    onChange={(e) => setNote(e.target.value)}
                                    className="flex w-full rounded-md border border-outline-variant/30 bg-surface-container-low px-4 py-3 text-base shadow-sm transition-colors focus-visible:outline-none focus-visible:ring-1 focus-visible:ring-primary resize-none"
                                    placeholder="Ghi chú thêm cho phiếu xuất (không bắt buộc)..."
                                    rows="4"
                                ></textarea>
                            </div>
                        </div>
                    </section>

                    <div className="bg-primary text-white p-8 rounded-xl shadow-lg relative overflow-hidden flex-1">
                        <div className="absolute -right-4 -bottom-4 opacity-10">
                            <span className="material-symbols-outlined text-9xl">
                                inventory
                            </span>
                        </div>
                        <p className="text-xs font-bold uppercase tracking-widest opacity-80 mb-2">
                            Tổng giá trị xuất kho
                        </p>
                        <h4 className="text-4xl font-black mb-6 truncate">
                            {totalInvoice.toLocaleString()} <span className="text-xl font-medium opacity-60">đ</span>
                        </h4>
                        <div className="flex items-center gap-2 text-sm bg-white/10 w-fit px-4 py-2 rounded-full backdrop-blur-md">
                            <span className="material-symbols-outlined text-sm">
                                verified
                            </span>
                            <span>Trạng thái: Đang soạn thảo</span>
                        </div>
                    </div>
                </div>

                <div className="col-span-12 lg:col-span-8">
                    <div className="bg-surface-container-lowest rounded-xl shadow-sm border border-outline-variant/10 flex flex-col h-full overflow-hidden">
                        <div className="p-8 flex justify-between items-center border-b border-outline-variant/10">
                            <h3 className="text-sm font-bold uppercase tracking-[0.1em] text-slate-500 flex items-center gap-2">
                                <span className="material-symbols-outlined text-lg">
                                    list_alt
                                </span>{" "}
                                Danh sách sản phẩm xuất
                            </h3>
                            <button
                                onClick={() => setIsAddModalOpen(true)}
                                className="flex items-center gap-2 text-sm font-black uppercase tracking-wider text-white bg-[#003d9b] hover:bg-blue-700 px-4 py-2.5 rounded-xl transition-all shadow-lg shadow-blue-900/20"
                            >
                                <span className="material-symbols-outlined text-base">
                                    add_circle
                                </span>{" "}
                                Thêm sản phẩm
                            </button>
                        </div>

                        <div className="flex-1 overflow-x-auto">
                            <table className="w-full text-left border-collapse">
                                <thead>
                                    <tr>
                                        <th className="px-8 py-5 text-xs font-bold text-slate-400 uppercase tracking-wider">
                                            Mã
                                        </th>
                                        <th className="px-8 py-5 text-xs font-bold text-slate-400 uppercase tracking-wider">
                                            Tên sản phẩm
                                        </th>
                                        <th className="px-8 py-5 text-xs font-bold text-slate-400 uppercase tracking-wider text-right">
                                            Tồn kho hiện tại
                                        </th>
                                        <th className="px-8 py-5 text-xs font-bold text-slate-400 uppercase tracking-wider text-center">
                                            Số lượng xuất
                                        </th>
                                        <th className="px-8 py-5 text-xs font-bold text-slate-400 uppercase tracking-wider text-right">
                                            Đơn giá
                                        </th>
                                        <th className="px-8 py-5 text-xs font-bold text-slate-400 uppercase tracking-wider text-right">
                                            Thành tiền
                                        </th>
                                        <th className="px-8 py-5"></th>
                                    </tr>
                                </thead>
                                <tbody className="divide-y divide-outline-variant/10">
                                    {items.length === 0 ? (
                                        <tr>
                                            <td
                                                colSpan="7"
                                                className="px-8 py-20 text-center text-slate-400 font-bold uppercase tracking-widest text-xs"
                                            >
                                                Chưa có sản phẩm nào trong phiếu
                                            </td>
                                        </tr>
                                    ) : (
                                        items.map((item) => (
                                            <tr
                                                key={item.tempId}
                                                className="hover:bg-surface-container-low/40 transition-colors"
                                            >
                                                <td className="px-8 py-6 font-mono text-sm font-bold text-primary">
                                                    {item.sku ||
                                                        `SP-${item.productId}`}
                                                </td>
                                                <td className="px-8 py-6">
                                                    <p className="text-base font-bold text-slate-900 mb-1">
                                                        {item.name}
                                                    </p>
                                                    <p className="text-xs text-slate-400 uppercase">
                                                        {item.category}
                                                    </p>
                                                </td>
                                                <td className="px-8 py-6 text-right">
                                                    <span className="text-base font-medium text-slate-600">
                                                        {item.currentStock.toLocaleString()}
                                                    </span>
                                                </td>
                                                <td className="px-8 py-6 text-center">
                                                    <Input
                                                        type="number"
                                                        min="1"
                                                        max={item.currentStock}
                                                        value={item.quantity}
                                                        onChange={(e) =>
                                                            updateQuantity(
                                                                item.tempId,
                                                                parseInt(
                                                                    e.target
                                                                        .value,
                                                                    10,
                                                                ) || 1,
                                                            )
                                                        }
                                                        className="w-24 h-10 mx-auto text-center bg-surface-container-low border-outline-variant/30 focus-visible:ring-primary font-bold text-base shadow-sm"
                                                    />
                                                </td>
                                                <td className="px-8 py-6 text-right">
                                                    <Input
                                                        type="number"
                                                        min="0"
                                                        value={item.exportPrice || 0}
                                                        onChange={(e) =>
                                                            updateExportPrice(
                                                                item.tempId,
                                                                parseInt(e.target.value, 10) || 0,
                                                            )
                                                        }
                                                        className="w-32 h-10 ml-auto text-right bg-surface-container-low border-outline-variant/30 focus-visible:ring-primary font-medium text-slate-700 text-base shadow-sm"
                                                    />
                                                </td>
                                                <td className="px-8 py-6 text-right font-bold text-primary whitespace-nowrap">
                                                    {(
                                                        (Number(item.quantity) || 0) *
                                                        (Number(item.exportPrice) || 0)
                                                    ).toLocaleString()} đ
                                                </td>
                                                <td className="px-8 py-6 text-right">
                                                    <button
                                                        onClick={() =>
                                                            removeItem(
                                                                item.tempId,
                                                            )
                                                        }
                                                        className="text-error/50 hover:text-error transition-colors"
                                                    >
                                                        <span className="material-symbols-outlined text-xl">
                                                            delete
                                                        </span>
                                                    </button>
                                                </td>
                                            </tr>
                                        ))
                                    )}
                                </tbody>
                            </table>
                        </div>

                        <div className="p-8 border-t border-outline-variant/10 flex items-center justify-between mt-auto">
                            <div className="flex items-center gap-6">
                                <button
                                    onClick={handleSaveDraft}
                                    className="text-base font-bold text-slate-600 hover:text-slate-900 transition-colors"
                                >
                                    Lưu bản nháp
                                </button>
                                <button
                                    onClick={handleCancelDraft}
                                    className="text-base font-bold text-error hover:text-red-700 transition-colors"
                                >
                                    Hủy bỏ
                                </button>
                            </div>
                            <button
                                onClick={handleSubmit}
                                disabled={
                                    items.length === 0 ||
                                    createTicketMutation.isPending
                                }
                                className="bg-primary text-white px-8 py-4 rounded-xl text-base font-bold shadow-lg shadow-primary/30 flex items-center gap-3 hover:scale-[1.02] active:scale-95 transition-all disabled:opacity-50 disabled:active:scale-100"
                            >
                                {createTicketMutation.isPending
                                    ? "Đang xử lý..."
                                    : "Xác nhận xuất kho"}
                                <span className="material-symbols-outlined text-xl">
                                    {createTicketMutation.isPending
                                        ? "sync"
                                        : "rocket_launch"}
                                </span>
                            </button>
                        </div>
                    </div>
                </div>
            </div>

            <Modal
                isOpen={isAddModalOpen}
                onClose={() => setIsAddModalOpen(false)}
                title="Tìm & Chọn Hàng Xuất Kho"
            >
                <AddStockOutItemForm
                    products={products}
                    isLoading={isProductsLoading}
                    onAdd={handleAddItemToList}
                    onClose={() => setIsAddModalOpen(false)}
                />
            </Modal>

            {/* Modal hiển thị báo thành công & in phiếu */}
            <PrintTicketModal
                isOpen={!!printData}
                onClose={() => setPrintData(null)}
                ticketData={printData}
                type="OUT"
            />
        </div>
    );
}
