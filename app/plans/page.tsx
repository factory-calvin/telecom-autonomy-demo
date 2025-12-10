"use client"

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
import { Plus } from "lucide-react"
import { HealthStatus } from "@/components/health-status"
import { PlansTable } from "@/components/plans-table"
import { usePlans, type Plan } from "@/hooks/use-plans"

export default function PlansPage() {
  const { plans, loading, error, deletePlan } = usePlans()

  const handleEdit = (plan: Plan) => {
    alert(`Edit plan: ${plan.name}`)
  }

  const handleDelete = async (plan: Plan) => {
    if (confirm(`Delete "${plan.name}"?`)) {
      await deletePlan(plan.id)
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
                  <BreadcrumbPage>Plans</BreadcrumbPage>
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
            <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-4 shrink-0">
              <CardTitle className="text-headline-2">Service Plans</CardTitle>
              <Button>
                <Plus className="h-4 w-4 mr-2" />
                Add Plan
              </Button>
            </CardHeader>
            <CardContent className="flex-1 overflow-hidden">
              {loading ? (
                <div className="text-center py-8 text-muted-foreground">Loading...</div>
              ) : error ? (
                <div className="text-center py-8 text-red-500">{error}</div>
              ) : (
                <div className="h-full overflow-auto">
                  <PlansTable plans={plans} onEdit={handleEdit} onDelete={handleDelete} />
                </div>
              )}
            </CardContent>
          </Card>
        </div>
      </SidebarInset>
    </SidebarProvider>
  )
}
