import pino from "pino"

const isServer = typeof window === "undefined"
const isDev = process.env.NODE_ENV === "development"

const browserLogger = {
  level: isDev ? "debug" : "info",
  browser: {
    asObject: true,
    write: {
      debug: (o: object) => console.debug("[DEBUG]", o),
      info: (o: object) => console.info("[INFO]", o),
      warn: (o: object) => console.warn("[WARN]", o),
      error: (o: object) => console.error("[ERROR]", o),
    },
  },
}

const serverLogger = {
  level: isDev ? "debug" : "info",
  transport: isDev
    ? {
        target: "pino-pretty",
        options: {
          colorize: true,
          translateTime: "SYS:standard",
          ignore: "pid,hostname",
        },
      }
    : undefined,
}

export const logger = pino(isServer ? serverLogger : browserLogger)

export function createLogger(context: string) {
  return logger.child({ context })
}
