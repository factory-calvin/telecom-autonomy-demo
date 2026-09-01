import { render, screen } from "@testing-library/react"
import { describe, it, expect, vi } from "vitest"
import { CustomersTable } from "@/components/customers-table"
import type { Customer } from "@/hooks/use-customers"

const customers: Customer[] = [
  {
    id: 1,
    first_name: "Ada",
    last_name: "Lovelace",
    email: "ada@example.com",
    phone: "555-0100",
    plan_id: 2,
    plan_name: "Standard",
    status: "ACTIVE",
    balance: 12.3,
    activated_at: "2026-01-01T00:00:00Z",
    created_at: "2026-01-01T00:00:00Z",
  },
  {
    id: 2,
    first_name: "Grace",
    last_name: "Hopper",
    email: "grace@example.com",
    phone: "555-0101",
    plan_id: null,
    plan_name: null,
    status: "SUSPENDED",
    balance: 0,
    activated_at: null,
    created_at: "2026-01-02T00:00:00Z",
  },
]

describe("CustomersTable", () => {
  it("renders the expected column headers", () => {
    render(<CustomersTable customers={customers} onEdit={vi.fn()} onDelete={vi.fn()} />)

    const headers = screen.getAllByRole("columnheader").map((cell) => cell.textContent)
    expect(headers).toEqual([
      "ID",
      "Name",
      "Email",
      "Phone",
      "Plan",
      "Status",
      "Balance",
      "Actions",
    ])
  })

  it("joins first and last name into a single cell", () => {
    render(<CustomersTable customers={customers} onEdit={vi.fn()} onDelete={vi.fn()} />)

    expect(screen.getByText("Ada Lovelace")).toBeInTheDocument()
    expect(screen.getByText("Grace Hopper")).toBeInTheDocument()
  })

  it("formats the balance to two decimal places", () => {
    render(<CustomersTable customers={customers} onEdit={vi.fn()} onDelete={vi.fn()} />)

    expect(screen.getByText("$12.30")).toBeInTheDocument()
    expect(screen.getByText("$0.00")).toBeInTheDocument()
  })

  it("falls back to a dash when the customer has no plan", () => {
    render(<CustomersTable customers={customers} onEdit={vi.fn()} onDelete={vi.fn()} />)

    expect(screen.getByText("Standard")).toBeInTheDocument()
    expect(screen.getByText("-")).toBeInTheDocument()
  })

  it("renders the raw status value as a badge", () => {
    render(<CustomersTable customers={customers} onEdit={vi.fn()} onDelete={vi.fn()} />)

    expect(screen.getByText("ACTIVE")).toBeInTheDocument()
    expect(screen.getByText("SUSPENDED")).toBeInTheDocument()
  })

  it("calls onEdit and onDelete with the row's customer", () => {
    const onEdit = vi.fn()
    const onDelete = vi.fn()
    render(<CustomersTable customers={customers} onEdit={onEdit} onDelete={onDelete} />)

    const buttons = screen.getAllByRole("button")
    buttons[0].click()
    buttons[1].click()

    expect(onEdit).toHaveBeenCalledWith(customers[0])
    expect(onDelete).toHaveBeenCalledWith(customers[0])
  })
})
