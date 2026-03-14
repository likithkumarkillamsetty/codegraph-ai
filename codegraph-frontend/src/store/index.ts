import { create } from 'zustand'
import { persist } from 'zustand/middleware'
import type { Project, Message } from '../types'

function nanoid() { return Math.random().toString(36).slice(2, 9) }

interface Store {
  theme: 'dark' | 'light'
  toggleTheme: () => void
  projects: Project[]
  activeProjectId: number | null
  messages: Record<number, Message[]>
  isThinking: boolean
  isEmbedding: boolean
  addProject: (p: Omit<Project, 'hash' | 'embedded'>) => void
  markEmbedded: (id: number) => void
  setActiveProject: (id: number | null) => void
  addMessage: (projectId: number, msg: Omit<Message, 'id' | 'timestamp'>) => void
  setThinking: (v: boolean) => void
  setEmbedding: (v: boolean) => void
}

export const useStore = create<Store>()(
  persist(
    (set, get) => ({
      theme: 'dark',
      toggleTheme: () => {
        const next = get().theme === 'dark' ? 'light' : 'dark'
        set({ theme: next })
        document.documentElement.classList.toggle('dark', next === 'dark')
      },
      projects: [],
      activeProjectId: null,
      messages: {},
      isThinking: false,
      isEmbedding: false,
      addProject: (p) => set(s => ({
        projects: [{ ...p, hash: nanoid(), embedded: false }, ...s.projects.filter(x => x.id !== p.id)],
        activeProjectId: p.id
      })),
      markEmbedded: (id) => set(s => ({
        projects: s.projects.map(p => p.id === id ? { ...p, embedded: true } : p)
      })),
      setActiveProject: (id) => set({ activeProjectId: id }),
      addMessage: (projectId, msg) => set(s => ({
        messages: {
          ...s.messages,
          [projectId]: [...(s.messages[projectId] || []), {
            ...msg, id: nanoid(), timestamp: new Date().toISOString()
          }]
        }
      })),
      setThinking: (v) => set({ isThinking: v }),
      setEmbedding: (v) => set({ isEmbedding: v }),
    }),
    {
      name: 'codegraph-store',
      partialize: s => ({ theme: s.theme, projects: s.projects, messages: s.messages, activeProjectId: s.activeProjectId })
    }
  )
)
