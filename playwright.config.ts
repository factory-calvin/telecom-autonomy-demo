import { defineConfig, devices } from "@playwright/test"

export default defineConfig({
  testDir: "./e2e",
  fullyParallel: true,
  forbidOnly: !!process.env.CI,
  retries: process.env.CI ? 2 : 0,
  workers: process.env.CI ? 1 : undefined,
  reporter: [["html", { open: "never" }], ["list"]],
  use: {
    baseURL: "http://localhost:3000",
    trace: "on-first-retry",
    screenshot: "only-on-failure",
  },
  projects: [
    {
      name: "chromium",
      use: { ...devices["Desktop Chrome"] },
    },
  ],
  webServer: [
    {
      command: "pnpm dev:backend",
      url: "http://localhost:8080/api/health",
      reuseExistingServer: !process.env.CI,
      timeout: 120000,
    },
    {
      command: "pnpm dev:frontend",
      url: "http://localhost:3000",
      reuseExistingServer: !process.env.CI,
      timeout: 120000,
    },
  ],
})
