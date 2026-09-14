import { render, screen } from "@testing-library/react"
import { describe, expect, it, vi } from "vitest"
import DashboardPage from "@/app/dashboard/page"

vi.mock("@/hooks/use-dashboard-stats", () => ({
  useDashboardStats: () => ({
    stats: {
      active_customers: 20,
      monthly_revenue: 1000,
      open_tickets: 3,
      devices_in_use: 18,
      at_risk_customers: 4,
      over_limit_customers: 2,
      acknowledgement_overdue_tickets: 7,
      resolution_overdue_tickets: 5,
      due_soon_tickets: 9,
      as_of: "2026-09-14T04:00:00Z",
    },
    customersByPlan: [],
    devicesByStatus: [],
    ticketsByStatus: [],
    revenueByPlan: [],
    loading: false,
    error: null,
  }),
}))

vi.mock("@/components/app-sidebar", () => ({
  AppSidebar: () => null,
}))

vi.mock("@/components/health-status", () => ({
  HealthStatus: () => null,
}))

vi.mock("@/components/ui/sidebar", () => ({
  SidebarProvider: ({ children }: { children: React.ReactNode }) => <div>{children}</div>,
  SidebarInset: ({ children }: { children: React.ReactNode }) => <main>{children}</main>,
  SidebarTrigger: () => <button type="button">Toggle sidebar</button>,
}))

vi.mock("@/components/ui/chart", () => ({
  ChartContainer: ({ children }: { children: React.ReactNode }) => <div>{children}</div>,
  ChartTooltip: () => null,
  ChartTooltipContent: () => null,
}))

vi.mock("recharts", () => ({
  Bar: () => null,
  BarChart: ({ children }: { children: React.ReactNode }) => <div>{children}</div>,
  CartesianGrid: () => null,
  Pie: () => null,
  PieChart: ({ children }: { children: React.ReactNode }) => <div>{children}</div>,
  XAxis: () => null,
  YAxis: () => null,
}))

describe("DashboardPage KPIs", () => {
  it("renders separate API counts for usage risk and ticket deadlines", () => {
    render(<DashboardPage />)

    expect(screen.getByText("At-risk customers")).toBeInTheDocument()
    expect(screen.getByText("4")).toBeInTheDocument()
    expect(screen.getByText("Over-limit customers")).toBeInTheDocument()
    expect(screen.getByText("2")).toBeInTheDocument()
    expect(screen.getByText("Acknowledgement overdue")).toBeInTheDocument()
    expect(screen.getByText("7")).toBeInTheDocument()
    expect(screen.getByText("Resolution overdue")).toBeInTheDocument()
    expect(screen.getByText("5")).toBeInTheDocument()
    expect(screen.getByText("Due soon")).toBeInTheDocument()
    expect(screen.getByText("9")).toBeInTheDocument()
    expect(screen.getAllByText("Current ticket count")).toHaveLength(3)
  })
})
