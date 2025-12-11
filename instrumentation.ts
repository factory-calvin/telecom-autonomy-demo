import { registerOTel } from "@vercel/otel"

export function register() {
  registerOTel({
    serviceName: "factoryfone-frontend",
    attributes: {
      "service.version": process.env.npm_package_version || "0.1.0",
      "deployment.environment": process.env.NODE_ENV || "development",
    },
  })
}
