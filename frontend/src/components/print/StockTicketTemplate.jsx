import React, { forwardRef } from 'react';

const StockTicketTemplate = forwardRef(({ ticketData, type = "OUT" }, ref) => {
    if (!ticketData) return null;

    return (
        <div ref={ref} className="p-10 bg-white text-black min-h-[1056px] w-[816px] mx-auto">
            <div className="flex justify-between items-start border-b-2 border-black pb-4 mb-6">
                <div>
                    <h2 className="text-2xl font-black">HỆ THỐNG QUẢN LÝ KHO</h2>
                    <p className="text-sm">Địa chỉ: Hà Nội, Việt Nam</p>
                </div>
                <div className="text-right">
                    <h1 className="text-3xl font-black uppercase">
                        {type === "OUT" ? "PHIẾU XUẤT KHO" : "PHIẾU NHẬP KHO"}
                    </h1>
                    <p className="text-sm italic">Ngày: {ticketData.date || new Date().toLocaleDateString('vi-VN')}</p>
                    <p className="text-sm font-bold">Mã phiếu: {ticketData.code}</p>
                </div>
            </div>

            <div className="mb-6 space-y-1">
                <p><strong>{type === "OUT" ? "Khách hàng / Dự án:" : "Nhà cung cấp:"}</strong> {ticketData.partnerName}</p>
                <p>
                    <strong>{type === "OUT" ? "Ghi chú:" : "Lý do nhập:"}</strong>{" "}
                    {ticketData.note || ticketData.reason || "-"}
                </p>
            </div>

            <table className="w-full text-left border-collapse border border-black mb-10">
                <thead>
                    <tr>
                        <th className="border border-black p-2">STT</th>
                        <th className="border border-black p-2">Mã SP</th>
                        <th className="border border-black p-2">Tên sản phẩm</th>
                        <th className="border border-black p-2 text-center">Số lượng</th>
                    </tr>
                </thead>
                <tbody>
                    {ticketData.items?.map((item, index) => (
                        <tr key={index}>
                            <td className="border border-black p-2 text-center">{index + 1}</td>
                            <td className="border border-black p-2">{item.sku || `SP-${item.productId}`}</td>
                            <td className="border border-black p-2">{item.name}</td>
                            <td className="border border-black p-2 text-center">{item.quantity}</td>
                        </tr>
                    ))}
                </tbody>
            </table>

            <div className="flex justify-between mt-20 text-center">
                <div>
                    <p className="font-bold">Người lập phiếu</p>
                    <p className="italic text-sm">(Ký, họ tên)</p>
                </div>
                <div>
                    <p className="font-bold">Người nhận hàng</p>
                    <p className="italic text-sm">(Ký, họ tên)</p>
                </div>
                <div>
                    <p className="font-bold">Thủ kho</p>
                    <p className="italic text-sm">(Ký, họ tên)</p>
                </div>
            </div>
        </div>
    );
});

export default StockTicketTemplate;