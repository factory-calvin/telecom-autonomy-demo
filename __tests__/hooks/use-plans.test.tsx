import { renderHook, waitFor, act } from "@testing-library/react"
import { describe, it, expect, vi, beforeEach, afterEach } from "vitest"
import { usePlans, type Plan } from "@/hooks/use-plans"

const basic: Plan = {
  id: 1,
  name: "Basic",
  monthly_price: 20,
  data_limit_gb: 10,
  minutes_limit: 500,
  sms_limit: 500,
  is_active: true,
}

function jsonResponse(body: unknown, ok = true) {
  return { ok, json: async () => body } as Response
}

describe("usePlans", () => {
  beforeEach(() => {
    vi.stubGlobal("fetch", vi.fn().mockResolvedValue(jsonResponse([basic])))
  })

  afterEach(() => {
    vi.unstubAllGlobals()
  })

  it("loads plans on mount and clears the loading flag", async () => {
    const { result } = renderHook(() => usePlans())

    expect(result.current.loading).toBe(true)
    await waitFor(() => expect(result.current.loading).toBe(false))

    expect(fetch).toHaveBeenCalledWith("/api/plans")
    expect(result.current.plans).toEqual([basic])
    expect(result.current.error).toBeNull()
  })

  it("surfaces an error message when the request fails", async () => {
    vi.stubGlobal("fetch", vi.fn().mockResolvedValue(jsonResponse(null, false)))

    const { result } = renderHook(() => usePlans())

    await waitFor(() => expect(result.current.loading).toBe(false))
    expect(result.current.error).toBe("Failed to fetch plans")
    expect(result.current.plans).toEqual([])
  })

  it("appends the created plan to local state", async () => {
    const { result } = renderHook(() => usePlans())
    await waitFor(() => expect(result.current.loading).toBe(false))

    const created = { ...basic, id: 2, name: "Student" }
    vi.mocked(fetch).mockResolvedValueOnce(jsonResponse(created))

    await act(async () => {
      await result.current.createPlan({ name: "Student", monthlyPrice: 15.5 })
    })

    expect(result.current.plans.map((p) => p.name)).toEqual(["Basic", "Student"])
  })

  it("replaces the updated plan in local state", async () => {
    const { result } = renderHook(() => usePlans())
    await waitFor(() => expect(result.current.loading).toBe(false))

    const updated = { ...basic, name: "Basic Plus" }
    vi.mocked(fetch).mockResolvedValueOnce(jsonResponse(updated))

    await act(async () => {
      await result.current.updatePlan(1, { name: "Basic Plus", monthlyPrice: 25 })
    })

    expect(result.current.plans).toEqual([updated])
  })

  it("removes the deleted plan from local state", async () => {
    const { result } = renderHook(() => usePlans())
    await waitFor(() => expect(result.current.loading).toBe(false))

    vi.mocked(fetch).mockResolvedValueOnce(jsonResponse(null))

    await act(async () => {
      await result.current.deletePlan(1)
    })

    expect(result.current.plans).toEqual([])
  })

  it("throws and leaves state untouched when a delete fails", async () => {
    const { result } = renderHook(() => usePlans())
    await waitFor(() => expect(result.current.loading).toBe(false))

    vi.mocked(fetch).mockResolvedValueOnce(jsonResponse(null, false))

    await expect(result.current.deletePlan(1)).rejects.toThrow("Failed to delete plan")
    expect(result.current.plans).toEqual([basic])
  })
})
