---
name: SED Bogotá Design System
colors:
  surface: '#f8f9ff'
  surface-dim: '#cbdbf5'
  surface-bright: '#f8f9ff'
  surface-container-lowest: '#ffffff'
  surface-container-low: '#eff4ff'
  surface-container: '#e5eeff'
  surface-container-high: '#dce9ff'
  surface-container-highest: '#d3e4fe'
  on-surface: '#0b1c30'
  on-surface-variant: '#434652'
  inverse-surface: '#213145'
  inverse-on-surface: '#eaf1ff'
  outline: '#747783'
  outline-variant: '#c4c6d3'
  surface-tint: '#345baf'
  primary: '#002869'
  on-primary: '#ffffff'
  primary-container: '#0b3d91'
  on-primary-container: '#8dadff'
  inverse-primary: '#b1c5ff'
  secondary: '#7e5700'
  on-secondary: '#ffffff'
  secondary-container: '#feb300'
  on-secondary-container: '#6a4800'
  tertiary: '#5f001b'
  on-tertiary: '#ffffff'
  tertiary-container: '#89002a'
  on-tertiary-container: '#ff8d99'
  error: '#ba1a1a'
  on-error: '#ffffff'
  error-container: '#ffdad6'
  on-error-container: '#93000a'
  primary-fixed: '#dae2ff'
  primary-fixed-dim: '#b1c5ff'
  on-primary-fixed: '#001947'
  on-primary-fixed-variant: '#144296'
  secondary-fixed: '#ffdeac'
  secondary-fixed-dim: '#ffba38'
  on-secondary-fixed: '#281900'
  on-secondary-fixed-variant: '#604100'
  tertiary-fixed: '#ffdadb'
  tertiary-fixed-dim: '#ffb2b8'
  on-tertiary-fixed: '#40000f'
  on-tertiary-fixed-variant: '#91022d'
  background: '#f8f9ff'
  on-background: '#0b1c30'
  surface-variant: '#d3e4fe'
typography:
  h1:
    fontFamily: Public Sans
    fontSize: 40px
    fontWeight: '700'
    lineHeight: 48px
    letterSpacing: -0.02em
  h2:
    fontFamily: Public Sans
    fontSize: 32px
    fontWeight: '700'
    lineHeight: 40px
    letterSpacing: -0.01em
  h3:
    fontFamily: Public Sans
    fontSize: 24px
    fontWeight: '600'
    lineHeight: 32px
  body-lg:
    fontFamily: Public Sans
    fontSize: 18px
    fontWeight: '400'
    lineHeight: 28px
  body-md:
    fontFamily: Public Sans
    fontSize: 16px
    fontWeight: '400'
    lineHeight: 24px
  body-sm:
    fontFamily: Public Sans
    fontSize: 14px
    fontWeight: '400'
    lineHeight: 20px
  label-bold:
    fontFamily: Public Sans
    fontSize: 12px
    fontWeight: '700'
    lineHeight: 16px
  table-data:
    fontFamily: Public Sans
    fontSize: 13px
    fontWeight: '400'
    lineHeight: 18px
rounded:
  sm: 0.125rem
  DEFAULT: 0.25rem
  md: 0.375rem
  lg: 0.5rem
  xl: 0.75rem
  full: 9999px
spacing:
  unit: 4px
  xs: 4px
  sm: 8px
  md: 16px
  lg: 24px
  xl: 40px
  gutter: 16px
  margin: 32px
---

## Brand & Style

This design system is built to reflect the authority, transparency, and service-oriented nature of the Secretaría de Educación del Distrito. The brand personality is **Institutional, Efficient, and Accessible**. It prioritizes the clear communication of administrative data while maintaining the formal visual language of the Bogotá district government.

The chosen aesthetic is **Corporate / Modern**. It utilizes a structured layout and a restrained decorative palette to ensure that users—ranging from administrative officials to citizens—can navigate complex information without cognitive overload. The interface focuses on functional clarity, employing a "utility-first" visual hierarchy that balances the weight of the Bogotá Red and Gold against a dominant, trustworthy Blue.

## Colors

The color palette is derived directly from the institutional identity of the District. 

- **Primary Blue (#0B3D91):** Used for primary navigation, headers, and main action buttons. It establishes the foundation of trust and officiality.
- **Institutional Gold (#FFB300):** Applied sparingly as an accent color for highlights, warning states, or secondary call-to-actions that require visibility without the urgency of red.
- **Bogotá Red (#92032E):** Reserved for high-importance semantic markers, specific district branding elements, and critical alerts.
- **Neutral Slate (#64748B):** Used for secondary text, borders, and UI scaffolding to provide contrast without competing with the primary brand colors.

The default mode is **Light**, utilizing high-contrast ratios to meet accessibility standards required for public government platforms.

## Typography

The typography system utilizes **Public Sans** for all levels of the hierarchy. As a typeface designed for government use, it offers exceptional readability across different screen sizes and densities.

- **Headlines:** Use Bold weights with slight negative letter-spacing to maintain a strong institutional presence.
- **Body Text:** Primarily uses the Regular weight at 16px for general content and 14px for administrative forms to increase information density.
- **Data Display:** A specific `table-data` style is defined at 13px to allow for high-density data grids without sacrificing legibility.
- **Labels:** Small, bold, and occasionally uppercase to clearly delineate form fields and metadata.

## Layout & Spacing

This design system employs a **Fixed Grid** model for large desktop resolutions (max-width: 1440px) to ensure consistency in complex forms, transitioning to a fluid model for smaller screens. 

The layout relies on a **12-column grid** with 16px gutters. This tight gutter width supports the "high-density" requirement, allowing side-by-side form inputs and multi-column data tables to exist without excessive whitespace. Spacing follows a 4px base unit, where administrative panels typically use 8px (sm) or 16px (md) internal padding to maximize the amount of visible data on the "above the fold" area.

## Elevation & Depth

To maintain a clean and professional appearance, this design system minimizes the use of heavy shadows. Depth is primarily communicated through **Tonal Layers** and **Low-contrast outlines**.

- **Level 0 (Background):** Solid #FFFFFF or a very light gray (#F8FAFC) for the canvas.
- **Level 1 (Cards/Panels):** Defined by a 1px border in a neutral-light shade (#E2E8F0). No shadow.
- **Level 2 (Interactive/Floating):** Used for dropdowns and tooltips, utilizing a soft, subtle ambient shadow (0px 4px 12px rgba(0, 0, 0, 0.05)) to separate them from the base administrative layers.
- **Active State:** Elements like focused inputs use a 2px outer glow in the Primary Blue color at 20% opacity.

## Shapes

The shape language is **Soft**, striking a balance between the rigid tradition of government (sharp) and modern user friendliness (rounded). 

- **Standard Elements:** Buttons, input fields, and checkboxes use a 4px (0.25rem) corner radius.
- **Containers:** Large data cards or dashboard sections also follow the 4px rule to maintain a consistent geometric rhythm.
- **Tags/Chips:** May use a slightly higher radius (8px) to differentiate them from functional buttons.

## Components

### Buttons
- **Primary:** Solid Primary Blue (#0B3D91) with white text. High-contrast and authoritative.
- **Secondary:** Outlined Blue or Solid Gold (#FFB300) for supporting actions.
- **Tertiary:** Text-only with bold weights for utility actions within tables.

### Input Fields & Forms
Forms must be compact. Use 14px text for labels placed directly above the input. Input height is capped at 36px to allow for high-density vertical stacking. Borders are 1px solid neutral.

### Data Tables
Tables are the core of this system. 
- **Header:** Light gray background (#F1F5F9) with bold 12px labels.
- **Rows:** Alternating zebra stripes (white and #F8FAFC) to aid horizontal scanning.
- **Density:** 8px vertical padding per cell.

### Chips & Status Indicators
Used for "Process State" (e.g., Pending, Approved). 
- **Approved:** Bogotá Red (#92032E) background with white text for high visibility or a softer tint for less critical statuses.
- **Warning:** Institutional Gold (#FFB300) to indicate items requiring attention.

### Cards
Clean, white backgrounds with a 1px neutral border. Headers within cards should have a subtle bottom border to separate titles from the content body.