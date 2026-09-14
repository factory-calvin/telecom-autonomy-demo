"use client"

import { useState, useEffect, useCallback, useRef } from "react"
import { createLogger } from "@/lib/logger"

const log = createLogger("useTickets")

export type DeadlineState =
  | "ON_TRACK"
  | "DUE_SOON"
  | "ACKNOWLEDGEMENT_OVERDUE"
  | "RESOLUTION_OVERDUE"

export interface Ticket {
  id: number
  customer_id: number
  customer_name: string
  subject: string
  description: string | null
  priority: "LOW" | "MEDIUM" | "HIGH" | "URGENT"
  status: "OPEN" | "IN_PROGRESS" | "RESOLVED" | "CLOSED"
  created_at: string | null
  acknowledged_at: string | null
  resolved_at: string | null
  priority_escalated_at: string | null
  age_hours: number
  age_days: number
  acknowledgement_due_at: string
  resolution_due_at: string
  deadline_state: DeadlineState
}

export interface TicketFilters {
  priority?: "LOW" | "MEDIUM" | "HIGH" | "URGENT" | null
  status?: "OPEN" | "IN_PROGRESS" | "RESOLVED" | "CLOSED" | null
  customerId?: number | null
  deadlineState?: DeadlineState | null
}

export interface TicketCreate {
  customerId: number
  subject: string
  description?: string
  priority: string
}

export interface TicketUpdate {
  customerId?: number
  subject: string
  description?: string
  priority?: string
  status?: string
}

interface PagedResponse {
  content: Ticket[]
  page: number
  totalPages: number
  totalElements: number
  hasNext: boolean
}

export function useTickets(filters: TicketFilters = {}) {
  const [tickets, setTickets] = useState<Ticket[]>([])
  const [loading, setLoading] = useState(true)
  const [loadingMore, setLoadingMore] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [hasNext, setHasNext] = useState(false)
  const [totalElements, setTotalElements] = useState(0)
  const pageRef = useRef(0)

  const buildUrl = useCallback(
    (pageNum: number) => {
      const params = new URLSearchParams()
      params.set("page", pageNum.toString())
      params.set("size", "50")
      if (filters.priority) params.set("priority", filters.priority)
      if (filters.status) params.set("status", filters.status)
      if (filters.customerId) params.set("customerId", filters.customerId.toString())
      if (filters.deadlineState) params.set("deadlineState", filters.deadlineState)
      return `/api/tickets?${params.toString()}`
    },
    [filters.priority, filters.status, filters.customerId, filters.deadlineState]
  )

  const fetchTickets = useCallback(
    async (reset = true) => {
      try {
        if (reset) {
          setLoading(true)
          pageRef.current = 0
        } else {
          setLoadingMore(true)
          pageRef.current += 1
        }

        log.info({ page: pageRef.current }, "Fetching tickets")
        const res = await fetch(buildUrl(pageRef.current))
        if (!res.ok) throw new Error("Failed to fetch tickets")
        const data: PagedResponse = await res.json()

        if (reset) {
          setTickets(data.content || [])
        } else {
          setTickets((prev) => [...prev, ...(data.content || [])])
        }

        setHasNext(data.hasNext ?? false)
        setTotalElements(data.totalElements ?? 0)
        setError(null)
        log.debug(
          { count: data.content?.length, totalElements: data.totalElements },
          "Tickets fetched"
        )
      } catch (e) {
        const message = e instanceof Error ? e.message : "Unknown error"
        log.error({ error: e }, "Failed to fetch tickets")
        setError(message)
      } finally {
        setLoading(false)
        setLoadingMore(false)
      }
    },
    [buildUrl]
  )

  const loadMore = useCallback(() => {
    if (!loadingMore && hasNext) {
      fetchTickets(false)
    }
  }, [fetchTickets, loadingMore, hasNext])

  const createTicket = useCallback(
    async (ticket: TicketCreate) => {
      log.info({ customerId: ticket.customerId, subject: ticket.subject }, "Creating ticket")
      const res = await fetch("/api/tickets", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify(ticket),
      })
      if (!res.ok) {
        log.error({ status: res.status }, "Failed to create ticket")
        throw new Error("Failed to create ticket")
      }
      const newTicket = await res.json()
      log.info({ ticketId: newTicket.id }, "Ticket created")
      fetchTickets(true)
      return newTicket
    },
    [fetchTickets]
  )

  const updateTicket = useCallback(async (id: number, ticket: TicketUpdate) => {
    log.info({ ticketId: id }, "Updating ticket")
    const res = await fetch(`/api/tickets/${id}`, {
      method: "PUT",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify(ticket),
    })
    if (!res.ok) {
      log.error({ ticketId: id, status: res.status }, "Failed to update ticket")
      throw new Error("Failed to update ticket")
    }
    const updated = await res.json()
    log.info({ ticketId: id, status: updated.status }, "Ticket updated")
    setTickets((prev) => prev.map((t) => (t.id === id ? updated : t)))
    return updated
  }, [])

  const deleteTicket = useCallback(async (id: number) => {
    log.info({ ticketId: id }, "Deleting ticket")
    const res = await fetch(`/api/tickets/${id}`, {
      method: "DELETE",
    })
    if (!res.ok) {
      log.error({ ticketId: id, status: res.status }, "Failed to delete ticket")
      throw new Error("Failed to delete ticket")
    }
    log.info({ ticketId: id }, "Ticket deleted")
    setTickets((prev) => prev.filter((t) => t.id !== id))
  }, [])

  useEffect(() => {
    fetchTickets(true)
  }, [fetchTickets])

  return {
    tickets,
    loading,
    loadingMore,
    error,
    hasNext,
    totalElements,
    loadMore,
    refresh: () => fetchTickets(true),
    createTicket,
    updateTicket,
    deleteTicket,
  }
}
