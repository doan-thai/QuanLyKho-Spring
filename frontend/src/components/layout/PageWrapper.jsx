import { motion } from 'framer-motion';

export default function PageWrapper({ children }) {
  return (
    <motion.div
      initial={{ opacity: 0, y: 15 }} // Vừa mờ vừa tụt xuống 15px
      animate={{ opacity: 1, y: 0 }}  // Hiện rõ và lướt lên vị trí cũ
      exit={{ opacity: 0, y: -15 }}   // Biến mất và trôi lên trên
      transition={{ duration: 0.3 }}
    >
      {children}
    </motion.div>
  );
}