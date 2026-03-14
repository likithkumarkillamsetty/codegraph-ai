# CodeGraph AI — Frontend

AI-powered GitHub repository analyzer using RAG (Retrieval-Augmented Generation).

## Tech Stack
- **React 18** + **Vite** + **TypeScript**
- **Tailwind CSS** — styling
- **Zustand** — state management (persisted)
- **@tanstack/react-query** — API calls
- **Framer Motion** — animations
- **react-syntax-highlighter** — code blocks
- **jspdf** — PDF export

## Setup

### Prerequisites
Make sure these are running before starting the frontend:
```bash
# 1. PostgreSQL via Docker
docker-compose up -d

# 2. Ollama with models
ollama serve
ollama pull nomic-embed-text
ollama pull gemma

# 3. Spring Boot backend (IntelliJ or terminal)
# Runs on http://localhost:8080
```

### Run frontend
```bash
npm install
npm run dev
# Opens at http://localhost:5173
```

### Build for production
```bash
npm run build
```

## Deployment (Vercel)
1. Push this folder to GitHub
2. Import to vercel.com
3. Update `vercel.json` with your deployed backend URL
4. Deploy

## API Endpoints Used
| Method | Endpoint | Purpose |
|--------|----------|---------|
| POST | `/api/projects` | Create project from GitHub URL |
| POST | `/api/projects/{id}/embed` | Generate vector embeddings |
| POST | `/api/projects/{id}/ask` | Ask question (RAG pipeline) |
| POST | `/api/projects/{id}/search` | Vector similarity search |
| GET  | `/api/projects/{id}/files` | List repository files |
