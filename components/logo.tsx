"use client"

import Image from "next/image"

export function Logo({ className }: { className?: string }) {
  return (
    <>
      <Image
        src="/logo-black-transparent.svg"
        alt="Logo"
        width={24}
        height={24}
        className={`dark:hidden ${className ?? ""}`}
      />
      <Image
        src="/logo-white-transparent.svg"
        alt="Logo"
        width={24}
        height={24}
        className={`hidden dark:block ${className ?? ""}`}
      />
    </>
  )
}
