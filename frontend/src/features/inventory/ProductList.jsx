import React, { useMemo, useRef, useState } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import {
    AlertCircle,
    ChevronLeft,
    ChevronRight,
    Filter,
    ImagePlus,
    Loader2,
    Pencil,
    Plus,
    Search,
    Trash2,
    TrendingUp,
    UploadCloud,
    X
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

const PAGE_SIZE = 10;

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
    const [currentPage, setCurrentPage] = useState(1);
    const [isUploading, setIsUploading] = useState(false);
    const [imagePreview, setImagePreview] = useState(null);
    const fileInputRef = useRef(null);

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
        onError: (error) => toast.error(error?.response?.data?.message || error?.response?.data || 'Không thể thêm sản phẩm')
    });

    const updateMutation = useMutation({
        mutationFn: ({ id, payload }) => productService.update(id, payload),
        onSuccess: () => {
            toast.success('Cập nhật sản phẩm thành công');
            closeProductModal();
            queryClient.invalidateQueries({ queryKey: ['products'] });
        },
        onError: (error) => toast.error(error?.response?.data?.message || error?.response?.data || 'Không thể cập nhật sản phẩm')
    });

    const deleteMutation = useMutation({
        mutationFn: (id) => productService.delete(id),
        onSuccess: () => {
            toast.success('Xóa thành công');
            queryClient.invalidateQueries({ queryKey: ['products'] });
        },
        onError: (error) => toast.error(error?.response?.data?.message || error?.response?.data || 'Lỗi khi xóa sản phẩm')
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
        setImagePreview(product.imageUrl || null);
        setIsProductModalOpen(true);
    };

    const closeProductModal = () => {
        setIsProductModalOpen(false);
        setEditingProduct(null);
        setProductForm(defaultForm);
        setPendingUpdate(null);
        setImagePreview(null);
    };

    const handleImageUpload = async (e) => {
        const file = e.target.files?.[0];
        if (!file) return;

        // Validate file type
        const allowed = ['image/jpeg', 'image/png', 'image/webp'];
        if (!allowed.includes(file.type)) {
            toast.error('Chỉ hỗ trợ file JPG, PNG, WEBP');
            return;
        }

        // Show local preview immediately
        const localUrl = URL.createObjectURL(file);
        setImagePreview(localUrl);

        try {
            setIsUploading(true);
            const response = await productService.uploadImage(file);
            // Backend trả về URL hoặc path
            const uploadedUrl = response?.data?.url || response?.data?.imageUrl || response?.data;
            if (uploadedUrl) {
                handleProductFormChange('imageUrl', String(uploadedUrl));
                setImagePreview(String(uploadedUrl));
                toast.success('Upload ảnh thành công');
            } else {
                toast.warning('Upload thành công nhưng không nhận được URL ảnh');
            }
        } catch {
            toast.error('Không thể upload ảnh');
            setImagePreview(productForm.imageUrl || null);
        } finally {
            setIsUploading(false);
            if (fileInputRef.current) fileInputRef.current.value = '';
        }
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

    // Phân trang
    const totalPages = Math.max(1, Math.ceil(filteredProducts.length / PAGE_SIZE));
    const safePage = Math.min(currentPage, totalPages);
    const pagedProducts = filteredProducts.slice((safePage - 1) * PAGE_SIZE, safePage * PAGE_SIZE);

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

                            {!isLoading && !isError && pagedProducts.map((product) => (
                                <tr key={product.id} className="hover:bg-slate-50 transition-colors">
                                    <td className="px-8 py-6">
                                        <div className="flex items-center gap-4">
                                            {/* Ảnh sản phẩm hoặc fallback chữ cái đầu */}
                                            {product.imageUrl ? (
                                                <img
                                                    src={product.imageUrl}
                                                    alt={product.name}
                                                    onError={(e) => {
                                                        e.currentTarget.style.display = 'none';
                                                        e.currentTarget.nextSibling.style.display = 'flex';
                                                    }}
                                                    className="w-12 h-12 rounded-lg object-cover border border-slate-200"
                                                />
                                            ) : null}
                                            <div
                                                className="w-12 h-12 rounded-lg bg-slate-100 items-center justify-center font-black text-primary uppercase border border-slate-200"
                                                style={{ display: product.imageUrl ? 'none' : 'flex' }}
                                            >
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

                <div className="p-6 border-t border-slate-100 bg-slate-50/30 flex flex-wrap items-center justify-between gap-4">
                    <p className="text-[11px] font-bold text-slate-400 uppercase tracking-widest">
                        Hiển thị{' '}
                        <span className="text-slate-900">{filteredProducts.length}</span>{' '}
                        / {products.length} sản phẩm
                        {totalPages > 1 && (
                            <span> &mdash; Trang <span className="text-slate-900">{safePage}</span> / {totalPages}</span>
                        )}
                    </p>
                    <div className="flex items-center gap-2">
                        <button className="p-2 rounded-lg border border-slate-200 bg-white hover:bg-slate-50" onClick={() => refetch()} title="Làm mới">
                            <TrendingUp size={18} />
                        </button>
                        {totalPages > 1 && (
                            <>
                                <button
                                    onClick={() => setCurrentPage((p) => Math.max(1, p - 1))}
                                    disabled={safePage === 1}
                                    className="flex items-center gap-1 px-3 py-2 rounded-lg border border-slate-200 bg-white text-xs font-black uppercase disabled:opacity-40 hover:bg-slate-50"
                                >
                                    <ChevronLeft size={15} /> Trước
                                </button>
                                {Array.from({ length: totalPages }, (_, i) => i + 1).map((p) => (
                                    <button
                                        key={p}
                                        onClick={() => setCurrentPage(p)}
                                        className={`w-8 h-8 rounded-lg text-xs font-black border ${
                                            p === safePage
                                                ? 'bg-[#003d9b] text-white border-[#003d9b]'
                                                : 'bg-white text-slate-500 border-slate-200 hover:bg-slate-50'
                                        }`}
                                    >
                                        {p}
                                    </button>
                                ))}
                                <button
                                    onClick={() => setCurrentPage((p) => Math.min(totalPages, p + 1))}
                                    disabled={safePage === totalPages}
                                    className="flex items-center gap-1 px-3 py-2 rounded-lg border border-slate-200 bg-white text-xs font-black uppercase disabled:opacity-40 hover:bg-slate-50"
                                >
                                    Tiếp <ChevronRight size={15} />
                                </button>
                            </>
                        )}
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

                    {/* Upload ảnh */}
                    <div className="space-y-2">
                        <label className="text-xs font-black uppercase tracking-widest text-slate-500">Ảnh sản phẩm (không bắt buộc)</label>
                        {/* Preview ảnh */}
                        {imagePreview && (
                            <div className="relative inline-block">
                                <img
                                    src={imagePreview}
                                    alt="preview"
                                    className="h-24 w-24 object-cover rounded-xl border border-slate-200"
                                    onError={(e) => { e.currentTarget.style.display = 'none'; }}
                                />
                                <button
                                    type="button"
                                    onClick={() => { setImagePreview(null); handleProductFormChange('imageUrl', ''); }}
                                    className="absolute -top-2 -right-2 w-5 h-5 bg-red-500 text-white rounded-full flex items-center justify-center"
                                >
                                    <X size={10} />
                                </button>
                            </div>
                        )}
                        {/* Input URL + nút upload */}
                        <div className="flex gap-2">
                            <input
                                value={productForm.imageUrl}
                                onChange={(e) => {
                                    handleProductFormChange('imageUrl', e.target.value);
                                    setImagePreview(e.target.value || null);
                                }}
                                className="flex-1 h-11 px-3 border border-slate-200 rounded-lg font-semibold outline-none focus:border-blue-500 text-sm"
                                placeholder="URL ảnh hoặc tải lên bên dưới"
                            />
                            <button
                                type="button"
                                onClick={() => fileInputRef.current?.click()}
                                disabled={isUploading}
                                className="flex items-center gap-1.5 px-3 h-11 rounded-lg border border-slate-200 bg-slate-50 text-slate-600 hover:bg-blue-50 hover:text-blue-700 hover:border-blue-300 text-xs font-black uppercase transition-colors disabled:opacity-60"
                            >
                                {isUploading ? <Loader2 size={14} className="animate-spin" /> : <UploadCloud size={14} />}
                                {isUploading ? 'Uploading...' : 'Tải ảnh'}
                            </button>
                        </div>
                        <input
                            ref={fileInputRef}
                            type="file"
                            accept=".jpg,.jpeg,.png,.webp"
                            onChange={handleImageUpload}
                            className="hidden"
                        />
                        <p className="text-[10px] text-slate-400 font-medium">Hỗ trợ: JPG, PNG, WEBP</p>
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