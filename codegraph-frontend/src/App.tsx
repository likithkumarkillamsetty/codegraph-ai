import { useEffect } from 'react'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { Toaster } from 'react-hot-toast'
import { motion } from 'framer-motion'
import { Sun, Moon, GitBranch } from 'lucide-react'
import { useStore } from './store'
import Sidebar from './components/Sidebar'
import Welcome from './components/Welcome'
import ChatInterface from './components/ChatInterface'

const qc = new QueryClient({ defaultOptions: { mutations: { retry: 0 } } })

function Inner() {
  const { theme, toggleTheme, activeProjectId, setActiveProject, projects } = useStore()
  const d = theme === 'dark'

  useEffect(() => {
    document.documentElement.classList.toggle('dark', d)
  }, [d])

  const hasActive = activeProjectId !== null && projects.some(p => p.id === activeProjectId)

  return (
    <div className={`h-screen flex flex-col overflow-hidden transition-colors duration-300 ${d?'bg-[#090b0e] text-[#cdd9e5]':'bg-[#f4f2ed] text-[#1c1c1a]'}`}>

      {/* Topbar */}
      <header className={`flex items-center gap-3 px-4 h-11 border-b flex-shrink-0 z-10 ${d?'bg-[#0e1117] border-[#1e2838]':'bg-[#faf9f6] border-[#d5d0c8]'}`}>
        <div className="flex items-center gap-2 font-mono text-[13px] font-bold">
          <motion.span animate={{filter:['drop-shadow(0 0 4px #3fb950)','drop-shadow(0 0 10px #3fb950)','drop-shadow(0 0 4px #3fb950)']}}
            transition={{duration:3,repeat:Infinity}} className={`text-[18px] ${d?'text-[#3fb950]':'text-[#1a7f37]'}`}>⬡</motion.span>
          <span className={d?'text-[#3fb950]':'text-[#1a7f37]'}>CodeGraph</span>
          <span className={`font-light ${d?'text-[#7a8fa8]':'text-[#5a5650]'}`}>AI</span>
        </div>

        <div className={`w-px h-5 ${d?'bg-[#1e2838]':'bg-[#d5d0c8]'}`}/>

        <div className={`flex items-center gap-1.5 px-2.5 py-1 rounded border font-mono text-[10px] ${d?'bg-[#141920] border-[#1e2838] text-[#7a8fa8]':'bg-[#eeecea] border-[#d5d0c8] text-[#5a5650]'}`}>
          <motion.div animate={{opacity:[1,.3,1]}} transition={{duration:2,repeat:Infinity}} className={`w-1.5 h-1.5 rounded-full ${d?'bg-[#3fb950]':'bg-[#1a7f37]'}`}/>
          <GitBranch size={9}/>main
        </div>

        <div className="flex-1"/>

        {/* Tech stack badges */}
        <div className="hidden md:flex items-center gap-1">
          {['React','TypeScript','Zustand','React Query','Framer Motion'].map(b=>(
            <span key={b} className={`font-mono text-[8px] px-1.5 py-0.5 rounded border ${d?'border-[#1e2838] text-[#3d5068] bg-[#0e1117]':'border-[#d5d0c8] text-[#9a9590]'}`}>{b}</span>
          ))}
        </div>

        <motion.button whileHover={{scale:1.05}} whileTap={{scale:.95}} onClick={toggleTheme}
          className={`w-8 h-8 rounded-lg border flex items-center justify-center transition-all ${d?'border-[#263348] text-[#7a8fa8] hover:border-[#3fb950] hover:text-[#3fb950] bg-[#141920]':'border-[#c5bfb5] text-[#5a5650] hover:border-[#1a7f37] hover:text-[#1a7f37] bg-[#eeecea]'}`}>
          {d ? <Sun size={13}/> : <Moon size={13}/>}
        </motion.button>
      </header>

      {/* Body */}
      <div className="flex-1 flex overflow-hidden">
        <div className="w-[252px] flex-shrink-0">
          <Sidebar onNew={() => setActiveProject(null)}/>
        </div>
        <main className="flex-1 flex flex-col overflow-hidden">
          {hasActive ? <ChatInterface/> : <Welcome onDone={() => {}}/> }
        </main>
      </div>

      <Toaster position="bottom-right" toastOptions={{
        style: { background: d?'#141920':'#faf9f6', color: d?'#cdd9e5':'#1c1c1a', border:`1px solid ${d?'#263348':'#c5bfb5'}`, fontFamily:'JetBrains Mono,monospace', fontSize:'11px', borderRadius:'8px' },
        success: { iconTheme: { primary:'#3fb950', secondary:'#000' } },
        error: { iconTheme: { primary:'#f85149', secondary:'#fff' } },
      }}/>
    </div>
  )
}

export default function App() {
  return <QueryClientProvider client={qc}><Inner/></QueryClientProvider>
}
