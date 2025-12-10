"use client"

import { useState, useRef, useEffect, useCallback } from "react"
import { AppSidebar } from "@/components/app-sidebar"
import {
  Breadcrumb,
  BreadcrumbItem,
  BreadcrumbLink,
  BreadcrumbList,
  BreadcrumbPage,
  BreadcrumbSeparator,
} from "@/components/ui/breadcrumb"
import { Separator } from "@/components/ui/separator"
import {
  SidebarInset,
  SidebarProvider,
  SidebarTrigger,
} from "@/components/ui/sidebar"
import { Button } from "@/components/ui/button"
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card"
import { Input } from "@/components/ui/input"
import { Label } from "@/components/ui/label"
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select"
import { Plus, X, Loader2 } from "lucide-react"
import { HealthStatus } from "@/components/health-status"
import { TicketsTable } from "@/components/tickets-table"
import { useTickets, type Ticket, type TicketFilters } from "@/hooks/use-tickets"

export default function TicketsPage() {
  const [filters, setFilters] = useState<TicketFilters>({})
  const { tickets, loading, loadingMore, error, hasNext, totalElements, loadMore, deleteTicket } = useTickets(filters)
  const scrollRef = useRef<HTMLDivElement>(null)

  const handleScroll = useCallback(() => {
    const el = scrollRef.current
    if (!el || loadingMore || !hasNext) return
    const { scrollTop, scrollHeight, clientHeight } = el
    if (scrollHeight - scrollTop - clientHeight < 200) {
      loadMore()
    }
  }, [loadMore, loadingMore, hasNext])

  useEffect(() => {
    const el = scrollRef.current
    if (!el) return
    el.addEventListener("scroll", handleScroll)
    return () => el.removeEventListener("scroll", handleScroll)
  }, [handleScroll])

  const clearFilters = () => setFilters({})
  const hasActiveFilters = filters.priority || filters.status || filters.customerId

  const handleEdit = (ticket: Ticket) => {
    alert(`Edit ticket: ${ticket.subject}`)
  }

  const handleDelete = async (ticket: Ticket) => {
    if (confirm(`Delete ticket "${ticket.subject}"?`)) {
      await deleteTicket(ticket.id)
    }
  }

  return (
    <SidebarProvider className="h-svh">
      <AppSidebar />
      <SidebarInset className="overflow-hidden">
        <header className="flex h-16 shrink-0 items-center gap-2 transition-[width,height] ease-linear group-has-data-[collapsible=icon]/sidebar-wrapper:h-12">
          <div className="flex items-center gap-2 px-4 flex-1">
            <SidebarTrigger className="-ml-1" />
            <Separator orientation="vertical" className="mr-2 data-[orientation=vertical]:h-4" />
            <Breadcrumb>
              <BreadcrumbList>
                <BreadcrumbItem className="hidden md:block">
                  <BreadcrumbLink href="/dashboard">Dashboard</BreadcrumbLink>
                </BreadcrumbItem>
                <BreadcrumbSeparator className="hidden md:block" />
                <BreadcrumbItem>
                  <BreadcrumbPage>Support Tickets</BreadcrumbPage>
                </BreadcrumbItem>
              </BreadcrumbList>
            </Breadcrumb>
            <div className="ml-auto">
              <HealthStatus />
            </div>
          </div>
        </header>
        <div className="flex flex-1 flex-col gap-4 p-4 pt-0 overflow-hidden">
          <Card className="flex flex-col flex-1 overflow-hidden">
            <CardHeader className="shrink-0 space-y-4">
              <div className="flex items-center justify-between">
                <CardTitle className="text-headline-2">Support Tickets</CardTitle>
                <div className="flex items-center gap-4">
                  <span className="text-sm text-muted-foreground">
                    {loading ? "Loading..." : `${(tickets?.length ?? 0).toLocaleString()} of ${(totalElements ?? 0).toLocaleString()} tickets`}
                  </span>
                  <Button>
                    <Plus className="h-4 w-4 mr-2" />
                    Create Ticket
                  </Button>
                </div>
              </div>
              <div className="flex flex-wrap items-end gap-4">
                <div className="space-y-1">
                  <Label htmlFor="priority-filter">Priority</Label>
                  <Select
                    value={filters.priority || "all"}
                    onValueChange={(v: string) => setFilters(f => ({ ...f, priority: v === "all" ? null : v as TicketFilters["priority"] }))}
                  >
                    <SelectTrigger id="priority-filter" className="w-[120px]">
                      <SelectValue />
                    </SelectTrigger>
                    <SelectContent>
                      <SelectItem value="all">All</SelectItem>
                      <SelectItem value="LOW">Low</SelectItem>
                      <SelectItem value="MEDIUM">Medium</SelectItem>
                      <SelectItem value="HIGH">High</SelectItem>
                      <SelectItem value="URGENT">Urgent</SelectItem>
                    </SelectContent>
                  </Select>
                </div>
                <div className="space-y-1">
                  <Label htmlFor="status-filter">Status</Label>
                  <Select
                    value={filters.status || "all"}
                    onValueChange={(v: string) => setFilters(f => ({ ...f, status: v === "all" ? null : v as TicketFilters["status"] }))}
                  >
                    <SelectTrigger id="status-filter" className="w-[140px]">
                      <SelectValue />
                    </SelectTrigger>
                    <SelectContent>
                      <SelectItem value="all">All</SelectItem>
                      <SelectItem value="OPEN">Open</SelectItem>
                      <SelectItem value="IN_PROGRESS">In Progress</SelectItem>
                      <SelectItem value="RESOLVED">Resolved</SelectItem>
                      <SelectItem value="CLOSED">Closed</SelectItem>
                    </SelectContent>
                  </Select>
                </div>
                <div className="space-y-1">
                  <Label htmlFor="customer-filter">Customer ID</Label>
                  <Input
                    id="customer-filter"
                    type="number"
                    placeholder="Any"
                    className="w-[100px]"
                    value={filters.customerId || ""}
                    onChange={(e) => setFilters(f => ({ ...f, customerId: e.target.value ? Number(e.target.value) : null }))}
                  />
                </div>
                {hasActiveFilters && (
                  <Button variant="ghost" size="sm" onClick={clearFilters}>
                    <X className="h-4 w-4 mr-1" />
                    Clear
                  </Button>
                )}
              </div>
            </CardHeader>
            <CardContent className="flex-1 overflow-hidden">
              {loading ? (
                <div className="flex items-center justify-center h-full text-muted-foreground">
                  <Loader2 className="h-6 w-6 animate-spin mr-2" />
                  Loading...
                </div>
              ) : error ? (
                <div className="text-center py-8 text-red-500">{error}</div>
              ) : (
                <div ref={scrollRef} className="h-full overflow-auto">
                  <TicketsTable tickets={tickets} onEdit={handleEdit} onDelete={handleDelete} />
                  {loadingMore && (
                    <div className="flex items-center justify-center py-4 text-muted-foreground">
                      <Loader2 className="h-4 w-4 animate-spin mr-2" />
                      Loading more...
                    </div>
                  )}
                  {!hasNext && tickets.length > 0 && (
                    <div className="text-center py-4 text-muted-foreground text-sm">
                      End of results
                    </div>
                  )}
                </div>
              )}
            </CardContent>
          </Card>
        </div>
      </SidebarInset>
    </SidebarProvider>
  )
}
