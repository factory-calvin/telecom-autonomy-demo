---
sidebar_position: 1
---

# Frontend Overview

The frontend is a Next.js 16 application using the App Router with React 19.

## Directory Structure

```
app/
├── layout.tsx          # Root layout with sidebar
├── globals.css         # Global styles + Tailwind
├── dashboard/          # Dashboard with KPIs and charts
├── customers/          # Customer management
├── plans/              # Plan catalog
├── devices/            # Device inventory
├── usage/              # Usage records
└── tickets/            # Support tickets

components/
├── ui/                 # shadcn/ui components
├── app-sidebar.tsx     # Navigation sidebar
├── *-table.tsx         # Data table components
└── *-form-dialog.tsx   # CRUD dialog forms

hooks/
├── use-customers.ts    # Customer data fetching
├── use-plans.ts        # Plan data fetching
├── use-devices.ts      # Device data fetching
├── use-tickets.ts      # Ticket data fetching
├── use-usage.ts        # Usage record fetching
└── use-dashboard-stats.ts
```

## Key Patterns

### Client Components

All interactive pages use the `"use client"` directive:

```tsx
"use client"

import { useCustomers } from "@/hooks/use-customers"

export default function CustomersPage() {
  const { customers, loading, createCustomer } = useCustomers()
  // ...
}
```

### Data Fetching Hooks

Custom hooks encapsulate API calls and state management:

```tsx
const {
  data, // Array of items
  loading, // Initial load state
  error, // Error message
  refresh, // Refetch data
  create, // Create new item
  update, // Update item
  delete: remove, // Delete item
} = useCustomers()
```

### UI Components

Built on shadcn/ui with Tailwind CSS:

```tsx
import { Button } from "@/components/ui/button"
import { Card, CardHeader, CardContent } from "@/components/ui/card"
import { DataTable } from "@/components/ui/table"
```

## Styling

Uses Tailwind CSS 4 with custom Telekom design tokens defined in `globals.css`:

- Typography: Tele Neo Office font family
- Colors: Magenta primary (#e20074), semantic colors
- Dark mode: Automatic based on system preference
