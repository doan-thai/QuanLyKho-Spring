import { NavLink } from 'react-router-dom';

export default function SideNavBar() {
  const menuItems = [
    { path: '/dashboard', icon: 'dashboard', label: 'Tổng quan' },
    { path: '/products', icon: 'inventory_2', label: 'Sản phẩm' },
    { path: '/stock-in', icon: 'input', label: 'Nhập kho' },
    { path: '/stock-out', icon: 'output', label: 'Xuất kho' },
    { path: '/suppliers', icon: 'local_shipping', label: 'Nhà cung cấp' },
  ];

  return (
    <aside className="fixed left-0 top-0 h-full z-40 w-64 border-r border-slate-200/20 bg-slate-100/70 backdrop-blur-xl font-sans Inter antialiased tracking-tight tonal-layering-no-borders">
      <div className="p-6 mb-4">
        <div className="flex items-center gap-3 mb-8">
          <div className="w-10 h-10 rounded-xl bg-gradient-to-br from-primary to-primary-container flex items-center justify-center text-white shadow-lg">
            <span className="material-symbols-outlined">package_2</span>
          </div>
          <div>
            <h1 className="text-xl font-bold tracking-tighter text-slate-900">SyncStock</h1>
            <p className="text-[10px] uppercase tracking-widest text-slate-500 font-semibold">Trung tâm điều phối kho</p>
          </div>
        </div>
        
        <nav className="space-y-1">
          {menuItems.map((item) => (
            <NavLink
              key={item.path}
              to={item.path}
              className={({ isActive }) =>
                `flex items-center gap-3 px-4 py-3 transition-all duration-200 group ${
                  isActive
                    ? 'bg-blue-100 text-blue-700 rounded-r-full font-semibold scale-98 active:opacity-80'
                    : 'text-slate-500 hover:text-slate-900 hover:bg-slate-200/50'
                }`
              }
            >
              <span className="material-symbols-outlined">{item.icon}</span>
              <span>{item.label}</span>
            </NavLink>
          ))}
        </nav>
      </div>

      <div className="absolute bottom-8 left-0 w-full px-6 space-y-1">
        <a href="#" className="flex items-center gap-3 px-4 py-3 text-slate-500 hover:text-slate-900 hover:bg-slate-200/50 transition-all duration-200">
          <span className="material-symbols-outlined">settings</span>
          <span>Cài đặt</span>
        </a>
        <a href="#" className="flex items-center gap-3 px-4 py-3 text-slate-500 hover:text-slate-900 hover:bg-slate-200/50 transition-all duration-200">
          <span className="material-symbols-outlined">help_outline</span>
          <span>Hỗ trợ</span>
        </a>
        <div className="mt-6 p-4 bg-white/50 rounded-2xl flex items-center gap-3">
          <div className="w-10 h-10 rounded-full bg-primary flex items-center justify-center text-white font-bold">A</div>
          <div className="overflow-hidden">
            <p className="text-xs font-bold text-slate-900 truncate">Quản trị viên</p>
            <p className="text-[10px] text-slate-500">Trưởng kho</p>
          </div>
        </div>
      </div>
    </aside>
  );
}