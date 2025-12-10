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
