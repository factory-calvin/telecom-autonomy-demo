import { render, screen } from "@testing-library/react"
import { describe, it, expect } from "vitest"
import { Logo } from "@/components/logo"

describe("Logo", () => {
  it("renders two images for light and dark mode", () => {
    render(<Logo />)
    const images = screen.getAllByRole("img", { name: /logo/i })
    expect(images).toHaveLength(2)
  })

  it("applies custom className", () => {
    render(<Logo className="custom-class" />)
    const images = screen.getAllByRole("img", { name: /logo/i })
    images.forEach((img) => {
      expect(img.className).toContain("custom-class")
    })
  })
})
