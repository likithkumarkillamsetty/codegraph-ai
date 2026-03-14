import { useState } from 'react'
import { Download } from 'lucide-react'
import { motion } from 'framer-motion'
import toast from 'react-hot-toast'
import { useStore } from '../store'
import type { Project } from '../types'

export default function ExportButton({ project }: { project: Project }) {
  const { messages, theme } = useStore()
  const [open, setOpen] = useState(false)
  const d = theme === 'dark'
  const msgs = messages[project.id] || []

  const exportTxt = () => {
    const lines = [
      `CodeGraph AI — Chat Export`,
      `Repository: ${project.name} (${project.githubUrl})`,
      `Exported: ${new Date().toLocaleString()}`,
      '─'.repeat(60),
      '',
      ...msgs.map(m => [
        `[${m.role.toUpperCase()}] ${new Date(m.timestamp).toLocaleTimeString()}`,
        m.content,
        m.sources?.length ? `Sources: ${m.sources.map(s=>s.filePath||'chunk').join(', ')}` : '',
        ''
      ].filter(Boolean).join('\n'))
    ].join('\n')

    const blob = new Blob([lines], { type: 'text/plain' })
    const a = document.createElement('a')
    a.href = URL.createObjectURL(blob)
    a.download = `codegraph-${project.name}-${Date.now()}.txt`
    a.click()
    toast.success('Chat exported as .txt!')
    setOpen(false)
  }

  const exportPdf = async () => {
    try {
      const jsPDFModule = await import('jspdf')
      const jsPDF = jsPDFModule.default || jsPDFModule.jsPDF
      const doc = new jsPDF({ unit: 'mm', format: 'a4' })
      const pageW = doc.internal.pageSize.getWidth()
      let y = 20

      doc.setFontSize(20)
      doc.setTextColor(63, 185, 80)
      doc.text('CodeGraph AI', 20, y); y += 10

      doc.setFontSize(10)
      doc.setTextColor(100, 100, 100)
      doc.text(`Repository: ${project.name}`, 20, y); y += 6
      doc.text(`URL: ${project.githubUrl}`, 20, y); y += 6
      doc.text(`Exported: ${new Date().toLocaleString()}`, 20, y); y += 10

      doc.line(20, y, pageW - 20, y); y += 8

      for (const msg of msgs) {
        if (y > 270) { doc.addPage(); y = 20 }
        doc.setFontSize(8)
        doc.setTextColor(100, 100, 100)
        doc.text(`${msg.role.toUpperCase()} — ${new Date(msg.timestamp).toLocaleTimeString()}`, 20, y); y += 5

        doc.setFontSize(10)
        doc.setTextColor(50, 50, 50)
        const lines = doc.splitTextToSize(msg.content, pageW - 40)
        for (const line of lines) {
          if (y > 275) { doc.addPage(); y = 20 }
          doc.text(line, 20, y); y += 5
        }
        y += 5
      }

      doc.save(`codegraph-${project.name}-${Date.now()}.pdf`)
      toast.success('Chat exported as PDF!')
      setOpen(false)
    } catch (e) {
      console.error('PDF error:', e)
      toast.error('PDF export failed')
    }
  }

  if (!msgs.length) return null

  return (
    <div className="relative">
      <motion.button whileHover={{scale:1.02}} whileTap={{scale:.97}} onClick={()=>setOpen(o=>!o)}
        className={`flex items-center gap-1.5 px-3 py-1.5 rounded-md border font-mono text-[10px] transition-all ${d?'border-[#263348] text-[#7a8fa8] hover:border-[#58d6e4] hover:text-[#58d6e4]':'border-[#c5bfb5] text-[#5a5650] hover:border-[#0d6e7a] hover:text-[#0d6e7a]'}`}>
        <Download size={11}/> Export
      </motion.button>

      {open && (
        <>
          <div className="fixed inset-0 z-10" onClick={()=>setOpen(false)}/>
          <motion.div initial={{opacity:0,y:4}} animate={{opacity:1,y:0}}
            className={`absolute right-0 top-9 z-20 rounded-lg border shadow-xl overflow-hidden min-w-[140px] ${d?'bg-[#141920] border-[#263348]':'bg-[#faf9f6] border-[#c5bfb5]'}`}>
            {[
              { label: '📄 Export as .txt', fn: exportTxt },
              { label: '📑 Export as PDF', fn: exportPdf },
            ].map(item => (
              <button key={item.label} onClick={item.fn}
                className={`w-full text-left px-4 py-2.5 font-mono text-[11px] transition-colors ${d?'text-[#cdd9e5] hover:bg-[#1a2130]':'text-[#1c1c1a] hover:bg-[#eeecea]'}`}>
                {item.label}
              </button>
            ))}
          </motion.div>
        </>
      )}
    </div>
  )
}
