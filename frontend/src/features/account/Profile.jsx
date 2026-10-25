import React, { useState } from 'react';
import { User, Mail, Shield, Building2 } from 'lucide-react';
import useAuthStore from '../../lib/authStore'; // Đường dẫn tới file authStore của sếp

export default function Profile() {
  // Lấy thông tin từ Zustand Store
  const { role } = useAuthStore();
  
  // Dữ liệu tài khoản (Chế độ chỉ đọc)
  const [formData] = useState({
    email: 'admin@syncstock.vn',
    branch: 'Chi nhánh Quận 1 (Kho chính)'
  });

  return (
    <div className="max-w-7xl mx-auto not-italic">
      {/* Header */}
      <div className="mb-10">
        <h2 className="text-3xl md:text-4xl font-black tracking-tight text-slate-900 mb-3 uppercase">
          Thông tin tài khoản
        </h2>
        <p className="text-sm md:text-base text-slate-500 font-medium">
          Thông tin định danh và quyền hạn của sếp trên hệ thống SyncStock.
        </p>
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-3 gap-8">
        {/* CỘT TRÁI: Định danh hệ thống */}
        <div className="lg:col-span-1 flex flex-col gap-6">
          <div className="bg-white p-8 rounded-3xl border border-slate-200 shadow-sm flex flex-col items-center text-center">
            <div className="mb-6">
              <div className="w-32 h-32 bg-blue-50 rounded-full flex items-center justify-center border-4 border-white shadow-lg overflow-hidden">
                <User size={64} className="text-blue-200" />
              </div>
            </div>
            
            <h3 className="text-lg md:text-xl font-black text-slate-900 mb-1">Tài khoản quản trị</h3>
            <p className="text-sm md:text-base text-blue-600 font-bold mb-4">{formData.email}</p>
            
            <div className="inline-flex items-center gap-2 px-4 py-2 bg-emerald-50 text-emerald-700 rounded-full">
              <Shield size={16} />
              <span className="text-xs md:text-sm font-black uppercase tracking-widest">{{
                                            Admin: "Quản Lý",
                                            ThuKho: "Thủ Kho",
                                            KeToan: "Kế Toán",
                                        }[role] || "QUẢN LÝ"}</span>
            </div>
          </div>
        </div>

        {/* CỘT PHẢI: Chi tiết thông tin (Read-only) */}
        <div className="lg:col-span-2 flex flex-col gap-6">
          <div className="bg-white p-8 rounded-3xl border border-slate-200 shadow-sm">
            <h3 className="text-base md:text-lg font-black uppercase tracking-widest text-slate-500 mb-8 border-b border-slate-100 pb-4">
              Hồ sơ hệ thống
            </h3>
            
            <div className="space-y-6">
              <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
                
                {/* Email - Read-only */}
                <div className="space-y-2">
                  <label className="text-xs md:text-sm font-black uppercase text-slate-400 ml-1">Địa chỉ Email</label>
                  <div className="relative">
                    <div className="absolute inset-y-0 left-0 pl-4 flex items-center pointer-events-none">
                      <Mail className="text-slate-300" size={18} />
                    </div>
                    <input 
                      type="email" 
                      value={formData.email} 
                      readOnly
                      className="w-full bg-slate-100 border border-slate-200 rounded-xl pl-12 py-3 text-slate-500 font-bold outline-none cursor-not-allowed text-sm md:text-base"
                    />
                  </div>
                </div>

                {/* Chi nhánh - Read-only */}
                <div className="space-y-2">
                  <label className="text-xs md:text-sm font-black uppercase text-slate-400 ml-1">Chi nhánh trực thuộc</label>
                  <div className="relative">
                    <div className="absolute inset-y-0 left-0 pl-4 flex items-center pointer-events-none">
                      <Building2 className="text-slate-300" size={18} />
                    </div>
                    <input 
                      type="text" 
                      value={formData.branch} 
                      readOnly
                      className="w-full bg-slate-100 border border-slate-200 rounded-xl pl-12 py-3 text-slate-500 font-bold outline-none cursor-not-allowed text-sm md:text-base"
                    />
                  </div>
                </div>

              </div>

              {/* Ghi chú bảo mật */}
              <div className="mt-8 p-4 bg-amber-50 rounded-2xl border border-amber-100">
                <p className="text-xs md:text-sm text-amber-700 font-medium leading-relaxed">
                  <strong>Lưu ý:</strong> Thông tin tài khoản và chi nhánh được thiết lập bởi hệ thống quản trị trung tâm. Để thay đổi các thông tin này, vui lòng liên hệ bộ phận Kỹ thuật.
                </p>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}