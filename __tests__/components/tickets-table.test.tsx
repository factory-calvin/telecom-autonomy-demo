import { render, screen } from "@testing-library/react"
import { describe, expect, it, vi } from "vitest"
import { TicketsTable } from "@/components/tickets-table"
import type { DeadlineState, Ticket } from "@/hooks/use-tickets"

function makeTicket(id: number, deadline_state: DeadlineState): Ticket {
  return {
    id,
    customer_id: id,
    customer_name: `Customer ${id}`,
    subject: `Ticket ${id}`,
    description: null,
    priority: "HIGH",
    status: "OPEN",
    created_at: "2026-09-01T10:00:00Z",
    acknowledged_at: null,
    resolved_at: null,
    priority_escalated_at: null,
    age_hours: 313,
    age_days: 13,
    acknowledgement_due_at: "2026-09-03T10:00:00Z",
    resolution_due_at: "2026-09-15T10:00:00Z",
    deadline_state,
  }
}

const tickets = [
  makeTicket(1, "ON_TRACK"),
  makeTicket(2, "DUE_SOON"),
  makeTicket(3, "ACKNOWLEDGEMENT_OVERDUE"),
  makeTicket(4, "RESOLUTION_OVERDUE"),
]

describe("TicketsTable", () => {
  it("shows API-provided whole age and every readable semantic state", () => {
    render(<TicketsTable tickets={tickets} onEdit={vi.fn()} onDelete={vi.fn()} />)

    expect(screen.getAllByText("13 days")).toHaveLength(4)
    expect(screen.getAllByText("313 whole hours")).toHaveLength(4)
    expect(screen.getByText("On track")).toBeVisible()
    expect(screen.getByText("Due soon")).toBeVisible()
    expect(screen.getByText("Acknowledgement overdue")).toBeVisible()
    expect(screen.getByText("Resolution overdue")).toBeVisible()
  })

  it("exposes both exact deadlines in each state label", () => {
    render(<TicketsTable tickets={[tickets[1]]} onEdit={vi.fn()} onDelete={vi.fn()} />)

    const badge = screen.getByLabelText(/Due soon\. Acknowledgement due .+ Resolution due .+\./)
    expect(badge).toHaveAttribute("title", expect.stringContaining("Acknowledgement due"))
    expect(badge).toHaveAttribute("title", expect.stringContaining("Resolution due"))
  })

  it("keeps visible text and dark-mode styles for due-soon and overdue states", () => {
    render(<TicketsTable tickets={tickets.slice(1)} onEdit={vi.fn()} onDelete={vi.fn()} />)

    expect(screen.getByText("Due soon")).toHaveClass("dark:text-amber-200")
    expect(screen.getByText("Acknowledgement overdue")).toHaveClass("dark:text-orange-200")
    expect(screen.getByText("Resolution overdue")).toHaveClass("dark:text-red-200")
  })
})
