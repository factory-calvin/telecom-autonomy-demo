import type { SidebarsConfig } from "@docusaurus/plugin-content-docs"

const sidebars: SidebarsConfig = {
  docsSidebar: [
    "getting-started",
    "architecture",
    {
      type: "category",
      label: "Frontend",
      items: ["frontend/overview", "frontend/components", "frontend/hooks"],
    },
    {
      type: "category",
      label: "Backend",
      items: ["backend/overview", "backend/api", "backend/models"],
    },
    {
      type: "category",
      label: "Development",
      items: ["development/setup", "development/testing", "development/deployment"],
    },
  ],
}

export default sidebars
