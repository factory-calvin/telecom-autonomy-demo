---
sidebar_position: 2
---

# Components

## UI Components (shadcn/ui)

Located in `components/ui/`, these are the base components:

| Component      | Description                                   |
| -------------- | --------------------------------------------- |
| `Button`       | Primary, secondary, destructive variants      |
| `Card`         | Content containers with header/content/footer |
| `Dialog`       | Modal dialogs for forms                       |
| `Table`        | Data tables with sorting                      |
| `Select`       | Dropdown selection                            |
| `Label`        | Form field labels                             |
| `Alert Dialog` | Confirmation dialogs                          |

### Adding New Components

```bash
pnpm dlx shadcn@latest add <component-name>
```

## Application Components

### App Sidebar

`components/app-sidebar.tsx` - Main navigation with collapsible sections.

### Data Tables

Each entity has a table component:

- `customers-table.tsx`
- `plans-table.tsx`
- `devices-table.tsx`
- `tickets-table.tsx`
- `usage-table.tsx`

```tsx
<CustomersTable customers={customers} onEdit={handleEdit} onDelete={handleDelete} />
```

`customers-table.tsx` includes a current-cycle data column. Finite plans show used GB,
percentage, allowance, and visible `At risk` or `Over limit` text when the API returns
`AT_RISK` or `OVER_LIMIT`. Unlimited plans show used GB without a percentage, allowance, or
risk flag. The customers page filters on those semantic API states rather than calculating
thresholds in the browser. The filter uses pressed buttons with a programmatic group label,
and risk badges retain visible text in light and dark themes so color is never the only cue.

`tickets-table.tsx` shows API-provided whole-day and whole-hour age plus visible `On track`,
`Due soon`, `Acknowledgement overdue`, or `Resolution overdue` text. Deadline badges include
the exact acknowledgement and resolution due timestamps in their accessible label and
tooltip. Light and dark themes use color as a secondary cue only. The tickets page passes
the selected semantic state to the backend as `deadlineState`; it does not reproduce
business-day calculations in the browser.

The dashboard has separate Acknowledgement overdue, Resolution overdue, and Due soon cards.
Each card displays the corresponding API value and labels it as a current ticket count, not
a monthly breach-rate denominator.

### Form Dialogs

CRUD dialogs for creating/editing entities:

- `customer-form-dialog.tsx`
- `plan-form-dialog.tsx`
- `device-form-dialog.tsx`
- `ticket-form-dialog.tsx`

```tsx
<CustomerFormDialog
  open={isOpen}
  onOpenChange={setIsOpen}
  customer={selectedCustomer} // null for create
  onSubmit={handleSubmit}
/>
```

## Component Guidelines

1. **Use `"use client"`** for interactive components
2. **Colocate** related components in feature folders
3. **Extract** reusable logic into hooks
4. **Avoid `useEffect`** in page components - use hooks instead
