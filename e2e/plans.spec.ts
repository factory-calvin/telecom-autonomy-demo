import { test, expect } from "@playwright/test"

test.describe("Plans Page", () => {
  test.beforeEach(async ({ page }) => {
    await page.goto("/plans")
  })

  test("should display plans table", async ({ page }) => {
    await expect(page.getByLabel("breadcrumb")).toBeVisible()

    const table = page.locator("table")
    await expect(table).toBeVisible()
  })

  test("should load plans from API", async ({ page }) => {
    const plansResponse = page.waitForResponse(
      (response) => response.url().includes("/api/plans") && response.status() === 200
    )
    await page.reload()
    const response = await plansResponse
    const data = await response.json()
    expect(Array.isArray(data)).toBeTruthy()
  })

  test("should display plan details including price", async ({ page }) => {
    await page.waitForSelector("table tbody tr", { timeout: 10000 })
    const rows = page.locator("table tbody tr")
    const rowCount = await rows.count()
    expect(rowCount).toBeGreaterThan(0)

    const priceCell = page.locator("table tbody tr td").filter({ hasText: "$" })
    const priceCount = await priceCell.count()
    expect(priceCount).toBeGreaterThan(0)
  })
})
