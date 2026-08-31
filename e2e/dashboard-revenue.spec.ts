import { test, expect } from "@playwright/test"

const BACKEND = "http://localhost:8080"

interface ApiCustomer {
  status: string
  plan_name: string | null
}

interface ApiPlan {
  name: string
  monthly_price: number
}

/**
 * Cross-checks the dashboard aggregates against the raw customer and plan lists.
 * The seeded database contains suspended and cancelled subscribers, so any
 * aggregation that forgets to filter on ACTIVE shows up here as a mismatch.
 */
test.describe("Dashboard revenue aggregates", () => {
  test("monthly revenue counts only active subscribers", async ({ request }) => {
    const [statsRes, customersRes, plansRes] = await Promise.all([
      request.get(`${BACKEND}/api/dashboard/stats`),
      request.get(`${BACKEND}/api/customers`),
      request.get(`${BACKEND}/api/plans`),
    ])
    expect(statsRes.ok()).toBeTruthy()
    expect(customersRes.ok()).toBeTruthy()
    expect(plansRes.ok()).toBeTruthy()

    const stats = await statsRes.json()
    const customers: ApiCustomer[] = await customersRes.json()
    const plans: ApiPlan[] = await plansRes.json()

    const priceByPlan = new Map(plans.map((p) => [p.name, p.monthly_price]))
    const expected = customers
      .filter((c) => c.status === "ACTIVE" && c.plan_name !== null)
      .reduce((sum, c) => sum + (priceByPlan.get(c.plan_name as string) ?? 0), 0)

    expect(Number(stats.monthly_revenue)).toBeCloseTo(expected, 2)
  })

  test("active customer count matches the customer list", async ({ request }) => {
    const [statsRes, customersRes] = await Promise.all([
      request.get(`${BACKEND}/api/dashboard/stats`),
      request.get(`${BACKEND}/api/customers`),
    ])

    const stats = await statsRes.json()
    const customers: ApiCustomer[] = await customersRes.json()
    const activeCount = customers.filter((c) => c.status === "ACTIVE").length

    expect(stats.active_customers).toBe(activeCount)
  })

  test("revenue by plan excludes plans with no active subscribers", async ({ request }) => {
    const [revenueRes, customersRes] = await Promise.all([
      request.get(`${BACKEND}/api/dashboard/revenue-by-plan`),
      request.get(`${BACKEND}/api/customers`),
    ])

    const revenue: { name: string; revenue: number }[] = await revenueRes.json()
    const customers: ApiCustomer[] = await customersRes.json()
    const plansWithActive = new Set(
      customers.filter((c) => c.status === "ACTIVE").map((c) => c.plan_name)
    )

    for (const row of revenue) {
      expect(plansWithActive.has(row.name)).toBeTruthy()
    }
  })
})
