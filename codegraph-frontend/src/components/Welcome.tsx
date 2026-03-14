import { useState, useRef, useEffect } from 'react'
import { motion } from 'framer-motion'
import { useMutation } from '@tanstack/react-query'
import toast from 'react-hot-toast'
import { GitBranch, Zap } from 'lucide-react'
import { projectApi } from '../api'
import { useStore } from '../store'
import GitLoader from './GitLoader'

export default function Welcome({ onDone }: { onDone: () => void }) {
  const { theme, addProject, setActiveProject } = useStore()
  const [url, setUrl] = useState('')
  const [phase, setPhase] = useState<'input'|'loading'>('input')
  const [activeUrl, setActiveUrl] = useState('')
  const projectReadyRef = useRef(false)
  const animationDoneRef = useRef(false)
  const d = theme === 'dark'

  const mut = useMutation({
    mutationFn: (repoUrl: string) => {
      const name = (() => { try { const p=new URL(repoUrl).pathname.split('/').filter(Boolean); return p[p.length-1].replace('.git','') } catch { return 'repo' } })()
      return projectApi.create(name, repoUrl)
    },
    onSuccess: (project) => {
      addProject(project)
      setActiveProject(project.id)
      projectReadyRef.current = true
      // If animation already done, navigate immediately
      if (animationDoneRef.current) {
        onDone()
      }
    },
    onError: (e: Error) => { toast.error(e.message); setPhase('input') },
  })

  const submit = () => {
    const u = url.trim()
    if (!u) { toast.error('Paste a GitHub URL'); return }
    if (!u.startsWith('http')) { toast.error('Must be a full https:// URL'); return }
    projectReadyRef.current = false
    animationDoneRef.current = false
    setActiveUrl(u)
    setPhase('loading')
    mut.mutate(u)
  }

  const handleLoaderDone = () => {
    animationDoneRef.current = true
    if (projectReadyRef.current) {
      // API already done, navigate immediately
      onDone()
    }
    // Otherwise wait — onSuccess will call onDone() when API finishes
  }

  if (phase === 'loading') return (
      <div className="flex-1 flex items-center justify-center p-8">
        <GitLoader repoUrl={activeUrl} onDone={handleLoaderDone}/>
      </div>
  )

  return (
      <motion.div initial={{opacity:0,y:20}} animate={{opacity:1,y:0}} transition={{duration:.5,ease:[.22,1,.36,1]}}
                  className="flex-1 flex flex-col items-center justify-center p-8 gap-0">

        <div className="w-[200px] h-[110px] mb-8">
          <svg viewBox="0 0 200 110" fill="none" className="w-full h-full">
            <motion.path d="M40 100 L40 30 L120 10" stroke={d?'#1e2838':'#d5d0c8'} strokeWidth="1.5" fill="none"
                         initial={{pathLength:0}} animate={{pathLength:1}} transition={{duration:1.2,ease:'easeOut'}}/>
            <motion.path d="M40 70 L100 45 L160 45 L160 80" stroke={d?'#1e2838':'#d5d0c8'} strokeWidth="1" fill="none"
                         initial={{pathLength:0}} animate={{pathLength:1}} transition={{duration:1,delay:.4,ease:'easeOut'}}/>
            <motion.path d="M160 80 L120 100" stroke={d?'#1e2838':'#d5d0c8'} strokeWidth="1" fill="none"
                         initial={{pathLength:0}} animate={{pathLength:1}} transition={{duration:.5,delay:1.2}}/>
            {[{cx:40,cy:100,c:'#3fb950',d:.8},{cx:40,cy:70,c:'#3fb950',d:.9},{cx:40,cy:45,c:'#3fb950',d:1},{cx:40,cy:30,c:'#3fb950',d:1.1},{cx:120,cy:10,c:'#3fb950',d:1.2}].map((n,i)=>(
                <motion.circle key={i} cx={n.cx} cy={n.cy} r="4" fill={n.c}
                               initial={{scale:0,opacity:0}} animate={{scale:1,opacity:1}} transition={{delay:n.d,duration:.3}}
                               style={{transformOrigin:`${n.cx}px ${n.cy}px`}}/>
            ))}
            {[{cx:100,cy:45,c:'#c19dff',d:1},{cx:160,cy:45,c:'#c19dff',d:1.1},{cx:160,cy:80,c:'#c19dff',d:1.2}].map((n,i)=>(
                <motion.circle key={i} cx={n.cx} cy={n.cy} r="4" fill={n.c}
                               initial={{scale:0,opacity:0}} animate={{scale:1,opacity:1}} transition={{delay:n.d,duration:.3}}
                               style={{transformOrigin:`${n.cx}px ${n.cy}px`}}/>
            ))}
            <motion.circle cx={120} cy={100} r="5" fill="#58d6e4"
                           initial={{scale:0,opacity:0}} animate={{scale:1,opacity:1}} transition={{delay:1.4,duration:.4}}
                           style={{transformOrigin:'120px 100px'}}/>
            {[{x:48,y:103,t:'feat: init'},{x:104,y:48,t:'refactor'},{x:126,y:13,t:'merge'}].map((l,i)=>(
                <motion.text key={i} x={l.x} y={l.y} fontFamily="JetBrains Mono" fontSize="7" fill={d?'#3d5068':'#9a9590'}
                             initial={{opacity:0}} animate={{opacity:1}} transition={{delay:1.5+i*.1}}>{l.t}</motion.text>
            ))}
          </svg>
        </div>

        <motion.div initial={{opacity:0}} animate={{opacity:1}} transition={{delay:.3}}
                    className={`flex items-center gap-3 font-mono text-[10px] tracking-[.14em] uppercase mb-3 ${d?'text-[#3fb950]':'text-[#1a7f37]'}`}>
          <div className={`h-px w-8 ${d?'bg-[#3fb950]/40':'bg-[#1a7f37]/40'}`}/>
          RAG · pgvector · Ollama
          <div className={`h-px w-8 ${d?'bg-[#3fb950]/40':'bg-[#1a7f37]/40'}`}/>
        </motion.div>

        <motion.h1 initial={{opacity:0,y:8}} animate={{opacity:1,y:0}} transition={{delay:.35}}
                   className={`font-serif text-[38px] text-center leading-tight mb-3 tracking-tight ${d?'text-[#cdd9e5]':'text-[#1c1c1a]'}`}>
          Analyze any{' '}
          <em className={`italic ${d?'text-[#3fb950]':'text-[#1a7f37]'}`}>Git repo</em>{' '}
          with AI
        </motion.h1>

        <motion.p initial={{opacity:0}} animate={{opacity:1}} transition={{delay:.4}}
                  className={`text-[13px] text-center max-w-md leading-relaxed mb-10 ${d?'text-[#7a8fa8]':'text-[#5a5650]'}`}>
          Paste a GitHub URL. CodeGraph clones it, chunks every file, generates vector embeddings via Ollama, and lets you query the entire codebase in natural language.
        </motion.p>

        <motion.div initial={{opacity:0,y:10}} animate={{opacity:1,y:0}} transition={{delay:.45}}
                    className={`w-full max-w-[560px] rounded-xl border overflow-hidden shadow-2xl ${d?'bg-[#0e1117] border-[#263348] shadow-black/50':'bg-[#faf9f6] border-[#c5bfb5]'}`}>

          <div className={`flex items-center gap-2 px-4 py-2.5 border-b ${d?'bg-[#141920] border-[#1e2838]':'bg-[#eeecea] border-[#d5d0c8]'}`}>
            <div className="flex gap-1.5">
              {['#ff5f57','#febc2e','#28c840'].map(c=><div key={c} className="w-2.5 h-2.5 rounded-full" style={{background:c}}/>)}
            </div>
            <span className={`flex-1 text-center font-mono text-[10px] ${d?'text-[#3d5068]':'text-[#9a9590]'}`}>codegraph-ai — bash</span>
            <GitBranch size={11} color={d?'#3fb950':'#1a7f37'}/>
            <span className={`font-mono text-[10px] ${d?'text-[#3fb950]':'text-[#1a7f37]'}`}>main</span>
          </div>

          <div className="p-4 flex flex-col gap-3">
            <div className={`flex items-center rounded-lg border overflow-hidden focus-within:ring-2 transition-all ${d?'border-[#263348] focus-within:border-[#3fb950] focus-within:ring-[#3fb950]/20':'border-[#c5bfb5] focus-within:border-[#1a7f37] focus-within:ring-[#1a7f37]/20'}`}>
              <div className={`px-3 py-3 font-mono text-[11px] border-r whitespace-nowrap ${d?'bg-[#141920] border-[#1e2838] text-[#3d5068]':'bg-[#eeecea] border-[#d5d0c8] text-[#9a9590]'}`}>
                git clone
              </div>
              <input type="text" value={url} onChange={e=>setUrl(e.target.value)} onKeyDown={e=>e.key==='Enter'&&submit()}
                     placeholder="https://github.com/user/repository" spellCheck={false} autoComplete="off"
                     className={`flex-1 px-4 py-3 bg-transparent outline-none font-mono text-[12px] placeholder:text-[#3d5068] ${d?'text-[#cdd9e5]':'text-[#1c1c1a]'}`}/>
            </div>

            <div className="flex items-center gap-1 px-1 flex-wrap">
              {['clone','scan','chunk','embed','query'].map((s,i,a)=>(
                  <div key={s} className="flex items-center gap-1">
                    <span className={`font-mono text-[9px] px-2 py-0.5 rounded border ${d?'border-[#263348] text-[#3d5068] bg-[#141920]':'border-[#d5d0c8] text-[#9a9590] bg-[#eeecea]'}`}>{s}</span>
                    {i<a.length-1&&<span className={`text-[9px] ${d?'text-[#3d5068]':'text-[#9a9590]'}`}>→</span>}
                  </div>
              ))}
            </div>

            <motion.button whileHover={{scale:1.01}} whileTap={{scale:.98}} onClick={submit}
                           disabled={mut.isPending}
                           className={`w-full py-3 rounded-lg font-mono text-[12px] font-bold flex items-center justify-center gap-2 transition-all disabled:opacity-50 disabled:cursor-not-allowed ${d?'bg-[#3fb950] text-black hover:brightness-110 shadow-lg shadow-[#3fb950]/20':'bg-[#1a7f37] text-white hover:brightness-110'}`}>
              <Zap size={13}/> Analyze Repository
            </motion.button>
          </div>
        </motion.div>
      </motion.div>
  )
}