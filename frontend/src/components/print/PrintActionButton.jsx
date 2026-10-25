import React, { useRef } from 'react';
import { useReactToPrint } from 'react-to-print';
import StockTicketTemplate from '../../components/print/StockTicketTemplate';

export default function PrintActionButton({ ticketData, type = "OUT", className }) {
    const printRef = useRef(null);

    const handlePrint = useReactToPrint({
        contentRef: printRef,
        documentTitle: `Phieu_${type}_${ticketData?.code || 'Lich_Su'}`,
    });

    return (
        <>
            {/* Nút bấm hiển thị trên giao diện */}
            <button 
                onClick={handlePrint} 
                title="In phiếu này"
                className={`flex items-center justify-center p-2 text-slate-500 hover:text-primary transition-colors ${className}`}
            >
                <span className="material-symbols-outlined text-xl">print</span>
            </button>

            {/* Template in được giấu đi */}
            <div className="hidden">
                <StockTicketTemplate ref={printRef} type={type} ticketData={ticketData} />
            </div>
        </>
    );
}