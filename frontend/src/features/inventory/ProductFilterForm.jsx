import React, { useState } from 'react';
import { Check, RotateCcw } from 'lucide-react';

const defaultFilters = {
    category: 'Tất cả',
    categoryQuery: '',
    status: 'Tất cả',
    minStock: ''
};

const fixedCategoryOptions = ['Tất cả', 'Điện tử', 'Gia dụng', 'Thực phẩm'];

export default function ProductFilterForm({ onApply, onReset, onClose, initialFilters }) {
  const [filters, setFilters] = useState({ ...defaultFilters, ...(initialFilters || {}) });

  const statuses = ['Tất cả', 'Còn hàng', 'Sắp hết', 'Hết hàng'];

  const handleReset = () => {
    setFilters({ ...defaultFilters });
    onReset?.();
  };

  const handleApply = () => {
    onApply({
      ...filters,
      category: filters.category || 'Tất cả',
      categoryQuery: (filters.categoryQuery || '').trim(),
    });
  };

  return (
    <div className="space-y-8 not-italic">
      <div className="space-y-4">
        {/* Caption: 14px */}
        <label className="text-sm font-black uppercase tracking-[0.2em] text-[#003d9b]">Danh mục hàng hóa</label>
        <div className="flex flex-wrap gap-2">
          {fixedCategoryOptions.map((cat) => (
            <button
              key={cat}
              type="button"
              onClick={() => setFilters({ ...filters, category: cat })}
              className={`px-5 py-3 rounded-xl text-base font-bold transition-all ${
                filters.category === cat 
                ? 'bg-[#003d9b] text-white shadow-lg shadow-blue-900/20' 
                : 'bg-slate-100 text-slate-500 hover:bg-slate-200'
              }`}
            >
              {cat}
            </button>
          ))}
        </div>

        <input
          type="text"
          placeholder="Hoặc nhập danh mục để tìm (vd: thực phẩm)..."
          value={filters.categoryQuery || ''}
          onChange={(e) => setFilters({ ...filters, categoryQuery: e.target.value })}
          className="w-full px-5 py-4 bg-slate-50 border border-slate-200 rounded-2xl focus:ring-1 focus:ring-[#003d9b] outline-none font-bold text-base text-slate-900"
        />
      </div>

      <div className="space-y-4">
        <label className="text-sm font-black uppercase tracking-[0.2em] text-[#003d9b]">Trạng thái tồn kho</label>
        <div className="grid grid-cols-2 gap-3">
          {statuses.map((status) => (
            <button
              key={status}
              type="button"
              onClick={() => setFilters({ ...filters, status: status })}
              className={`flex items-center justify-between px-5 py-5 rounded-2xl border-2 transition-all ${
                filters.status === status 
                ? 'border-[#003d9b] bg-blue-50 text-[#003d9b]' 
                : 'border-slate-100 bg-white text-slate-400'
              }`}
            >
              {/* Body: 16px */}
              <span className="text-base font-black uppercase tracking-tight">{status}</span>
              {filters.status === status && <Check size={18} strokeWidth={3} />}
            </button>
          ))}
        </div>
      </div>

      <div className="space-y-4">
        <label className="text-sm font-black uppercase tracking-[0.2em] text-[#003d9b]">Ngưỡng tồn tối thiểu</label>
        <input 
          type="number" 
          placeholder="Nhập số lượng..."
          value={filters.minStock}
          onChange={(e) => setFilters({...filters, minStock: e.target.value})}
          className="w-full px-5 py-4 bg-slate-50 border border-slate-200 rounded-2xl focus:ring-1 focus:ring-[#003d9b] outline-none font-bold text-base text-slate-900"
        />
      </div>

      <div className="pt-6 border-t border-slate-100 flex items-center gap-3">
        <button 
          type="button"
          onClick={handleReset}
          className="flex-1 flex items-center justify-center gap-2 py-4 text-sm font-black uppercase tracking-widest text-slate-400 hover:bg-slate-100 rounded-2xl transition-all"
        >
          <RotateCcw size={16} /> Làm mới
        </button>
        <button 
          type="button"
          onClick={handleApply}
          className="flex-[2] bg-[#003d9b] text-white py-4 rounded-2xl text-base font-black uppercase tracking-widest shadow-xl shadow-blue-900/30 hover:scale-[1.02] active:scale-95 transition-all"
        >
          Áp dụng bộ lọc
        </button>
      </div>
    </div>
  );
}