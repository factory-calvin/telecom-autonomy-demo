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

describe("DashboardPage usage-risk KPIs", () => {
  it("renders separate API counts for at-risk and over-limit customers", () => {
    render(<DashboardPage />)

    expect(screen.getByText("At-risk customers")).toBeInTheDocument()
    expect(screen.getByText("4")).toBeInTheDocument()
    expect(screen.getByText("Over-limit customers")).toBeInTheDocument()
    expect(screen.getByText("2")).toBeInTheDocument()
  })
})
