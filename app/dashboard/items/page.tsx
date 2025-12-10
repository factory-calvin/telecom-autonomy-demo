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
import {
  SidebarInset,
  SidebarProvider,
  SidebarTrigger,
} from "@/components/ui/sidebar"
import { Button } from "@/components/ui/button"
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card"
import { Plus } from "lucide-react"
import { HealthStatus } from "@/components/health-status"
import { ItemsTable } from "@/components/items-table"
import { ItemFormDialog } from "@/components/item-form-dialog"
import { useItems, type Item } from "@/hooks/use-items"

export default function ItemsPage() {
  const { items, loading, error, createItem, updateItem, deleteItem } =
    useItems()
  const [dialogOpen, setDialogOpen] = useState(false)
  const [editingItem, setEditingItem] = useState<Item | null>(null)

  const handleCreate = () => {
    setEditingItem(null)
    setDialogOpen(true)
  }

  const handleEdit = (item: Item) => {
    setEditingItem(item)
    setDialogOpen(true)
  }

  const handleDelete = async (item: Item) => {
    if (confirm(`Delete "${item.name}"?`)) {
      await deleteItem(item.id)
    }
  }

  const handleSubmit = async (data: { name: string; description?: string | null }) => {
    if (editingItem) {
      await updateItem(editingItem.id, data)
    } else {
      await createItem(data)
    }
  }

  return (
    <SidebarProvider>
      <AppSidebar />
      <SidebarInset>
        <header className="flex h-16 shrink-0 items-center gap-2 transition-[width,height] ease-linear group-has-data-[collapsible=icon]/sidebar-wrapper:h-12">
          <div className="flex items-center gap-2 px-4 flex-1">
            <SidebarTrigger className="-ml-1" />
            <Separator
              orientation="vertical"
              className="mr-2 data-[orientation=vertical]:h-4"
            />
            <Breadcrumb>
              <BreadcrumbList>
                <BreadcrumbItem className="hidden md:block">
                  <BreadcrumbLink href="/dashboard">Dashboard</BreadcrumbLink>
                </BreadcrumbItem>
                <BreadcrumbSeparator className="hidden md:block" />
                <BreadcrumbItem>
                  <BreadcrumbPage>Items</BreadcrumbPage>
                </BreadcrumbItem>
              </BreadcrumbList>
            </Breadcrumb>
            <div className="ml-auto">
              <HealthStatus />
            </div>
          </div>
        </header>
        <div className="flex flex-1 flex-col gap-4 p-4 pt-0">
          <Card>
            <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-4">
              <CardTitle>Items</CardTitle>
              <Button onClick={handleCreate}>
                <Plus className="h-4 w-4 mr-2" />
                Add Item
              </Button>
            </CardHeader>
            <CardContent>
              {loading ? (
                <div className="text-center py-8 text-muted-foreground">
                  Loading...
                </div>
              ) : error ? (
                <div className="text-center py-8 text-red-500">{error}</div>
              ) : (
                <ItemsTable
                  items={items}
                  onEdit={handleEdit}
                  onDelete={handleDelete}
                  onCreate={handleCreate}
                />
              )}
            </CardContent>
          </Card>
        </div>
        <ItemFormDialog
          open={dialogOpen}
          onOpenChange={setDialogOpen}
          item={editingItem}
          onSubmit={handleSubmit}
        />
      </SidebarInset>
    </SidebarProvider>
  )
}
