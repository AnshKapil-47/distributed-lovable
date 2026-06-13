<div align="center">

# 🚀 Distributed Lovable Clone

### AI-Powered Full Stack Development Platform

</div>

---

## 🌟 Overview

Distributed Lovable Clone is a cloud-native AI application builder inspired by Lovable.

Users can:

- 🤖 Generate applications using AI
- 💬 Chat with AI assistants
- 📁 Manage project workspaces
- 📝 Edit source code
- 🚀 Deploy projects instantly
- 🌐 Preview applications in real time
- 🔒 Securely manage authentication

The platform is built using a scalable microservices architecture running on Kubernetes.

---

# ✨ Features

### 🤖 AI Development

- AI-powered code generation
- Chat-based project creation
- Context-aware modifications
- Streaming AI responses
- Multi-file generation support

### 🔐 Authentication & Security

- JWT Authentication
- Role-based Authorization
- Secure API Gateway
- Protected Microservices
- Token Validation

### 📁 Workspace Management

- Project creation
- File management
- Source code editing
- Workspace synchronization
- Project version tracking

### 🚀 Deployment & Preview

- One-click deployment
- Preview environments
- Dockerized applications
- Real-time build status
- Automated container execution

### ☁️ Cloud Native

- Kubernetes deployment
- Horizontal scalability - Kafka
- Service discovery
- Centralized configuration
- Fault isolation

---

# 🏗 Architecture

```text
                    ┌───────────────┐
                    │   Frontend    │
                    │ React + Vite  │
                    └───────┬───────┘
                            │
                            ▼
                  ┌──────────────────┐
                  │   API Gateway    │
                  └────────┬─────────┘
                           │
      ┌────────────────────┼────────────────────┐
      ▼                    ▼                    ▼

┌────────────┐    ┌──────────────┐    ┌──────────────┐
│ Auth       │    │ Workspace    │    │ Intelligence │
│ Service    │    │ Service      │    │ Service      │
└─────┬──────┘    └──────┬───────┘    └──────┬───────┘
      │                  │                   │
      ▼                  ▼                   ▼

 ┌──────────┐      ┌──────────┐      ┌──────────┐
 │PostgreSQL│      │PostgreSQL│      │ OpenAI   │
 └──────────┘      └──────────┘      └──────────┘

                           │
                           ▼

                 ┌─────────────────┐
                 │ Preview Runner  │
                 └─────────────────┘
```

---

# 🧩 Microservices

## 🔐 Account Service

Handles:

- User registration
- Login
- JWT generation
- User management

---

## 🧠 Intelligence Service

Handles:

- AI communication
- Prompt processing
- Response streaming
- Code generation

---

## 📁 Workspace Service

Handles:

- Projects
- Files
- Workspace operations
- Project persistence

---

## 🌐 API Gateway

Handles:

- Routing
- Authentication
- Load balancing
- Request filtering

---

## ⚙️ Config Server

Handles:

- Centralized configuration
- Environment management
- Dynamic property loading

---

## 🏃 Preview Runner

Handles:

- Application builds
- Container execution
- Live preview environments

---

# 🛠 Tech Stack

## Backend

- Java 21
- Spring Boot
- Spring Security
- Spring Ai
- Spring Cloud Gateway
- Spring Data JPA
- Hibernate

## Database

- PostgreSQL
- Redis

## AI

- OpenAI
- OpenRouter

## DevOps

- Docker
- Kubernetes
- Kafka
- GKE
- GitHub Actions


---

# 🔄 CI/CD Pipeline

GitHub Actions automatically performs:

- Build Application
- Run Tests
- Create Docker Images
- Push Images to Docker Hub
- Deploy to Kubernetes Cluster

 GitHub Push
     │
     ▼
 GitHub Actions
     │
     ▼
 Build & Test
     │
     ▼
 Docker Image
     │
     ▼
 Docker Hub
     │
     ▼
 Kubernetes Deployment

---

# 🔐 Security

- JWT Authentication
- Secure API Gateway
- Environment Variables
- Kubernetes Secrets
- Protected Endpoints

---

# 💳 Stripe Payment Integration
- Payment intent creation
- Webhook handling
- Secure key management via environment variables
