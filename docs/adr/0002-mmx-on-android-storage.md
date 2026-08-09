# ADR 0002: MMX sidecar strategy on Android storage

## Status

Accepted (MVP strategy)

## Context

FreeMind-MMX stores volatile attributes (`FOLDED`, `CREATED`, `MODIFIED`) in a hidden sibling file `.<basename>.mmx`, joined with `.mm` by node `ID` at load time. Android apps should use the Storage Access Framework (`content://` URIs). Many providers do not expose reliable sibling create/list for hidden dotfiles, and cloud providers may ignore or hide them.

Silently discarding MMX metadata would violate the fork’s purpose and lose fold/timestamp state.

## Decision

1. **Default semantic behavior** matches FreeMind-MMX: content in `.mm`, volatile attrs in `.mmx`, UTF-8 text, join by `ID`.
2. **Filesystem / document-tree case:** when the app can resolve a parent document tree and create/open a sibling, use the classic `.<basename>.mmx` name.
3. **Single-document SAF case:** if a sibling cannot be created automatically, keep MMX metadata in memory for the session and:
   - prompt (or offer) an explicit “linked `.mmx`” document via SAF when saving/loading, and/or
   - store a non-authoritative recovery cache of MMX attrs keyed by document URI fingerprint in app-private storage for crash recovery only.
4. **Never** replace `.mm` with a proprietary primary format.
5. **Never** silently drop loaded MMX attributes on save; if the sidecar cannot be written, surface a clear error/warning and keep the in-memory state until the user resolves storage.

## Consequences

- Implementation is more complex than desktop path I/O.
- Some cloud workflows need an explicit paired-file UX.
- Recent-docs metadata must remember URI pairs and persisted permissions.
- Unit tests cover join/split independently of Android providers.
