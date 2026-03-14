import { motion, AnimatePresence } from 'framer-motion'
import { Plus, Database, Cpu, GitBranch } from 'lucide-react'
import { useStore } from '../store'

const COLORS = ['#3fb950','#c19dff','#58d6e4','#e3a03c']

export default function Sidebar({ onNew }: { onNew: () => void }) {
  const { projects, activeProjectId, setActiveProject, theme } = useStore()
  const d = theme === 'dark'

  return (
    <aside className={`flex flex-col h-full border-r transition-colors ${d ? 'bg-[#0e1117] border-[#1e2838]' : 'bg-[#faf9f6] border-[#d5d0c8]'}`}>
      <div className={`px-4 pt-4 pb-2 font-mono text-[9px] font-bold tracking-[.14em] uppercase ${d ? 'text-[#3d5068]' : 'text-[#9a9590]'}`}>
        ⌥ Repositories
      </div>

      <motion.button whileHover={{scale:1.01}} whileTap={{scale:.98}} onClick={onNew}
        className={`mx-3 mb-2 flex items-center gap-2 px-3 py-2 rounded-lg border border-dashed font-mono text-[11px] transition-all cursor-pointer ${d ? 'border-[#263348] text-[#7a8fa8] hover:border-[#3fb950] hover:text-[#3fb950]' : 'border-[#c5bfb5] text-[#5a5650] hover:border-[#1a7f37] hover:text-[#1a7f37]'}`}>
        <Plus size={12}/> clone new repo
      </motion.button>

      <div className="flex-1 overflow-y-auto">
        <AnimatePresence>
          {projects.length === 0 && (
            <div className={`px-4 py-3 font-mono text-[10px] ${d ? 'text-[#3d5068]' : 'text-[#9a9590]'}`}>No repositories yet</div>
          )}
          {projects.map((p, i) => {
            const color = COLORS[i % COLORS.length]
            const active = p.id === activeProjectId
            return (
              <motion.div key={p.id} initial={{opacity:0,x:-10}} animate={{opacity:1,x:0}} transition={{delay:i*.05}}
                onClick={() => setActiveProject(p.id)}
                className={`flex gap-3 items-start px-3 py-2 cursor-pointer border-l-2 transition-all ${active ? `${d?'bg-[#141920]':'bg-[#eeecea]'} border-[${d?'#3fb950':'#1a7f37'}]` : `border-transparent ${d?'hover:bg-[#141920]':'hover:bg-[#eeecea]'}`}`}
              >
                <div className="flex flex-col items-center pt-1 flex-shrink-0">
                  <div className="w-2 h-2 rounded-full" style={{background:color,boxShadow:active?`0 0 6px ${color}`:undefined}}/>
                  {i < projects.length-1 && <div className={`w-px flex-1 min-h-4 mt-1 ${d?'bg-[#1e2838]':'bg-[#d5d0c8]'}`}/>}
                </div>
                <div className="flex-1 min-w-0">
                  <div className={`font-mono text-[11px] truncate ${d?'text-[#cdd9e5]':'text-[#1c1c1a]'}`}>/{p.name}</div>
                  <div className={`font-mono text-[9px] mt-0.5 ${d?'text-[#3d5068]':'text-[#9a9590]'}`}>{p.hash}</div>
                </div>
                <span className={`font-mono text-[8px] px-1.5 py-0.5 rounded border flex-shrink-0 mt-0.5 ${p.embedded ? (d?'border-[#3fb950]/40 text-[#3fb950]':'border-[#1a7f37]/40 text-[#1a7f37]') : (d?'border-[#e3a03c]/40 text-[#e3a03c]':'border-[#b56b00]/40 text-[#b56b00]')}`}>
                  {p.embedded ? 'indexed' : 'pending'}
                </span>
              </motion.div>
            )
          })}
        </AnimatePresence>
      </div>

      <div className={`border-t px-3 py-3 font-mono text-[9px] space-y-1 ${d?'border-[#1e2838] text-[#3d5068]':'border-[#d5d0c8] text-[#9a9590]'}`}>
        <div className="flex items-center gap-1.5"><Database size={9}/> pgvector · PostgreSQL</div>
        <div className="flex items-center gap-1.5"><Cpu size={9}/> Ollama · Gemma · nomic-embed</div>
        <div className="flex items-center gap-1.5"><GitBranch size={9}/> RAG Pipeline · Spring Boot</div>
      </div>
    </aside>
  )
}
