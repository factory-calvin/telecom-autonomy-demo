import { themes as prismThemes } from "prism-react-renderer"
import type { Config } from "@docusaurus/types"
import type * as Preset from "@docusaurus/preset-classic"

const config: Config = {
  title: "FactoryFone Docs",
  tagline: "Telecom Admin Portal Documentation",
  favicon: "img/favicon.ico",

  future: {
    v4: true,
  },

  url: "https://factory-academy.github.io",
  baseUrl: "/NextJS-Springboot-Demo/",

  organizationName: "Factory-Academy",
  projectName: "NextJS-Springboot-Demo",

  onBrokenLinks: "throw",

  i18n: {
    defaultLocale: "en",
    locales: ["en"],
  },

  presets: [
    [
      "classic",
      {
        docs: {
          sidebarPath: "./sidebars.ts",
          editUrl: "https://github.com/Factory-Academy/NextJS-Springboot-Demo/tree/main/docs/",
        },
        blog: false,
        theme: {
          customCss: "./src/css/custom.css",
        },
      } satisfies Preset.Options,
    ],
  ],

  themeConfig: {
    colorMode: {
      defaultMode: "dark",
      respectPrefersColorScheme: true,
    },
    navbar: {
      title: "FactoryFone",
      items: [
        {
          type: "docSidebar",
          sidebarId: "docsSidebar",
          position: "left",
          label: "Docs",
        },
        {
          href: "https://github.com/Factory-Academy/NextJS-Springboot-Demo",
          label: "GitHub",
          position: "right",
        },
      ],
    },
    footer: {
      style: "dark",
      links: [
        {
          title: "Documentation",
          items: [
            { label: "Getting Started", to: "/docs/getting-started" },
            { label: "Architecture", to: "/docs/architecture" },
            { label: "API Reference", to: "/docs/backend/api" },
          ],
        },
        {
          title: "Resources",
          items: [
            {
              label: "GitHub",
              href: "https://github.com/Factory-Academy/NextJS-Springboot-Demo",
            },
            {
              label: "API Docs (Swagger)",
              href: "http://localhost:8080/swagger-ui.html",
            },
          ],
        },
      ],
      copyright: `Copyright © ${new Date().getFullYear()} Factory Academy. Built with Docusaurus.`,
    },
    prism: {
      theme: prismThemes.github,
      darkTheme: prismThemes.dracula,
      additionalLanguages: ["java", "bash", "json"],
    },
  } satisfies Preset.ThemeConfig,
}

export default config
