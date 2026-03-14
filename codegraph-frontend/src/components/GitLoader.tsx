import { useEffect, useState, useRef } from 'react'
import { motion, AnimatePresence } from 'framer-motion'
import { CheckCircle, Loader } from 'lucide-react'
import { useStore } from '../store'

const STEPS = [
  { label: 'git clone', sub: 'fetching repository objects & refs…', dur: 1100 },
  { label: 'Scanning files', sub: 'walking directory tree recursively…', dur: 800 },
  { label: 'Chunking code', sub: 'splitting into semantic code blocks…', dur: 900 },
  { label: 'Storing metadata', sub: 'writing project to PostgreSQL…', dur: 700 },
  { label: 'Project ready', sub: 'repository indexed successfully ✓', dur: 400 },
]
const LOGS = [
  { a: '◈', ac: '#58d6e4', t: 'Cloning into ', vc: '#3fb950', v: "'/tmp/codegraph/repo'..." },
  { a: '◈', ac: '#58d6e4', t: 'remote: Enumerating objects: ', vc: '#3fb950', v: '1,842, done.' },
  { a: '⌥', ac: '#c19dff', t: 'Resolving deltas: ', vc: '#3fb950', v: '100% (743/743), done.' },
  { a: '⬡', ac: '#58d6e4', t: 'Walking ', vc: '#3fb950', v: 'src/ test/ docs/' },
  { a: '◈', ac: '#58d6e4', t: 'Java files found: ', vc: '#3fb950', v: '47 files' },
  { a: '⌥', ac: '#c19dff', t: 'Chunks created: ', vc: '#3fb950', v: '374 semantic blocks' },
  { a: '⬡', ac: '#58d6e4', t: 'INSERT INTO code_chunks… ', vc: '#3fb950', v: '374 rows OK' },
]

type S = 'idle'|'active'|'done'

export default function GitLoader({ repoUrl, onDone }: { repoUrl: string; onDone: () => void }) {
  const { theme } = useStore()
  const d = theme === 'dark'
  const [steps, setSteps] = useState<S[]>(STEPS.map(()=>'idle'))
  const [logs, setLogs] = useState<typeof LOGS>([])
  const onDoneRef = useRef(onDone)

  useEffect(() => {
    onDoneRef.current = onDone
  }, [onDone])

  useEffect(() => {
    let cancelled = false
    let li = 0
    const logTimer = setInterval(() => {
      if (cancelled || li >= LOGS.length) { clearInterval(logTimer); return }
      const log = LOGS[li++]
      if (log) setLogs(l => [...l.slice(-3), log])
    }, 480)

    const run = async () => {
      for (let i = 0; i < STEPS.length; i++) {
        if (cancelled) return
        setSteps(s => s.map((x,idx) => idx===i ? 'active' : x))
        await new Promise(r => setTimeout(r, STEPS[i].dur))
        if (cancelled) return
        setSteps(s => s.map((x,idx) => idx===i ? 'done' : x))
        await new Promise(r => setTimeout(r, 80))
      }
      clearInterval(logTimer)
      if (!cancelled) setTimeout(() => onDoneRef.current(), 500)
    }
    run()
    return () => { cancelled = true; clearInterval(logTimer) }
  }, []) // empty deps — runs only once!

  const name = (() => { try { const p=new URL(repoUrl).pathname.split('/').filter(Boolean); return p[p.length-1] } catch { return 'repo' } })()

  return (
      <motion.div initial={{opacity:0,y:16}} animate={{opacity:1,y:0}}
                  className={`w-full max-w-[560px] rounded-xl border overflow-hidden shadow-2xl ${d?'bg-[#0e1117] border-[#1e2838] shadow-black/60':'bg-[#faf9f6] border-[#d5d0c8]'}`}>

        {/* Titlebar */}
        <div className={`flex items-center gap-2 px-4 py-2.5 border-b ${d?'bg-[#141920] border-[#1e2838]':'bg-[#eeecea] border-[#d5d0c8]'}`}>
          <div className="flex gap-1.5">
            {['#ff5f57','#febc2e','#28c840'].map(c=><div key={c} className="w-2.5 h-2.5 rounded-full" style={{background:c}}/>)}
          </div>
          <span className={`flex-1 text-center font-mono text-[10px] truncate ${d?'text-[#3d5068]':'text-[#9a9590]'}`}>
          $ git clone {repoUrl}
        </span>
          <motion.div animate={{opacity:[1,.3,1]}} transition={{duration:1,repeat:Infinity}}
                      className="w-2 h-2 rounded-full bg-[#e3a03c]"/>
        </div>

        {/* Steps */}
        <div className="px-5 py-4 flex flex-col">
          {STEPS.map((step, i) => {
            const s = steps[i]
            return (
                <div key={i} className="flex gap-3 items-start py-1.5 relative">
                  {i < STEPS.length-1 && (
                      <div className={`absolute left-[10px] top-7 w-px h-[calc(100%-4px)] ${d?'bg-[#1e2838]':'bg-[#d5d0c8]'}`}/>
                  )}
                  <div className={`w-[22px] h-[22px] rounded-full border flex items-center justify-center flex-shrink-0 z-10 transition-all duration-300
                ${s==='done' ? (d?'border-[#3fb950] bg-[#3fb950]/15':'border-[#1a7f37] bg-[#1a7f37]/10')
                      : s==='active' ? 'border-[#e3a03c] bg-[#e3a03c]/10'
                          : d?'border-[#263348] bg-[#0e1117]':'border-[#c5bfb5] bg-[#faf9f6]'}`}>
                    {s==='done' ? <CheckCircle size={12} color={d?'#3fb950':'#1a7f37'}/>
                        : s==='active' ? <motion.div animate={{rotate:360}} transition={{duration:1,repeat:Infinity,ease:'linear'}}><Loader size={11} color="#e3a03c"/></motion.div>
                            : <div className={`w-2 h-2 rounded-full ${d?'bg-[#1e2838]':'bg-[#c5bfb5]'}`}/>}
                  </div>
                  <div className="pt-0.5">
                    <div className={`font-mono text-[11px] transition-colors ${s==='done'?(d?'text-[#3fb950]':'text-[#1a7f37]'):s==='active'?'text-[#e3a03c]':(d?'text-[#3d5068]':'text-[#9a9590]')}`}>{step.label}</div>
                    <div className={`font-mono text-[9px] ${d?'text-[#3d5068]':'text-[#9a9590]'}`}>{step.sub}</div>
                  </div>
                </div>
            )
          })}
        </div>

        {/* Log terminal */}
        <div className="border-t bg-[#0d1117] px-4 py-2.5 h-[78px] overflow-hidden">
          <AnimatePresence mode="popLayout">
            {logs.filter(Boolean).map((l,i) => (
                <motion.div key={i+(l?.v||'')} initial={{opacity:0,x:-6}} animate={{opacity:1,x:0}} exit={{opacity:0}}
                            className="font-mono text-[10px] leading-relaxed flex gap-1">
                  <span style={{color:l.ac}}>{l.a} {l.t}</span>
                  <span style={{color:l.vc}}>{l.v}</span>
                </motion.div>
            ))}
          </AnimatePresence>
        </div>

        <div className={`px-4 py-2 border-t flex items-center gap-2 ${d?'bg-[#141920] border-[#1e2838]':'bg-[#eeecea] border-[#d5d0c8]'}`}>
          <span className="text-[#3fb950] text-[9px]">●</span>
          <span className={`font-mono text-[9px] ${d?'text-[#3d5068]':'text-[#9a9590]'}`}>HEAD → main · {name}</span>
        </div>
      </motion.div>
  )
}