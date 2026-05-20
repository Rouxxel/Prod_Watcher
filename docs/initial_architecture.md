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

- Framework: React + TypeScript
- Data Fetching & Server State: TanStack Query
- UI Layer: Tailwind CSS + component library

## Responsibilities

- Display products, stock, transactions, and sales
- Handle user interactions
- Manage server state efficiently
- Communicate with backend APIs
- Support role-based interfaces depending on user type

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

- Framework: .NET

## Key Principles

- Keep routes thin
- Separate business logic from API routes
- Validate all incoming data
- Use service layers for inventory and sales logic
- Maintain transactional integrity

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

- Supabase Auth

## Authorization

Role-based access control (RBAC)

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

- Vercel

## Backend

- Render

## Database & Auth

- Supabase

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
