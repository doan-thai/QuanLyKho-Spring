import React, { useMemo, useState } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import {
    AlertCircle,
    Filter,
    Loader2,
    Pencil,
    Plus,
    Search,
    Trash2,
    TrendingUp
} from 'lucide-react';
import { toast } from 'sonner';
import Modal from '../../components/ui/Modal';
import ProductFilterForm from './ProductFilterForm';
import { productService } from '../../services/productService';
import useAuthStore from '../../lib/authStore';

const defaultFilters = {
    category: 'Tất cả',
    categoryQuery: '',
    status: 'Tất cả',
    minStock: ''
};

const normalizeText = (value) =>
    String(value || '')
        .normalize('NFD')
        .replace(/[\u0300-\u036f]/g, '')
        .trim()
        .toLowerCase();

const defaultForm = {
    name: '',
    category: '',
    unit: '',
    stock: 0,
    minStock: 10,
    imageUrl: ''
};

export default function ProductList() {
    const queryClient = useQueryClient();
    const role = useAuthStore((state) => state.role);
    const canManageProducts = role === 'Admin' || role === 'ThuKho';
    const [isFilterModalOpen, setIsFilterModalOpen] = useState(false);
    const [isProductModalOpen, setIsProductModalOpen] = useState(false);
    const [searchName, setSearchName] = useState('');
    const [searchProductId, setSearchProductId] = useState('');
    const [editingProduct, setEditingProduct] = useState(null);
    const [productForm, setProductForm] = useState(defaultForm);
    const [activeFilters, setActiveFilters] = useState(() => ({ ...defaultFilters }));
    const [deleteCandidate, setDeleteCandidate] = useState(null);
    const [pendingUpdate, setPendingUpdate] = useState(null);

    const { data: products = [], isLoading, isError, refetch } = useQuery({
        queryKey: ['products'],
        queryFn: async () => {
            const response = await productService.getAll({ pageNumber: 1, pageSize: 100 });
            return response?.data?.data || [];
        },
        onError: () => toast.error('Không thể kết nối máy chủ!')
    });

    const createMutation = useMutation({
        mutationFn: (payload) => productService.create(payload),
        onSuccess: () => {
            toast.success('Thêm sản phẩm thành công');
            closeProductModal();
            queryClient.invalidateQueries({ queryKey: ['products'] });
        },
        onError: () => toast.error('Không thể thêm sản phẩm')
    });

    const updateMutation = useMutation({
        mutationFn: ({ id, payload }) => productService.update(id, payload),
        onSuccess: () => {
            toast.success('Cập nhật sản phẩm thành công');
            closeProductModal();
            queryClient.invalidateQueries({ queryKey: ['products'] });
        },
        onError: () => toast.error('Không thể cập nhật sản phẩm')
    });

    const deleteMutation = useMutation({
        mutationFn: (id) => productService.delete(id),
        onSuccess: () => {
            toast.success('Xóa thành công');
            queryClient.invalidateQueries({ queryKey: ['products'] });
        },
        onError: () => toast.error('Lỗi khi xóa sản phẩm')
    });

    const filteredProducts = useMemo(() => {
        if (!Array.isArray(products)) {
            return [];
        }

        return products.filter((product) => {
            const lowerName = searchName.trim().toLowerCase();
            const lowerProductId = searchProductId.trim().toLowerCase();
            const normalizedProductCategory = normalizeText(product.category);
            const normalizedSelectedCategory = normalizeText(activeFilters.category);
            const normalizedCategoryQuery = normalizeText(activeFilters.categoryQuery);

            const matchesName =
                lowerName === '' ||
                product.name?.toLowerCase().includes(lowerName);

            const matchesProductId =
                lowerProductId === '' ||
                String(product.id || '').toLowerCase().includes(lowerProductId);

            const matchesCategory =
                activeFilters.category === 'Tất cả' ||
                normalizedProductCategory === normalizedSelectedCategory;

            const matchesCategoryQuery =
                normalizedCategoryQuery === '' ||
                normalizedProductCategory.includes(normalizedCategoryQuery);

            let matchesStatus = true;
            if (activeFilters.status === 'Hết hàng') {
                matchesStatus = product.stock === 0;
            } else if (activeFilters.status === 'Sắp hết') {
                matchesStatus = product.stock > 0 && product.stock <= product.minStock;
            } else if (activeFilters.status === 'Còn hàng') {
                matchesStatus = product.stock > product.minStock;
            }

            const matchesMinStock =
                activeFilters.minStock === '' ||
                product.stock >= Number.parseInt(activeFilters.minStock, 10);

            return (
                matchesName &&
                matchesProductId &&
                matchesCategory &&
                matchesCategoryQuery &&
                matchesStatus &&
                matchesMinStock
            );
        });
    }, [products, searchName, searchProductId, activeFilters]);

    const handleDeleteProduct = (id, name) => {
        if (!canManageProducts) {
            toast.warning('Bạn không có quyền xóa sản phẩm');
            return;
        }

        setDeleteCandidate({ id, name });
    };

    const openCreateModal = () => {
        if (!canManageProducts) {
            toast.warning('Bạn không có quyền thêm sản phẩm');
            return;
        }

        setEditingProduct(null);
        setProductForm(defaultForm);
        setIsProductModalOpen(true);
    };

    const openEditModal = (product) => {
        if (!canManageProducts) {
            toast.warning('Bạn không có quyền chỉnh sửa sản phẩm');
            return;
        }

        setEditingProduct(product);
        setProductForm({
            name: product.name || '',
            category: product.category || '',
            unit: product.unit || '',
            stock: Number(product.stock || 0),
            minStock: Number(product.minStock || 0),
            imageUrl: product.imageUrl || ''
        });
        setIsProductModalOpen(true);
    };

    const closeProductModal = () => {
        setIsProductModalOpen(false);
        setEditingProduct(null);
        setProductForm(defaultForm);
        setPendingUpdate(null);
    };

    const handleProductFormChange = (field, value) => {
        setProductForm((prev) => ({ ...prev, [field]: value }));
    };

    const handleSubmitProduct = (event) => {
        event.preventDefault();

        if (!canManageProducts) {
            toast.warning('Bạn không có quyền thao tác sản phẩm');
            return;
        }

        if (!productForm.name.trim() || !productForm.category.trim() || !productForm.unit.trim()) {
            toast.warning('Tên, danh mục và đơn vị tính là bắt buộc');
            return;
        }

        const payload = {
            id: editingProduct?.id || 0,
            name: productForm.name.trim(),
            category: productForm.category.trim(),
            unit: productForm.unit.trim(),
            stock: Math.max(0, Number.parseInt(String(productForm.stock), 10) || 0),
            minStock: Math.max(0, Number.parseInt(String(productForm.minStock), 10) || 0),
            imageUrl: productForm.imageUrl?.trim() || null
        };

        if (editingProduct) {
            setPendingUpdate({
                id: editingProduct.id,
                name: payload.name,
                payload
            });
            setIsProductModalOpen(false);
            return;
        }

        createMutation.mutate(payload);
    };

    const confirmDeleteProduct = () => {
        if (!deleteCandidate) {
            return;
        }

        deleteMutation.mutate(deleteCandidate.id);
        setDeleteCandidate(null);
    };

    const confirmUpdateProduct = () => {
        if (!pendingUpdate) {
            return;
        }

        updateMutation.mutate({
            id: pendingUpdate.id,
            payload: pendingUpdate.payload
        });
        setPendingUpdate(null);
    };

    const cancelUpdateConfirmation = () => {
        setPendingUpdate(null);
        setIsProductModalOpen(true);
    };

    const isSaving = createMutation.isPending || updateMutation.isPending;
    const tableColumnCount = canManageProducts ? 5 : 4;

    return (
        <div className="max-w-7xl mx-auto not-italic">
            <div className="mb-10 flex flex-wrap items-start justify-between gap-4">
                <div>
                    <h2 className="text-4xl font-black tracking-tight text-slate-900 mb-3 uppercase">
                        Danh sách sản phẩm
                    </h2>
                    <p className="text-slate-500 text-base max-w-lg font-medium">
                        Quản lý kho hàng với dữ liệu thời gian thực từ hệ thống.
                    </p>
                </div>
                {canManageProducts && (
                    <button
                        onClick={openCreateModal}
                        className="inline-flex items-center gap-2 bg-[#003d9b] text-white px-5 py-3 rounded-xl text-sm font-black uppercase tracking-widest hover:bg-blue-700 transition-colors"
                    >
                        <Plus size={16} /> Thêm sản phẩm
                    </button>
                )}
            </div>

            <div className="grid grid-cols-1 md:grid-cols-4 gap-6 mb-10">
                <div className="bg-white p-6 rounded-xl shadow-sm border-l-4 border-l-primary">
                    <p className="text-xs font-bold text-slate-400 uppercase mb-2">Tổng sản phẩm</p>
                    <span className="text-4xl font-black text-slate-900">{products.length}</span>
                </div>
            </div>

            <div className="mb-8 bg-white p-3 rounded-xl shadow-sm border border-slate-200">
                <div className="grid grid-cols-1 lg:grid-cols-[1fr_280px_auto] items-center gap-3">
                    <div className="flex items-center gap-3 px-4 border border-slate-100 rounded-lg">
                        <Search size={20} className="text-slate-400" />
                        <input
                            className="w-full bg-transparent border-none focus:ring-0 text-base py-2 font-bold outline-none"
                            placeholder="Tìm theo tên sản phẩm..."
                            value={searchName}
                            onChange={(e) => setSearchName(e.target.value)}
                        />
                    </div>

                    <div className="flex items-center gap-3 px-4 border border-slate-100 rounded-lg">
                        <span className="text-xs font-black uppercase tracking-widest text-slate-400">ID</span>
                        <input
                            className="w-full bg-transparent border-none focus:ring-0 text-base py-2 font-bold outline-none"
                            placeholder="Tìm theo ID sản phẩm"
                            value={searchProductId}
                            onChange={(e) => setSearchProductId(e.target.value)}
                        />
                    </div>

                    <button
                        onClick={() => setIsFilterModalOpen(true)}
                        className={`px-6 py-3 font-black text-[10px] uppercase rounded-lg flex items-center justify-center gap-2 ${
                            (
                                activeFilters.category !== 'Tất cả' ||
                                activeFilters.categoryQuery !== '' ||
                                activeFilters.status !== 'Tất cả' ||
                                activeFilters.minStock !== ''
                            )
                                ? 'bg-[#003d9b] text-white'
                                : 'bg-slate-100 text-slate-500'
                        }`}
                    >
                        <Filter size={16} /> Bộ lọc nâng cao
                    </button>
                </div>
            </div>

            <div className="bg-white rounded-xl shadow-sm border border-slate-200 overflow-hidden min-h-[400px] flex flex-col">
                <div className="overflow-x-auto flex-1">
                    <table className="w-full text-left border-collapse">
                        <thead className="bg-slate-50/60">
                            <tr>
                                <th className="px-8 py-5 text-[10px] font-black text-slate-500 uppercase tracking-widest">Sản phẩm</th>
                                <th className="px-8 py-5 text-[10px] font-black text-slate-500 uppercase tracking-widest text-center">Tồn kho</th>
                                <th className="px-8 py-5 text-[10px] font-black text-slate-500 uppercase tracking-widest text-right">Mức tối thiểu</th>
                                <th className="px-8 py-5 text-[10px] font-black text-slate-500 uppercase tracking-widest text-center">Trạng thái</th>
                                {canManageProducts && (
                                    <th className="px-8 py-5 text-right text-[10px] font-black text-slate-500 uppercase tracking-widest">Thao tác</th>
                                )}
                            </tr>
                        </thead>
                        <tbody className="divide-y divide-slate-100">
                            {isLoading && (
                                <tr>
                                    <td colSpan={tableColumnCount} className="py-20 text-center">
                                        <Loader2 className="animate-spin mx-auto text-primary mb-2" size={32} />
                                        <p className="text-xs font-black uppercase text-slate-400">Đang đồng bộ dữ liệu...</p>
                                    </td>
                                </tr>
                            )}

                            {isError && (
                                <tr>
                                    <td colSpan={tableColumnCount} className="py-20 text-center text-red-500">
                                        <AlertCircle className="mx-auto mb-2" size={32} />
                                        <p className="font-bold">Mất kết nối API. Vui lòng kiểm tra lại.</p>
                                    </td>
                                </tr>
                            )}

                            {!isLoading && !isError && filteredProducts.length === 0 && (
                                <tr>
                                    <td colSpan={tableColumnCount} className="py-20 text-center text-slate-400 font-bold uppercase tracking-widest text-xs">
                                        Không có sản phẩm phù hợp bộ lọc
                                    </td>
                                </tr>
                            )}

                            {!isLoading && !isError && filteredProducts.map((product) => (
                                <tr key={product.id} className="hover:bg-slate-50 transition-colors">
                                    <td className="px-8 py-6">
                                        <div className="flex items-center gap-4">
                                            <div className="w-12 h-12 rounded-lg bg-slate-100 flex items-center justify-center font-black text-primary uppercase border border-slate-200">
                                                {product.name?.charAt(0)}
                                            </div>
                                            <div>
                                                <p className="font-black text-slate-900 text-base uppercase">{product.name}</p>
                                                <p className="text-[10px] text-[#003d9b] font-black font-mono">ID: {product.id}</p>
                                                <p className="text-[10px] text-slate-500 font-semibold uppercase">{product.category} • {product.unit}</p>
                                            </div>
                                        </div>
                                    </td>
                                    <td className="px-8 py-6 text-center text-lg font-black text-slate-700">{product.stock}</td>
                                    <td className="px-8 py-6 text-right font-mono font-bold text-slate-600">{product.minStock}</td>
                                    <td className="px-8 py-6 text-center">
                                        <span className={`px-4 py-1.5 rounded-full text-[10px] font-black uppercase ${
                                            product.stock > product.minStock
                                                ? 'bg-emerald-50 text-emerald-600'
                                                : product.stock > 0
                                                    ? 'bg-orange-50 text-orange-600'
                                                    : 'bg-red-50 text-red-600'
                                        }`}>
                                            {product.stock > product.minStock ? 'Còn hàng' : product.stock > 0 ? 'Sắp hết' : 'Hết hàng'}
                                        </span>
                                    </td>
                                    {canManageProducts && (
                                        <td className="px-8 py-6 text-right">
                                            <div className="inline-flex items-center gap-2">
                                                <button
                                                    onClick={() => openEditModal(product)}
                                                    className="inline-flex items-center gap-1 px-3 py-2 rounded-lg bg-blue-50 text-blue-700 hover:bg-blue-100 text-xs font-black uppercase"
                                                >
                                                    <Pencil size={14} /> Sửa
                                                </button>
                                                <button
                                                    onClick={() => handleDeleteProduct(product.id, product.name)}
                                                    className="inline-flex items-center gap-1 px-3 py-2 rounded-lg bg-red-50 text-red-700 hover:bg-red-100 text-xs font-black uppercase"
                                                >
                                                    <Trash2 size={14} /> Xóa
                                                </button>
                                            </div>
                                        </td>
                                    )}
                                </tr>
                            ))}
                        </tbody>
                    </table>
                </div>

                <div className="p-8 border-t border-slate-100 bg-slate-50/30 flex items-center justify-between">
                    <p className="text-[11px] font-bold text-slate-400 uppercase tracking-widest">
                        Hiển thị <span className="text-slate-900">{filteredProducts.length}</span> / {products.length} sản phẩm
                    </p>
                    <div className="flex gap-2">
                        <button className="p-2 rounded-lg border border-slate-200 bg-white" onClick={() => refetch()}>
                            <TrendingUp size={18} />
                        </button>
                    </div>
                </div>
            </div>

            <Modal isOpen={isFilterModalOpen} onClose={() => setIsFilterModalOpen(false)} title="Bộ lọc nâng cao">
                <ProductFilterForm
                    initialFilters={activeFilters}
                    onApply={(filters) => {
                        setActiveFilters({ ...defaultFilters, ...filters });
                        setIsFilterModalOpen(false);
                    }}
                    onReset={() => setActiveFilters({ ...defaultFilters })}
                    onClose={() => setIsFilterModalOpen(false)}
                />
            </Modal>

            <Modal
                isOpen={isProductModalOpen}
                onClose={closeProductModal}
                title={editingProduct ? 'Chỉnh sửa sản phẩm' : 'Thêm sản phẩm'}
            >
                <form onSubmit={handleSubmitProduct} className="space-y-4">
                    <div className="space-y-2">
                        <label className="text-xs font-black uppercase tracking-widest text-slate-500">Tên sản phẩm</label>
                        <input
                            value={productForm.name}
                            onChange={(e) => handleProductFormChange('name', e.target.value)}
                            className="w-full h-11 px-3 border border-slate-200 rounded-lg font-semibold outline-none focus:border-blue-500"
                            placeholder="Nhập tên sản phẩm"
                        />
                    </div>

                    <div className="grid grid-cols-2 gap-4">
                        <div className="space-y-2">
                            <label className="text-xs font-black uppercase tracking-widest text-slate-500">Danh mục</label>
                            <input
                                value={productForm.category}
                                onChange={(e) => handleProductFormChange('category', e.target.value)}
                                className="w-full h-11 px-3 border border-slate-200 rounded-lg font-semibold outline-none focus:border-blue-500"
                                placeholder="Ví dụ: Điện tử"
                            />
                        </div>
                        <div className="space-y-2">
                            <label className="text-xs font-black uppercase tracking-widest text-slate-500">Đơn vị tính</label>
                            <input
                                value={productForm.unit}
                                onChange={(e) => handleProductFormChange('unit', e.target.value)}
                                className="w-full h-11 px-3 border border-slate-200 rounded-lg font-semibold outline-none focus:border-blue-500"
                                placeholder="Ví dụ: cái"
                            />
                        </div>
                    </div>

                    <div className="grid grid-cols-2 gap-4">
                        <div className="space-y-2">
                            <label className="text-xs font-black uppercase tracking-widest text-slate-500">Tồn kho</label>
                            <input
                                type="number"
                                min="0"
                                value={productForm.stock}
                                onChange={(e) => handleProductFormChange('stock', e.target.value)}
                                className="w-full h-11 px-3 border border-slate-200 rounded-lg font-semibold outline-none focus:border-blue-500"
                            />
                        </div>
                        <div className="space-y-2">
                            <label className="text-xs font-black uppercase tracking-widest text-slate-500">Mức tối thiểu</label>
                            <input
                                type="number"
                                min="0"
                                value={productForm.minStock}
                                onChange={(e) => handleProductFormChange('minStock', e.target.value)}
                                className="w-full h-11 px-3 border border-slate-200 rounded-lg font-semibold outline-none focus:border-blue-500"
                            />
                        </div>
                    </div>

                    <div className="space-y-2">
                        <label className="text-xs font-black uppercase tracking-widest text-slate-500">Image URL (không bắt buộc)</label>
                        <input
                            value={productForm.imageUrl}
                            onChange={(e) => handleProductFormChange('imageUrl', e.target.value)}
                            className="w-full h-11 px-3 border border-slate-200 rounded-lg font-semibold outline-none focus:border-blue-500"
                            placeholder="/images/abc.jpg"
                        />
                    </div>

                    <div className="flex justify-end gap-3 pt-4 border-t border-slate-100">
                        <button
                            type="button"
                            onClick={closeProductModal}
                            className="px-4 py-2 rounded-lg text-sm font-black uppercase text-slate-500 hover:bg-slate-100"
                        >
                            Hủy
                        </button>
                        <button
                            type="submit"
                            disabled={isSaving}
                            className="px-5 py-2 rounded-lg text-sm font-black uppercase bg-[#003d9b] text-white hover:bg-blue-700 disabled:opacity-60"
                        >
                            {isSaving ? 'Đang lưu...' : editingProduct ? 'Lưu thay đổi' : 'Thêm mới'}
                        </button>
                    </div>
                </form>
            </Modal>

            <Modal
                isOpen={!!deleteCandidate}
                onClose={() => setDeleteCandidate(null)}
                title="Xác nhận xóa sản phẩm"
            >
                <div className="space-y-6">
                    <div className="rounded-xl border border-red-100 bg-red-50 px-4 py-3 text-sm font-semibold text-red-700">
                        Hành động này sẽ xóa sản phẩm khỏi hệ thống.
                    </div>
                    <div>
                        <p className="text-sm text-slate-600">Bạn có chắc chắn muốn xóa sản phẩm này?</p>
                        <p className="mt-2 text-lg font-black text-slate-900 uppercase">{deleteCandidate?.name || '-'}</p>
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
                            onClick={confirmDeleteProduct}
                            className="px-5 py-2 rounded-lg text-sm font-black uppercase bg-red-600 text-white hover:bg-red-700"
                        >
                            Xác nhận xóa
                        </button>
                    </div>
                </div>
            </Modal>

            <Modal
                isOpen={!!pendingUpdate}
                onClose={cancelUpdateConfirmation}
                title="Xác nhận lưu thay đổi"
            >
                <div className="space-y-6">
                    <div className="rounded-xl border border-blue-100 bg-blue-50 px-4 py-3 text-sm font-semibold text-blue-700">
                        Bạn đang cập nhật thông tin sản phẩm.
                    </div>
                    <div>
                        <p className="text-sm text-slate-600">Lưu thay đổi cho sản phẩm sau?</p>
                        <p className="mt-2 text-lg font-black text-slate-900 uppercase">{pendingUpdate?.name || '-'}</p>
                    </div>
                    <div className="flex justify-end gap-3 pt-2 border-t border-slate-100">
                        <button
                            type="button"
                            onClick={cancelUpdateConfirmation}
                            className="px-4 py-2 rounded-lg text-sm font-black uppercase text-slate-500 hover:bg-slate-100"
                        >
                            Quay lại chỉnh sửa
                        </button>
                        <button
                            type="button"
                            onClick={confirmUpdateProduct}
                            className="px-5 py-2 rounded-lg text-sm font-black uppercase bg-[#003d9b] text-white hover:bg-blue-700"
                        >
                            Xác nhận lưu
                        </button>
                    </div>
                </div>
            </Modal>
        </div>
    );
}