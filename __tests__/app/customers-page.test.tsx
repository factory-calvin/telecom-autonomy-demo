import { fireEvent, render, screen } from "@testing-library/react"
import { beforeEach, describe, expect, it, vi } from "vitest"
import CustomersPage from "@/app/customers/page"
import type { Customer } from "@/hooks/use-customers"

const customers: Customer[] = [
  {
    id: 1,
    first_name: "Ada",
    last_name: "At Risk",
    email: "ada@example.com",
    phone: "555-0100",
    plan_id: 1,
    plan_name: "Basic",
    status: "ACTIVE",
    balance: 0,
    activated_at: null,
    created_at: null,
    current_cycle_data_used_gb: 8.5,
    data_limit_gb: 10,
    data_usage_percentage: 85,
    data_usage_state: "AT_RISK",
  },
  {
    id: 2,
    first_name: "Grace",
    last_name: "Over Limit",
    email: "grace@example.com",
    phone: "555-0101",
    plan_id: 1,
    plan_name: "Basic",
    status: "ACTIVE",
    balance: 0,
    activated_at: null,
    created_at: null,
    current_cycle_data_used_gb: 10.2,
    data_limit_gb: 10,
    data_usage_percentage: 102,
    data_usage_state: "OVER_LIMIT",
  },
  {
    id: 3,
    first_name: "Annie",
    last_name: "Unlimited",
    email: "annie@example.com",
    phone: "555-0102",
    plan_id: 2,
    plan_name: "Unlimited",
    status: "ACTIVE",
    balance: 0,
    activated_at: null,
    created_at: null,
    current_cycle_data_used_gb: 12.4,
    data_limit_gb: null,
    data_usage_percentage: null,
    data_usage_state: "UNLIMITED",
  },
]

const useCustomersMock = vi.fn()

vi.mock("@/hooks/use-customers", async () => {
  const actual =
    await vi.importActual<typeof import("@/hooks/use-customers")>("@/hooks/use-customers")
  return { ...actual, useCustomers: () => useCustomersMock() }
})

vi.mock("@/hooks/use-plans", () => ({
  usePlans: () => ({ plans: [] }),
}))

vi.mock("@/components/app-sidebar", () => ({
  AppSidebar: () => null,
}))

vi.mock("@/components/health-status", () => ({
  HealthStatus: () => null,
}))

vi.mock("@/components/customer-form-dialog", () => ({
  CustomerFormDialog: () => null,
}))

vi.mock("@/components/ui/sidebar", () => ({
  SidebarProvider: ({ children }: { children: React.ReactNode }) => <div>{children}</div>,
  SidebarInset: ({ children }: { children: React.ReactNode }) => <main>{children}</main>,
  SidebarTrigger: () => <button type="button">Toggle sidebar</button>,
}))

describe("CustomersPage usage-risk filter", () => {
  beforeEach(() => {
    useCustomersMock.mockReturnValue({
      customers,
      loading: false,
      error: null,
      createCustomer: vi.fn(),
      updateCustomer: vi.fn(),
      deleteCustomer: vi.fn(),
    })
  })

  it("filters by the semantic API state and restores all rows", () => {
    render(<CustomersPage />)

    expect(screen.getByText("Ada At Risk")).toBeInTheDocument()
    expect(screen.getByText("Grace Over Limit")).toBeInTheDocument()
    expect(screen.getByText("Annie Unlimited")).toBeInTheDocument()

    fireEvent.click(screen.getByRole("button", { name: "Over limit" }))
    expect(screen.queryByText("Ada At Risk")).not.toBeInTheDocument()
    expect(screen.getByText("Grace Over Limit")).toBeInTheDocument()
    expect(screen.queryByText("Annie Unlimited")).not.toBeInTheDocument()

    fireEvent.click(screen.getByRole("button", { name: "At risk" }))
    expect(screen.getByText("Ada At Risk")).toBeInTheDocument()
    expect(screen.queryByText("Grace Over Limit")).not.toBeInTheDocument()

    fireEvent.click(screen.getByRole("button", { name: "All customers" }))
    expect(screen.getByText("Grace Over Limit")).toBeInTheDocument()
    expect(screen.getByText("Annie Unlimited")).toBeInTheDocument()
  })

  it("reapplies the selected semantic filter when customer data reloads", () => {
    const { rerender } = render(<CustomersPage />)
    fireEvent.click(screen.getByRole("button", { name: "Over limit" }))

    useCustomersMock.mockReturnValue({
      customers: [{ ...customers[0], id: 4, data_usage_state: "OVER_LIMIT" }],
      loading: false,
      error: null,
      createCustomer: vi.fn(),
      updateCustomer: vi.fn(),
      deleteCustomer: vi.fn(),
    })
    rerender(<CustomersPage />)

    expect(screen.getByText("Ada At Risk")).toBeInTheDocument()
  })
})
