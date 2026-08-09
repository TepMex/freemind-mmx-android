# ADR 0005: Editing via reversible domain commands

## Status

Accepted

## Context

Milestone 4 requires add/edit/delete/reorder with undo/redo. Serializing the whole XML document per keystroke is undesirable.

## Decision

1. Represent edits as explicit `MindMapCommand` values in `:mindmap-core`.
2. Capture enough prior state in each command to invert it (e.g. old text, removed subtree snapshot, sibling indices).
3. Commit node text edits when the edit dialog closes — one undo step per finished edit, not per character.
4. Keep fold toggles on the undo stack for predictable UX; MMX still keeps fold out of `.mm` on save.

## Consequences

- Editing never requires the UI to manipulate XML.
- Undo/redo is testable on the JVM without Android.
