import { useRef, useEffect, useState } from 'react'
import { motion, AnimatePresence } from 'framer-motion'
import { useMutation } from '@tanstack/react-query'
import toast from 'react-hot-toast'
import { Send, Layers, CheckCheck, GitCommit, GitBranch } from 'lucide-react'
import { projectApi } from '../api'
import { useStore } from '../store'
import SnippetCard from './SnippetCard'
import ExportButton from './ExportButton'
import type { Message } from '../types'
import ReactMarkdown from 'react-markdown'
const SUGGESTIONS = [
  'What does this project do?','List all REST API endpoints',
  'Explain the database schema','How is authentication handled?',
  'What design patterns are used?','Explain the RAG pipeline',
]

export default function ChatInterface() {
  const { theme, activeProjectId, projects, messages, addMessage, isThinking, setThinking, isEmbedding, setEmbedding, markEmbedded } = useStore()
  const bottomRef = useRef<HTMLDivElement>(null)
  const taRef = useRef<HTMLTextAreaElement>(null)
  const [input, setInput] = useState('')
  const d = theme === 'dark'

  const project = projects.find(p => p.id === activeProjectId)
  const msgs: Message[] = activeProjectId ? (messages[activeProjectId] || []) : []

  useEffect(() => { bottomRef.current?.scrollIntoView({ behavior: 'smooth' }) }, [msgs, isThinking])

  const embedMut = useMutation({
    mutationFn: async () => {
      if (!project) throw new Error('No project')
      setEmbedding(true)
      return projectApi.embed(project.id)
    },
    onSuccess: () => {
      if (project) markEmbedded(project.id)
      setEmbedding(false)
      toast.success('Embeddings ready — chat enabled!')
      if (activeProjectId) addMessage(activeProjectId, {
        role: 'system',
        content: '⬡ Vector embeddings generated via nomic-embed-text. pgvector indexed. RAG pipeline is ready — ask anything!',
      })
    },
    onError: (e: Error) => { setEmbedding(false); toast.error(`Embedding failed: ${e.message}`) },
  })

  const askMut = useMutation({
    mutationFn: async (q: string) => {
      if (!project) throw new Error('No project')

      // First call ask API
      const response = await projectApi.ask(project.id, q)

      let sources: any[] = []

      // Only fetch snippets if backend says they are relevant
      if (response.showSnippets) {
        try {
          sources = await projectApi.search(project.id, q)
        } catch {
          sources = []
        }
      }

      return {
        answer: response.answer,
        sources: sources.slice(0, 3),
      }
    },

    onSuccess: ({ answer, sources }) => {
      setThinking(false)

      if (activeProjectId)
        addMessage(activeProjectId, {
          role: 'ai',
          content: answer,
          sources: sources,
        })
    },

    onError: (e: Error) => {
      setThinking(false)
      if (activeProjectId)
        addMessage(activeProjectId, {
          role: 'system',
          content: `✖ Error: ${e.message}`,
        })
    },
  })

  const send = () => {
    const q = input.trim()
    if (!q || isThinking || !project?.embedded) return
    setInput('')
    if (taRef.current) taRef.current.style.height = 'auto'
    if (activeProjectId) addMessage(activeProjectId, { role: 'user', content: q })
    setThinking(true)
    askMut.mutate(q)
  }

  if (!project) return null

  const nodeColor = { user: d?'#c19dff':'#6840c0', ai: d?'#58d6e4':'#0d6e7a', system: d?'#e3a03c':'#b56b00' }

  return (
    <div className={`flex-1 flex flex-col overflow-hidden ${d?'bg-[#090b0e]':'bg-[#f4f2ed]'}`}>

      {/* Header */}
      <div className={`flex items-center gap-3 px-5 py-2.5 border-b flex-shrink-0 flex-wrap gap-y-2 ${d?'bg-[#0e1117] border-[#1e2838]':'bg-[#faf9f6] border-[#d5d0c8]'}`}>
        <div className={`flex items-center gap-2 px-3 py-1.5 rounded-md border font-mono text-[11px] ${d?'bg-[#141920] border-[#263348]':'bg-[#eeecea] border-[#c5bfb5]'}`}>
          <motion.div animate={{opacity:[1,.3,1]}} transition={{duration:2,repeat:Infinity}} className={`w-1.5 h-1.5 rounded-full ${d?'bg-[#3fb950]':'bg-[#1a7f37]'}`}/>
          <span className={d?'text-[#3d5068]':'text-[#9a9590]'}>~/</span>
          <span className={d?'text-[#cdd9e5]':'text-[#1c1c1a]'}>{project.name}</span>
        </div>
        <div className={`flex items-center gap-1.5 font-mono text-[10px] ${d?'text-[#3d5068]':'text-[#9a9590]'}`}>
          <GitCommit size={10}/>{project.hash}
        </div>
        <div className="flex-1"/>
        <ExportButton project={project}/>
        <motion.button whileHover={{scale:1.02}} whileTap={{scale:.97}}
          onClick={() => !project.embedded && !isEmbedding && embedMut.mutate()}
          disabled={project.embedded || isEmbedding}
          className={`flex items-center gap-1.5 px-3 py-1.5 rounded-md border font-mono text-[10px] transition-all
            ${project.embedded
              ? (d?'border-[#3fb950]/40 text-[#3fb950] cursor-default':'border-[#1a7f37]/40 text-[#1a7f37] cursor-default')
              : isEmbedding
                ? 'border-[#e3a03c]/40 text-[#e3a03c]'
                : (d?'border-[#263348] text-[#7a8fa8] hover:border-[#3fb950] hover:text-[#3fb950]':'border-[#c5bfb5] text-[#5a5650] hover:border-[#1a7f37] hover:text-[#1a7f37]')}`}>
          {project.embedded ? <><CheckCheck size={11}/>indexed</>
            : isEmbedding ? <><motion.div animate={{rotate:360}} transition={{duration:1,repeat:Infinity,ease:'linear'}}><Layers size={11}/></motion.div>embedding…</>
            : <><Layers size={11}/>generate embeddings</>}
        </motion.button>
        <div className={`flex items-center gap-1.5 font-mono text-[10px] ${d?'text-[#3d5068]':'text-[#9a9590]'}`}>
          <GitBranch size={10}/>main
        </div>
      </div>

      {/* Embed progress */}
      <AnimatePresence>
        {isEmbedding && (
          <motion.div initial={{height:0}} animate={{height:'auto'}} exit={{height:0}}
            className={`border-b overflow-hidden ${d?'bg-[#0e1117] border-[#1e2838]':'bg-[#faf9f6] border-[#d5d0c8]'}`}>
            <div className="px-5 py-2 flex flex-col gap-1.5">
              <div className={`font-mono text-[10px] ${d?'text-[#7a8fa8]':'text-[#5a5650]'}`}>Generating vector embeddings via Ollama (nomic-embed-text)…</div>
              <div className={`h-0.5 rounded-full overflow-hidden ${d?'bg-[#1e2838]':'bg-[#d5d0c8]'}`}>
                <motion.div className="h-full rounded-full bg-gradient-to-r from-[#3fb950] to-[#58d6e4]"
                  animate={{width:['0%','85%']}} transition={{duration:8,ease:'easeInOut'}}/>
              </div>
            </div>
          </motion.div>
        )}
      </AnimatePresence>

      {/* Messages */}
      <div className="flex-1 overflow-y-auto px-5 py-5 flex flex-col gap-4">
        {msgs.length === 0 ? (
          <motion.div initial={{opacity:0}} animate={{opacity:1}} className="flex-1 flex flex-col items-center justify-center gap-4 py-16">
            <div className={`text-5xl ${d?'opacity-10':'opacity-15'}`}>⬡</div>
            <div className={`font-mono text-[12px] ${d?'text-[#3d5068]':'text-[#9a9590]'}`}>
              {project.embedded ? 'Ask anything about this codebase' : 'Generate embeddings to enable chat'}
            </div>
            {project.embedded && (
              <div className="flex flex-wrap gap-2 justify-center max-w-lg mt-2">
                {SUGGESTIONS.map(s=>(
                  <button key={s} onClick={()=>{setInput(s);taRef.current?.focus()}}
                    className={`px-3 py-1.5 rounded-full border text-[11px] transition-all ${d?'border-[#263348] text-[#7a8fa8] hover:border-[#58d6e4] hover:text-[#58d6e4] bg-[#0e1117]':'border-[#c5bfb5] text-[#5a5650] hover:border-[#0d6e7a] hover:text-[#0d6e7a] bg-[#faf9f6]'}`}>
                    {s}
                  </button>
                ))}
              </div>
            )}
          </motion.div>
        ) : (
          <>
            {msgs.map((msg, i) => {
              const isLast = i === msgs.length - 1
              const color = nodeColor[msg.role]
              const time = new Date(msg.timestamp).toLocaleTimeString([],{hour:'2-digit',minute:'2-digit'})
              return (
                <motion.div key={msg.id} initial={{opacity:0,y:8}} animate={{opacity:1,y:0}} transition={{duration:.3}} className="flex gap-3">
                  <div className="flex flex-col items-center pt-1 flex-shrink-0">
                    <div className="w-2.5 h-2.5 rounded-full flex-shrink-0" style={{background:color,boxShadow:`0 0 6px ${color}50`}}/>
                    {!isLast && <div className={`w-px flex-1 min-h-3 mt-1 ${d?'bg-[#1e2838]':'bg-[#d5d0c8]'}`}/>}
                  </div>
                  <div className="flex-1 min-w-0 pb-1">
                    <div className={`font-mono text-[9px] mb-1.5 flex gap-2 ${d?'text-[#3d5068]':'text-[#9a9590]'}`}>
                      <span className={d?'text-[#7a8fa8]':'text-[#5a5650]'}>
                        {msg.role==='user'?'you':msg.role==='ai'?'codegraph·ai':'system'}
                      </span>
                      <span>{time}</span>
                    </div>
                    <div className={`rounded-lg px-4 py-3 text-[13px] leading-relaxed border
                      ${msg.role==='user'?(d?'bg-[#141920] border-[#c19dff]/20':'bg-[#eeecea] border-[#6840c0]/20')
                      :msg.role==='system'?(d?'bg-[#e3a03c]/5 border-[#e3a03c]/20 font-mono text-[11px] text-[#e3a03c]':'bg-[#b56b00]/5 border-[#b56b00]/20 font-mono text-[11px] text-[#b56b00]')
                      :(d?'bg-[#0e1117] border-[#58d6e4]/15':'bg-[#faf9f6] border-[#0d6e7a]/15')}
                      ${d?'text-[#cdd9e5]':'text-[#1c1c1a]'}`}>
                      <ReactMarkdown>{msg.content}</ReactMarkdown>
                    </div>

                    {/* Sources */}
                    {msg.sources && msg.sources.length > 0 && (
                      <div className="mt-2 space-y-2">
                        <div className={`font-mono text-[9px] flex items-center gap-2 ${d?'text-[#3d5068]':'text-[#9a9590]'}`}>
                          <span>◈</span>
                          <span>{msg.sources.length} relevant snippet{msg.sources.length>1?'s':''} retrieved from codebase</span>
                        </div>
                        {msg.sources.map((s,si)=>(
                          <SnippetCard key={si} snippet={s} index={si}/>
                        ))}
                      </div>
                    )}
                  </div>
                </motion.div>
              )
            })}

            {/* Thinking */}
            {isThinking && (
              <motion.div initial={{opacity:0,y:8}} animate={{opacity:1,y:0}} className="flex gap-3">
                <div className="flex flex-col items-center pt-1 flex-shrink-0">
                  <motion.div animate={{opacity:[1,.3,1]}} transition={{duration:1,repeat:Infinity}}
                    className="w-2.5 h-2.5 rounded-full" style={{background:d?'#58d6e4':'#0d6e7a'}}/>
                </div>
                <div>
                  <div className={`font-mono text-[9px] mb-1.5 ${d?'text-[#7a8fa8]':'text-[#5a5650]'}`}>codegraph·ai</div>
                  <div className={`rounded-lg px-4 py-3 border inline-flex items-center gap-2 ${d?'bg-[#0e1117] border-[#58d6e4]/15':'bg-[#faf9f6] border-[#0d6e7a]/15'}`}>
                    <span className={`font-mono text-[11px] ${d?'text-[#7a8fa8]':'text-[#5a5650]'}`}>searching codebase</span>
                    <div className="flex gap-1">
                      {[0,1,2].map(i=>(
                        <motion.div key={i} className={`w-1 h-1 rounded-full ${d?'bg-[#58d6e4]':'bg-[#0d6e7a]'}`}
                          animate={{opacity:[.2,1,.2]}} transition={{duration:1.2,repeat:Infinity,delay:i*.2}}/>
                      ))}
                    </div>
                  </div>
                </div>
              </motion.div>
            )}
          </>
        )}
        <div ref={bottomRef}/>
      </div>

      {/* Input */}
      <div className={`px-5 pb-4 pt-3 border-t flex-shrink-0 ${d?'bg-[#0e1117] border-[#1e2838]':'bg-[#faf9f6] border-[#d5d0c8]'}`}>
        <div className={`flex items-end gap-3 rounded-xl border px-4 py-3 transition-all focus-within:ring-2
          ${d?'bg-[#141920] border-[#263348] focus-within:border-[#58d6e4] focus-within:ring-[#58d6e4]/15':'bg-[#eeecea] border-[#c5bfb5] focus-within:border-[#0d6e7a] focus-within:ring-[#0d6e7a]/10'}`}>
          <span className={`font-mono text-[16px] pb-0.5 flex-shrink-0 ${d?'text-[#3fb950]':'text-[#1a7f37]'}`}>›</span>
          <textarea ref={taRef} rows={1} value={input}
            onChange={e=>{setInput(e.target.value);e.target.style.height='auto';e.target.style.height=Math.min(e.target.scrollHeight,120)+'px'}}
            onKeyDown={e=>{if(e.key==='Enter'&&!e.shiftKey){e.preventDefault();send()}}}
            placeholder={project.embedded?'Ask anything about this codebase…':'Generate embeddings first to enable chat'}
            disabled={!project.embedded||isThinking}
            className={`flex-1 bg-transparent outline-none resize-none text-[13px] leading-relaxed max-h-[120px] overflow-y-auto disabled:opacity-50 disabled:cursor-not-allowed ${d?'text-[#cdd9e5] caret-[#58d6e4] placeholder:text-[#3d5068]':'text-[#1c1c1a] caret-[#0d6e7a] placeholder:text-[#9a9590]'}`}/>
          <motion.button whileHover={{scale:1.05}} whileTap={{scale:.95}} onClick={send}
            disabled={!project.embedded||isThinking||!input.trim()}
            className={`w-8 h-8 rounded-lg flex items-center justify-center flex-shrink-0 transition-all disabled:opacity-30 disabled:cursor-not-allowed ${d?'bg-[#58d6e4] text-black':'bg-[#0d6e7a] text-white'}`}>
            <Send size={13}/>
          </motion.button>
        </div>
        <div className={`font-mono text-[9px] mt-1.5 px-1 ${d?'text-[#3d5068]':'text-[#9a9590]'}`}>
          Enter to send · Shift+Enter for newline · powered by Gemma via Ollama
        </div>
      </div>
    </div>
  )
}
