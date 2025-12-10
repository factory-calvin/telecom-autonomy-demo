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
import type { Plan } from "@/hooks/use-plans"

interface PlansTableProps {
  plans: Plan[]
  onEdit: (plan: Plan) => void
  onDelete: (plan: Plan) => void
}

export function PlansTable({ plans, onEdit, onDelete }: PlansTableProps) {
  const formatLimit = (limit: number | null) => (limit === null ? "Unlimited" : limit.toString())

  return (
    <Table>
      <TableHeader className="sticky top-0 bg-background z-10">
        <TableRow>
          <TableHead className="w-[60px]">ID</TableHead>
          <TableHead>Name</TableHead>
          <TableHead className="text-right">Price/mo</TableHead>
          <TableHead className="text-right">Data</TableHead>
          <TableHead className="text-right">Minutes</TableHead>
          <TableHead className="text-right">SMS</TableHead>
          <TableHead>Status</TableHead>
          <TableHead className="w-[100px] text-right">Actions</TableHead>
        </TableRow>
      </TableHeader>
      <TableBody>
        {plans.map((plan) => (
          <TableRow key={plan.id}>
            <TableCell className="font-medium">{plan.id}</TableCell>
            <TableCell>{plan.name}</TableCell>
            <TableCell className="text-right">${plan.monthly_price.toFixed(2)}</TableCell>
            <TableCell className="text-right">{formatLimit(plan.data_limit_gb)}{plan.data_limit_gb ? " GB" : ""}</TableCell>
            <TableCell className="text-right">{formatLimit(plan.minutes_limit)}</TableCell>
            <TableCell className="text-right">{formatLimit(plan.sms_limit)}</TableCell>
            <TableCell>
              <span className={`px-2 py-1 rounded-full text-xs font-medium ${plan.is_active ? "bg-green-100 text-green-800" : "bg-gray-100 text-gray-800"}`}>
                {plan.is_active ? "Active" : "Inactive"}
              </span>
            </TableCell>
            <TableCell className="text-right">
              <div className="flex justify-end gap-1">
                <Button variant="ghost" size="icon" onClick={() => onEdit(plan)}>
                  <Pencil className="h-4 w-4" />
                </Button>
                <Button variant="ghost" size="icon" onClick={() => onDelete(plan)}>
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
