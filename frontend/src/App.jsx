import { BrowserRouter, Navigate, Route, Routes } from "react-router-dom";
import DashboardLayout from "./components/layout/DashboardLayout";
import Dashboard from "./features/dashboard/Dashboard";
import Reports from "./features/dashboard/Reports";
import ProductList from "./features/inventory/ProductList";
import SupplierList from "./features/suppliers/SupplierList";
import UserManagement from "./features/admin/UserManagement";
import StockIn from "./features/transactions/StockIn";
import StockInHistory from "./features/transactions/StockInHistory";
import StockOut from "./features/transactions/StockOut";
import Profile from "./features/account/Profile";

import Login from "./features/auth/Login";
import ProtectedRoute from "./components/auth/ProtectedRoute";

export default function App() {
    return (
        <BrowserRouter>
            <Routes>
                <Route path="/login" element={<Login />} />

                <Route
                    element={
                        <ProtectedRoute
                            allowedRoles={["Admin", "ThuKho", "KeToan"]}
                        />
                    }
                >
                    <Route path="/" element={<DashboardLayout />}>
                        <Route
                            index
                            element={<Navigate to="/dashboard" replace />}
                        />

                        {/* CÁC TRANG DÙNG CHUNG */}
                        <Route path="dashboard" element={<Dashboard />} />
                        <Route path="products" element={<ProductList />} />
                        <Route path="suppliers" element={<SupplierList />} />
                        <Route path="stock-history" element={<StockInHistory />} />
                        <Route path="profile" element={<Profile />} />

                        {/* CÁC TRANG CỦA ADMIN & THỦ KHO */}
                        <Route
                            element={
                                <ProtectedRoute
                                    allowedRoles={["Admin", "ThuKho"]}
                                />
                            }
                        >
                            <Route path="stock-in" element={<StockIn />} />
                            <Route path="stock-out" element={<StockOut />} />
                        </Route>

                        {/* TRANG CỦA ADMIN & KẾ TOÁN */}
                        <Route
                            element={
                                <ProtectedRoute
                                    allowedRoles={["Admin", "KeToan"]}
                                />
                            }
                        >
                            <Route path="reports" element={<Reports />} />
                        </Route>

                        {/* TRANG CHỈ DÀNH CHO ADMIN */}
                        <Route
                            element={
                                <ProtectedRoute
                                    allowedRoles={["Admin"]}
                                />
                            }
                        >
                            <Route path="users" element={<UserManagement />} />
                        </Route>
                    </Route>
                </Route>

                <Route path="*" element={<Navigate to="/login" replace />} />
            </Routes>
        </BrowserRouter>
    );
}
