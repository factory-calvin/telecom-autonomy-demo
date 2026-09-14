import { fireEvent, render, screen } from "@testing-library/react"
import { beforeEach, describe, expect, it, vi } from "vitest"
import TicketsPage from "@/app/tickets/page"

const useTicketsMock = vi.fn()

vi.mock("@/hooks/use-tickets", async () => {
  const actual = await vi.importActual<typeof import("@/hooks/use-tickets")>("@/hooks/use-tickets")
  return { ...actual, useTickets: (filters: unknown) => useTicketsMock(filters) }
})

vi.mock("@/hooks/use-infinite-scroll", () => ({ useInfiniteScroll: vi.fn() }))
vi.mock("@/components/app-sidebar", () => ({ AppSidebar: () => null }))
vi.mock("@/components/health-status", () => ({ HealthStatus: () => null }))
vi.mock("@/components/ticket-form-dialog", () => ({ TicketFormDialog: () => null }))
vi.mock("@/components/tickets-table", () => ({
  TicketsTable: ({ tickets }: { tickets: unknown[] }) => (
    <div data-testid="tickets-table">{tickets.length} rows</div>
  ),
}))
vi.mock("@/components/ui/sidebar", () => ({
  SidebarProvider: ({ children }: { children: React.ReactNode }) => <div>{children}</div>,
  SidebarInset: ({ children }: { children: React.ReactNode }) => <main>{children}</main>,
  SidebarTrigger: () => <button type="button">Toggle sidebar</button>,
}))
vi.mock("@/components/ui/select", () => ({
  Select: ({
    children,
    value,
    onValueChange,
  }: {
    children: React.ReactNode
    value: string
    onValueChange: (value: string) => void
  }) => (
    <select
      aria-label="mock-select"
      value={value}
      onChange={(event) => onValueChange(event.target.value)}
    >
      {children}
    </select>
  ),
  SelectTrigger: ({ children }: { children: React.ReactNode }) => <>{children}</>,
  SelectValue: () => null,
  SelectContent: ({ children }: { children: React.ReactNode }) => <>{children}</>,
  SelectItem: ({ children, value }: { children: React.ReactNode; value: string }) => (
    <option value={value}>{children}</option>
  ),
}))

function hookValue(overrides: Record<string, unknown> = {}) {
  return {
    tickets: [{ id: 1 }],
    loading: false,
    loadingMore: false,
    error: null,
    hasNext: false,
    totalElements: 1,
    loadMore: vi.fn(),
    createTicket: vi.fn(),
    updateTicket: vi.fn(),
    deleteTicket: vi.fn(),
    ...overrides,
  }
}

describe("TicketsPage", () => {
  beforeEach(() => {
    useTicketsMock.mockReset()
    useTicketsMock.mockImplementation(() => hookValue())
  })

  it("passes the selected semantic deadline state to useTickets", () => {
    render(<TicketsPage />)

    const deadlineSelect = screen.getAllByLabelText("mock-select")[2]
    fireEvent.change(deadlineSelect, { target: { value: "RESOLUTION_OVERDUE" } })

    expect(useTicketsMock).toHaveBeenLastCalledWith({
      deadlineState: "RESOLUTION_OVERDUE",
    })
  })

  it.each([
    ["loading", { loading: true }, "Loading..."],
    ["error", { error: "Failed to fetch tickets" }, "Failed to fetch tickets"],
    ["empty", { tickets: [], totalElements: 0 }, "No tickets match the selected filters."],
  ])("renders the %s state", (_name, overrides, expected) => {
    useTicketsMock.mockImplementation(() => hookValue(overrides))
    render(<TicketsPage />)

    expect(screen.getAllByText(expected).length).toBeGreaterThan(0)
  })

  it("keeps populated result and pagination messaging", () => {
    render(<TicketsPage />)

    expect(screen.getByText("1 of 1 tickets")).toBeInTheDocument()
    expect(screen.getByTestId("tickets-table")).toHaveTextContent("1 rows")
    expect(screen.getByText("End of results")).toBeInTheDocument()
  })
})
