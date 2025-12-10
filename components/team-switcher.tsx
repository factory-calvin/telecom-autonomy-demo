"use client"

import * as React from "react"
import Link from "next/link"

import {
  SidebarMenu,
  SidebarMenuItem,
} from "@/components/ui/sidebar"

export function TeamSwitcher({
  teams,
}: {
  teams: {
    name: string
    logo: React.ElementType
    plan: string
  }[]
}) {
  const team = teams[0]

  if (!team) {
    return null
  }

  return (
    <SidebarMenu>
      <SidebarMenuItem>
        <Link
          href="/dashboard"
          className="flex items-center gap-3 px-2 py-3 rounded-md hover:bg-sidebar-accent transition-colors group-data-[collapsible=icon]:justify-center group-data-[collapsible=icon]:p-0"
        >
          <team.logo className="size-8 shrink-0" />
          <div className="grid flex-1 text-left leading-tight group-data-[collapsible=icon]:hidden">
            <span className="text-headline-3 text-primary">{team.name}</span>
            <span className="text-caption text-muted-foreground">{team.plan}</span>
          </div>
        </Link>
      </SidebarMenuItem>
    </SidebarMenu>
  )
}
