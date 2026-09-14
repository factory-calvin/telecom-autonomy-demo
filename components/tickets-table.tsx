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
import { CircleCheck, Clock3, Pencil, Siren, Trash2, TriangleAlert } from "lucide-react"
import type { DeadlineState, Ticket } from "@/hooks/use-tickets"

interface TicketsTableProps {
  tickets: Ticket[]
  onEdit: (ticket: Ticket) => void
  onDelete: (ticket: Ticket) => void
}

const priorityColors: Record<string, string> = {
  LOW: "bg-gray-100 text-gray-800",
  MEDIUM: "bg-blue-100 text-blue-800",
  HIGH: "bg-orange-100 text-orange-800",
  URGENT: "bg-red-100 text-red-800",
}

const statusColors: Record<string, string> = {
  OPEN: "bg-yellow-100 text-yellow-800",
  IN_PROGRESS: "bg-blue-100 text-blue-800",
  RESOLVED: "bg-green-100 text-green-800",
  CLOSED: "bg-gray-100 text-gray-800",
}

const deadlinePresentation: Record<
  DeadlineState,
  { label: string; className: string; icon: typeof CircleCheck }
> = {
  ON_TRACK: {
    label: "On track",
    className: "bg-green-100 text-green-900 dark:bg-green-950 dark:text-green-200",
    icon: CircleCheck,
  },
  DUE_SOON: {
    label: "Due soon",
    className: "bg-amber-100 text-amber-900 dark:bg-amber-950 dark:text-amber-200",
    icon: Clock3,
  },
  ACKNOWLEDGEMENT_OVERDUE: {
    label: "Acknowledgement overdue",
    className: "bg-orange-100 text-orange-900 dark:bg-orange-950 dark:text-orange-200",
    icon: TriangleAlert,
  },
  RESOLUTION_OVERDUE: {
    label: "Resolution overdue",
    className: "bg-red-100 text-red-900 dark:bg-red-950 dark:text-red-200",
    icon: Siren,
  },
}

const deadlineFormatter = new Intl.DateTimeFormat(undefined, {
  year: "numeric",
  month: "short",
  day: "numeric",
  hour: "numeric",
  minute: "2-digit",
  timeZoneName: "short",
})

function formatDeadline(value: string) {
  return deadlineFormatter.format(new Date(value))
}

export function TicketsTable({ tickets, onEdit, onDelete }: TicketsTableProps) {
  return (
    <Table>
      <TableHeader className="bg-background sticky top-0 z-10">
        <TableRow>
          <TableHead className="w-[60px]">ID</TableHead>
          <TableHead>Customer</TableHead>
          <TableHead>Subject</TableHead>
          <TableHead>Priority</TableHead>
          <TableHead>Status</TableHead>
          <TableHead>Age</TableHead>
          <TableHead>Deadline</TableHead>
          <TableHead className="w-[100px] text-right">Actions</TableHead>
        </TableRow>
      </TableHeader>
      <TableBody>
        {tickets.map((ticket) => (
          <TableRow key={ticket.id}>
            <TableCell className="font-medium">{ticket.id}</TableCell>
            <TableCell>{ticket.customer_name}</TableCell>
            <TableCell className="max-w-[200px] truncate">{ticket.subject}</TableCell>
            <TableCell>
              <span
                className={`rounded-full px-2 py-1 text-xs font-medium ${priorityColors[ticket.priority]}`}
              >
                {ticket.priority}
              </span>
            </TableCell>
            <TableCell>
              <span
                className={`rounded-full px-2 py-1 text-xs font-medium ${statusColors[ticket.status]}`}
              >
                {ticket.status.replace("_", " ")}
              </span>
            </TableCell>
            <TableCell className="text-muted-foreground">
              <span className="text-foreground font-medium">{ticket.age_days} days</span>
              <span className="block text-xs">{ticket.age_hours} whole hours</span>
            </TableCell>
            <TableCell>
              {(() => {
                const presentation = deadlinePresentation[ticket.deadline_state]
                const DeadlineIcon = presentation.icon
                const exactDeadlines = `Acknowledgement due ${formatDeadline(ticket.acknowledgement_due_at)}. Resolution due ${formatDeadline(ticket.resolution_due_at)}.`

                return (
                  <span
                    className={`inline-flex items-center gap-1 rounded-full px-2 py-1 text-xs font-medium ${presentation.className}`}
                    aria-label={`${presentation.label}. ${exactDeadlines}`}
                    title={exactDeadlines}
                  >
                    <DeadlineIcon className="h-3.5 w-3.5" aria-hidden="true" />
                    {presentation.label}
                  </span>
                )
              })()}
            </TableCell>
            <TableCell className="text-right">
              <div className="flex justify-end gap-1">
                <Button variant="ghost" size="icon" onClick={() => onEdit(ticket)}>
                  <Pencil className="h-4 w-4" />
                </Button>
                <Button variant="ghost" size="icon" onClick={() => onDelete(ticket)}>
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
