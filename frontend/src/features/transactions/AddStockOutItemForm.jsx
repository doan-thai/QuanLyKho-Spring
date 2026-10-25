import React, { useState, useMemo } from 'react';
import { Loader2 } from 'lucide-react';

export default function AddStockOutItemForm({ products = [], isLoading, onAdd, onClose }) {
    const [searchTerm, setSearchTerm] = useState('');
    const [selectedProduct, setSelectedProduct] = useState(null);
    const [quantity, setQuantity] = useState(1);
    const [exportPrice, setExportPrice] = useState(0);

    const filteredProducts = useMemo(() => {
        if (!Array.isArray(products)) return [];
        const keyword = searchTerm.toLowerCase();

        return products.filter((product) =>
            product.name?.toLowerCase().includes(keyword) ||
            String(product.id || '').includes(keyword)
        );
    }, [searchTerm, products]);

    const handleSelectProduct = (product) => {
        if ((product.stock || 0) === 0) return;
        setSelectedProduct(product);
        setQuantity(1);
        setExportPrice(0);
    };

    const handleSubmit = () => {
        if (!selectedProduct) return;
        if (quantity < 1 || quantity > selectedProduct.stock) return;

        onAdd({
            productId: selectedProduct.id,
            sku: `SP-${selectedProduct.id}`,
            name: selectedProduct.name,
            category: selectedProduct.category,
            currentStock: selectedProduct.stock,
            quantity,
            exportPrice: Math.max(0, exportPrice)
        });
    };

    return (
        <div className="space-y-6">
            <div>
                <p className="text-sm font-black uppercase tracking-widest text-[#003d9b] mb-3">Sản phẩm trong kho</p>
                <div className="relative mb-4">
                    <span className="material-symbols-outlined absolute left-4 top-1/2 -translate-y-1/2 text-slate-400">search</span>
                    <input
                        type="text"
                        placeholder="Gõ tên hoặc mã sản phẩm để tìm..."
                        value={searchTerm}
                        onChange={(e) => setSearchTerm(e.target.value)}
                        className="w-full h-14 pl-12 pr-4 bg-slate-50/50 border border-slate-200 rounded-xl text-base font-bold text-slate-700 outline-none focus:ring-2 focus:ring-[#003d9b]/20 focus:border-[#003d9b] transition-all"
                    />
                </div>

                <div className="h-48 overflow-y-auto space-y-2 pr-2 custom-scrollbar">
                    {isLoading ? (
                        <div className="h-full flex items-center justify-center text-slate-400">
                            <Loader2 size={24} className="animate-spin" />
                        </div>
                    ) : filteredProducts.length === 0 ? (
                        <div className="h-full flex items-center justify-center text-slate-400 text-xs font-bold uppercase">Không có sản phẩm phù hợp</div>
                    ) : (
                        filteredProducts.map((product) => (
                            <div
                                key={product.id}
                                onClick={() => handleSelectProduct(product)}
                                className={`p-4 rounded-xl cursor-pointer border min-h-[92px] ${
                                    product.stock === 0
                                        ? 'opacity-50 cursor-not-allowed'
                                        : selectedProduct?.id === product.id
                                            ? 'bg-[#f4f7ff] border-[#003d9b]'
                                            : 'bg-white border-slate-200'
                                }`}
                            >
                                <div className="flex justify-between items-center h-full gap-4">
                                    <div className="min-w-0">
                                        <p className="font-black text-slate-900 text-base mb-0.5 leading-tight truncate">{product.name}</p>
                                        <p className="text-sm font-bold text-slate-500">SP-{product.id}</p>
                                    </div>
                                    <div className="text-right">
                                        <p className="text-sm font-bold text-slate-400 uppercase mb-0.5">Tồn kho</p>
                                        <p className="font-black text-xl text-[#003d9b]">{product.stock}</p>
                                    </div>
                                </div>
                            </div>
                        ))
                    )}
                </div>
            </div>

            {selectedProduct && (
                <div className="pt-6 border-t border-slate-100">
                    <p className="text-sm font-black uppercase tracking-widest text-[#003d9b] mb-3">Thông tin xuất kho</p>
                    <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
                        <div>
                            <p className="text-xs font-bold uppercase tracking-widest text-slate-400 mb-2">Số lượng xuất</p>
                            <input
                                type="number"
                                min="1"
                                max={selectedProduct.stock}
                                value={quantity}
                                onChange={(e) => setQuantity(parseInt(e.target.value, 10) || 1)}
                                className="w-full h-16 bg-white border border-slate-200 rounded-xl text-2xl font-black text-[#003d9b] text-center"
                            />
                        </div>
                        <div>
                            <p className="text-xs font-bold uppercase tracking-widest text-slate-400 mb-2">Đơn giá xuất (đ)</p>
                            <input
                                type="number"
                                min="0"
                                value={exportPrice}
                                onChange={(e) => setExportPrice(parseInt(e.target.value, 10) || 0)}
                                className="w-full h-16 bg-white border border-slate-200 rounded-xl text-2xl font-black text-[#003d9b] text-center"
                            />
                        </div>
                        <div className="bg-slate-50 p-4 rounded-xl border border-slate-100 flex flex-col justify-center">
                            <p className="text-sm font-bold uppercase tracking-widest text-slate-400 mb-1">Thành tiền</p>
                            <p className="text-2xl font-black text-slate-900">
                                {(Math.max(1, quantity) * Math.max(0, exportPrice)).toLocaleString()} đ
                            </p>
                            <p className="text-xs font-semibold text-slate-500 mt-2">
                                Tồn sau xuất: {Math.max(0, selectedProduct.stock - quantity)}
                            </p>
                        </div>
                    </div>
                </div>
            )}

            <div className="flex items-center justify-end gap-6 pt-6 mt-2">
                <button onClick={onClose} className="text-base font-black uppercase tracking-widest text-slate-500 hover:text-slate-900">Hủy</button>
                <button
                    onClick={handleSubmit}
                    disabled={!selectedProduct}
                    className="inline-flex items-center gap-2 bg-gradient-to-r from-[#003d9b] to-[#0f5bd8] text-white px-8 py-4 rounded-xl text-sm font-black uppercase tracking-widest hover:brightness-110 transition-all shadow-lg shadow-blue-900/30 disabled:opacity-50 disabled:cursor-not-allowed"
                >
                    <span className="material-symbols-outlined text-base">library_add</span>
                    Đưa vào phiếu xuất
                </button>
            </div>
        </div>
    );
}
