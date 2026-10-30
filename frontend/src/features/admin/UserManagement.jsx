import React, { useMemo, useState } from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { ChevronDown, Loader2, Plus, Search, Shield, Trash2, Users } from "lucide-react";
import { toast } from "sonner";
import { authService } from "../../services/authService";
import Modal from "../../components/ui/Modal";

const defaultForm = {
    username: "",
    password: "",
    role: "ThuKho",
    isActive: true,
};

const roleFilterOptions = ["Tất cả", "Admin", "ThuKho", "KeToan"];

const roleLabelMap = {
    Admin: "Quản trị viên",
    ThuKho: "Thủ kho",
    KeToan: "Kế toán",
};

const roleBadgeClassMap = {
    Admin: "bg-violet-100 text-violet-700",
    ThuKho: "bg-blue-100 text-blue-700",
    KeToan: "bg-orange-100 text-orange-700",
};

const normalizeText = (value) =>
    String(value || "")
        .normalize("NFD")
        .replace(/[\u0300-\u036f]/g, "")
        .trim()
        .toLowerCase();

export default function UserManagement() {
    const queryClient = useQueryClient();
    const [form, setForm] = useState(defaultForm);
    const [searchUserId, setSearchUserId] = useState("");
    const [searchRole, setSearchRole] = useState("Tất cả");
    const [deleteCandidate, setDeleteCandidate] = useState(null);
    const [roleChangeTarget, setRoleChangeTarget] = useState(null); // { user, newRole }

    const {
        data: users = [],
        isLoading,
        isError,
    } = useQuery({
        queryKey: ["admin-users"],
        queryFn: async () => {
            const response = await authService.getUsers();
            return Array.isArray(response?.data) ? response.data : [];
        },
    });

    const createUserMutation = useMutation({
        mutationFn: (payload) => authService.createUser(payload),
        onSuccess: () => {
            toast.success("Tạo tài khoản thành công");
            setForm(defaultForm);
            queryClient.invalidateQueries({ queryKey: ["admin-users"] });
        },
        onError: (error) => {
            const message =
                error?.response?.data?.message ||
                error?.response?.data?.title ||
                error?.response?.data ||
                "Không thể tạo tài khoản";
            toast.error(String(message));
        },
    });

    const updateStatusMutation = useMutation({
        mutationFn: ({ id, isActive }) =>
            authService.updateUserStatus(id, { isActive }),
        onSuccess: () => {
            toast.success("Cập nhật trạng thái thành công");
            queryClient.invalidateQueries({ queryKey: ["admin-users"] });
        },
        onError: (error) => {
            const message =
                error?.response?.data?.message ||
                error?.response?.data?.title ||
                error?.response?.data ||
                "Không thể cập nhật trạng thái";
            toast.error(String(message));
        },
    });

    const updateRoleMutation = useMutation({
        mutationFn: ({ id, role }) => authService.updateUserRole(id, { role }),
        onSuccess: () => {
            toast.success("Đổi vai trò thành công");
            setRoleChangeTarget(null);
            queryClient.invalidateQueries({ queryKey: ["admin-users"] });
        },
        onError: (error) => {
            const message =
                error?.response?.data?.message ||
                error?.response?.data?.title ||
                error?.response?.data ||
                "Không thể đổi vai trò";
            toast.error(String(message));
        },
    });

    const deleteUserMutation = useMutation({
        mutationFn: (id) => authService.deleteUser(id),
        onSuccess: () => {
            toast.success("Xóa tài khoản thành công");
            setDeleteCandidate(null);
            queryClient.invalidateQueries({ queryKey: ["admin-users"] });
        },
        onError: (error) => {
            const message =
                error?.response?.data?.message ||
                error?.response?.data?.title ||
                error?.response?.data ||
                "Không thể xóa tài khoản";
            toast.error(String(message));
        },
    });

    const filteredUsers = useMemo(() => {
        const keywordId = searchUserId.trim().toLowerCase();

        return users
            .filter((user) => {
                const matchesId =
                    keywordId === "" ||
                    String(user?.id || "").toLowerCase().includes(keywordId);

                const matchesRole =
                    searchRole === "Tất cả" ||
                    normalizeText(user?.role) === normalizeText(searchRole);

                return matchesId && matchesRole;
            })
            .sort((a, b) => {
            const left = String(a?.username || "").toLowerCase();
            const right = String(b?.username || "").toLowerCase();
            return left.localeCompare(right);
        });
    }, [users, searchUserId, searchRole]);

    const handleCreateUser = (event) => {
        event.preventDefault();

        const username = form.username.trim();
        const password = form.password.trim();

        if (!username) {
            toast.warning("Vui lòng nhập tên đăng nhập");
            return;
        }

        if (password.length < 6) {
            toast.warning("Mật khẩu cần ít nhất 6 ký tự");
            return;
        }

        createUserMutation.mutate({
            username,
            password,
            role: form.role,
            isActive: form.isActive,
        });
    };

    const handleToggleStatus = (user) => {
        updateStatusMutation.mutate({
            id: user.id,
            isActive: !Boolean(user.isActive),
        });
    };

    const handleDeleteUser = (user) => {
        if (String(user.role) === "Admin") {
            toast.warning("Không thể xóa tài khoản Admin");
            return;
        }
        setDeleteCandidate(user);
    };

    const confirmDeleteUser = () => {
        if (!deleteCandidate) return;
        deleteUserMutation.mutate(deleteCandidate.id);
    };

    const handleRoleChange = (user, newRole) => {
        if (String(user.role) === "Admin" || newRole === user.role) return;
        setRoleChangeTarget({ user, newRole });
    };

    const confirmRoleChange = () => {
        if (!roleChangeTarget) return;
        updateRoleMutation.mutate({ id: roleChangeTarget.user.id, role: roleChangeTarget.newRole });
    };

    return (
        <div className="max-w-7xl mx-auto">
            <div className="mb-10">
                <h2 className="text-4xl font-black tracking-tight text-slate-900 mb-3 uppercase">
                    Quản lý tài khoản
                </h2>
                <p className="text-slate-500 font-medium">
                    Tạo và quản lý tài khoản vai trò Thủ kho, Kế toán trực tiếp trên hệ thống.
                </p>
            </div>

            <form
                onSubmit={handleCreateUser}
                className="bg-white rounded-2xl border border-slate-200 shadow-sm p-6 mb-8"
            >
                <div className="flex items-center gap-2 text-[#003d9b] mb-5">
                    <Users size={18} />
                    <p className="text-xs font-black uppercase tracking-widest">
                        Tạo tài khoản mới
                    </p>
                </div>

                <div className="grid grid-cols-1 md:grid-cols-4 gap-4">
                    <input
                        type="text"
                        placeholder="Tên đăng nhập"
                        value={form.username}
                        onChange={(event) =>
                            setForm((prev) => ({
                                ...prev,
                                username: event.target.value,
                            }))
                        }
                        className="bg-slate-50 border border-slate-200 rounded-xl px-4 py-3 font-bold text-slate-700 outline-none"
                    />
                    <input
                        type="password"
                        placeholder="Mật khẩu"
                        value={form.password}
                        onChange={(event) =>
                            setForm((prev) => ({
                                ...prev,
                                password: event.target.value,
                            }))
                        }
                        className="bg-slate-50 border border-slate-200 rounded-xl px-4 py-3 font-bold text-slate-700 outline-none"
                    />
                    <select
                        value={form.role}
                        onChange={(event) =>
                            setForm((prev) => ({
                                ...prev,
                                role: event.target.value,
                            }))
                        }
                        className="bg-slate-50 border border-slate-200 rounded-xl px-4 py-3 font-bold text-slate-700 outline-none"
                    >
                        <option value="ThuKho">Thủ kho</option>
                        <option value="KeToan">Kế toán</option>
                    </select>
                    <button
                        type="submit"
                        disabled={createUserMutation.isPending}
                        className="inline-flex justify-center items-center gap-2 bg-[#003d9b] text-white px-5 py-3 rounded-xl text-xs font-black uppercase tracking-widest hover:bg-blue-700 transition-colors disabled:opacity-60"
                    >
                        {createUserMutation.isPending ? (
                            <Loader2 size={16} className="animate-spin" />
                        ) : (
                            <Plus size={16} />
                        )}
                        Tạo tài khoản
                    </button>
                </div>
            </form>

            <div className="mb-8 bg-white p-3 rounded-xl shadow-sm border border-slate-200">
                <div className="grid grid-cols-1 lg:grid-cols-[280px_260px] items-center gap-3">
                    <div className="flex items-center gap-3 px-4 border border-slate-100 rounded-lg">
                        <span className="text-xs font-black uppercase tracking-widest text-slate-400">
                            ID
                        </span>
                        <input
                            className="w-full bg-transparent border-none focus:ring-0 text-base py-2 font-bold outline-none"
                            placeholder="Tìm theo ID tài khoản"
                            value={searchUserId}
                            onChange={(event) =>
                                setSearchUserId(event.target.value)
                            }
                        />
                    </div>

                    <div className="flex items-center gap-3 px-4 border border-slate-100 rounded-lg">
                        <Search size={18} className="text-slate-400" />
                        <select
                            className="w-full bg-transparent border-none focus:ring-0 text-base py-2 font-bold outline-none"
                            value={searchRole}
                            onChange={(event) => setSearchRole(event.target.value)}
                        >
                            {roleFilterOptions.map((role) => (
                                <option key={role} value={role}>
                                    {role === "ThuKho"
                                        ? "Vai trò: Thủ kho"
                                        : role === "KeToan"
                                          ? "Vai trò: Kế toán"
                                          : role === "Admin"
                                            ? "Vai trò: Admin"
                                            : "Tất cả vai trò"}
                                </option>
                            ))}
                        </select>
                    </div>
                </div>
            </div>

            <div className="bg-white rounded-2xl shadow-sm border border-slate-200 overflow-hidden">
                <div className="overflow-x-auto">
                    <table className="w-full text-left border-collapse">
                        <thead className="bg-slate-50/50 border-b border-slate-100">
                            <tr>
                                <th className="px-8 py-5 text-[10px] font-black text-slate-400 uppercase tracking-widest">
                                    Tài khoản
                                </th>
                                <th className="px-8 py-5 text-[10px] font-black text-slate-400 uppercase tracking-widest">
                                    Vai trò
                                </th>
                                <th className="px-8 py-5 text-[10px] font-black text-slate-400 uppercase tracking-widest">
                                    Trạng thái
                                </th>
                                <th className="px-8 py-5 text-right text-[10px] font-black text-slate-400 uppercase tracking-widest">
                                    Hành động
                                </th>
                            </tr>
                        </thead>
                        <tbody className="divide-y divide-slate-100">
                            {isLoading && (
                                <tr>
                                    <td colSpan="4" className="py-20 text-center">
                                        <Loader2
                                            className="animate-spin mx-auto text-[#003d9b]"
                                            size={30}
                                        />
                                    </td>
                                </tr>
                            )}

                            {isError && (
                                <tr>
                                    <td
                                        colSpan="4"
                                        className="py-14 text-center text-red-500 text-sm font-bold"
                                    >
                                        Không tải được danh sách tài khoản
                                    </td>
                                </tr>
                            )}

                            {!isLoading &&
                                !isError &&
                                filteredUsers.map((user) => {
                                    const isAdmin = String(user.role) === "Admin";
                                    const busy =
                                        updateStatusMutation.isPending ||
                                        deleteUserMutation.isPending ||
                                        updateRoleMutation.isPending;
                                    const roleClass =
                                        roleBadgeClassMap[user.role] ||
                                        "bg-slate-100 text-slate-700";
                                    const roleLabel =
                                        roleLabelMap[user.role] ||
                                        String(user.role || "Không xác định");

                                    return (
                                        <tr key={user.id} className="hover:bg-slate-50/50">
                                            <td className="px-8 py-5">
                                                <p className="font-black text-slate-900">
                                                    {user.username}
                                                </p>
                                                <p className="text-xs text-slate-400 font-bold mt-1">
                                                    ID: {user.id}
                                                </p>
                                            </td>
                                            <td className="px-8 py-5">
                                                {isAdmin ? (
                                                    <span
                                                        className={`inline-flex items-center px-3 py-1 rounded-full text-xs font-black uppercase ${roleClass}`}
                                                    >
                                                        {roleLabel}
                                                    </span>
                                                ) : (
                                                    /* Dropdown đổi vai trò cho non-Admin */
                                                    <div className="relative inline-block">
                                                        <select
                                                            value={user.role}
                                                            disabled={busy}
                                                            onChange={(e) => handleRoleChange(user, e.target.value)}
                                                            className={`appearance-none pl-3 pr-7 py-1.5 rounded-full text-xs font-black uppercase border-2 cursor-pointer outline-none transition-colors disabled:opacity-60 ${roleClass} border-current`}
                                                        >
                                                            <option value="ThuKho">Thủ kho</option>
                                                            <option value="KeToan">Kế toán</option>
                                                        </select>
                                                        <ChevronDown size={12} className="absolute right-2 top-1/2 -translate-y-1/2 pointer-events-none opacity-60" />
                                                    </div>
                                                )}
                                            </td>
                                            <td className="px-8 py-5">
                                                <button
                                                    type="button"
                                                    disabled={busy}
                                                    onClick={() => handleToggleStatus(user)}
                                                    className={`px-3 py-2 rounded-lg text-xs font-black uppercase tracking-wide transition-colors disabled:opacity-60 ${
                                                        user.isActive
                                                            ? "bg-emerald-100 text-emerald-700"
                                                            : "bg-amber-100 text-amber-700"
                                                    }`}
                                                >
                                                    {user.isActive
                                                        ? "Đang hoạt động"
                                                        : "Đã khóa"}
                                                </button>
                                            </td>
                                            <td className="px-8 py-5 text-right">
                                                <button
                                                    type="button"
                                                    onClick={() => handleDeleteUser(user)}
                                                    disabled={busy || isAdmin}
                                                    className="inline-flex items-center gap-1 px-3 py-2 rounded-lg text-xs font-black uppercase tracking-wide bg-rose-50 text-rose-700 hover:bg-rose-100 transition-colors disabled:opacity-50"
                                                >
                                                    <Trash2 size={14} /> Xóa
                                                </button>
                                            </td>
                                        </tr>
                                    );
                                })}

                            {!isLoading && !isError && filteredUsers.length === 0 && (
                                <tr>
                                    <td
                                        colSpan="4"
                                        className="py-14 text-center text-slate-400 text-sm font-bold"
                                    >
                                        Không có tài khoản phù hợp bộ lọc
                                    </td>
                                </tr>
                            )}
                        </tbody>
                    </table>
                </div>
            </div>

            {/* Modal xác nhận xóa tài khoản */}
            <Modal
                isOpen={!!deleteCandidate}
                onClose={() => setDeleteCandidate(null)}
                title="Xác nhận xóa tài khoản"
            >
                <div className="space-y-6">
                    <div className="rounded-xl border border-red-100 bg-red-50 px-4 py-3 text-sm font-semibold text-red-700">
                        Hành động này sẽ xóa vĩnh viễn tài khoản khỏi hệ thống.
                    </div>
                    <div>
                        <p className="text-sm text-slate-600">Bạn có chắc chắn muốn xóa tài khoản?</p>
                        <p className="mt-2 text-lg font-black text-slate-900 uppercase">{deleteCandidate?.username || '-'}</p>
                        <p className="text-xs text-slate-400 font-bold mt-1">ID: {deleteCandidate?.id}</p>
                    </div>
                    <div className="flex justify-end gap-3 pt-2 border-t border-slate-100">
                        <button
                            type="button"
                            onClick={() => setDeleteCandidate(null)}
                            className="px-4 py-2 rounded-lg text-sm font-black uppercase text-slate-500 hover:bg-slate-100"
                        >
                            Hủy
                        </button>
                        <button
                            type="button"
                            onClick={confirmDeleteUser}
                            disabled={deleteUserMutation.isPending}
                            className="px-5 py-2 rounded-lg text-sm font-black uppercase bg-red-600 text-white hover:bg-red-700 disabled:opacity-60"
                        >
                            {deleteUserMutation.isPending ? 'Đang xóa...' : 'Xác nhận xóa'}
                        </button>
                    </div>
                </div>
            </Modal>

            {/* Modal xác nhận đổi vai trò */}
            <Modal
                isOpen={!!roleChangeTarget}
                onClose={() => setRoleChangeTarget(null)}
                title="Xác nhận đổi vai trò"
            >
                <div className="space-y-6">
                    <div className="rounded-xl border border-blue-100 bg-blue-50 px-4 py-3 text-sm font-semibold text-blue-700">
                        Bạn đang thay đổi vai trò của tài khoản này.
                    </div>
                    <div>
                        <p className="text-sm text-slate-600">Tài khoản:</p>
                        <p className="mt-1 text-lg font-black text-slate-900 uppercase">{roleChangeTarget?.user?.username}</p>
                        <div className="mt-3 flex items-center gap-3 text-sm font-bold">
                            <span className={`px-3 py-1 rounded-full text-xs font-black uppercase ${roleBadgeClassMap[roleChangeTarget?.user?.role] || 'bg-slate-100 text-slate-700'}`}>
                                {roleLabelMap[roleChangeTarget?.user?.role] || roleChangeTarget?.user?.role}
                            </span>
                            <span className="text-slate-400">→</span>
                            <span className={`px-3 py-1 rounded-full text-xs font-black uppercase ${roleBadgeClassMap[roleChangeTarget?.newRole] || 'bg-slate-100 text-slate-700'}`}>
                                {roleLabelMap[roleChangeTarget?.newRole] || roleChangeTarget?.newRole}
                            </span>
                        </div>
                    </div>
                    <div className="flex justify-end gap-3 pt-2 border-t border-slate-100">
                        <button
                            type="button"
                            onClick={() => setRoleChangeTarget(null)}
                            className="px-4 py-2 rounded-lg text-sm font-black uppercase text-slate-500 hover:bg-slate-100"
                        >
                            Hủy
                        </button>
                        <button
                            type="button"
                            onClick={confirmRoleChange}
                            disabled={updateRoleMutation.isPending}
                            className="px-5 py-2 rounded-lg text-sm font-black uppercase bg-[#003d9b] text-white hover:bg-blue-700 disabled:opacity-60"
                        >
                            {updateRoleMutation.isPending ? 'Đang lưu...' : 'Xác nhận đổi vai trò'}
                        </button>
                    </div>
                </div>
            </Modal>
        </div>
    );
}
