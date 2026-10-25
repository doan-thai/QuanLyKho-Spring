/** @type {import('tailwindcss').Config} */
export default {
  darkMode: ["class"],
  content: [
    "./index.html",
    "./src/**/*.{js,ts,jsx,tsx}",
  ],
  theme: {
    extend: {
      colors: {
        // --- 1. CÁC BIẾN SHADCN CẦN (Đã được link sang màu gốc của bạn) ---
        border: "#c3c6d6", // Dùng outline-variant của bạn
        input: "#c3c6d6",
        ring: "#003d9b", // Dùng primary của bạn làm viền focus
        foreground: "#091c35", // Dùng on-background của bạn
        destructive: {
          DEFAULT: "#ba1a1a", // Dùng error của bạn
          foreground: "#ffffff",
        },
        muted: {
          DEFAULT: "#f0f3ff", // Dùng surface-container-low
          foreground: "#434654", // Dùng on-surface-variant
        },
        accent: {
          DEFAULT: "#dfe8ff", // Dùng surface-container-high
          foreground: "#091c35",
        },
        popover: {
          DEFAULT: "#ffffff", 
          foreground: "#091c35",
        },
        card: {
          DEFAULT: "#ffffff",
          foreground: "#091c35",
        },

        // --- 2. GIỮ NGUYÊN 100% MÀU CŨ CỦA BẠN DƯỚI ĐÂY ---
        "on-tertiary-fixed": "#380d00",
        "secondary-container": "#b6c8fe",
        "on-secondary-container": "#415382",
        "on-background": "#091c35",
        "surface-container-lowest": "#ffffff",
        "on-surface": "#091c35",
        "surface-container": "#e7eeff",
        "surface-container-high": "#dfe8ff",
        "surface-variant": "#d6e3ff",
        "primary-container": "#0052cc",
        "surface-container-highest": "#d6e3ff",
        "on-tertiary": "#ffffff",
        "on-secondary": "#ffffff",
        "background": "#f9f9ff",
        "on-tertiary-fixed-variant": "#812800",
        "surface-dim": "#cadbfc",
        "tertiary-fixed": "#ffdbcf",
        "surface-container-low": "#f0f3ff",
        "on-secondary-fixed": "#021945",
        "on-error": "#ffffff",
        "on-surface-variant": "#434654",
        "on-error-container": "#93000a",
        "on-primary-fixed-variant": "#0040a2",
        "tertiary-container": "#a33500",
        "primary-fixed-dim": "#b2c5ff",
        "secondary-fixed": "#dae2ff",
        "tertiary-fixed-dim": "#ffb59b",
        "error-container": "#ffdad6",
        "on-primary-fixed": "#001848",
        "inverse-surface": "#20314b",
        "surface": "#f9f9ff",
        "on-primary": "#ffffff",
        "on-secondary-fixed-variant": "#344573",
        "outline-variant": "#c3c6d6",
        "inverse-on-surface": "#ecf0ff",
        "tertiary": "#7b2600",
        "secondary-fixed-dim": "#b4c5fb",
        "inverse-primary": "#b2c5ff",
        "on-tertiary-container": "#ffc6b2",
        "outline": "#737685",
        "primary-fixed": "#dae2ff",
        "on-primary-container": "#c4d2ff",
        "surface-bright": "#f9f9ff",
        "surface-tint": "#0c56d0",

        // --- 3. CÁC MÀU GỐC ĐƯỢC CHUYỂN THÀNH OBJECT ĐỂ CẢ BẠN VÀ SHADCN ĐỀU XÀI ĐƯỢC ---
        "primary": {
          DEFAULT: "#003d9b",
          foreground: "#ffffff",
        },
        "secondary": {
          DEFAULT: "#4c5d8d",
          foreground: "#ffffff",
        },
        "error": {
          DEFAULT: "#ba1a1a",
          foreground: "#ffffff",
        },
      },
      fontFamily: {
        // Phục hồi lại font cũ của bạn (Sửa luôn lỗi font-body đỏ lòm)
        "headline": ["Inter", "sans-serif"],
        "body": ["Inter", "sans-serif"],
        "label": ["Inter", "sans-serif"],
        // Cấp thêm cho Shadcn
        "sans": ["Inter", "sans-serif"],
      },
      borderRadius: {
        // Giữ lại của bạn
        "DEFAULT": "0.125rem",
        "xl": "0.5rem",
        "full": "0.75rem",
        // Thêm của Shadcn
        "lg": "var(--radius)",
        "md": "calc(var(--radius) - 2px)",
        "sm": "calc(var(--radius) - 4px)",
      },
    },
  },
  plugins: [require("tailwindcss-animate")],
}