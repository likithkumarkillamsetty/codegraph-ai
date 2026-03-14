export default {
  content: ['./index.html','./src/**/*.{js,ts,jsx,tsx}'],
  darkMode: 'class',
  theme: {
    extend: {
      fontFamily: {
        mono: ['"JetBrains Mono"','monospace'],
        serif: ['"Instrument Serif"','serif'],
        sans: ['Geist','system-ui','sans-serif']
      }
    }
  },
  plugins: [require('@tailwindcss/typography')]
}
