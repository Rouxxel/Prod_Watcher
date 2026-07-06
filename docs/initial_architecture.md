# Inventory & Sales Web App Architecture

## Overview

This document summarizes the planned architecture for a business inventory and sales management web application.

The platform will initially focus on **inventory management** and later expand into a full **sales and cashier system** with accounting-oriented features such as taxes, pricing calculations, and transaction tracking.

The main architectural goals are:

- Scalability
- Maintainability
- Separation of concerns
- Business logic correctness
- Extensibility for future accounting and POS features

---

# Core Product Vision

The system will operate in **two primary modes**:

1. Inventory Mode
2. Selling / Cashier Mode

The inventory side will be developed first to establish a strong and reliable foundation for stock management and product tracking.

---

# Frontend

- Framework: React 19 + TypeScript (TanStack Start)
- Routing: TanStack Router (file-based)
- Data Fetching & Server State: TanStack Query
- UI Layer: Tailwind CSS v4 + shadcn/ui

## Responsibilities

- Display products, stock, transactions, and sales
- Handle user interactions
- Manage server state efficiently
- Communicate with backend APIs
- Support role-based interfaces depending on user type

## Deployment

- **Primary:** Vercel
- **Optional:** Cloudflare Workers (edge; repo includes `wrangler.jsonc`)

---

# Application Modes

## 1. Inventory Mode

### Intended Users

- Warehouse workers
- Warehouse managers
- Inspectors
- Administrators

### Features

- Create products
- Modify product information
- Organize products
- Register stock movements
- Inventory inspections
- Low stock alerts
- Audit trails and history

### Architectural Notes

Inventory should be modeled using a transaction-based system:
- Stock IN
- Stock OUT
- Transfers
- Manual adjustments

---

## 2. Selling / Cashier Mode

### Intended Users

- Cashiers
- Store employees
- Managers

### Features

- Product lookup
- Real-time inventory validation
- Automatic stock deduction after sales
- Receipt generation
- Tax calculation
- Transaction history

### Architectural Notes

Sales operations must:
- Be transactional
- Prevent negative stock
- Synchronize inventory instantly
- Maintain accurate accounting records

---

# Backend

- Framework: **Java — Spring Boot 3** (Java 17+)
- API style: RESTful (`/api/v1/...`)
- Persistence: Spring Data JPA
- Migrations: Flyway (`backend/src/main/resources/db/migration/`)
- Validation: Jakarta Bean Validation on DTOs
- Security: Spring Security + Supabase JWT validation
- Structure: `controller` → `service` → `repository` → `entity` / `dto`

## Key Principles

- Keep controllers thin
- Separate business logic in service layers
- Validate all incoming data
- Use service layers for inventory and sales logic
- Maintain transactional integrity (`@Transactional` on stock and sales operations)
- Config-driven endpoints and rate limits via `config_file.json`

---

# Database

- Platform: Supabase (PostgreSQL)

## Core Entities

- Products
- Warehouses
- Inventory Transactions
- Sales
- Users
- Roles
- Suppliers

### Inventory Model

Avoid relying only on static quantities.

Instead:

Current Stock = Sum of Inventory Transactions

---

# Authentication & Authorization

## Authentication

- Supabase Auth (`auth.users`)
- **Owner bootstrap:** public `/signup` → email confirmation → first user becomes `admin`
- **Staff provisioning:** admin creates users via Admin API (`email_confirm: true`, admin-set password) — no invite/confirm email
- **Promote to admin:** separate action on `/users`, not part of normal provisioning

## Authorization

Role-based access control (RBAC)

- Roles stored in a separate `user_roles` table (not on the user profile row)
- Enforced in the Java service layer and via Supabase RLS policies

### Example Roles

- Admin
- Warehouse Worker
- Warehouse Manager
- Inspector
- Cashier

---

# Infrastructure

## Core Services

- Supabase Auth
- Supabase Storage

## Background Processing

- Low stock alerts
- Scheduled reports
- Accounting summaries
- Inventory reconciliation

## Observability

- Logging
- Monitoring
- Error tracking

---

# Deployment Strategy

## Frontend

- **Primary:** [Vercel](https://vercel.com) — deploy the TanStack Start app as a Vite/Node SSR or static SPA build (recommended default)
- **Optional:** [Cloudflare Workers](https://workers.cloudflare.com) — edge runtime via existing `wrangler.jsonc` and `@cloudflare/vite-plugin` when lower latency or Workers-specific features are needed

Both targets set `VITE_API_BASE_URL` to the Render API. Supabase and database credentials live on the backend only. Supabase Auth redirect URLs and backend CORS must list whichever frontend URL(s) are in use.

## Backend

- Render (Spring Boot JAR / Docker)

## Database & Auth

- Supabase (PostgreSQL + Auth + Storage)

---

# Development Roadmap

## Phase 1 — Inventory System

- Products
- Warehouses
- Stock movements
- User roles
- Inventory tracking
- Audit logs

## Phase 2 — Selling / POS System

- Cashier interface
- Sales transactions
- Real-time stock deduction
- Receipts
- Taxes and pricing logic

## Phase 3 — Accounting & Reporting

- Financial reporting
- Automated tax calculations
- Revenue tracking

---

# Guiding Principles

- Keep architecture layered and modular
- Treat inventory as a transactional system
- Optimize for correctness first
- Build extensible business logic
