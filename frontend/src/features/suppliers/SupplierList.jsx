import React, { useState, useMemo } from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import {
    Search,
    Filter,
    Pencil,
    Trash2,
    ChevronLeft,
    ChevronRight,
    Loader2,
    AlertCircle,
    Plus,
} from "lucide-react";
import { toast } from "sonner";
import Modal from "../../components/ui/Modal";
import SupplierFilterForm from "./SupplierFilterForm";
import { supplierService } from "../../services/supplierService";
import useAuthStore from "../../lib/authStore";

const defaultSupplierForm = {
    name: "",
    contactPerson: "",
    phone: "",
    email: "",
    address: "",
    isActive: true,
};

// HÀM BỔ TRỢ: XÓA DẤU TIẾNG VIỆT
const normalizeStr = (str) =>
    String(str || "")
        .normalize("NFD")
        .replace(/[\u0300-\u036f]/g, "")
        .toLowerCase();

const phonePattern = /^(0\d{9}|\+84\d{9})$/;

export default function SupplierList() {
    const queryClient = useQueryClient();
    const role = useAuthStore((state) => state.role);
    const canManageSuppliers = role === "Admin" || role === "ThuKho";
    const [isFilterModalOpen, setIsFilterModalOpen] = useState(false);
    const [isCreateModalOpen, setIsCreateModalOpen] = useState(false);
    const [isEditModalOpen, setIsEditModalOpen] = useState(false);
    const [editingSupplier, setEditingSupplier] = useState(null);
    const [searchTerm, setSearchTerm] = useState("");
    const [createForm, setCreateForm] = useState(defaultSupplierForm);
    const [editForm, setEditForm] = useState(defaultSupplierForm);
    const [activeFilters, setActiveFilters] = useState({
        categories: [],
        status: "Tất cả",
        region: "Tất cả",
    });

    const {
        data: suppliers = [],
        isLoading,
        isError,
    } = useQuery({
        queryKey: ["suppliers"],
        queryFn: async () => {
            const response = await supplierService.getAll();
            return response.data || [];
        },
    });

    const createSupplierMutation = useMutation({
        mutationFn: (payload) => supplierService.create(payload),
        onSuccess: () => {
            toast.success("Thêm nhà cung cấp thành công");
            setCreateForm({ ...defaultSupplierForm });
            setIsCreateModalOpen(false);
            queryClient.invalidateQueries({ queryKey: ["suppliers"] });
            queryClient.invalidateQueries({
                queryKey: ["supplier-categories"],
            });
        },
        onError: (error) => {
            const message =
                error?.response?.data?.title ||
                error?.response?.data ||
                "Không thể thêm nhà cung cấp";
            toast.error(String(message));
        },
    });

    const updateSupplierMutation = useMutation({
        mutationFn: ({ id, payload }) => supplierService.update(id, payload),
        onSuccess: () => {
            toast.success("Cập nhật nhà cung cấp thành công");
            setIsEditModalOpen(false);
            setEditingSupplier(null);
            setEditForm({ ...defaultSupplierForm });
            queryClient.invalidateQueries({ queryKey: ["suppliers"] });
        },
        onError: (error) => {
            const message =
                error?.response?.data?.title ||
                error?.response?.data ||
                "Không thể cập nhật nhà cung cấp";
            toast.error(String(message));
        },
    });

    const deleteSupplierMutation = useMutation({
        mutationFn: (id) => supplierService.delete(id),
        onSuccess: () => {
            toast.success("Xóa nhà cung cấp thành công");
            queryClient.invalidateQueries({ queryKey: ["suppliers"] });
        },
        onError: (error) => {
            const message =
                error?.response?.data?.title ||
                error?.response?.data ||
                "Không thể xóa nhà cung cấp";
            toast.error(String(message));
        },
    });

    const handleCreateInput = (field, value) => {
        setCreateForm((prev) => ({ ...prev, [field]: value }));
    };

    const handleEditInput = (field, value) => {
        setEditForm((prev) => ({ ...prev, [field]: value }));
    };

    const handleCreateSupplier = (event) => {
        event.preventDefault();

        if (!canManageSuppliers) {
            toast.warning("Bạn không có quyền thêm nhà cung cấp");
            return;
        }

        const name = createForm.name.trim();
        const phone = createForm.phone.trim();
        if (!name) {
            toast.warning("Vui lòng nhập tên nhà cung cấp");
            return;
        }

        if (!phone) {
            toast.warning("Vui lòng nhập số điện thoại nhà cung cấp");
            return;
        }

        if (!phonePattern.test(phone)) {
            toast.warning("Số điện thoại không đúng định dạng (vd: 0901234567 hoặc +84901234567)");
            return;
        }

        createSupplierMutation.mutate({
            name,
            contactPerson: createForm.contactPerson.trim() || null,
            phone,
            email: createForm.email.trim() || null,
            address: createForm.address.trim() || null,
            isActive: createForm.isActive,
        });
    };

    const openCreateSupplierModal = () => {
        if (!canManageSuppliers) {
            toast.warning("Bạn không có quyền thêm nhà cung cấp");
            return;
        }

        setCreateForm({ ...defaultSupplierForm });
        setIsCreateModalOpen(true);
    };

    const openEditSupplierModal = (supplier) => {
        if (!canManageSuppliers) {
            toast.warning("Bạn không có quyền chỉnh sửa nhà cung cấp");
            return;
        }

        const accepted = window.confirm(`Xác nhận chỉnh sửa nhà cung cấp: ${supplier.name}?`);
        if (!accepted) {
            return;
        }

        setEditingSupplier(supplier);
        setEditForm({
            name: supplier.name || "",
            contactPerson: supplier.contactPerson || "",
            phone: supplier.phone || "",
            email: supplier.email || "",
            address: supplier.address || "",
            isActive: Boolean(supplier.isActive),
        });
        setIsEditModalOpen(true);
    };

    const handleUpdateSupplier = (event) => {
        event.preventDefault();

        if (!canManageSuppliers) {
            toast.warning("Bạn không có quyền chỉnh sửa nhà cung cấp");
            return;
        }

        if (!editingSupplier?.id) {
            toast.error("Không tìm thấy nhà cung cấp cần cập nhật");
            return;
        }

        const name = editForm.name.trim();
        const phone = editForm.phone.trim();
        if (!name) {
            toast.warning("Vui lòng nhập tên nhà cung cấp");
            return;
        }

        if (!phone) {
            toast.warning("Vui lòng nhập số điện thoại nhà cung cấp");
            return;
        }

        if (!phonePattern.test(phone)) {
            toast.warning("Số điện thoại không đúng định dạng (vd: 0901234567 hoặc +84901234567)");
            return;
        }

        updateSupplierMutation.mutate({
            id: editingSupplier.id,
            payload: {
                name,
                contactPerson: editForm.contactPerson.trim() || null,
                phone,
                email: editForm.email.trim() || null,
                address: editForm.address.trim() || null,
                isActive: editForm.isActive,
            },
        });
    };

    const handleDeleteSupplier = (supplier) => {
        if (!canManageSuppliers) {
            toast.warning("Bạn không có quyền xóa nhà cung cấp");
            return;
        }

        const accepted = window.confirm(`Xác nhận xóa nhà cung cấp: ${supplier.name}?`);
        if (!accepted) {
            return;
        }

        deleteSupplierMutation.mutate(supplier.id);
    };

    // ĐOẠN CODE ĐÃ ĐƯỢC NÂNG CẤP XÓA DẤU ĐỂ TÌM KIẾM CHUẨN XÁC
    const filteredSuppliers = useMemo(() => {
        if (!Array.isArray(suppliers)) return [];

        return suppliers.filter((sup) => {
            // Chuẩn hóa từ khóa tìm kiếm và địa chỉ về chữ thường không dấu
            const searchLower = normalizeStr(searchTerm);
            const addressNorm = normalizeStr(sup.address);

            const matchesSearch =
                normalizeStr(sup.name).includes(searchLower) ||
                normalizeStr(sup.id).includes(searchLower) ||
                normalizeStr(sup.contactPerson).includes(searchLower) ||
                normalizeStr(sup.phone).includes(searchLower) ||
                normalizeStr(sup.email).includes(searchLower) ||
                addressNorm.includes(searchLower);

            const statusLabel = sup.isActive ? "Đang hợp tác" : "Tạm dừng";
            const matchesStatus =
                activeFilters.status === "Tất cả" ||
                statusLabel === activeFilters.status;

            // So sánh Khu vực (bỏ dấu)
            const matchesRegion =
                activeFilters.region === "Tất cả" ||
                addressNorm.includes(normalizeStr(activeFilters.region));

            // So sánh Ngành hàng/Categories (bỏ dấu)
            const matchesCategory =
                activeFilters.categories.length === 0 ||
                activeFilters.categories.some((c) =>
                    addressNorm.includes(normalizeStr(c)),
                );

            return (
                matchesSearch &&
                matchesStatus &&
                matchesRegion &&
                matchesCategory
            );
        });
    }, [suppliers, searchTerm, activeFilters]);

    return (
        <div className="max-w-7xl mx-auto not-italic">
            <div className="mb-10 flex items-start justify-between gap-4 flex-wrap">
                <div>
                    <h2 className="text-4xl font-black tracking-tight text-slate-900 mb-3 uppercase">
                        Danh sách đối tác
                    </h2>
                    <p className="text-slate-500 font-medium">
                        Hệ thống quản lý chuỗi cung ứng và quan hệ nhà cung cấp
                        SyncStock.
                    </p>
                </div>
                {canManageSuppliers && (
                    <button
                        onClick={openCreateSupplierModal}
                        className="inline-flex items-center gap-2 bg-[#003d9b] text-white px-5 py-3 rounded-xl text-xs font-black uppercase tracking-widest hover:bg-blue-700 transition-colors"
                    >
                        <Plus size={16} /> Thêm nhà cung cấp
                    </button>
                )}
            </div>

            <div className="mb-8">
                <div className="bg-white p-3 rounded-2xl shadow-sm border border-slate-200 flex items-center gap-4 focus-within:border-[#003d9b]/50 transition-all">
                    <div className="flex-1 flex items-center gap-3 px-4">
                        <Search size={20} className="text-slate-300" />
                        <input
                            className="w-full bg-transparent border-none focus:ring-0 text-base py-2 text-slate-900 font-bold outline-none"
                            placeholder="Tìm kiếm theo tên, mã hoặc thông tin liên hệ..."
                            type="text"
                            value={searchTerm}
                            onChange={(e) => setSearchTerm(e.target.value)}
                        />
                    </div>
                    <button
                        onClick={() => setIsFilterModalOpen(true)}
                        className={`px-6 py-3 font-black text-[10px] uppercase tracking-widest rounded-xl transition-all flex items-center gap-2 ${
                            activeFilters.categories.length > 0 ||
                            activeFilters.status !== "Tất cả" ||
                            activeFilters.region !== "Tất cả"
                                ? "bg-[#003d9b] text-white"
                                : "bg-slate-100 text-slate-500 hover:bg-slate-200"
                        }`}
                    >
                        <Filter size={16} /> Bộ lọc nâng cao
                    </button>
                </div>
            </div>

            <div className="bg-white rounded-2xl shadow-sm border border-slate-200 overflow-hidden">
                <div className="overflow-x-auto">
                    <table className="w-full text-left border-collapse">
                        <thead className="bg-slate-50/50 border-b border-slate-100">
                            <tr>
                                <th className="px-8 py-5 text-[10px] font-black text-slate-400 uppercase tracking-widest">
                                    Nhà cung cấp
                                </th>
                                <th className="px-8 py-5 text-[10px] font-black text-slate-400 uppercase tracking-widest">
                                    Liên hệ
                                </th>
                                <th className="px-8 py-5 text-[10px] font-black text-slate-400 uppercase tracking-widest">
                                    Địa chỉ
                                </th>
                                <th className="px-8 py-5 text-[10px] font-black text-slate-400 uppercase tracking-widest text-center">
                                    Tình trạng
                                </th>
                                <th className="px-8 py-5 text-right"></th>
                            </tr>
                        </thead>
                        <tbody className="divide-y divide-slate-100">
                            {isLoading && (
                                <tr>
                                    <td
                                        colSpan="5"
                                        className="py-20 text-center"
                                    >
                                        <Loader2
                                            className="animate-spin mx-auto text-[#003d9b]"
                                            size={32}
                                        />
                                    </td>
                                </tr>
                            )}
                            {isError && (
                                <tr>
                                    <td
                                        colSpan="5"
                                        className="py-20 text-center text-red-500"
                                    >
                                        <AlertCircle
                                            className="mx-auto"
                                            size={32}
                                        />
                                        <p className="font-bold mt-2">
                                            Lỗi kết nối API!
                                        </p>
                                    </td>
                                </tr>
                            )}
                            {!isLoading &&
                                !isError &&
                                filteredSuppliers.length === 0 && (
                                    <tr>
                                        <td
                                            colSpan="5"
                                            className="py-20 text-center text-slate-400 font-bold uppercase tracking-widest text-xs"
                                        >
                                            Không tìm thấy đối tác nào
                                        </td>
                                    </tr>
                                )}

                            {!isLoading &&
                                !isError &&
                                filteredSuppliers.map((sup) => (
                                    <tr
                                        key={sup.id}
                                        className="hover:bg-slate-50/50 transition-colors"
                                    >
                                        <td className="px-8 py-6">
                                            <p className="font-black text-slate-900 text-base uppercase tracking-tight">
                                                {sup.name}
                                            </p>
                                            <p className="text-[10px] font-bold text-slate-400 uppercase tracking-widest mt-1">
                                                ID: {sup.id}
                                            </p>
                                        </td>
                                        <td className="px-8 py-6">
                                            <p className="text-sm font-black text-slate-700">
                                                {sup.contactPerson || "Chưa có"}
                                            </p>
                                            <p className="text-xs text-slate-400 font-medium">
                                                {sup.phone ||
                                                    sup.email ||
                                                    "Chưa có"}
                                            </p>
                                        </td>
                                        <td className="px-8 py-6">
                                            <p className="text-sm text-slate-600 font-medium">
                                                {sup.address ||
                                                    "Chưa có địa chỉ"}
                                            </p>
                                        </td>
                                        <td className="px-8 py-6 text-center">
                                            <span
                                                className={`px-4 py-1.5 rounded-full text-[10px] font-black uppercase tracking-wider ${
                                                    sup.isActive
                                                        ? "bg-emerald-50 text-emerald-600"
                                                        : "bg-red-50 text-red-600"
                                                }`}
                                            >
                                                {sup.isActive
                                                    ? "Đang hợp tác"
                                                    : "Tạm dừng"}
                                            </span>
                                        </td>
                                        <td className="px-8 py-6 text-right">
                                            {canManageSuppliers ? (
                                                <div className="inline-flex items-center gap-2">
                                                    <button
                                                        onClick={() => openEditSupplierModal(sup)}
                                                        className="inline-flex items-center gap-1 px-3 py-2 rounded-lg bg-blue-50 text-blue-700 hover:bg-blue-100 text-xs font-black uppercase"
                                                    >
                                                        <Pencil size={14} /> Sửa
                                                    </button>
                                                    <button
                                                        onClick={() => handleDeleteSupplier(sup)}
                                                        className="inline-flex items-center gap-1 px-3 py-2 rounded-lg bg-red-50 text-red-700 hover:bg-red-100 text-xs font-black uppercase"
                                                    >
                                                        <Trash2 size={14} /> Xóa
                                                    </button>
                                                </div>
                                            ) : (
                                                <span className="text-xs text-slate-300 font-bold">---</span>
                                            )}
                                        </td>
                                    </tr>
                                ))}
                        </tbody>
                    </table>
                </div>

                <div className="p-8 border-t border-slate-100 flex items-center justify-between bg-slate-50/30">
                    <p className="text-[11px] font-bold text-slate-400 uppercase tracking-widest">
                        Hiển thị{" "}
                        <span className="text-slate-900">
                            {filteredSuppliers.length}
                        </span>{" "}
                        / {suppliers.length} đối tác
                    </p>
                    <div className="flex gap-2">
                        <button
                            className="p-2 rounded-lg border border-slate-200 bg-white opacity-30"
                            disabled
                        >
                            <ChevronLeft size={18} />
                        </button>
                        <button className="p-2 rounded-lg border border-slate-200 bg-white hover:bg-slate-50">
                            <ChevronRight size={18} />
                        </button>
                    </div>
                </div>
            </div>

            <Modal
                isOpen={isFilterModalOpen}
                onClose={() => setIsFilterModalOpen(false)}
                title="Lọc nhà cung cấp"
            >
                <SupplierFilterForm
                    initialFilters={activeFilters}
                    onApply={(f) => {
                        setActiveFilters(f);
                        setIsFilterModalOpen(false);
                    }}
                    onReset={() =>
                        setActiveFilters({
                            categories: [],
                            status: "Tất cả",
                            region: "Tất cả",
                        })
                    }
                    onClose={() => setIsFilterModalOpen(false)}
                />
            </Modal>

            <Modal
                isOpen={isCreateModalOpen}
                onClose={() => setIsCreateModalOpen(false)}
                title="Thêm nhà cung cấp"
            >
                <form onSubmit={handleCreateSupplier} className="space-y-4">
                    <div className="space-y-2">
                        <label className="text-xs font-black uppercase tracking-widest text-slate-500">
                            Tên nhà cung cấp
                        </label>
                        <input
                            value={createForm.name}
                            onChange={(e) =>
                                handleCreateInput("name", e.target.value)
                            }
                            placeholder="Nhập tên nhà cung cấp"
                            className="w-full h-11 px-3 border border-slate-200 rounded-lg font-semibold outline-none focus:border-blue-500"
                        />
                    </div>

                    <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                        <div className="space-y-2">
                            <label className="text-xs font-black uppercase tracking-widest text-slate-500">
                                Người liên hệ
                            </label>
                            <input
                                value={createForm.contactPerson}
                                onChange={(e) =>
                                    handleCreateInput(
                                        "contactPerson",
                                        e.target.value,
                                    )
                                }
                                placeholder="Tên người liên hệ"
                                className="w-full h-11 px-3 border border-slate-200 rounded-lg font-semibold outline-none focus:border-blue-500"
                            />
                        </div>
                        <div className="space-y-2">
                            <label className="text-xs font-black uppercase tracking-widest text-slate-500">
                                Số điện thoại
                            </label>
                            <input
                                value={createForm.phone}
                                onChange={(e) =>
                                    handleCreateInput("phone", e.target.value)
                                }
                                placeholder="0900000000"
                                className="w-full h-11 px-3 border border-slate-200 rounded-lg font-semibold outline-none focus:border-blue-500"
                            />
                        </div>
                    </div>

                    <div className="space-y-2">
                        <label className="text-xs font-black uppercase tracking-widest text-slate-500">
                            Email
                        </label>
                        <input
                            type="email"
                            value={createForm.email}
                            onChange={(e) =>
                                handleCreateInput("email", e.target.value)
                            }
                            placeholder="example@domain.com"
                            className="w-full h-11 px-3 border border-slate-200 rounded-lg font-semibold outline-none focus:border-blue-500"
                        />
                    </div>

                    <div className="space-y-2">
                        <label className="text-xs font-black uppercase tracking-widest text-slate-500">
                            Địa chỉ
                        </label>
                        <input
                            value={createForm.address}
                            onChange={(e) =>
                                handleCreateInput("address", e.target.value)
                            }
                            placeholder="Ví dụ: TP HCM"
                            className="w-full h-11 px-3 border border-slate-200 rounded-lg font-semibold outline-none focus:border-blue-500"
                        />
                    </div>

                    <label className="inline-flex items-center gap-3 text-sm font-semibold text-slate-700">
                        <input
                            type="checkbox"
                            checked={createForm.isActive}
                            onChange={(e) =>
                                handleCreateInput("isActive", e.target.checked)
                            }
                            className="w-4 h-4 accent-[#003d9b]"
                        />
                        Đánh dấu là đang hợp tác
                    </label>

                    <div className="flex justify-end gap-3 pt-4 border-t border-slate-100">
                        <button
                            type="button"
                            onClick={() => setIsCreateModalOpen(false)}
                            className="px-4 py-2 rounded-lg text-sm font-black uppercase text-slate-500 hover:bg-slate-100"
                        >
                            Hủy
                        </button>
                        <button
                            type="submit"
                            disabled={createSupplierMutation.isPending}
                            className="px-5 py-2 rounded-lg text-sm font-black uppercase bg-[#003d9b] text-white hover:bg-blue-700 disabled:opacity-60"
                        >
                            {createSupplierMutation.isPending
                                ? "Đang lưu..."
                                : "Lưu nhà cung cấp"}
                        </button>
                    </div>
                </form>
            </Modal>

            <Modal
                isOpen={isEditModalOpen}
                onClose={() => {
                    setIsEditModalOpen(false);
                    setEditingSupplier(null);
                    setEditForm({ ...defaultSupplierForm });
                }}
                title="Chỉnh sửa nhà cung cấp"
            >
                <form onSubmit={handleUpdateSupplier} className="space-y-4">
                    <div className="space-y-2">
                        <label className="text-xs font-black uppercase tracking-widest text-slate-500">
                            Tên nhà cung cấp
                        </label>
                        <input
                            value={editForm.name}
                            onChange={(e) =>
                                handleEditInput("name", e.target.value)
                            }
                            placeholder="Nhập tên nhà cung cấp"
                            className="w-full h-11 px-3 border border-slate-200 rounded-lg font-semibold outline-none focus:border-blue-500"
                        />
                    </div>

                    <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                        <div className="space-y-2">
                            <label className="text-xs font-black uppercase tracking-widest text-slate-500">
                                Người liên hệ
                            </label>
                            <input
                                value={editForm.contactPerson}
                                onChange={(e) =>
                                    handleEditInput(
                                        "contactPerson",
                                        e.target.value,
                                    )
                                }
                                placeholder="Tên người liên hệ"
                                className="w-full h-11 px-3 border border-slate-200 rounded-lg font-semibold outline-none focus:border-blue-500"
                            />
                        </div>
                        <div className="space-y-2">
                            <label className="text-xs font-black uppercase tracking-widest text-slate-500">
                                Số điện thoại
                            </label>
                            <input
                                value={editForm.phone}
                                onChange={(e) =>
                                    handleEditInput("phone", e.target.value)
                                }
                                placeholder="0900000000"
                                className="w-full h-11 px-3 border border-slate-200 rounded-lg font-semibold outline-none focus:border-blue-500"
                            />
                        </div>
                    </div>

                    <div className="space-y-2">
                        <label className="text-xs font-black uppercase tracking-widest text-slate-500">
                            Email
                        </label>
                        <input
                            type="email"
                            value={editForm.email}
                            onChange={(e) =>
                                handleEditInput("email", e.target.value)
                            }
                            placeholder="example@domain.com"
                            className="w-full h-11 px-3 border border-slate-200 rounded-lg font-semibold outline-none focus:border-blue-500"
                        />
                    </div>

                    <div className="space-y-2">
                        <label className="text-xs font-black uppercase tracking-widest text-slate-500">
                            Địa chỉ
                        </label>
                        <input
                            value={editForm.address}
                            onChange={(e) =>
                                handleEditInput("address", e.target.value)
                            }
                            placeholder="Ví dụ: TP HCM"
                            className="w-full h-11 px-3 border border-slate-200 rounded-lg font-semibold outline-none focus:border-blue-500"
                        />
                    </div>

                    <label className="inline-flex items-center gap-3 text-sm font-semibold text-slate-700">
                        <input
                            type="checkbox"
                            checked={editForm.isActive}
                            onChange={(e) =>
                                handleEditInput("isActive", e.target.checked)
                            }
                            className="w-4 h-4 accent-[#003d9b]"
                        />
                        Đánh dấu là đang hợp tác
                    </label>

                    <div className="flex justify-end gap-3 pt-4 border-t border-slate-100">
                        <button
                            type="button"
                            onClick={() => {
                                setIsEditModalOpen(false);
                                setEditingSupplier(null);
                                setEditForm({ ...defaultSupplierForm });
                            }}
                            className="px-4 py-2 rounded-lg text-sm font-black uppercase text-slate-500 hover:bg-slate-100"
                        >
                            Hủy
                        </button>
                        <button
                            type="submit"
                            disabled={updateSupplierMutation.isPending}
                            className="px-5 py-2 rounded-lg text-sm font-black uppercase bg-[#003d9b] text-white hover:bg-blue-700 disabled:opacity-60"
                        >
                            {updateSupplierMutation.isPending
                                ? "Đang lưu..."
                                : "Lưu thay đổi"}
                        </button>
                    </div>
                </form>
            </Modal>
        </div>
    );
}
