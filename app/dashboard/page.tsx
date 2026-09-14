"use client"

import type { ReactNode } from "react"
import { AppSidebar } from "@/components/app-sidebar"
import {
  Breadcrumb,
  BreadcrumbItem,
  BreadcrumbList,
  BreadcrumbPage,
} from "@/components/ui/breadcrumb"
import { Separator } from "@/components/ui/separator"
import { SidebarInset, SidebarProvider, SidebarTrigger } from "@/components/ui/sidebar"
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card"
import { HealthStatus } from "@/components/health-status"
import { useDashboardStats } from "@/hooks/use-dashboard-stats"
import {
  Users,
  DollarSign,
  Ticket,
  Smartphone,
  TriangleAlert,
  CircleAlert,
  Clock3,
  MessageSquareWarning,
  Siren,
} from "lucide-react"
import {
  ChartContainer,
  ChartTooltip,
  ChartTooltipContent,
  type ChartConfig,
} from "@/components/ui/chart"
import { Bar, BarChart, Pie, PieChart, XAxis, YAxis, CartesianGrid } from "recharts"

const customersByPlanConfig: ChartConfig = {
  value: { label: "Customers" },
  Basic: { label: "Basic", color: "var(--chart-1)" },
  Standard: { label: "Standard", color: "var(--chart-2)" },
  Premium: { label: "Premium", color: "var(--chart-3)" },
  Unlimited: { label: "Unlimited", color: "var(--chart-4)" },
  Family: { label: "Family", color: "var(--chart-5)" },
  "Business Starter": { label: "Business Starter", color: "var(--chart-6)" },
  "Business Pro": { label: "Business Pro", color: "var(--chart-7)" },
  Student: { label: "Student", color: "var(--chart-8)" },
}

const revenueByPlanConfig: ChartConfig = {
  revenue: { label: "Revenue" },
  Basic: { label: "Basic", color: "var(--chart-1)" },
  Standard: { label: "Standard", color: "var(--chart-2)" },
  Premium: { label: "Premium", color: "var(--chart-3)" },
  Unlimited: { label: "Unlimited", color: "var(--chart-4)" },
  Family: { label: "Family", color: "var(--chart-5)" },
  "Business Starter": { label: "Business Starter", color: "var(--chart-6)" },
  "Business Pro": { label: "Business Pro", color: "var(--chart-7)" },
  Student: { label: "Student", color: "var(--chart-8)" },
}

const devicesByStatusConfig: ChartConfig = {
  value: { label: "Devices" },
  ASSIGNED: { label: "Assigned", color: "var(--chart-1)" },
  AVAILABLE: { label: "Available", color: "var(--chart-3)" },
  DAMAGED: { label: "Damaged", color: "var(--chart-5)" },
  LOST: { label: "Lost", color: "#e82010" },
}

const ticketsByStatusConfig: ChartConfig = {
  value: { label: "Tickets" },
  OPEN: { label: "Open", color: "var(--chart-5)" },
  IN_PROGRESS: { label: "In Progress", color: "var(--chart-1)" },
  RESOLVED: { label: "Resolved", color: "var(--chart-3)" },
  CLOSED: { label: "Closed", color: "var(--chart-4)" },
}

interface KpiCardProps {
  title: string
  value: ReactNode
  description: string
  icon: ReactNode
}

function KpiCard({ title, value, description, icon }: KpiCardProps) {
  return (
    <Card>
      <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
        <CardTitle className="text-body-small">{title}</CardTitle>
        {icon}
      </CardHeader>
      <CardContent>
        <div className="text-headline-2">{value}</div>
        <p className="text-caption text-muted-foreground">{description}</p>
      </CardContent>
    </Card>
  )
}

export default function DashboardPage() {
  const {
    stats,
    customersByPlan,
    devicesByStatus,
    ticketsByStatus,
    revenueByPlan,
    loading,
    error,
  } = useDashboardStats()

  if (loading) {
    return (
      <SidebarProvider>
        <AppSidebar />
        <SidebarInset>
          <div className="flex h-screen items-center justify-center">
            <div className="text-muted-foreground">Loading dashboard...</div>
          </div>
        </SidebarInset>
      </SidebarProvider>
    )
  }

  if (error) {
    return (
      <SidebarProvider>
        <AppSidebar />
        <SidebarInset>
          <div className="flex h-screen items-center justify-center">
            <div className="text-red-500">{error}</div>
          </div>
        </SidebarInset>
      </SidebarProvider>
    )
  }

  return (
    <SidebarProvider>
      <AppSidebar />
      <SidebarInset>
        <header className="flex h-16 shrink-0 items-center gap-2 transition-[width,height] ease-linear group-has-data-[collapsible=icon]/sidebar-wrapper:h-12">
          <div className="flex flex-1 items-center gap-2 px-4">
            <SidebarTrigger className="-ml-1" />
            <Separator orientation="vertical" className="mr-2 data-[orientation=vertical]:h-4" />
            <Breadcrumb>
              <BreadcrumbList>
                <BreadcrumbItem>
                  <BreadcrumbPage>Dashboard</BreadcrumbPage>
                </BreadcrumbItem>
              </BreadcrumbList>
            </Breadcrumb>
            <div className="ml-auto">
              <HealthStatus />
            </div>
          </div>
        </header>

        <div className="flex flex-1 flex-col gap-4 p-4 pt-0">
          {/* KPI Cards */}
          <div className="grid gap-4 md:grid-cols-2 lg:grid-cols-3">
            <KpiCard
              title="Active Customers"
              value={stats?.active_customers || 0}
              description="Total active subscribers"
              icon={<Users className="text-muted-foreground h-4 w-4" />}
            />
            <KpiCard
              title="Monthly Revenue"
              value={`$${stats?.monthly_revenue?.toFixed(2) || "0.00"}`}
              description="From active subscriptions"
              icon={<DollarSign className="text-muted-foreground h-4 w-4" />}
            />
            <KpiCard
              title="Open Tickets"
              value={stats?.open_tickets || 0}
              description="Requiring attention"
              icon={<Ticket className="text-muted-foreground h-4 w-4" />}
            />
            <KpiCard
              title="Devices in Use"
              value={stats?.devices_in_use || 0}
              description="Assigned to customers"
              icon={<Smartphone className="text-muted-foreground h-4 w-4" />}
            />
            <KpiCard
              title="At-risk customers"
              value={stats?.at_risk_customers ?? 0}
              description="At or above 80% this cycle"
              icon={<TriangleAlert className="h-4 w-4 text-amber-600 dark:text-amber-400" />}
            />
            <KpiCard
              title="Over-limit customers"
              value={stats?.over_limit_customers ?? 0}
              description="At or above 100% this cycle"
              icon={<CircleAlert className="h-4 w-4 text-red-600 dark:text-red-400" />}
            />
            <KpiCard
              title="Acknowledgement overdue"
              value={stats?.acknowledgement_overdue_tickets ?? 0}
              description="Current ticket count"
              icon={
                <MessageSquareWarning className="h-4 w-4 text-orange-600 dark:text-orange-400" />
              }
            />
            <KpiCard
              title="Resolution overdue"
              value={stats?.resolution_overdue_tickets ?? 0}
              description="Current ticket count"
              icon={<Siren className="h-4 w-4 text-red-600 dark:text-red-400" />}
            />
            <KpiCard
              title="Due soon"
              value={stats?.due_soon_tickets ?? 0}
              description="Current ticket count"
              icon={<Clock3 className="h-4 w-4 text-amber-600 dark:text-amber-400" />}
            />
          </div>

          {/* Charts Row 1 */}
          <div className="grid gap-4 md:grid-cols-2">
            <Card>
              <CardHeader>
                <CardTitle className="text-headline-3">Customers by Plan</CardTitle>
              </CardHeader>
              <CardContent>
                <ChartContainer config={customersByPlanConfig} className="mx-auto h-[250px] w-full">
                  <PieChart accessibilityLayer>
                    <ChartTooltip
                      cursor={false}
                      content={<ChartTooltipContent hideLabel nameKey="name" />}
                    />
                    <Pie
                      data={customersByPlan.map((item) => ({
                        ...item,
                        fill:
                          customersByPlanConfig[item.name as keyof typeof customersByPlanConfig]
                            ?.color || "var(--chart-1)",
                      }))}
                      dataKey="value"
                      nameKey="name"
                      innerRadius={60}
                      outerRadius={100}
                    />
                  </PieChart>
                </ChartContainer>
              </CardContent>
            </Card>

            <Card>
              <CardHeader>
                <CardTitle className="text-headline-3">Revenue by Plan</CardTitle>
              </CardHeader>
              <CardContent>
                <ChartContainer config={revenueByPlanConfig} className="h-[250px] w-full">
                  <BarChart
                    accessibilityLayer
                    data={revenueByPlan.map((item) => ({
                      ...item,
                      fill:
                        revenueByPlanConfig[item.name as keyof typeof revenueByPlanConfig]?.color ||
                        "var(--chart-1)",
                    }))}
                  >
                    <CartesianGrid vertical={false} />
                    <XAxis dataKey="name" tickLine={false} axisLine={false} tickMargin={10} />
                    <YAxis
                      tickLine={false}
                      axisLine={false}
                      tickFormatter={(value) => `$${value}`}
                    />
                    <ChartTooltip content={<ChartTooltipContent nameKey="name" />} />
                    <Bar dataKey="revenue" radius={4} />
                  </BarChart>
                </ChartContainer>
              </CardContent>
            </Card>
          </div>

          {/* Charts Row 2 */}
          <div className="grid gap-4 md:grid-cols-2">
            <Card>
              <CardHeader>
                <CardTitle className="text-headline-3">Device Status</CardTitle>
              </CardHeader>
              <CardContent>
                <ChartContainer config={devicesByStatusConfig} className="h-[250px] w-full">
                  <BarChart
                    accessibilityLayer
                    data={devicesByStatus.map((item) => ({
                      ...item,
                      fill:
                        devicesByStatusConfig[item.name as keyof typeof devicesByStatusConfig]
                          ?.color || "var(--chart-4)",
                    }))}
                    layout="vertical"
                  >
                    <CartesianGrid horizontal={false} />
                    <XAxis type="number" tickLine={false} axisLine={false} />
                    <YAxis
                      dataKey="name"
                      type="category"
                      tickLine={false}
                      axisLine={false}
                      tickMargin={10}
                      width={85}
                    />
                    <ChartTooltip content={<ChartTooltipContent nameKey="name" />} />
                    <Bar dataKey="value" radius={4} />
                  </BarChart>
                </ChartContainer>
              </CardContent>
            </Card>

            <Card>
              <CardHeader>
                <CardTitle className="text-headline-3">Ticket Status</CardTitle>
              </CardHeader>
              <CardContent>
                <ChartContainer config={ticketsByStatusConfig} className="mx-auto h-[250px] w-full">
                  <PieChart accessibilityLayer>
                    <ChartTooltip
                      cursor={false}
                      content={<ChartTooltipContent hideLabel nameKey="name" />}
                    />
                    <Pie
                      data={ticketsByStatus.map((item) => ({
                        ...item,
                        fill:
                          ticketsByStatusConfig[item.name as keyof typeof ticketsByStatusConfig]
                            ?.color || "var(--chart-4)",
                      }))}
                      dataKey="value"
                      nameKey="name"
                      innerRadius={60}
                      outerRadius={100}
                    />
                  </PieChart>
                </ChartContainer>
              </CardContent>
            </Card>
          </div>
        </div>
      </SidebarInset>
    </SidebarProvider>
  )
}
