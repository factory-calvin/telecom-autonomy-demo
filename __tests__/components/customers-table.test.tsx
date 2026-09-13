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
    current_cycle_data_used_gb: 2.5,
    data_limit_gb: 15,
    data_usage_percentage: 16.67,
    data_usage_state: "WITHIN_LIMIT",
  },
  {
    id: 2,
    first_name: "Grace",
    last_name: "Hopper",
    email: "grace@example.com",
    phone: "555-0101",
    plan_id: 3,
    plan_name: "Premium",
    status: "SUSPENDED",
    balance: 0,
    activated_at: null,
    created_at: "2026-01-02T00:00:00Z",
    current_cycle_data_used_gb: 8.5,
    data_limit_gb: 10,
    data_usage_percentage: 85,
    data_usage_state: "AT_RISK",
  },
  {
    id: 3,
    first_name: "Katherine",
    last_name: "Johnson",
    email: "katherine@example.com",
    phone: "555-0102",
    plan_id: 3,
    plan_name: "Premium",
    status: "ACTIVE",
    balance: 4,
    activated_at: "2026-01-03T00:00:00Z",
    created_at: "2026-01-03T00:00:00Z",
    current_cycle_data_used_gb: 10.2,
    data_limit_gb: 10,
    data_usage_percentage: 102,
    data_usage_state: "OVER_LIMIT",
  },
  {
    id: 4,
    first_name: "Annie",
    last_name: "Easley",
    email: "annie@example.com",
    phone: "555-0103",
    plan_id: 4,
    plan_name: "Unlimited",
    status: "ACTIVE",
    balance: 8,
    activated_at: "2026-01-04T00:00:00Z",
    created_at: "2026-01-04T00:00:00Z",
    current_cycle_data_used_gb: 12.4,
    data_limit_gb: null,
    data_usage_percentage: null,
    data_usage_state: "UNLIMITED",
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
      "Current-cycle data",
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
    const customerWithoutPlan = { ...customers[0], plan_id: null, plan_name: null }
    render(<CustomersTable customers={[customerWithoutPlan]} onEdit={vi.fn()} onDelete={vi.fn()} />)

    expect(screen.getByText("-")).toBeInTheDocument()
  })

  it("renders the raw status value as a badge", () => {
    render(<CustomersTable customers={customers} onEdit={vi.fn()} onDelete={vi.fn()} />)

    expect(screen.getAllByText("ACTIVE")).toHaveLength(3)
    expect(screen.getByText("SUSPENDED")).toBeInTheDocument()
  })

  it("shows finite usage, percentage, allowance, and semantic risk text", () => {
    render(<CustomersTable customers={customers} onEdit={vi.fn()} onDelete={vi.fn()} />)

    expect(screen.getByLabelText("8.5 GB, 85% of 10 GB, At risk")).toBeInTheDocument()
    expect(screen.getByText("At risk")).toBeVisible()
    expect(screen.getByLabelText("10.2 GB, 102% of 10 GB, Over limit")).toBeInTheDocument()
    expect(screen.getByText("Over limit")).toBeVisible()
  })

  it("shows Unlimited usage without percentage, allowance, or risk text", () => {
    render(<CustomersTable customers={[customers[3]]} onEdit={vi.fn()} onDelete={vi.fn()} />)

    expect(screen.getByLabelText("12.4 GB used on an Unlimited plan")).toHaveTextContent("12.4 GB")
    expect(screen.queryByText(/%|of .* GB|At risk|Over limit/)).not.toBeInTheDocument()
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

  it("gives edit and delete actions accessible customer names", () => {
    render(<CustomersTable customers={[customers[0]]} onEdit={vi.fn()} onDelete={vi.fn()} />)

    expect(screen.getByRole("button", { name: "Edit Ada Lovelace" })).toBeInTheDocument()
    expect(screen.getByRole("button", { name: "Delete Ada Lovelace" })).toBeInTheDocument()
  })
})
