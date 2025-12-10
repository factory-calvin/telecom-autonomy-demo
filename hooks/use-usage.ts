"use client"

import { useState, useEffect, useCallback, useRef } from "react"

export interface UsageRecord {
  id: number
  customer_id: number
  customer_name: string
  type: "CALL" | "DATA" | "SMS"
  quantity: number
  cost: number
  recorded_at: string | null
}

export interface UsageFilters {
  type?: "CALL" | "DATA" | "SMS" | null
  customerId?: number | null
  dateFrom?: string | null
  dateTo?: string | null
}

interface PagedResponse {
  content: UsageRecord[]
  page: number
  totalPages: number
  totalElements: number
  hasNext: boolean
}

export function useUsage(filters: UsageFilters = {}) {
  const [records, setRecords] = useState<UsageRecord[]>([])
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
      if (filters.type) params.set("type", filters.type)
      if (filters.customerId) params.set("customerId", filters.customerId.toString())
      if (filters.dateFrom) params.set("dateFrom", filters.dateFrom)
      if (filters.dateTo) params.set("dateTo", filters.dateTo)
      return `/api/usage?${params.toString()}`
    },
    [filters.type, filters.customerId, filters.dateFrom, filters.dateTo]
  )

  const fetchRecords = useCallback(
    async (reset = true) => {
      try {
        if (reset) {
          setLoading(true)
          pageRef.current = 0
        } else {
          setLoadingMore(true)
          pageRef.current += 1
        }

        const res = await fetch(buildUrl(pageRef.current))
        if (!res.ok) throw new Error("Failed to fetch usage records")
        const data: PagedResponse = await res.json()

        if (reset) {
          setRecords(data.content || [])
        } else {
          setRecords((prev) => [...prev, ...(data.content || [])])
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
    },
    [buildUrl]
  )

  const loadMore = useCallback(() => {
    if (!loadingMore && hasNext) {
      fetchRecords(false)
    }
  }, [fetchRecords, loadingMore, hasNext])

  useEffect(() => {
    fetchRecords(true)
  }, [fetchRecords])

  return {
    records,
    loading,
    loadingMore,
    error,
    hasNext,
    totalElements,
    loadMore,
    refresh: () => fetchRecords(true),
  }
}
