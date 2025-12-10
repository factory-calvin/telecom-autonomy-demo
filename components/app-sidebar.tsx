"use client"

import * as React from "react"
import { usePathname } from "next/navigation"
import {
  LayoutDashboard,
  Users,
  CreditCard,
  Smartphone,
  BarChart3,
  Ticket,
} from "lucide-react"

import { Logo } from "@/components/logo"
import { NavMain } from "@/components/nav-main"
import { NavUser } from "@/components/nav-user"
import { TeamSwitcher } from "@/components/team-switcher"
import {
  Sidebar,
  SidebarContent,
  SidebarFooter,
  SidebarHeader,
  SidebarRail,
} from "@/components/ui/sidebar"

const user = {
  name: "Admin User",
  email: "admin@factoryfone.com",
  avatar: "",
}

const teams = [
  {
    name: "FactoryFone",
    logo: Logo,
    plan: "Admin Portal",
  },
]

const navItems = [
  {
    title: "Dashboard",
    url: "/dashboard",
    icon: LayoutDashboard,
  },
  {
    title: "Customers",
    url: "/customers",
    icon: Users,
  },
  {
    title: "Plans",
    url: "/plans",
    icon: CreditCard,
  },
  {
    title: "Devices",
    url: "/devices",
    icon: Smartphone,
  },
  {
    title: "Usage",
    url: "/usage",
    icon: BarChart3,
  },
  {
    title: "Support Tickets",
    url: "/tickets",
    icon: Ticket,
  },
]

export function AppSidebar({ ...props }: React.ComponentProps<typeof Sidebar>) {
  const pathname = usePathname()

  const navItemsWithActive = navItems.map((item) => ({
    ...item,
    isActive: pathname === item.url || pathname.startsWith(item.url + "/"),
  }))

  return (
    <Sidebar collapsible="icon" {...props}>
      <SidebarHeader>
        <TeamSwitcher teams={teams} />
      </SidebarHeader>
      <SidebarContent>
        <NavMain items={navItemsWithActive} />
      </SidebarContent>
      <SidebarFooter>
        <NavUser user={user} />
      </SidebarFooter>
      <SidebarRail />
    </Sidebar>
  )
}
