import { render, screen } from "@testing-library/react"
import { describe, it, expect, vi } from "vitest"
import { PlansTable } from "@/components/plans-table"
import type { Plan } from "@/hooks/use-plans"

const plans: Plan[] = [
  {
    id: 1,
    name: "Basic",
    monthly_price: 20,
    data_limit_gb: 10,
    minutes_limit: 500,
    sms_limit: 500,
    is_active: true,
  },
  {
    id: 2,
    name: "Premium Max",
    monthly_price: 99.5,
    data_limit_gb: null,
    minutes_limit: null,
    sms_limit: null,
    is_active: false,
  },
]

describe("PlansTable", () => {
  it("renders the expected column headers", () => {
    render(<PlansTable plans={plans} onEdit={vi.fn()} onDelete={vi.fn()} />)

    const headers = screen.getAllByRole("columnheader").map((cell) => cell.textContent)
    expect(headers).toEqual([
      "ID",
      "Name",
      "Price/mo",
      "Data",
      "Minutes",
      "SMS",
      "Status",
      "Actions",
    ])
  })

  it("renders one row per plan", () => {
    render(<PlansTable plans={plans} onEdit={vi.fn()} onDelete={vi.fn()} />)

    expect(screen.getByText("Basic")).toBeInTheDocument()
    expect(screen.getByText("Premium Max")).toBeInTheDocument()
  })

  it("formats the monthly price to two decimal places", () => {
    render(<PlansTable plans={plans} onEdit={vi.fn()} onDelete={vi.fn()} />)

    expect(screen.getByText("$20.00")).toBeInTheDocument()
    expect(screen.getByText("$99.50")).toBeInTheDocument()
  })

  it("labels null limits as Unlimited and omits the GB suffix", () => {
    render(<PlansTable plans={plans} onEdit={vi.fn()} onDelete={vi.fn()} />)

    expect(screen.getByText("10 GB")).toBeInTheDocument()
    // Data, minutes, and SMS on the second plan are all null.
    expect(screen.getAllByText("Unlimited")).toHaveLength(3)
  })

  it("shows an Active or Inactive badge per plan", () => {
    render(<PlansTable plans={plans} onEdit={vi.fn()} onDelete={vi.fn()} />)

    expect(screen.getByText("Active")).toBeInTheDocument()
    expect(screen.getByText("Inactive")).toBeInTheDocument()
  })

  it("calls onEdit and onDelete with the row's plan", () => {
    const onEdit = vi.fn()
    const onDelete = vi.fn()
    render(<PlansTable plans={plans} onEdit={onEdit} onDelete={onDelete} />)

    const buttons = screen.getAllByRole("button")
    buttons[0].click()
    buttons[1].click()

    expect(onEdit).toHaveBeenCalledWith(plans[0])
    expect(onDelete).toHaveBeenCalledWith(plans[0])
  })
})
