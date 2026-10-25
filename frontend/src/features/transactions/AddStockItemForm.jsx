import React, { useMemo, useState } from 'react';
import { Loader2 } from 'lucide-react';

export default function AddStockItemForm({ products = [], isLoading, onAdd, onClose }) {
  const [searchTerm, setSearchTerm] = useState('');
  const [selectedProduct, setSelectedProduct] = useState(null);
  const [quantity, setQuantity] = useState(1);
  const [importPrice, setImportPrice] = useState(0);

  const filteredProducts = useMemo(() => {
    if (!Array.isArray(products)) return [];

    const keyword = searchTerm.toLowerCase();
    return products.filter((product) =>
      product.name?.toLowerCase().includes(keyword) ||
      String(product.id || '').includes(keyword)
    );
  }, [products, searchTerm]);

  const handleSubmit = (e) => {
    e.preventDefault();

    if (!selectedProduct) return;

    onAdd({
      productId: selectedProduct.id,
      sku: `SP-${selectedProduct.id}`,
      name: selectedProduct.name,
      category: selectedProduct.category,
      quantity: Math.max(1, quantity),
      importPrice: Math.max(0, importPrice)
    });
  };

  return (
    <form onSubmit={handleSubmit} className="space-y-6 not-italic">
      <div className="space-y-2">
        <label className="text-sm font-black uppercase tracking-[0.2em] text-[#003d9b]">Sản phẩm trong kho</label>
        <input
          type="text"
          value={searchTerm}
          onChange={(e) => setSearchTerm(e.target.value)}
          placeholder="Tìm theo tên hoặc mã sản phẩm..."
          className="w-full h-12 px-4 bg-slate-50 border border-slate-200 rounded-xl font-bold outline-none"
        />
      </div>

      <div className="h-56 overflow-y-auto border border-slate-100 rounded-xl p-2 space-y-2">
        {isLoading ? (
          <div className="h-full flex items-center justify-center text-slate-400">
            <Loader2 size={24} className="animate-spin" />
          </div>
        ) : filteredProducts.length === 0 ? (
          <div className="h-full flex items-center justify-center text-slate-400 text-xs font-bold uppercase">Không có sản phẩm phù hợp</div>
        ) : (
          filteredProducts.map((product) => (
            <button
              type="button"
              key={product.id}
              onClick={() => setSelectedProduct(product)}
              className={`w-full p-3 rounded-lg border text-left transition-colors min-h-[72px] ${
                selectedProduct?.id === product.id ? 'border-[#003d9b] bg-blue-50' : 'border-slate-200 hover:bg-slate-50'
              }`}
            >
              <p className="font-black text-slate-800 truncate">{product.name}</p>
              <p className="text-xs text-slate-500 font-bold">SP-{product.id} • {product.category}</p>
            </button>
          ))
        )}
      </div>

      {selectedProduct && (
        <div className="grid grid-cols-2 gap-4">
          <div>
            <label className="text-sm font-bold text-slate-600">Số lượng</label>
            <input
              type="number"
              min="1"
              value={quantity}
              onChange={(e) => setQuantity(parseInt(e.target.value, 10) || 1)}
              className="w-full h-12 px-4 bg-slate-50 border border-slate-200 rounded-xl font-bold"
            />
          </div>
          <div>
            <label className="text-sm font-bold text-slate-600">Đơn giá nhập (đ)</label>
            <input
              type="number"
              min="0"
              value={importPrice}
              onChange={(e) => setImportPrice(parseInt(e.target.value, 10) || 0)}
              className="w-full h-12 px-4 bg-slate-50 border border-slate-200 rounded-xl font-bold"
            />
          </div>
        </div>
      )}

      <div className="flex items-center justify-end gap-4 pt-4 border-t border-slate-100">
        <button onClick={onClose} type="button" className="px-6 py-3 text-sm font-black uppercase tracking-widest text-slate-500 hover:text-slate-800">Hủy</button>
        <button
          type="submit"
          disabled={!selectedProduct}
          className="px-8 py-3 bg-[#003d9b] text-white rounded-xl font-black text-sm uppercase tracking-widest shadow-lg disabled:opacity-50"
        >
          Đưa vào phiếu
        </button>
      </div>
    </form>
  );
}
