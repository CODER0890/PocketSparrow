/** @type {import('tailwindcss').Config} */
export default {
  content: [
    "./index.html",
    "./src/**/*.{js,ts,jsx,tsx}",
  ],
  theme: {
    extend: {
      colors: {
        cyber: {
          bg: "#030712",
          card: "#0b1120",
          cardBorder: "#1e293b",
          cyan: "#06b6d4",
          blue: "#2563eb",
          emerald: "#10b981",
          rose: "#f43f5e"
        }
      },
      fontFamily: {
        mono: [
          "Fira Code",
          "JetBrains Mono",
          "ui-monospace",
          "SFMono-Regular",
          "Menlo",
          "Monaco",
          "Consolas",
          "monospace"
        ]
      }
    },
  },
  plugins: [],
}
