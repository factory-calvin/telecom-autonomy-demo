"use client"

import { useState, useEffect, useCallback, useRef } from "react"

export interface Ticket {
  id: number
  customer_id: number
  customer_name: string
  subject: string
  description: string | null
  priority: "LOW" | "MEDIUM" | "HIGH" | "URGENT"
  status: "OPEN" | "IN_PROGRESS" | "RESOLVED" | "CLOSED"
  created_at: string | null
  resolved_at: string | null
}

export interface TicketFilters {
  priority?: "LOW" | "MEDIUM" | "HIGH" | "URGENT" | null
  status?: "OPEN" | "IN_PROGRESS" | "RESOLVED" | "CLOSED" | null
  customerId?: number | null
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

  const buildUrl = useCallback((pageNum: number) => {
    const params = new URLSearchParams()
    params.set("page", pageNum.toString())
    params.set("size", "50")
    if (filters.priority) params.set("priority", filters.priority)
    if (filters.status) params.set("status", filters.status)
    if (filters.customerId) params.set("customerId", filters.customerId.toString())
    return `/api/tickets?${params.toString()}`
  }, [filters.priority, filters.status, filters.customerId])

  const fetchTickets = useCallback(async (reset = true) => {
    try {
      if (reset) {
        setLoading(true)
        pageRef.current = 0
      } else {
        setLoadingMore(true)
        pageRef.current += 1
      }

      const res = await fetch(buildUrl(pageRef.current))
      if (!res.ok) throw new Error("Failed to fetch tickets")
      const data: PagedResponse = await res.json()

      if (reset) {
        setTickets(data.content || [])
      } else {
        setTickets(prev => [...prev, ...(data.content || [])])
      }

      setHasNext(data.hasNext ?? false)
      setTotalElements(data.totalElements ?? 0)
      setError(null)
    } catch (e) {
      setError(e instanceof Error ? e.message : "Unknown error")
    } finally {
      setLoading(false)
      setLoadingMore(false)
    }
  }, [buildUrl])

  const loadMore = useCallback(() => {
    if (!loadingMore && hasNext) {
      fetchTickets(false)
    }
  }, [fetchTickets, loadingMore, hasNext])

  const createTicket = useCallback(async (ticket: TicketCreate) => {
    const res = await fetch("/api/tickets", {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify(ticket),
    })
    if (!res.ok) throw new Error("Failed to create ticket")
    const newTicket = await res.json()
    fetchTickets(true)
    return newTicket
  }, [fetchTickets])

  const updateTicket = useCallback(async (id: number, ticket: TicketUpdate) => {
    const res = await fetch(`/api/tickets/${id}`, {
      method: "PUT",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify(ticket),
    })
    if (!res.ok) throw new Error("Failed to update ticket")
    const updated = await res.json()
    setTickets((prev) => prev.map((t) => (t.id === id ? updated : t)))
    return updated
  }, [])

  const deleteTicket = useCallback(async (id: number) => {
    const res = await fetch(`/api/tickets/${id}`, {
      method: "DELETE",
    })
    if (!res.ok) throw new Error("Failed to delete ticket")
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
