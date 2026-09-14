import { act, renderHook, waitFor } from "@testing-library/react"
import { afterEach, describe, expect, it, vi } from "vitest"
import { useTickets, type Ticket } from "@/hooks/use-tickets"

const ticket: Ticket = {
  id: 17,
  customer_id: 4,
  customer_name: "Ada Lovelace",
  subject: "Intermittent service",
  description: null,
  priority: "HIGH",
  status: "OPEN",
  created_at: "2026-09-01T10:00:00Z",
  acknowledged_at: null,
  resolved_at: null,
  priority_escalated_at: null,
  age_hours: 312,
  age_days: 13,
  acknowledgement_due_at: "2026-09-03T10:00:00Z",
  resolution_due_at: "2026-09-15T10:00:00Z",
  deadline_state: "ACKNOWLEDGEMENT_OVERDUE",
}

function response(content: Ticket[], page = 0, hasNext = false) {
  return {
    ok: true,
    json: async () => ({
      content,
      page,
      totalPages: hasNext ? 2 : 1,
      totalElements: hasNext ? 2 : content.length,
      hasNext,
      as_of: "2026-09-14T10:00:00Z",
    }),
  } as Response
}

describe("useTickets", () => {
  afterEach(() => {
    vi.unstubAllGlobals()
  })

  it("passes the semantic deadlineState to the API and preserves projected values", async () => {
    vi.stubGlobal(
      "fetch",
      vi.fn(async () => response([ticket]))
    )

    const { result } = renderHook(() => useTickets({ deadlineState: "ACKNOWLEDGEMENT_OVERDUE" }))

    await waitFor(() => expect(result.current.loading).toBe(false))

    expect(fetch).toHaveBeenCalledWith(
      "/api/tickets?page=0&size=50&deadlineState=ACKNOWLEDGEMENT_OVERDUE"
    )
    expect(result.current.tickets[0]).toMatchObject({
      age_hours: 312,
      age_days: 13,
      deadline_state: "ACKNOWLEDGEMENT_OVERDUE",
    })
  })

  it("keeps pagination metadata and appends the next API page", async () => {
    const secondTicket = { ...ticket, id: 18, deadline_state: "DUE_SOON" as const }
    vi.stubGlobal(
      "fetch",
      vi
        .fn()
        .mockResolvedValueOnce(response([ticket], 0, true))
        .mockResolvedValueOnce(response([secondTicket], 1, false))
    )

    const { result } = renderHook(() => useTickets())
    await waitFor(() => expect(result.current.loading).toBe(false))
    expect(result.current.hasNext).toBe(true)
    expect(result.current.totalElements).toBe(2)

    act(() => result.current.loadMore())
    await waitFor(() => expect(result.current.loadingMore).toBe(false))

    expect(fetch).toHaveBeenLastCalledWith("/api/tickets?page=1&size=50")
    expect(result.current.tickets.map(({ id }) => id)).toEqual([17, 18])
    expect(result.current.hasNext).toBe(false)
  })

  it("handles empty and error responses", async () => {
    vi.stubGlobal(
      "fetch",
      vi.fn(async () => response([]))
    )
    const empty = renderHook(() => useTickets())
    await waitFor(() => expect(empty.result.current.loading).toBe(false))
    expect(empty.result.current.tickets).toEqual([])
    empty.unmount()

    vi.stubGlobal(
      "fetch",
      vi.fn(async () => ({ ok: false }) as Response)
    )
    const failed = renderHook(() => useTickets())
    await waitFor(() => expect(failed.result.current.loading).toBe(false))
    expect(failed.result.current.error).toBe("Failed to fetch tickets")
  })
})
