# Bladna – Eco-Civic Platform

## Overview

**Bladna** (Arabic: بلدنا — *our country*) is a full-stack environmental civic platform developed as part of the **PIDEV – 3rd Year Engineering Program** at **Esprit School of Engineering** (Academic Year 2025–2026).

The platform connects citizens around shared environmental goals: reporting and logging waste collections, organizing eco-friendly community events, raising donations, and participating in a moderated forum — all powered by AI-driven services and modern cloud integrations.

---

## Features

### Waste Collection
- Collectors submit waste reports with location, type, quantity, and GPS coordinates
- AI-powered waste type auto-detection from images via **HuggingFace** (`google/vit-base-patch16-224`)
- Personalized recycling advice per waste type
- Filter and search by location, type, and status

### Eco-Events
- Full event lifecycle management (create, register, attend, close)
- Photo uploads stored on **Cloudinary**
- Paid event registration via **Stripe Checkout**
- Capacity tracking with cumulative daily stat charts
- AI fraud detection on registrations using **Rubix ML IsolationForest**
- PDF export of event details and registration tickets via **DomPDF**

### Donations
- Three donation types: monetary, goods, and services
- Admin approval/rejection workflow (pending → completed/rejected)

### Forum
- Topic and response creation with moderation pipeline
- Category management with auto-generated slugs
- Spam detection service
- AI forum assistant chatbot (predefined answers + optional **OpenAI API**)

### Authentication & Security
- Email/password login with **JWT tokens** stored as HTTP-only cookies
- **Google OAuth2** social login
- **Face recognition** login and enrollment via **Azure Face API** and **CompreFace**
- Password reset via signed email links (**Azure Communication Services**)
- **reCAPTCHA v2** on login and registration forms
- Role-based access control: `ROLE_USER`, `ROLE_ADMIN`, `ROLE_ADMIN_EVENT_MANAGER`

### Admin Panel
- Live stats dashboard (collections, users, donations, events)
- User management: list, filter, search, edit, delete, block/unblock
- Donation approval and forum moderation workflows
- Forum category management

### Transparency Dashboard
- Public metrics: total waste collected, funds raised, trees planted, events organized, budget usage

---

## Tech Stack

### Frontend
- **Twig 3.x** – server-side templating engine
- **Bootstrap 5.3** – responsive UI framework
- **Bootstrap Icons 1.11** + **Font Awesome 6** – iconography
- **Stimulus.js** (`@hotwired/stimulus`) – lightweight JS controllers
- **Symfony UX Turbo** (`symfony/ux-turbo`) – SPA-like page transitions
- **Symfony Asset Mapper** – modern asset pipeline (no bundler required)
- Google Fonts: **Poppins** + **Inter**

### Backend
- **PHP 8.2+**
- **Symfony 7.4** – MVC web framework
- **Doctrine ORM 3.x** – database abstraction and migrations
- **PostgreSQL 16** – relational database
- **LexikJWT 3.2** – stateless JWT authentication
- **KnpU OAuth2 Client** + `league/oauth2-google` – Google social login
- **Stripe PHP SDK 19.x** – payment processing
- **Cloudinary PHP SDK 3.x** – cloud media storage
- **Rubix ML 2.5** – machine learning (IsolationForest fraud detection)
- **HuggingFace API** – vision transformer image classification
- **Azure Face API** + **CompreFace** – face recognition authentication
- **Azure Communication Services** – transactional email delivery
- **DomPDF 3.x** – PDF generation
- **Symfony Messenger** – asynchronous email and notification queue
- **Swoole** (`runtime/swoole`) – high-performance async PHP runtime
- **reCAPTCHA** (`excelwebzone/recaptcha-bundle`) – bot protection

---

## Architecture

The application follows a layered **MVC architecture** built on top of Symfony conventions:

```
┌─────────────────────────────────────────────────────────────────┐
│                          Browser / Client                        │
└────────────────────────────┬────────────────────────────────────┘
                             │ HTTP (JWT cookie)
┌────────────────────────────▼────────────────────────────────────┐
│                     Symfony Router & Firewall                    │
│              (JWT auth chain: User + AdminAccount)               │
└────────┬──────────────────────────────────────┬─────────────────┘
         │                                       │
┌────────▼──────────┐                  ┌─────────▼────────────────┐
│   Controllers     │                  │   REST API (/api/*)       │
│  (src/Controller) │                  │   Stateless JWT firewall  │
└────────┬──────────┘                  └──────────────────────────┘
         │
┌────────▼──────────┐
│   Service Layer   │  ← StripeCheckoutService, FraudDetectionService,
│  (src/Service)    │     WasteImageClassifierService, FaceRecognitionManager,
└────────┬──────────┘     CloudinaryService, ForumAssistantService, ...
         │
┌────────▼──────────┐
│  Doctrine ORM     │  ← Entities, Repositories, Migrations
│  (src/Entity,     │
│   src/Repository) │
└────────┬──────────┘
         │
┌────────▼──────────┐     ┌──────────────────────────────────────┐
│   PostgreSQL DB   │     │    Symfony Messenger (Async Queue)   │
└───────────────────┘     │    Emails · Notifications            │
                          └──────────────────────────────────────┘
```

**Key patterns:**
- **Service Layer** – all business logic isolated in `src/Service/` (AI, payments, storage, email, fraud detection)
- **Repository Pattern** – typed Doctrine repositories for all entities
- **Enum-driven domain modeling** – 15 PHP-backed enums for statuses and types
- **Ownership-based access control** – every user-scoped resource validates `entity->getUser() === currentUser`
- **Async messaging** – email and notifications dispatched via Symfony Messenger
- **Docker-first** – full `compose.yaml` stack for local development and production parity

---

## Contributors

| Name | Role |
|------|------|
| **Mohamed Ali Hmem** | Full-Stack Developer |
| **Azmi Ghabi** | Full-Stack Developer |
| **Jawher BenJeddou** | Full-Stack Developer |
| **Mariem Abbes** | Full-Stack Developer |
| **Yosra Bejaoui** | Full-Stack Developer |

---

## Academic Context

Developed at **Esprit School of Engineering – Tunisia**
PIDEV – 3A | Academic Year 2025–2026

---

## Getting Started

### Prerequisites
- Docker & Docker Compose
- PHP 8.2+ and Composer (for local development without Docker)

### Run with Docker
```bash
# Clone the repository
git clone <repository-url>
cd PI_project

# Copy and configure environment variables
cp .env .env.local
# Edit .env.local with your API keys (Stripe, Cloudinary, Azure, etc.)

# Start the stack
docker compose up -d

# Install PHP dependencies
docker compose exec php composer install

# Run database migrations
docker compose exec php bin/console doctrine:migrations:migrate

# (Optional) Load fixtures
docker compose exec php bin/console doctrine:fixtures:load
```

The application will be available at **http://localhost:8000**.

### Local Development (without Docker)
```bash
composer install
cp .env .env.local   # Configure DATABASE_URL and API keys
php bin/console doctrine:migrations:migrate
symfony server:start
```

### Required Environment Variables
| Variable | Description |
|----------|-------------|
| `DATABASE_URL` | PostgreSQL connection string |
| `JWT_SECRET_KEY` / `JWT_PUBLIC_KEY` | JWT signing keys |
| `STRIPE_SECRET_KEY` / `STRIPE_PUBLIC_KEY` | Stripe API keys |
| `CLOUDINARY_URL` | Cloudinary connection string |
| `AZURE_FACE_API_KEY` | Azure Face API subscription key |
| `AZURE_COMMUNICATION_CONNECTION_STRING` | Azure email service |
| `GOOGLE_CLIENT_ID` / `GOOGLE_CLIENT_SECRET` | Google OAuth2 credentials |
| `RECAPTCHA_PUBLIC_KEY` / `RECAPTCHA_PRIVATE_KEY` | reCAPTCHA keys |
| `OPENAI_API_KEY` | OpenAI API key (optional — forum assistant) |

---

## Acknowledgments

- [Symfony](https://symfony.com/) – PHP framework
- [Stripe](https://stripe.com/) – payment infrastructure
- [Cloudinary](https://cloudinary.com/) – media management
- [HuggingFace](https://huggingface.co/) – open-source AI models
- [Rubix ML](https://rubixml.com/) – PHP machine learning library
- [Azure Cognitive Services](https://azure.microsoft.com/en-us/products/ai-services/) – Face API and Communication Services
- [CompreFace](https://github.com/exadel-inc/CompreFace) – open-source face recognition
- [LexikJWT](https://github.com/lexik/LexikJWTAuthenticationBundle) – JWT authentication for Symfony
- [DomPDF](https://github.com/dompdf/dompdf) – PHP PDF generation
