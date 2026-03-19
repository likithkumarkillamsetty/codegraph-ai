# CodeGraph AI 🧠

<div align="center">

![CodeGraph AI Banner](https://img.shields.io/badge/CodeGraph-AI-1a56db?style=for-the-badge&logo=github&logoColor=white)
![Java](https://img.shields.io/badge/Java_21-ED8B00?style=for-the-badge&logo=openjdk&logoColor=white)
![Spring Boot](https://img.shields.io/badge/Spring_Boot_4-6DB33F?style=for-the-badge&logo=spring&logoColor=white)
![React](https://img.shields.io/badge/React_18-20232A?style=for-the-badge&logo=react&logoColor=61DAFB)
![TypeScript](https://img.shields.io/badge/TypeScript-007ACC?style=for-the-badge&logo=typescript&logoColor=white)
![PostgreSQL](https://img.shields.io/badge/PostgreSQL_15-316192?style=for-the-badge&logo=postgresql&logoColor=white)
![Docker](https://img.shields.io/badge/Docker-2496ED?style=for-the-badge&logo=docker&logoColor=white)

**An AI-powered Code Intelligence SaaS platform that lets you semantically search and chat with any GitHub repository using natural language.**

[🚀 Live Demo](#) · [📖 Documentation](#architecture) · [🐛 Report Bug](https://github.com/likithkumarkillamsetty/codegraph-ai/issues) · [✨ Request Feature](https://github.com/likithkumarkillamsetty/codegraph-ai/issues)

</div>

---

## 📌 Table of Contents

- [About The Project](#about-the-project)
- [Problem Statement](#problem-statement)
- [Features](#features)
- [Tech Stack](#tech-stack)
- [Architecture](#architecture)
- [How It Works](#how-it-works)
- [AI Pipeline Deep Dive](#ai-pipeline-deep-dive)
- [Project Structure](#project-structure)
- [Getting Started](#getting-started)
- [Environment Variables](#environment-variables)
- [API Reference](#api-reference)
- [Screenshots](#screenshots)
- [Roadmap](#roadmap)
- [Contributing](#contributing)
- [Author](#author)

---

## 🎯 About The Project

CodeGraph AI is a full-stack AI-powered code intelligence platform built from scratch. It allows developers to clone any public GitHub repository and ask natural language questions about the codebase — getting accurate, context-aware answers with relevant code snippets.

> **No LangChain. No wrappers. Pure custom RAG pipeline built in Java.**

---

## 🔥 Problem Statement

Software developers spend up to **58% of their time** reading and understanding existing code rather than writing new code. Key pain points:

- 🔴 New developers take **3–6 months** to onboard into a complex codebase
- 🔴 No tool exists that understands the **semantic meaning** of your code
- 🔴 GitHub search is **keyword-based** — it can't answer "how does auth work?"
- 🔴 ChatGPT/Claude have **no access** to your private or specific repository
- 🔴 Reading hundreds of files manually is **slow and error-prone**

**CodeGraph AI solves this** by letting you ask any question about any repository and get accurate, code-grounded answers in seconds.

---

## ✨ Features

| Feature | Description |
|---------|-------------|
| 🔍 **Semantic Code Search** | Vector similarity search using pgvector cosine distance |
| 🤖 **Natural Language Q&A** | Ask anything about the codebase in plain English |
| 📁 **Multi-file Support** | Scans `.java` `.properties` `.yml` `.xml` `.json` `.sql` `.md` `.txt` |
| ⚡ **Batch Embeddings** | Process 10 chunks per API call for fast indexing |
| 🧠 **Smart Intent Detection** | Distinguishes casual chat from code-related questions |
| 🎯 **Class-Level Retrieval** | Detect class names in queries and fetch directly |
| 📄 **Export Conversations** | Download chat as PDF or TXT |
| 🌙 **Dark / Light Theme** | Full theme support across all components |
| 📊 **Code Snippets** | View exact source code used to generate each answer |
| 🔒 **Secure API Keys** | Environment variable injection — no keys in code |

---

## 🛠️ Tech Stack

### Backend
| Technology | Version | Purpose |
|------------|---------|---------|
| Java | 21 | Core backend language |
| Spring Boot | 4.0.3 | REST API framework |
| PostgreSQL | 15 | Primary database |
| pgvector | latest | Vector similarity search extension |
| Spring Data JPA | 4.0.3 | ORM for entity management |
| Spring Security | 7.0.3 | Security configuration |
| JGit | 6.8.0 | GitHub repository cloning |
| HikariCP | 7.0.2 | Database connection pooling |
| Docker | latest | PostgreSQL containerization |
| Maven | 3.x | Build and dependency management |

### AI & ML
| Technology | Purpose |
|------------|---------|
| HuggingFace API (BAAI/bge-base-en-v1.5) | 768-dim vector embeddings for code chunks |
| Groq API (LLaMA 3.3 70B) | LLM for context-aware answer generation |
| pgvector | Cosine similarity search over stored embeddings |
| Custom RAG Pipeline | Retrieval-Augmented Generation built from scratch |

### Frontend
| Technology | Version | Purpose |
|------------|---------|---------|
| React | 18 | UI framework |
| TypeScript | 5.x | Type-safe JavaScript |
| Vite | 5.x | Build tool and dev server |
| Tailwind CSS | 3.x | Utility-first styling |
| Zustand | 4.x | Global state management |
| TanStack Query | 5.x | Server state and data fetching |
| Framer Motion | 11.x | Animations and transitions |
| react-markdown | 9.x | Render LLM responses as markdown |
| react-syntax-highlighter | 15.x | Syntax highlighting in snippets |
| jsPDF | 2.x | Client-side PDF export |
| Axios | 1.x | HTTP client for API calls |

---

## 🏗️ Architecture

```
┌─────────────────────────────────────────────────────────────────┐
│                         FRONTEND (React 18)                      │
│  Zustand Store │ TanStack Query │ Framer Motion │ react-markdown  │
└────────────────────────────┬────────────────────────────────────┘
                             │ HTTP (Vite Proxy /api → :8080)
┌────────────────────────────▼────────────────────────────────────┐
│                    BACKEND (Spring Boot 4)                       │
│                                                                  │
│  ProjectController  │  SearchService  │  EmbeddingService        │
│  ChunkStorageService │ CodeChunkService │ FileScannerService      │
│  OllamaChatService  │  ProjectService                            │
└──────────┬───────────────────────┬──────────────────────────────┘
           │                       │
┌──────────▼──────┐    ┌───────────▼──────────────────────────────┐
│  PostgreSQL 15  │    │            EXTERNAL APIS                  │
│  + pgvector     │    │                                           │
│                 │    │  HuggingFace API  →  BAAI/bge-base-en-v1.5│
│  projects       │    │  Groq API         →  LLaMA 3.3 70B        │
│  code_chunks    │    │  GitHub           →  JGit clone           │
│  (768-dim       │    │                                           │
│   vectors)      │    └───────────────────────────────────────────┘
└─────────────────┘
```

---

## ⚙️ How It Works

### Step 1 — Clone Repository
User pastes a GitHub URL. Backend clones the repo using JGit to local storage.

### Step 2 — Scan & Chunk
`FileScannerService` walks all directories finding supported files. `CodeChunkService` splits each file into semantic chunks (up to 1500 chars) on method boundaries.

### Step 3 — Generate Embeddings
`ChunkStorageService` batches 10 chunks at a time and calls HuggingFace API with `BAAI/bge-base-en-v1.5` to generate 768-dimensional float vectors.

### Step 4 — Store in pgvector
Each chunk is stored in PostgreSQL `code_chunks` table with its `file_path`, `content`, and `embedding vector(768)`.

### Step 5 — Ask a Question
User types a question. `SearchService` runs multi-layer intent detection:

```
Question received
      ↓
Is it casual chat? (no code keywords) → Answer directly via Groq, no RAG
      ↓
Does it mention a class name? (PascalCase word) → Fetch that class's chunks directly
      ↓
Does it mention a specific file? (.yml, .properties, etc.) → Fetch that file's chunks
      ↓
Is it a project-level question? (project, system, architecture) → Fetch config files + top vectors
      ↓
Run vector similarity search → Get top 3 most relevant chunks
      ↓
Build prompt with code context → Send to Groq LLaMA 3.3 70B
      ↓
Return answer + showSnippets flag to frontend
```

---

## 🤖 AI Pipeline Deep Dive

### Vector Embeddings
Text is converted to a 768-dimensional float vector using `BAAI/bge-base-en-v1.5`. Semantically similar texts produce vectors that are close together in high-dimensional space — enabling meaning-based search.

```
"How does authentication work?"  →  [0.12, -0.45, 0.89, ...]  (768 values)
"JWT filter in SecurityConfig"   →  [0.11, -0.44, 0.91, ...]  (very close!)
```

### Cosine Similarity Search (pgvector)
```sql
SELECT *, embedding <-> CAST(:embedding AS vector) AS similarity
FROM code_chunks
WHERE project_id = :projectId
ORDER BY embedding <-> CAST(:embedding AS vector)
LIMIT 10
```

The `<->` operator computes cosine distance — lower = more similar.

### Prompt Engineering
Three different prompt templates are used based on query type:

**Class Explanation:**
```
You are CodeGraph AI — an expert software engineer.
Explain what this class is responsible for, its methods, and how it fits the system.
[Code Context]
[User Question]
```

**Project Overview:**
```
You are CodeGraph AI. Explain the project — what it solves, technologies used, architecture.
Prioritize: pom.xml, application.properties, docker-compose.yml, README
[Context]
```

**General RAG:**
```
Answer briefly first. Then explain relevant code in detail.
Mention specific classes, methods, and files. Describe logic step-by-step.
Use ONLY provided code context. Do not invent code.
[Context]
```

---

## 📁 Project Structure

```
codegraph-ai/
├── codegraph-backend/
│   ├── src/main/java/com/likith/AI/Code/Intelligence/SaaS/
│   │   ├── controller/
│   │   │   └── ProjectController.java        # REST endpoints
│   │   ├── service/
│   │   │   ├── SearchService.java            # RAG pipeline + intent detection
│   │   │   ├── EmbeddingService.java         # HuggingFace API calls
│   │   │   ├── OllamaChatService.java        # Groq LLaMA API calls
│   │   │   ├── ChunkStorageService.java      # Store chunks + embeddings
│   │   │   ├── CodeChunkService.java         # Split files into chunks
│   │   │   ├── FileScannerService.java       # Scan repo files
│   │   │   └── ProjectService.java           # Project CRUD + cloning
│   │   ├── repository/
│   │   │   └── CodeChunkRepository.java      # pgvector similarity query
│   │   ├── entity/
│   │   │   ├── CodeChunkEntity.java          # code_chunks table mapping
│   │   │   └── Project.java                  # projects table mapping
│   │   ├── dto/
│   │   │   ├── AskResponse.java              # answer + showSnippets
│   │   │   ├── SearchResult.java             # filePath + content + similarity
│   │   │   └── CreateProjectRequest.java     # name + githubUrl
│   │   └── AiCodeIntelligenceSaaSApplication.java
│   ├── src/main/resources/
│   │   └── application.properties            # DB config + env variable refs
│   ├── docker-compose.yml                    # PostgreSQL + pgvector container
│   ├── .env.example                          # Template for required env vars
│   └── pom.xml
│
└── codegraph-frontend/
    ├── src/
    │   ├── components/
    │   │   ├── ChatInterface.tsx             # Chat UI + markdown rendering
    │   │   ├── SnippetCard.tsx               # Code snippet display
    │   │   ├── Sidebar.tsx                   # Repository list
    │   │   ├── GitLoader.tsx                 # Clone animation
    │   │   └── ExportButton.tsx              # PDF + TXT export
    │   ├── pages/
    │   │   └── Welcome.tsx                   # Clone repo form
    │   ├── store/
    │   │   └── useProjectStore.ts            # Zustand global state
    │   ├── api/
    │   │   └── index.ts                      # Axios API client
    │   └── main.tsx
    ├── vite.config.ts                        # Proxy /api → localhost:8080
    └── package.json
```

---

## 🚀 Getting Started

### Prerequisites

- Java 21+
- Docker Desktop
- Node.js 18+
- HuggingFace account → [Get free token](https://huggingface.co/settings/tokens)
- Groq account → [Get free API key](https://console.groq.com)

### Backend Setup

**1. Clone the repository**
```bash
git clone https://github.com/likithkumarkillamsetty/codegraph-ai.git
cd codegraph-ai/codegraph-backend
```

**2. Start PostgreSQL with pgvector**
```bash
docker-compose up -d
```

**3. Create `.env` file** (copy from `.env.example`)
```bash
HF_TOKEN=your_huggingface_token_here
GROQ_API_KEY=your_groq_api_key_here
```

**4. Set environment variables in IntelliJ**
```
Run → Edit Configurations → Environment Variables
Add: HF_TOKEN and GROQ_API_KEY
```

**5. Run the application**
```bash
./mvnw spring-boot:run
```
Backend starts at `http://localhost:8080`

### Frontend Setup

```bash
cd ../codegraph-frontend
npm install
npm run dev
```
Frontend starts at `http://localhost:5173`

### Docker Startup Order
```
1. Admin PowerShell: net stop postgresql-x64-18   (stop local postgres)
2. docker-compose up -d                            (start pgvector postgres)
3. Run Spring Boot in IntelliJ
4. npm run dev in frontend folder
```

---

## 🔑 Environment Variables

| Variable | Description | Where to Get |
|----------|-------------|--------------|
| `HF_TOKEN` | HuggingFace user access token for BAAI embeddings | [huggingface.co/settings/tokens](https://huggingface.co/settings/tokens) |
| `GROQ_API_KEY` | Groq API key for LLaMA 3.3 70B inference | [console.groq.com](https://console.groq.com) |

**application.properties references:**
```properties
hf.token=${HF_TOKEN}
groq.api.key=${GROQ_API_KEY}
```

---

## 📡 API Reference

### Create Project
```http
POST /api/projects
Content-Type: application/json

{
  "name": "my-repo",
  "githubUrl": "https://github.com/user/repo"
}
```

### Generate Embeddings
```http
POST /api/projects/{id}/embed
```

### Ask a Question (RAG)
```http
POST /api/projects/{id}/ask
Content-Type: application/json

{
  "question": "How does authentication work in this project?"
}
```

**Response:**
```json
{
  "answer": "The authentication is handled by...",
  "showSnippets": true
}
```

### Vector Search
```http
POST /api/projects/{id}/search
Content-Type: text/plain

How does the payment service work?
```

### List Files
```http
GET /api/projects/{id}/files
```

---

## 🗄️ Database Schema

```sql
-- Projects table
CREATE TABLE projects (
    id          BIGSERIAL PRIMARY KEY,
    name        VARCHAR(255),
    github_url  VARCHAR(255),
    local_path  VARCHAR(255),
    created_at  TIMESTAMP
);

-- Code chunks with vector embeddings
CREATE TABLE code_chunks (
    id          BIGSERIAL PRIMARY KEY,
    project_id  BIGINT REFERENCES projects(id),
    file_path   TEXT,
    content     TEXT,
    embedding   vector(768)    -- pgvector 768-dim float array
);

-- Vector similarity index
CREATE INDEX ON code_chunks USING ivfflat (embedding vector_cosine_ops);
```

---

## 🖼️ Screenshots

> Add screenshots of your running application here

| Clone Repository | Chat Interface | Code Snippets |
|-----------------|----------------|---------------|
| *(screenshot)* | *(screenshot)* | *(screenshot)* |

---

## 🗺️ Roadmap

- [x] Custom RAG pipeline in Java
- [x] HuggingFace embeddings (BAAI/bge-base-en-v1.5)
- [x] Groq LLaMA 3.3 70B integration
- [x] Multi-layer intent detection
- [x] Batch embedding
- [x] PDF/TXT export
- [x] Dark/Light theme
- [ ] Delete repository button
- [ ] Deployment on Railway + Vercel
- [ ] User authentication (JWT)
- [ ] Private GitHub repo support (OAuth)
- [ ] Streaming LLM responses
- [ ] Conversation history persistence
- [ ] Support for Python, JavaScript, TypeScript files

---

## 🤝 Contributing

Contributions are welcome!

1. Fork the repository
2. Create your feature branch (`git checkout -b feature/AmazingFeature`)
3. Commit your changes (`git commit -m 'Add AmazingFeature'`)
4. Push to the branch (`git push origin feature/AmazingFeature`)
5. Open a Pull Request

---

## 👨‍💻 Author

**Likith Kumar Killamsetty**

- 🎓 B.Tech CSE — ANITS Visakhapatnam (CGPA: 9.39)
- 📧 killamsettylikithkumar@gmail.com
- 📱 +91 7675925110
- 💼 [LinkedIn](https://www.linkedin.com/in/likithkumarkillamsetty/)
- 🐙 [GitHub](https://github.com/likithkumarkillamsetty)
- 💻 [LeetCode](https://leetcode.com/u/likithkumarkillamsetty/)

---

## 📄 License

This project is open source and available under the [MIT License](LICENSE).

---

<div align="center">

**⭐ Star this repository if you found it helpful!**

Made with ❤️ by [Likith Kumar Killamsetty](https://github.com/likithkumarkillamsetty)

</div>
