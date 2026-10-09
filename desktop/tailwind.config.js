/** @type {import('tailwindcss').Config} */
export default {
  content: [
    "./index.html",
    "./src/**/*.{js,ts,jsx,tsx}",
  ],
  theme: {
    extend: {
      colors: {
        background: {
          DEFAULT: "#09090B",
          subtle: "#0E0E12",
          elevated: "#131318",
          card: "#16161D",
        },
        surface: {
          DEFAULT: "rgba(255, 255, 255, 0.03)",
          hover: "rgba(255, 255, 255, 0.06)",
          active: "rgba(255, 255, 255, 0.09)",
          border: "rgba(255, 255, 255, 0.08)",
          borderHover: "rgba(255, 255, 255, 0.16)",
        },
        accent: {
          DEFAULT: "#38BDF8",
          hover: "#0EA5E9",
          muted: "rgba(56, 189, 248, 0.10)",
          border: "rgba(56, 189, 248, 0.25)",
        },
        indigo: {
          DEFAULT: "#6366F1",
          hover: "#4F46E5",
          muted: "rgba(99, 102, 241, 0.10)",
        },
        status: {
          safe: "#10B981",
          safeBg: "rgba(16, 185, 129, 0.08)",
          safeBorder: "rgba(16, 185, 129, 0.20)",
          warning: "#F59E0B",
          warningBg: "rgba(245, 158, 11, 0.08)",
          warningBorder: "rgba(245, 158, 11, 0.20)",
          danger: "#F43F5E",
          dangerBg: "rgba(244, 63, 94, 0.08)",
          dangerBorder: "rgba(244, 63, 94, 0.20)",
        },
        brand: {
          text: "#FAFAFA",
          secondary: "#A1A1AA",
          muted: "#71717A",
          faint: "#52525B",
        }
      },
      fontFamily: {
        sans: ["Geist", "Inter", "-apple-system", "BlinkMacSystemFont", "system-ui", "sans-serif"],
        mono: ["Geist Mono", "JetBrains Mono", "Fira Code", "ui-monospace", "SFMono-Regular", "monospace"],
      },
      borderRadius: {
        sm: "0.25rem",
        md: "0.375rem",
        lg: "0.5rem",
        xl: "0.75rem",
        "2xl": "1rem",
      },
      boxShadow: {
        glass: "0 8px 32px 0 rgba(0, 0, 0, 0.45)",
        card: "0 1px 3px 0 rgba(0, 0, 0, 0.3), 0 1px 2px -1px rgba(0, 0, 0, 0.3)",
      }
    },
  },
  plugins: [],
}
