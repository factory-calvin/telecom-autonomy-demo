import { defineConfig, globalIgnores } from "eslint/config"
import nextVitals from "eslint-config-next/core-web-vitals"
import nextTs from "eslint-config-next/typescript"

const eslintConfig = defineConfig([
  ...nextVitals,
  ...nextTs,
  // Override default ignores of eslint-config-next.
  globalIgnores([
    // Default ignores of eslint-config-next:
    ".next/**",
    "out/**",
    "build/**",
    "next-env.d.ts",
    // Ignore backend build artifacts
    "backend/build/**",
    // Ignore docs build and generated files
    "docs/build/**",
    "docs/.docusaurus/**",
    "docs/node_modules/**",
  ]),
  {
    rules: {
      "no-restricted-globals": [
        "error",
        {
          name: "alert",
          message: "Use AlertDialog from @/components/ui/alert-dialog instead of native alert().",
        },
        {
          name: "confirm",
          message: "Use AlertDialog from @/components/ui/alert-dialog instead of native confirm().",
        },
        {
          name: "prompt",
          message: "Use Dialog from @/components/ui/dialog instead of native prompt().",
        },
      ],
    },
  },
  // Restrict useEffect in app pages (but not in hooks, UI primitives, providers, or reusable components)
  {
    files: ["app/**/*.{ts,tsx}"],
    rules: {
      "no-restricted-imports": [
        "error",
        {
          paths: [
            {
              name: "react",
              importNames: ["useEffect"],
              message:
                "useEffect is discouraged in pages. Move data fetching to hooks or use React Server Components.",
            },
          ],
        },
      ],
    },
  },
])

export default eslintConfig
