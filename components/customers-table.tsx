"use client"

import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from "@/components/ui/table"
import { Button } from "@/components/ui/button"
import { Pencil, Trash2 } from "lucide-react"
import type { Customer } from "@/hooks/use-customers"

interface CustomersTableProps {
  customers: Customer[]
  onEdit: (customer: Customer) => void
  onDelete: (customer: Customer) => void
}

const statusColors: Record<string, string> = {
  ACTIVE: "bg-green-100 text-green-800",
  SUSPENDED: "bg-yellow-100 text-yellow-800",
  CANCELLED: "bg-red-100 text-red-800",
}

export function CustomersTable({ customers, onEdit, onDelete }: CustomersTableProps) {
  return (
    <Table>
      <TableHeader className="bg-background sticky top-0 z-10">
        <TableRow>
          <TableHead className="w-[60px]">ID</TableHead>
          <TableHead>Name</TableHead>
          <TableHead>Email</TableHead>
          <TableHead>Phone</TableHead>
          <TableHead>Plan</TableHead>
          <TableHead>Status</TableHead>
          <TableHead className="text-right">Balance</TableHead>
          <TableHead className="w-[100px] text-right">Actions</TableHead>
        </TableRow>
      </TableHeader>
      <TableBody>
        {customers.map((customer) => (
          <TableRow key={customer.id}>
            <TableCell className="font-medium">{customer.id}</TableCell>
            <TableCell>
              {customer.first_name} {customer.last_name}
            </TableCell>
            <TableCell className="text-muted-foreground">{customer.email}</TableCell>
            <TableCell>{customer.phone}</TableCell>
            <TableCell>{customer.plan_name || "-"}</TableCell>
            <TableCell>
              <span
                className={`rounded-full px-2 py-1 text-xs font-medium ${statusColors[customer.status]}`}
              >
                {customer.status}
              </span>
            </TableCell>
            <TableCell className="text-right">${customer.balance.toFixed(2)}</TableCell>
            <TableCell className="text-right">
              <div className="flex justify-end gap-1">
                <Button variant="ghost" size="icon" onClick={() => onEdit(customer)}>
                  <Pencil className="h-4 w-4" />
                </Button>
                <Button variant="ghost" size="icon" onClick={() => onDelete(customer)}>
                  <Trash2 className="h-4 w-4" />
                </Button>
              </div>
            </TableCell>
          </TableRow>
        ))}
      </TableBody>
    </Table>
  )
}
