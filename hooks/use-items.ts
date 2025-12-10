"use client"

import { useState, useEffect, useCallback } from "react"

const API_BASE = "http://localhost:8000"

export interface Item {
  id: number
  name: string
  description: string | null
  created_at: string
  updated_at: string | null
}

export interface ItemCreate {
  name: string
  description?: string | null
}

export interface ItemUpdate {
  name: string
  description?: string | null
}

export function useItems() {
  const [items, setItems] = useState<Item[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)

  const fetchItems = useCallback(async () => {
    try {
      setLoading(true)
      const res = await fetch(`${API_BASE}/api/items`)
      if (!res.ok) throw new Error("Failed to fetch items")
      const data = await res.json()
      setItems(data)
      setError(null)
    } catch (e) {
      setError(e instanceof Error ? e.message : "Unknown error")
    } finally {
      setLoading(false)
    }
  }, [])

  const createItem = useCallback(async (item: ItemCreate) => {
    const res = await fetch(`${API_BASE}/api/items`, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify(item),
    })
    if (!res.ok) throw new Error("Failed to create item")
    const newItem = await res.json()
    setItems((prev) => [...prev, newItem])
    return newItem
  }, [])

  const updateItem = useCallback(async (id: number, item: ItemUpdate) => {
    const res = await fetch(`${API_BASE}/api/items/${id}`, {
      method: "PUT",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify(item),
    })
    if (!res.ok) throw new Error("Failed to update item")
    const updated = await res.json()
    setItems((prev) => prev.map((i) => (i.id === id ? updated : i)))
    return updated
  }, [])

  const deleteItem = useCallback(async (id: number) => {
    const res = await fetch(`${API_BASE}/api/items/${id}`, {
      method: "DELETE",
    })
    if (!res.ok) throw new Error("Failed to delete item")
    setItems((prev) => prev.filter((i) => i.id !== id))
  }, [])

  useEffect(() => {
    fetchItems()
  }, [fetchItems])

  return {
    items,
    loading,
    error,
    fetchItems,
    createItem,
    updateItem,
    deleteItem,
  }
}
