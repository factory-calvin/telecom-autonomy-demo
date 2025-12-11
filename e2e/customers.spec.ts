import { test, expect } from "@playwright/test"

test.describe("Customers Page", () => {
  test.beforeEach(async ({ page }) => {
    await page.goto("/customers")
  })

  test("should display customers table", async ({ page }) => {
    await expect(page.getByLabel("breadcrumb")).toBeVisible()

    const table = page.locator("table")
    await expect(table).toBeVisible()
  })

  test("should load customers from API", async ({ page }) => {
    const customersResponse = page.waitForResponse(
      (response) => response.url().includes("/api/customers") && response.status() === 200
    )
    await page.reload()
    const response = await customersResponse
    const data = await response.json()
    expect(Array.isArray(data)).toBeTruthy()
  })

  test("should display customer data in table rows", async ({ page }) => {
    await page.waitForSelector("table tbody tr", { timeout: 10000 })
    const rows = page.locator("table tbody tr")
    const rowCount = await rows.count()
    expect(rowCount).toBeGreaterThan(0)
  })

  test("should have add customer button", async ({ page }) => {
    const addButton = page.getByRole("button", { name: /add|new|create/i })
    await expect(addButton).toBeVisible()
  })
})
