import React, { useRef } from 'react';
import { useReactToPrint } from 'react-to-print';
import Modal from '../../components/ui/Modal'; // Đường dẫn tới Modal của bạn
import StockTicketTemplate from '../../components/print/StockTicketTemplate';

export default function PrintTicketModal({ isOpen, onClose, ticketData, type }) {
    const printRef = useRef(null);

    const handlePrint = useReactToPrint({
        contentRef: printRef,
        documentTitle: `Phieu_${type === 'OUT' ? 'Xuat' : 'Nhap'}_${ticketData?.code || 'Kho'}`,
    });

    if (!isOpen || !ticketData) return null;

    return (
        <Modal isOpen={isOpen} onClose={onClose} title="Hoàn tất giao dịch">
            <div className="flex flex-col items-center gap-6 p-4">
                <div className="w-20 h-20 bg-green-100 text-green-600 rounded-full flex items-center justify-center mb-2">
                    <span className="material-symbols-outlined text-5xl">check_circle</span>
                </div>
                <h3 className="text-2xl font-bold text-slate-800 text-center">Đã lưu phiếu {ticketData.code}!</h3>
                <p className="text-slate-500 text-center mb-4">Giao dịch đã được ghi nhận vào hệ thống. Bạn có muốn xuất file PDF / in phiếu này không?</p>
                
                <div className="flex gap-4 w-full">
                    <button 
                        onClick={onClose} 
                        className="flex-1 px-4 py-3 bg-surface-container-low text-slate-700 font-bold rounded-xl hover:bg-slate-200 transition-colors"
                    >
                        Đóng & Tạo mới
                    </button>
                    <button 
                        onClick={handlePrint} 
                        className="flex-1 px-4 py-3 bg-[#003d9b] text-white font-bold rounded-xl shadow-lg hover:bg-blue-800 transition-colors flex items-center justify-center gap-2"
                    >
                        <span className="material-symbols-outlined">print</span> In Phiếu Ngay
                    </button>
                </div>

                {/* Phần HTML dùng để in được ẨN ĐI */}
                <div className="hidden">
                    <StockTicketTemplate ref={printRef} type={type} ticketData={ticketData} />
                </div>
            </div>
        </Modal>
    );
}