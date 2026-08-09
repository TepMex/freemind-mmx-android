# ADR 0003: Compose Canvas for mind-map rendering

## Status

Accepted (initial choice; revisitable)

## Context

The map must render as a real mind map (root-centered, left/right branches, connectors), not as a vertical list. Rendering must support pan, zoom, selection, and fold. Architecture requires layout/geometry to remain independent of UI widgets.

Options considered: Compose `Canvas`, custom `View`/`SurfaceView`, WebView (rejected by product guardrails).

## Decision

Use Jetpack Compose with a single (or few) `Canvas`/custom layout drawing pass for the map viewport in MVP.

Keep `:mindmap-layout` producing node bounds and connector endpoints in pure Kotlin. The UI applies a viewport transform (pan/zoom) and performs hit testing against layout results.

Revisit a custom `View` only if measurement shows Compose drawing/recomposition cannot meet performance targets for multi-thousand-node maps.

## Consequences

- Domain and layout stay testable on the JVM.
- Avoids thousands of per-node composables.
- Renderer can be swapped later without rewriting editing or file format code.
