# High-Contrast Dark Mode & Monet Readability Overhaul

Architectural plan to permanently resolve text legibility issues in dark mode and Android 12+ Monet dynamic theming across the Floating Bottom Menu Bar, Photo Vault milestone cards, and Selected Growth Stage components using bright filled containers with high-contrast dark text and primary accent navigation pills.

## User Review & Critical Decisions

> [!IMPORTANT]
> The following decisions were confirmed by the user in Phase 1 and govern this readability fix:

- **Confirmed Decision 1 (Container Badges & Cards Styling)**: Container badges and cards—specifically the Weekly Milestone Lock card and tags in Photo Vault, and the Selected Growth Stage badges in Plants—will use **bright filled containers with high-contrast dark text** (e.g., radiant amber fill with deep ebony text for milestone locks, and radiant emerald/sage fill with deep forest text for selected stages), guaranteeing immediate, effortless readability against dark backgrounds.
- **Confirmed Decision 2 (Floating Bottom Menu Bar Under Monet)**: Navigation items in the Floating Bottom Menu Bar will use a **high-contrast primary accent with solid background pill** for selected tabs (vivid primary accent with solid pill background) and crisp, high-luminance white (`#F0F6FC`, 100% opacity) for unselected tabs, preventing wallpaper-tinted Monet dimming.

---

## 1. Overview & Core Concept

- **Problem Addressed**: In Android 12+ Monet dynamic dark mode, system wallpaper palette generation assigns muted, low-luminance colors to `primaryContainer`, `secondaryContainer`, and `tertiaryContainer`. When combined with low opacity or muted on-container tokens, text in the Floating Bottom Menu Bar, Vault milestone lock banners, and plant growth stage pills blends into the dark background, becoming unreadable.
- **What This Change Delivers**:
  1. **Floating Bottom Menu Bar**: Selected tab renders inside a solid, high-visibility container pill with crisp primary accenting; unselected tabs render at full 100% luminance (`#F0F6FC` in dark mode) with zero opacity reduction.
  2. **Photo Vault Milestone Lock & Tags**: Replaces muddy, semi-transparent amber overlays (`tertiaryContainer.copy(alpha = 0.45f)`) with a radiant, solid bright amber container paired with sharp, high-contrast dark typography (`#261900` / `#1F2328`) exceeding WCAG AAA standards.
  3. **Selected Growth Stage Badges & Dialog**: Replaces dim container pills with high-luminance filled status pills displaying bold, high-contrast dark labels on both the plant home cards and the Change Growth Stage dialog rows.
  4. **Monet Dynamic Scheme Sanitizer (`Theme.kt`)**: Automatically wraps `dynamicDarkColorScheme(context)` with contrast guarantees, ensuring `onSurface` (`#F0F6FC`), `onSurfaceVariant` (`#D0D7DE`), and container text roles maintain crisp contrast regardless of user wallpaper tint.

---

## 2. User Experience & Visual Design

### Key Visual Upgrades

1. **Floating Bottom Menu Bar (Dark Mode & Monet)**:
   - *Surface*: Solid elevation surface with crisp `outlineVariant` border.
   - *Selected Item*: Snappy bounce pill filled with solid `primaryContainer` / primary accent; icon and label rendered in vivid, high-contrast primary accent.
   - *Unselected Items*: Pure high-luminance text and iconography (`#F0F6FC` in dark mode, `#1F2328` in light mode) with 100% opacity, completely removing muddy or washed-out gray.

2. **Photo Vault ("Weekly Milestone Lock Active" & Badges)**:
   - *Milestone Lock Banner*: Solid, high-radiance amber container (`PlantAmberDark` / `#F6BE48`), featuring bold dark text (`#261900`) and a dark lock icon (`#261900`), instantly readable at a glance.
   - *Milestone Tags ("Week 1", "Week 2", "Baseline")*: Bright filled chips (`#F6BE48` / `#FFDEA1`) with bold dark text (`#261900`).
   - *Day Badges*: Bright emerald pill with deep forest text (`#00210B`).

3. **Selected Growth Stage (Cards & Dialog)**:
   - *Home Plant Cards*: The active stage badge (e.g. "Vegetative (18/6)", "Flowering (12/12)") uses a bright filled container (`#A3F5B1` / `#CEECCF`) with crisp dark text (`#00210B` / `#0A2011`) and a high-contrast edit icon.
   - *Change Growth Stage Dialog*: Selected lifecycle rows highlight with a solid bright container pill and sharp dark label (`#00210B`), while the photoperiod badge (`18h / 6h`) uses a bold contrasting badge.

---

## 3. Key Product Decisions & Trade-Offs

- **Decision 1: Bright Filled Containers with High-Contrast Dark Text**
  - *Chosen Approach*: In dark mode, highlight badges and informational cards use luminous pastel/bright fills with dark text (inverse surface pattern), rather than dark fills with light text.
  - *Why*: User-confirmed requirement. High-luminance pills pop immediately against dark background surfaces (`#0D1117` / `#161B22`) and guarantee 10:1+ contrast ratios, making essential status information impossible to miss.
- **Decision 2: Monet Theme Sanitization in `Theme.kt`**
  - *Chosen Approach*: When `dynamicDarkColorScheme` is selected, override critical text tokens (`onSurface`, `onSurfaceVariant`, `onPrimaryContainer`, `onTertiaryContainer`) to enforce high-contrast thresholds while retaining the user's dynamic hue tint.
  - *Why*: System Monet palettes can otherwise degrade readability if the user's wallpaper generates low-contrast luminance pairs.

---

## 4. Technical Architecture & Component Hierarchy

### System Contrast Architecture

```
┌────────────────────────────────────────────────────────────────────────┐
│                   High-Contrast Theme & Component Flow                 │
│                                                                        │
│  [THEME ENGINE: Theme.kt]                                              │
│  dynamicDarkColorScheme() ──► Contrast Sanitizer                       │
│                                 ├── Enforce onSurface = #F0F6FC        │
│                                 ├── Enforce onSurfaceVariant = #D0D7DE │
│                                 └── Enforce High-Luminance Containers  │
│                                                                        │
│  [COMPONENTS]                                                          │
│                                                                        │
│  1. FloatingBottomBar.kt                                               │
│     ├── Selected: Solid primaryContainer pill + High-contrast accent   │
│     └── Unselected: Crisp #F0F6FC (100% opacity, zero wash-out)        │
│                                                                        │
│  2. PhotoVaultScreen.kt                                                │
│     ├── Weekly Milestone Lock Banner:                                  │
│     │   Solid Bright Amber Fill (#F6BE48) + Bold Dark Text (#261900)   │
│     └── Milestone Chips: Radiant Fill + Sharp Dark Text                │
│                                                                        │
│  3. HomeScreen.kt                                                      │
│     ├── Plant Card Stage Badge: Bright Sage/Emerald + Deep Dark Text   │
│     └── Change Growth Stage Dialog:                                    │
│         Selected Row: Solid Bright Pill + Bold Dark Stage Label        │
└────────────────────────────────────────────────────────────────────────┘
```

### Components to Update

1. **`Theme.kt`**:
   - Sanitize `dynamicDarkColorScheme` so that `onSurface` and `onSurfaceVariant` are locked to high-contrast values (`#F0F6FC` and `#D0D7DE`), preventing Monet wallpaper luminance degradation.
2. **`FloatingBottomBar.kt`**:
   - Update unselected icon and text color to pure `MaterialTheme.colorScheme.onSurface` (100% opacity).
   - Ensure selected tab text uses bold primary accent with full readability on dark surface.
3. **`PhotoVaultScreen.kt`**:
   - Refactor "Weekly Milestone Lock Active" banner to use solid bright amber container with bold dark text (`#261900`).
   - Refactor milestone pills ("Week X", "Baseline") to bright filled chips with dark text.
4. **`HomeScreen.kt`**:
   - Refactor stage badges on plant cards to bright filled containers with bold dark text.
   - Refactor Change Growth Stage dialog rows to use bright filled selection pill with deep contrast label.
