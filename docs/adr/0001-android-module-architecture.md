# ADR 0001: Multi-module Android architecture

## Status

Accepted

## Context

The repository contains a GPL FreeMind / FreeMind-MMX desktop Java codebase that is tightly coupled to Swing/AWT. The product goal is a native Android mind-mapping app with FreeMind `.mm` compatibility and FreeMind-MMX sidecar behavior. Directly porting Swing UI is not viable.

## Decision

Create a new Gradle project under `android/` with these modules:

| Module | Responsibility |
|--------|----------------|
| `:app` | Android application, Jetpack Compose UI, SAF, intents, ViewModels |
| `:mindmap-core` | Pure Kotlin domain model, editing operations, undo/redo |
| `:freemind-format` | `.mm` / `.mmx` parse and write; MMX join/split |
| `:mindmap-layout` | Tree-to-geometry layout and hit-testing helpers (no Compose) |

Leave legacy `freemind/`, `pda/`, `flash/`, etc. intact as reference and fixtures.

Preferred stack: Kotlin, Gradle Kotlin DSL, Jetpack Compose, Material 3, AndroidX, ViewModel, StateFlow, coroutines. No database for MVP; documents remain `.mm` / `.mmx` files.

## Consequences

- Format and domain tests run on the JVM without an emulator.
- UI can change rendering strategy without rewriting the model.
- GPL obligations apply to the Android project in this repository; prefer clean-room reimplementation of format/domain over copying Swing-coupled classes.
