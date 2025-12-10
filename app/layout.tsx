import type { Metadata } from "next"
import localFont from "next/font/local"
import { Geist_Mono } from "next/font/google"
import { ThemeProvider } from "@/components/theme-provider"
import "./globals.css"

const teleNeoOffice = localFont({
  src: [
    {
      path: "./fonts/Tele Neo Office Thin.ttf",
      weight: "200",
      style: "normal",
    },
    {
      path: "./fonts/Tele Neo Office.ttf",
      weight: "400",
      style: "normal",
    },
    {
      path: "./fonts/Tele Neo Office Medium.ttf",
      weight: "500",
      style: "normal",
    },
    {
      path: "./fonts/Tele Neo Office Bold.ttf",
      weight: "700",
      style: "normal",
    },
    {
      path: "./fonts/Tele Neo Office Extrabold.ttf",
      weight: "800",
      style: "normal",
    },
  ],
  variable: "--font-tele-neo",
  display: "swap",
})

const geistMono = Geist_Mono({
  variable: "--font-geist-mono",
  subsets: ["latin"],
})

export const metadata: Metadata = {
  title: "FactoryFone Admin",
  description: "Telecom user administration portal",
}

export default function RootLayout({
  children,
}: Readonly<{
  children: React.ReactNode
}>) {
  return (
    <html lang="en" suppressHydrationWarning>
      <body className={`${teleNeoOffice.className} ${geistMono.variable} antialiased`}>
        <ThemeProvider
          attribute="class"
          defaultTheme="system"
          enableSystem
          disableTransitionOnChange
        >
          {children}
        </ThemeProvider>
      </body>
    </html>
  )
}
