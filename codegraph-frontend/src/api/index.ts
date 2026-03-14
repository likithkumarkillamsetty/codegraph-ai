import axios from 'axios'
import type {AskResponse, Project, SearchResult} from '../types'

const api = axios.create({ baseURL: '/api', timeout: 120000 })

export const projectApi = {
  create: (name: string, githubUrl: string): Promise<Project> =>
    api.post('/projects', { name, githubUrl }).then(r => r.data),

  embed: (id: number): Promise<string> =>
    api.post(`/projects/${id}/embed`).then(r => r.data),

    ask: (projectId: number, question: string): Promise<AskResponse> =>
    axios.post(`/api/projects/${projectId}/ask`, { question }).then(r => r.data),

  search: (id: number, query: string): Promise<SearchResult[]> =>
    api.post(`/projects/${id}/search`, query, {
      headers: { 'Content-Type': 'text/plain' }
    }).then(r => r.data),

  getFiles: (id: number): Promise<string[]> =>
    api.get(`/projects/${id}/files`).then(r => r.data),
}
