const isDev = process.env.NODE_ENV === "development"

type LogLevel = "debug" | "info" | "warn" | "error"

interface Logger {
  debug: (obj: object, msg?: string) => void
  info: (obj: object, msg?: string) => void
  warn: (obj: object, msg?: string) => void
  error: (obj: object, msg?: string) => void
  child: (bindings: object) => Logger
}

function createBrowserLogger(context?: string): Logger {
  const prefix = context ? `[${context}]` : ""

  const log = (level: LogLevel, obj: object, msg?: string) => {
    const output = msg ? { ...obj, msg } : obj
    const prefixedMsg = prefix ? `${prefix} ${msg || ""}` : msg
    switch (level) {
      case "debug":
        if (isDev) console.debug(prefixedMsg, output)
        break
      case "info":
        console.info(prefixedMsg, output)
        break
      case "warn":
        console.warn(prefixedMsg, output)
        break
      case "error":
        console.error(prefixedMsg, output)
        break
    }
  }

  return {
    debug: (obj: object, msg?: string) => log("debug", obj, msg),
    info: (obj: object, msg?: string) => log("info", obj, msg),
    warn: (obj: object, msg?: string) => log("warn", obj, msg),
    error: (obj: object, msg?: string) => log("error", obj, msg),
    child: (bindings: object) => createBrowserLogger((bindings as { context?: string }).context),
  }
}

export const logger: Logger = createBrowserLogger()

export function createLogger(context: string): Logger {
  return logger.child({ context })
}
