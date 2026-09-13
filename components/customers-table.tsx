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

const numberFormatter = new Intl.NumberFormat("en-US", {
  maximumFractionDigits: 2,
})

const usageRiskPresentation = {
  AT_RISK: {
    label: "At risk",
    className:
      "border-amber-300 bg-amber-50 text-amber-900 dark:border-amber-700 dark:bg-amber-950 dark:text-amber-100",
  },
  OVER_LIMIT: {
    label: "Over limit",
    className:
      "border-red-300 bg-red-50 text-red-900 dark:border-red-700 dark:bg-red-950 dark:text-red-100",
  },
} as const

function CustomerDataUsage({ customer }: { customer: Customer }) {
  const used = `${numberFormatter.format(customer.current_cycle_data_used_gb)} GB`

  if (customer.data_usage_state === "UNLIMITED") {
    return <span aria-label={`${used} used on an Unlimited plan`}>{used}</span>
  }

  const risk =
    customer.data_usage_state === "AT_RISK" || customer.data_usage_state === "OVER_LIMIT"
      ? usageRiskPresentation[customer.data_usage_state]
      : null
  const percentage =
    customer.data_usage_percentage === null
      ? null
      : `${numberFormatter.format(customer.data_usage_percentage)}%`
  const limit =
    customer.data_limit_gb === null ? null : `${numberFormatter.format(customer.data_limit_gb)} GB`
  const accessibleDescription = [
    used,
    percentage && limit ? `${percentage} of ${limit}` : null,
    risk?.label,
  ]
    .filter(Boolean)
    .join(", ")

  return (
    <div className="flex min-w-36 flex-col items-start gap-1" aria-label={accessibleDescription}>
      <span>{used}</span>
      {percentage && limit ? (
        <span className="text-muted-foreground text-xs">
          {percentage} of {limit}
        </span>
      ) : null}
      {risk ? (
        <span className={`rounded-full border px-2 py-0.5 text-xs font-medium ${risk.className}`}>
          <span aria-hidden="true">{customer.data_usage_state === "AT_RISK" ? "▲ " : "● "}</span>
          {risk.label}
        </span>
      ) : null}
    </div>
  )
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
          <TableHead>Current-cycle data</TableHead>
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
            <TableCell>
              <CustomerDataUsage customer={customer} />
            </TableCell>
            <TableCell className="text-right">${customer.balance.toFixed(2)}</TableCell>
            <TableCell className="text-right">
              <div className="flex justify-end gap-1">
                <Button
                  variant="ghost"
                  size="icon"
                  aria-label={`Edit ${customer.first_name} ${customer.last_name}`}
                  onClick={() => onEdit(customer)}
                >
                  <Pencil className="h-4 w-4" />
                </Button>
                <Button
                  variant="ghost"
                  size="icon"
                  aria-label={`Delete ${customer.first_name} ${customer.last_name}`}
                  onClick={() => onDelete(customer)}
                >
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
