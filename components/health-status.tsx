"use client"

import { useEffect, useState } from "react"

interface HealthData {
  status: string
  service: string
  version: string
}

export function HealthStatus() {
  const [health, setHealth] = useState<HealthData | null>(null)
  const [error, setError] = useState<string | null>(null)
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    const checkHealth = async () => {
      try {
        const res = await fetch("/api/health")
        if (!res.ok) throw new Error("API unavailable")
        const data = await res.json()
        setHealth(data)
        setError(null)
      } catch {
        setError("Backend offline")
        setHealth(null)
      } finally {
        setLoading(false)
      }
    }

    checkHealth()
    const interval = setInterval(checkHealth, 30000)
    return () => clearInterval(interval)
  }, [])

  if (loading) {
    return (
      <div className="text-muted-foreground flex items-center gap-2">
        <div className="h-2 w-2 animate-pulse rounded-full bg-yellow-500" />
        <span className="text-sm">Checking...</span>
      </div>
    )
  }

  if (error) {
    return (
      <div className="flex items-center gap-2">
        <div className="h-2 w-2 rounded-full bg-red-500" />
        <span className="text-sm text-red-500">{error}</span>
      </div>
    )
  }

  return (
    <div className="flex items-center gap-2">
      <div className="h-2 w-2 rounded-full bg-green-500" />
      <span className="text-muted-foreground text-sm">
        {health?.service} v{health?.version}
      </span>
    </div>
  )
}
