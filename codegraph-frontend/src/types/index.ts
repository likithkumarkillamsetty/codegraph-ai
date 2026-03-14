export interface Project {
  id: number
  name: string
  githubUrl: string
  localPath?: string
  embedded: boolean
  hash: string
}

export interface AskResponse {
  answer: string
  showSnippets: boolean
}

export interface SearchResult {
  filePath?: string
  fileName?: string
  content?: string
  codeChunk?: string
  score?: number
  similarity?: number
}

export interface Message {
  id: string
  role: 'user' | 'ai' | 'system'
  content: string
  sources?: SearchResult[]
  timestamp: string
}
