import { test, expect } from "@playwright/test"

test.describe("API Health", () => {
  test("backend health endpoint should respond", async ({ request }) => {
    const response = await request.get("http://localhost:8080/api/health")
    expect(response.ok()).toBeTruthy()
  })

  test("customers API should respond", async ({ request }) => {
    const response = await request.get("http://localhost:8080/api/customers")
    expect(response.ok()).toBeTruthy()
    const data = await response.json()
    expect(Array.isArray(data)).toBeTruthy()
  })

  test("plans API should respond", async ({ request }) => {
    const response = await request.get("http://localhost:8080/api/plans")
    expect(response.ok()).toBeTruthy()
    const data = await response.json()
    expect(Array.isArray(data)).toBeTruthy()
  })

  test("devices API should respond", async ({ request }) => {
    const response = await request.get("http://localhost:8080/api/devices")
    expect(response.ok()).toBeTruthy()
    const data = await response.json()
    expect(Array.isArray(data)).toBeTruthy()
  })

  test("dashboard stats API should respond", async ({ request }) => {
    const response = await request.get("http://localhost:8080/api/dashboard/stats")
    expect(response.ok()).toBeTruthy()
  })
})
