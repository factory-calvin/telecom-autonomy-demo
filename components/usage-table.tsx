"use client"

import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from "@/components/ui/table"
import type { UsageRecord } from "@/hooks/use-usage"

interface UsageTableProps {
  records: UsageRecord[]
}

const typeColors: Record<string, string> = {
  CALL: "bg-blue-100 text-blue-800",
  DATA: "bg-purple-100 text-purple-800",
  SMS: "bg-green-100 text-green-800",
}

const typeUnits: Record<string, string> = {
  CALL: "min",
  DATA: "MB",
  SMS: "",
}

export function UsageTable({ records }: UsageTableProps) {
  return (
    <Table>
      <TableHeader className="sticky top-0 bg-background z-10">
        <TableRow>
          <TableHead className="w-[60px]">ID</TableHead>
          <TableHead>Customer</TableHead>
          <TableHead>Type</TableHead>
          <TableHead className="text-right">Quantity</TableHead>
          <TableHead className="text-right">Cost</TableHead>
          <TableHead>Date</TableHead>
        </TableRow>
      </TableHeader>
      <TableBody>
        {records.map((record) => (
          <TableRow key={record.id}>
            <TableCell className="font-medium">{record.id}</TableCell>
            <TableCell>{record.customer_name}</TableCell>
            <TableCell>
              <span className={`px-2 py-1 rounded-full text-xs font-medium ${typeColors[record.type]}`}>
                {record.type}
              </span>
            </TableCell>
            <TableCell className="text-right">
              {record.quantity} {typeUnits[record.type]}
            </TableCell>
            <TableCell className="text-right">${record.cost.toFixed(2)}</TableCell>
            <TableCell className="text-muted-foreground">
              {record.recorded_at ? new Date(record.recorded_at).toLocaleDateString() : "-"}
            </TableCell>
          </TableRow>
        ))}
      </TableBody>
    </Table>
  )
}
