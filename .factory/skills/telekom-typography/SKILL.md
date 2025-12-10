# Telekom Design System Skill

This skill provides guidance on using the Tele Neo Office font family, Telekom Scale design system typography, and color palette in this project.

## Font Family

The project uses **Tele Neo Office** as the primary font family, loaded via `next/font/local` in `app/layout.tsx`.

### Available Weights

| Weight Name | CSS Variable | CSS Value | Tailwind Class |
|-------------|--------------|-----------|----------------|
| Extra Bold | `--telekom-font-weight-extra-bold` | 800 | `font-extrabold` |
| Bold | `--telekom-font-weight-bold` | 700 | `font-bold` |
| Medium | `--telekom-font-weight-medium` | 500 | `font-medium` |
| Regular | `--telekom-font-weight-regular` | 400 | `font-normal` |
| Thin | `--telekom-font-weight-thin` | 200 | `font-thin` |

### Font Files Location

Font files are located in `app/fonts/`:
- `Tele Neo Office.ttf` (Regular - 400)
- `Tele Neo Office Thin.ttf` (200)
- `Tele Neo Office Medium.ttf` (500)
- `Tele Neo Office Bold.ttf` (700)
- `Tele Neo Office Extrabold.ttf` (800)

## Typography Classes

Custom typography utility classes are defined in `globals.css`:

| Class | Use Case | Font Weight | Size |
|-------|----------|-------------|------|
| `.text-headline-1` | Page titles | Bold (700) | 2rem |
| `.text-headline-2` | Section/Card titles | Bold (700) | 1.5rem |
| `.text-headline-3` | Subsection titles | Medium (500) | 1.25rem |
| `.text-lead` | Intro/lead text | Regular (400) | 1.125rem |
| `.text-body` | Body text | Regular (400) | 1rem |
| `.text-body-small` | Secondary body | Regular (400) | 0.875rem |
| `.text-ui` | UI labels, buttons | Medium (500) | 0.875rem |
| `.text-caption` | Captions, hints | Regular (400) | 0.75rem |

### Usage Example

```tsx
<h1 className="text-headline-1">Page Title</h1>
<h2 className="text-headline-2">Card Title</h2>
<h3 className="text-headline-3">Section Title</h3>
<p className="text-lead">Introduction paragraph with larger text.</p>
<p className="text-body">Regular body text content.</p>
<p className="text-body-small">Secondary information.</p>
<span className="text-ui">Button Label</span>
<span className="text-caption">Small caption or hint text.</span>
```

## Color Palette

### Telekom Core Colors

| Name | Light Mode | Dark Mode |
|------|------------|-----------|
| Magenta (Primary) | `#e20074` | `#e20074` |
| Magenta Hover | `#c00063` | `#c00063` |
| Magenta Pressed | `#9e0051` | `#9e0051` |

### Semantic Colors (Light Mode)

| Token | Value | Use Case |
|-------|-------|----------|
| `--background` | `#ffffff` | Page background |
| `--foreground` | `#000000` | Default text |
| `--card` | `#ffffff` | Card backgrounds |
| `--muted` | `#f7f7f8` | Subtle backgrounds |
| `--muted-foreground` | `rgba(0,0,0,0.65)` | Secondary text |
| `--primary` | `#e20074` | Primary actions |
| `--destructive` | `#e82010` | Danger/delete |
| `--success` | `#00b367` | Success states |
| `--warning` | `#f97012` | Warning states |
| `--info` | `#2238df` | Informational |
| `--border` | `rgba(0,0,0,0.14)` | Borders |
| `--ring` | `#2238df` | Focus rings |

### Semantic Colors (Dark Mode)

| Token | Value | Use Case |
|-------|-------|----------|
| `--background` | `#0d0d0d` | Page background |
| `--foreground` | `#ffffff` | Default text |
| `--card` | `#141414` | Card backgrounds |
| `--muted` | `#1a1a1a` | Subtle backgrounds |
| `--muted-foreground` | `rgba(255,255,255,0.65)` | Secondary text |
| `--primary` | `#e20074` | Primary actions |
| `--destructive` | `#ff5c5c` | Danger/delete |
| `--success` | `#00b36b` | Success states |
| `--warning` | `#ff8c42` | Warning states |
| `--info` | `#6b7eff` | Informational |
| `--border` | `rgba(255,255,255,0.14)` | Borders |
| `--ring` | `#6b7eff` | Focus rings |

### Chart Colors (Telekom Additional Palette)

| Token | Value | Color Name |
|-------|-------|------------|
| `--chart-1` | `#00a0de` | Cyan |
| `--chart-2` | `#e20074` | Magenta |
| `--chart-3` | `#00b367` | Green |
| `--chart-4` | `#794ae9` | Violet |
| `--chart-5` | `#f97012` | Orange |

## Text Color Utilities

Custom text color classes using Telekom tokens:

| Class | Purpose |
|-------|---------|
| `.text-telekom-standard` | Standard text color |
| `.text-telekom-additional` | Secondary text (65% opacity) |
| `.text-telekom-disabled` | Disabled text (40% opacity) |
| `.text-telekom-link` | Link color |
| `.text-telekom-success` | Success text |
| `.text-telekom-danger` | Error/danger text |
| `.text-telekom-warning` | Warning text |
| `.text-telekom-info` | Informational text |
| `.text-telekom-magenta` | Brand magenta |

### Usage Example

```tsx
<p className="text-telekom-standard">Standard text</p>
<p className="text-telekom-additional">Secondary information</p>
<a className="text-telekom-link hover:text-telekom-link">Link text</a>
<span className="text-telekom-success">Success!</span>
<span className="text-telekom-danger">Error occurred</span>
<span className="text-telekom-warning">Warning message</span>
```

## Best Practices

### Typography

1. **Hierarchy**: Use consistent heading levels
   - `text-headline-1` for page titles
   - `text-headline-2` for card/section titles
   - `text-headline-3` for subsections
2. **Readability**: Use `text-body` for main content, `text-body-small` for secondary
3. **UI Elements**: Use `text-ui` for buttons, labels, and interactive elements
4. **Captions**: Use `text-caption` for hints, timestamps, and metadata

### Colors

1. **Brand Consistency**: Always use `--primary` (#e20074 Magenta) for primary CTAs
2. **Semantic Usage**: Use functional colors consistently:
   - Success (green): Confirmations, successful actions
   - Destructive (red): Errors, destructive actions
   - Warning (orange): Cautions, potential issues
   - Info (blue): Links, additional information
3. **Contrast**: Ensure sufficient contrast for accessibility (WCAG AA minimum)
4. **Dark Mode**: Colors automatically switch based on theme

## Configuration Files

- Font loading: `app/layout.tsx`
- CSS variables & utilities: `app/globals.css`
- Font files: `app/fonts/`

## Reference

- [Telekom Scale Design System - Colors](https://telekom.github.io/scale/?path=/docs/guidelines-colors--page)
- [Telekom Scale Design System - Design Tokens](https://telekom.github.io/scale/?path=/docs/guidelines-design-tokens--page)
