import { useState } from 'react'
import { motion, AnimatePresence } from 'framer-motion'
import { ChevronRight, FileCode, Copy, Check } from 'lucide-react'
import { Prism as SyntaxHighlighter } from 'react-syntax-highlighter'
import { oneDark, oneLight } from 'react-syntax-highlighter/dist/esm/styles/prism'
import { useStore } from '../store'
import type { SearchResult } from '../types'

export default function SnippetCard({ snippet, index }: { snippet: SearchResult; index: number }) {
  const { theme } = useStore()
  const d = theme === 'dark'
  const [open, setOpen] = useState(index === 0)
  const [copied, setCopied] = useState(false)

  const fileName = (snippet.filePath || snippet.fileName || '').split(/[/\\]/).pop() || `chunk-${index+1}`
  const code = snippet.content || snippet.codeChunk || ''

  const copy = async (e: React.MouseEvent) => {
    e.stopPropagation()
    await navigator.clipboard.writeText(code)
    setCopied(true); setTimeout(()=>setCopied(false), 2000)
  }

  return (
      <div className={`rounded-lg border overflow-hidden ${d?'border-[#263348]':'border-[#c5bfb5]'}`}>
        <button onClick={()=>setOpen(o=>!o)}
                className={`w-full flex items-center gap-2 px-3 py-2 text-left transition-colors ${d?'bg-[#141920] hover:bg-[#1a2130]':'bg-[#eeecea] hover:bg-[#e5e2dc]'}`}>
          <FileCode size={11} color={d?'#58d6e4':'#0d6e7a'}/>
          <span className={`font-mono text-[10px] flex-1 truncate ${d?'text-[#58d6e4]':'text-[#0d6e7a]'}`}>{fileName}</span>
          {snippet.filePath && (
              <span className={`font-mono text-[9px] truncate max-w-[160px] hidden sm:block ${d?'text-[#3d5068]':'text-[#9a9590]'}`}>{snippet.filePath}</span>
          )}
          <div onClick={copy} className={`flex-shrink-0 p-1 rounded transition-colors cursor-pointer ${d?'hover:bg-[#263348] text-[#3d5068]':'hover:bg-[#d5d0c8] text-[#9a9590]'}`}>
            {copied ? <Check size={11} color="#3fb950"/> : <Copy size={11}/>}
          </div>
          <motion.div animate={{rotate:open?90:0}} transition={{duration:.2}}>
            <ChevronRight size={12} color={d?'#3d5068':'#9a9590'}/>
          </motion.div>
        </button>

        <AnimatePresence initial={false}>
          {open && (
              <motion.div initial={{height:0}} animate={{height:'auto'}} exit={{height:0}}
                          transition={{duration:.25,ease:'easeInOut'}} style={{overflow:'hidden'}}>
                <div className="max-h-60 overflow-auto">
                  <SyntaxHighlighter language="java" style={d ? oneDark : oneLight}
                                     customStyle={{margin:0,padding:'12px 14px',fontSize:'11px',lineHeight:'1.7',background:d?'#0d1117':'#f8f8f8',borderRadius:0}}>
                    {code || '// No code content'}
                  </SyntaxHighlighter>
                </div>
              </motion.div>
          )}
        </AnimatePresence>
      </div>
  )
}