"use client"

import { useState } from "react"
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
import { SidebarInset, SidebarProvider, SidebarTrigger } from "@/components/ui/sidebar"
import { Button } from "@/components/ui/button"
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card"
import {
  AlertDialog,
  AlertDialogAction,
  AlertDialogCancel,
  AlertDialogContent,
  AlertDialogDescription,
  AlertDialogFooter,
  AlertDialogHeader,
  AlertDialogTitle,
} from "@/components/ui/alert-dialog"
import { Plus } from "lucide-react"
import { HealthStatus } from "@/components/health-status"
import { CustomersTable } from "@/components/customers-table"
import { CustomerFormDialog } from "@/components/customer-form-dialog"
import {
  useCustomers,
  type Customer,
  type CustomerCreate,
  type CustomerUpdate,
} from "@/hooks/use-customers"
import { usePlans } from "@/hooks/use-plans"

type UsageRiskFilter = "ALL" | "AT_RISK" | "OVER_LIMIT"

const usageRiskFilters: { value: UsageRiskFilter; label: string }[] = [
  { value: "ALL", label: "All customers" },
  { value: "AT_RISK", label: "At risk" },
  { value: "OVER_LIMIT", label: "Over limit" },
]

export default function CustomersPage() {
  const { customers, loading, error, createCustomer, updateCustomer, deleteCustomer } =
    useCustomers()
  const { plans } = usePlans()

  const [formOpen, setFormOpen] = useState(false)
  const [editingCustomer, setEditingCustomer] = useState<Customer | null>(null)
  const [deleteDialogOpen, setDeleteDialogOpen] = useState(false)
  const [customerToDelete, setCustomerToDelete] = useState<Customer | null>(null)
  const [usageRiskFilter, setUsageRiskFilter] = useState<UsageRiskFilter>("ALL")

  const filteredCustomers =
    usageRiskFilter === "ALL"
      ? customers
      : customers.filter((customer) => customer.data_usage_state === usageRiskFilter)

  const handleAdd = () => {
    setEditingCustomer(null)
    setFormOpen(true)
  }

  const handleEdit = (customer: Customer) => {
    setEditingCustomer(customer)
    setFormOpen(true)
  }

  const handleDeleteClick = (customer: Customer) => {
    setCustomerToDelete(customer)
    setDeleteDialogOpen(true)
  }

  const handleDeleteConfirm = async () => {
    if (customerToDelete) {
      await deleteCustomer(customerToDelete.id)
      setDeleteDialogOpen(false)
      setCustomerToDelete(null)
    }
  }

  const handleFormSubmit = async (data: CustomerCreate | CustomerUpdate) => {
    if (editingCustomer) {
      await updateCustomer(editingCustomer.id, data as CustomerUpdate)
    } else {
      await createCustomer(data as CustomerCreate)
    }
  }

  return (
    <SidebarProvider className="h-svh">
      <AppSidebar />
      <SidebarInset className="overflow-hidden">
        <header className="flex h-16 shrink-0 items-center gap-2 transition-[width,height] ease-linear group-has-data-[collapsible=icon]/sidebar-wrapper:h-12">
          <div className="flex flex-1 items-center gap-2 px-4">
            <SidebarTrigger className="-ml-1" />
            <Separator orientation="vertical" className="mr-2 data-[orientation=vertical]:h-4" />
            <Breadcrumb>
              <BreadcrumbList>
                <BreadcrumbItem className="hidden md:block">
                  <BreadcrumbLink href="/dashboard">Dashboard</BreadcrumbLink>
                </BreadcrumbItem>
                <BreadcrumbSeparator className="hidden md:block" />
                <BreadcrumbItem>
                  <BreadcrumbPage>Customers</BreadcrumbPage>
                </BreadcrumbItem>
              </BreadcrumbList>
            </Breadcrumb>
            <div className="ml-auto">
              <HealthStatus />
            </div>
          </div>
        </header>
        <div className="flex flex-1 flex-col gap-4 overflow-hidden p-4 pt-0">
          <Card className="flex flex-1 flex-col overflow-hidden">
            <CardHeader className="flex shrink-0 flex-col items-stretch gap-3 space-y-0 pb-4 sm:flex-row sm:items-center sm:justify-between">
              <CardTitle className="text-headline-2">Customers</CardTitle>
              <div className="flex flex-wrap items-center gap-2">
                <span className="text-muted-foreground text-sm">Usage risk</span>
                <div
                  className="flex flex-wrap gap-1"
                  role="group"
                  aria-label="Current-cycle data usage filter"
                >
                  {usageRiskFilters.map((filter) => (
                    <Button
                      key={filter.value}
                      type="button"
                      size="sm"
                      variant={usageRiskFilter === filter.value ? "default" : "outline"}
                      aria-pressed={usageRiskFilter === filter.value}
                      onClick={() => setUsageRiskFilter(filter.value)}
                    >
                      {filter.label}
                    </Button>
                  ))}
                </div>
                <Button onClick={handleAdd}>
                  <Plus className="mr-2 h-4 w-4" />
                  Add Customer
                </Button>
              </div>
            </CardHeader>
            <CardContent className="flex-1 overflow-hidden">
              {loading ? (
                <div className="text-muted-foreground py-8 text-center">Loading...</div>
              ) : error ? (
                <div className="py-8 text-center text-red-500">{error}</div>
              ) : (
                <div className="h-full overflow-auto">
                  <CustomersTable
                    customers={filteredCustomers}
                    onEdit={handleEdit}
                    onDelete={handleDeleteClick}
                  />
                </div>
              )}
            </CardContent>
          </Card>
        </div>

        <CustomerFormDialog
          open={formOpen}
          onOpenChange={setFormOpen}
          customer={editingCustomer}
          plans={plans}
          onSubmit={handleFormSubmit}
        />

        <AlertDialog open={deleteDialogOpen} onOpenChange={setDeleteDialogOpen}>
          <AlertDialogContent>
            <AlertDialogHeader>
              <AlertDialogTitle>Delete Customer</AlertDialogTitle>
              <AlertDialogDescription>
                Are you sure you want to delete {customerToDelete?.first_name}{" "}
                {customerToDelete?.last_name}? This action cannot be undone.
              </AlertDialogDescription>
            </AlertDialogHeader>
            <AlertDialogFooter>
              <AlertDialogCancel>Cancel</AlertDialogCancel>
              <AlertDialogAction onClick={handleDeleteConfirm}>Delete</AlertDialogAction>
            </AlertDialogFooter>
          </AlertDialogContent>
        </AlertDialog>
      </SidebarInset>
    </SidebarProvider>
  )
}
