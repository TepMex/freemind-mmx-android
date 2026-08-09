# Android Port Analysis — FreeMind-MMX

Phase 0 archaeology for turning this repository into a modern Android mind-mapping app.
Legacy FreeMind / FreeMind-MMX source under `freemind/` remains the behavioral specification.
The new Android application lives under `android/` and must not treat Swing UI as portable.

**Date:** 2026-08-09  
**Source revision:** `a3674b0` (MMX tip on `master`)

---

## 1. Current architecture (legacy)

| Area | Location | Notes |
|------|----------|-------|
| Desktop app entry | `freemind/freemind/main/FreeMind.java` | Swing `JFrame` application |
| Map model | `freemind/freemind/modes/MindMapMapModel.java` | Load/save `.mm` / `.mmx` |
| Node model | `freemind/freemind/modes/NodeAdapter.java` | Implements Swing `MutableTreeNode` |
| XML parse/serialize | `freemind/freemind/main/XMLElement.java` | NanoXML 2 Lite + MMX attlist filters |
| XML → model | `freemind/freemind/modes/XMLElementAdapter.java` | Attribute/child dispatch |
| Schema (docs) | `freemind/freemind.xsd` | Not enforced at runtime |
| MMX join | `freemind/.../freemind_join_mm_mmx.xslt` + Saxon | Join by node `@ID` |
| View / layout | `freemind/freemind/view/**` | AWT/Swing rendering |
| Controller / actions | `freemind/freemind/controller/**` | Desktop menus/actions |
| PDA experiment | `pda/` | SuperWaba rewrite; ideas only |
| Flash viewer | `flash/` | Browser viewer; not Android |
| Plugins | `freemind/plugins/**`, `plugins/` | Desktop extensions |

FreeMind-MMX is a fork that keeps map **content** VCS-friendly by moving volatile attributes into a hidden `.mmx` sidecar and writing UTF-8 text instead of numeric character entities.

---

## 2. Document model (where it lives)

Primary types:

- `MindMap` / `MapAdapter` / `MindMapMapModel` — map file, root, dirty flag, registry
- `MindMapNode` / `NodeAdapter` / `MindMapNodeModel` — tree nodes
- `HistoryInformation` — `CREATED` / `MODIFIED`
- `MindMapLinkRegistry` — node IDs and arrow links
- `EdgeAdapter`, `CloudAdapter`, `ArrowLinkAdapter` — decorations / connectors
- `MindIcon`, attributes (`Attribute`, registry), hooks

`NodeAdapter` stores (among other fields): text / rich text, notes, parent/children, folded, left/right position, style, colors, font, gaps/shift, hyperlink, edge/cloud/icons/hooks/attributes, history timestamps.

**Classification:** domain algorithms are category **B/C** (extract behavior, do not reuse Swing tree APIs). Swing views/controllers are **D**.

---

## 3. How `.mm` files are parsed

Entry path:

1. `MindMapMapModel.load(File)` → `loadTree`
2. Optionally join with `.mmx` via `Tools.getActualReader(file, frame)`
3. `ModeController.createNodeTreeFromXml` → `XMLElementAdapter.parseFromReader`
4. File I/O uses UTF-8 `UnicodeReader` (BOM-aware)

Document shape (from `freemind.xsd` + runtime):

```xml
<?xml version="1.0" encoding="UTF-8"?>
<map version="1.0.1">
  <!-- optional attribute_registry -->
  <node ID="..." TEXT="..." POSITION="left|right" FOLDED="true|false" ...>
    <richcontent TYPE="NODE|NOTE"><html>...</html></richcontent>
    <edge .../><cloud .../><font .../><icon BUILTIN="..."/>
    <arrowlink DESTINATION="..."/><linktarget .../>
    <hook NAME="..."/><attribute NAME="..." VALUE="..."/>
    <node>...</node>
  </node>
</map>
```

Runtime parser is permissive NanoXML-based code; the XSD is documentation, not a hard validator.

---

## 4. How `.mm` files are written

`MindMapMapModel.saveInternal` → `getXml(writer, true, managed_attr)`:

- Always writes XML declaration with charset `UTF-8` (`FreeMind.DEFAULT_CHARSET`)
- Writes `<map version="1.0.1">` and MMX comment banners
- Saves attribute registry unless writing `.mmx` (`managed_attr == 1`)
- Root `NodeAdapter.save(..., managed_attr)` recursively writes nodes

Node attributes commonly written: `TEXT`, `ID`, `POSITION` (first-level only), `COLOR`, `BACKGROUND_COLOR`, `STYLE`, `LINK`, `VGAP`/`HGAP`/`VSHIFT`, `FOLDED`, `CREATED`, `MODIFIED`, `ENCRYPTED_CONTENT`.

Child elements: `richcontent`, `edge`, `cloud`, `font`, `icon`, `arrowlink`/`linktarget`, `hook`, `attribute`, nested `node`.

---

## 5. FreeMind-MMX vs upstream FreeMind

| Topic | Upstream-ish FreeMind | FreeMind-MMX (this fork) |
|-------|------------------------|---------------------------|
| Encoding | Non-ASCII often as `&#xNN;` entities | Raw UTF-8 when `wh_nonascii_in_utf8=true` |
| Fold / timestamps in `.mm` | Stored in the map file | Preferentially moved to `.mmx` |
| Fold dirtying map | Fold changes mark dirty | `resources_save_folding_state=false` by default |
| Sidecar | None | Hidden `.<basename>.mmx` |
| Load | Single file | Saxon XSLT join by `@ID` when sidecar present |
| Version banner | Standard | Adds “MMX Hack” comments / about string |

MMX-related commits of interest: `629966c` (load/join), `26d38d4` (save sidecar), `836a351` (UTF-8 writers), `a3674b0` (do not entity-escape UTF-8), `1cfc248` / `e099cdd` (settings).

---

## 6. `.mmx` format observations

### Filename convention

For `path/MyMap.mm` → sidecar `path/.MyMap.mmx`  
(dot prefix + basename without `.mm` + `.mmx`).

If the primary file extension is not `.mm`, sidecar is `.<full-name>.mmx`.

### `managed_attr` modes

| Value | Role |
|-------|------|
| `0` | `.mm` — content file; blacklist volatile attrs; force branch `FOLDED="true"` when fold separation enabled |
| `1` | `.mmx` — whitelist-only: always `ID`; optionally `FOLDED`, `CREATED`, `MODIFIED` |
| `2` | Vanilla all-in-one (documented; not default save path) |

### Attributes in `.mm` vs `.mmx` (defaults)

Moved to `.mmx` when corresponding `wh_separate_attr_*` flags are true:

- `FOLDED`
- `CREATED`
- `MODIFIED`

Always present on `.mmx` nodes used for join: `ID`.

Content that stays in `.mm`: text/richcontent, position, colors, style, link, edge/cloud/font/icon/hook/attribute/arrow links, children.

Important fold quirk (from `NodeAdapter.save`): when writing `.mm` with fold separation enabled, non-root non-leaf nodes are written with `FOLDED="true"` regardless of actual state. Real fold state is restored from `.mmx` on join. Opening an MMX `.mm` alone therefore shows branches collapsed.

### Load join

`Tools.getActualReader`:

1. If MMX disabled or sidecar missing → read `.mm` only
2. Else run `freemind_join_mm_mmx.xslt` with Saxon, parameter `mmx_file` = sidecar URI
3. XSLT copies `.mm` node attributes, then overlays attributes from the `.mmx` node with the same `@ID`
4. On failure → fall back to `.mm` only

**Join requires stable node IDs.** Nodes without `ID` never receive `.mmx` overlays.

### Settings (defaults in `freemind.properties`)

- `wh_save_extra_attrs_in_aux_file=true`
- `wh_separate_attr_folded=true`
- `wh_separate_attr_created=true`
- `wh_separate_attr_modified=true`
- `wh_nonascii_in_utf8=true`
- `resources_save_folding_state=false`

---

## 7. Feature inventory (persistence)

| Feature | XML representation | MVP plan |
|---------|--------------------|----------|
| Node text | `TEXT` or `<richcontent TYPE="NODE">` | Required |
| Hierarchy | Nested `<node>` | Required |
| IDs | `ID` | Required (MMX join + links) |
| Position | `POSITION=left\|right` on root children | Required |
| Folded | `FOLDED` (+ MMX split) | Required |
| Timestamps | `CREATED` / `MODIFIED` millis | Preserve; MMX split |
| Colors | `COLOR`, `BACKGROUND_COLOR` | Later (parse/preserve early) |
| Fonts/styles | `<font .../>`, `STYLE` | Later (preserve) |
| Icons | `<icon BUILTIN="..."/>` | Later (preserve) |
| Hyperlinks | `LINK` | Later (preserve) |
| Notes | `<richcontent TYPE="NOTE">` | Later (preserve) |
| Arrows | `arrowlink` / `linktarget` | Later (preserve) |
| Clouds | `<cloud .../>` | Later (preserve) |
| Edges | `<edge .../>` | Later (preserve) |
| Attributes | `<attribute .../>` + registry | Later (preserve) |
| Hooks / plugins | `<hook NAME="...">` | Preserve unknown via opaque XML |
| Encryption | `ENCRYPTED_CONTENT` | Out of MVP |

---

## 8. Unknown XML handling (compatibility risk)

Legacy behavior:

- **Unknown node attributes:** ignored by `XMLElementAdapter` (not stored) — **round-trip loss today**
- **Unknown child elements:** largely dropped unless recognized or handled as hooks
- **Unknown hooks:** substituted with `PermanentNodeHookSubstituteUnknown` so they can round-trip

For the Android port we should improve on this where practical:

- Keep an `unknownAttributes` / `unknownChildren` bag on nodes and map-level extensions
- Prefer semantic equivalence tests over byte-identical XML
- Document remaining lossy cases (encrypted nodes, desktop-only hooks)

PDA insight (`pda/`): `FreeMindNode.atts` keeps attribute lists and rewrites them — useful pattern for round-trip safety, even though PDA itself is not an Android implementation.

---

## 9. UTF-8 handling

- Writers use `UTF-8` explicitly
- `XMLElement.writeEncoded`: when `wh_nonascii_in_utf8` is true, printable non-ASCII is written raw; control chars still entity-escaped
- `HtmlTools` / rich content path can unescape UTF-8 entities when the flag is on (`a3674b0`)
- Loader must accept both entity-encoded legacy maps and raw UTF-8 MMX maps

---

## 10. Pure Java vs Swing/AWT coupling

### Category A/B candidates (behavior to reuse after extraction)

- MMX split/join rules and filename convention
- Attribute white/black lists for `.mm` vs `.mmx`
- UTF-8 write policy
- Schema-level knowledge of node/map structure
- Link registry ID assignment ideas
- Sample maps / tests as fixtures

### Category C (reimplement)

- Domain model without Swing `TreeNode`
- Parser/writer (XmlPull / kotlinx or hand-rolled) without NanoXML+Resources singleton
- Layout engine (desktop view is not portable)
- Editing operations + undo/redo
- Android UI (Compose)
- SAF document workflow + MMX sidecar strategy

### Category D (leave intact, do not port)

- `freemind.view.*`, menu XML, installers, Windows launcher, Flash, MediaWiki plugin, SuperWaba PDA UI

Do **not** compile the Swing FreeMind tree as an Android app.

---

## 11. Tests and fixtures (legacy)

- Unit tests: `freemind/tests/freemind/` (`HtmlConversionTests`, `ToolsTests`, …)
- Sample map: `freemind/tests/freemind/testmap.mm` (richcontent NODE/NOTE, attributes, IDs)
- Docs/examples: `freemind/doc/*.mm`, `admin/docs/features/**/*.mm`
- **No committed `.mmx` samples** found — Android tests must synthesize sidecars

---

## 12. Licensing considerations

| Component | License |
|-----------|---------|
| FreeMind / FreeMind-MMX | GPL-2.0-or-later (`freemind/license`) |
| NanoXML 2 Lite (`XMLElement.java`) | zlib-style (Marc De Scheemaecker) |
| PDA tree | GPL / LGPL texts under `pda/` |
| Bundled jars (Saxon, Xalan, …) | Various — check before redistributing |

**Implication:** The new Android app in this repository should be treated as GPL-compatible (recommended: GPL-2.0-or-later to match FreeMind). Prefer clean-room reimplementation of format/domain rather than copying Swing-coupled classes. If any legacy Java is copied, preserve copyright headers and GPL obligations.

Do not ship unnecessary desktop jars (Saxon, etc.) into the Android APK; reimplement MMX join in Kotlin.

---

## 13. Compatibility risks

1. Opening MMX `.mm` without `.mmx` collapses branches and loses timestamps.
2. Join depends on `ID`; missing IDs break overlay.
3. Hidden dotfiles (`.name.mmx`) are unreliable on some Android document providers / cloud SAF roots.
4. Mixed UTF-8 and entity-encoded legacy files.
5. Unknown attrs/elements are lossy in legacy code; Android should aim to preserve more.
6. Rich HTML notes/nodes need a deliberate rendering strategy later.
7. Freeplane / vanilla FreeMind ignore `.mmx` — interoperability expectations must be documented.
8. GPL coupling if Java sources are copied carelessly.

---

## 14. Android SAF + `.mmx` strategy (decision summary)

See ADR `0002-mmx-on-android-storage.md`.

Preferred approach for MVP:

1. When the `.mm` URI is a filesystem path we can resolve and a sibling write is allowed → use classic `.<basename>.mmx`.
2. Otherwise, offer an explicit paired `.mmx` document (user-selected or same-provider sibling create when possible).
3. Persist pairing metadata in app-private recent-docs storage (URI permissions), **never** invent a proprietary primary format.
4. Never silently discard loaded MMX metadata on save.

---

## 15. Proposed Android architecture

New code under `android/` (legacy `freemind/` untouched unless a tiny reference fix is required):

```
android/
  settings.gradle.kts
  app/                 → :app (android-app) Compose UI, SAF, ViewModels
  mindmap-core/        → :mindmap-core  pure Kotlin domain + editing + undo
  freemind-format/     → :freemind-format  .mm / .mmx parse & write
  mindmap-layout/      → :mindmap-layout  tree → geometry (no Compose)
```

Boundaries:

1. **freemind-format** — XML ↔ domain DTOs; MMX join/split; UTF-8 policy; unknown XML bags
2. **mindmap-core** — `MindMap` / `MindMapNode`; reversible edit operations; selection-agnostic
3. **mindmap-layout** — node bounds, connectors, fold-aware layout, hit testing helpers
4. **app** — Material 3 Compose UI, canvas renderer, ViewModel/StateFlow, SAF, intents, recents

Testing:

- JVM unit tests for format + core + layout (no emulator)
- Instrumented/UI tests only where valuable later

Rendering default: Compose `Canvas` with a viewport transform; keep layout engine swappable.

---

## 16. Milestone status

| Milestone | Status |
|-----------|--------|
| 0 Archaeology + this document | Done |
| 1 Skeleton (Gradle, modules, Material 3 shell, CI docs) | In progress |
| 2 FreeMind reader | Next |
| 3 Map viewer | Pending |
| 4 Basic editor + undo | Pending |
| 5 Writer + round-trip tests | Pending |
| 6 MMX behavior | Pending |
| 7 Android polish | Pending |

---

## 17. Reusable vs rewrite checklist

| Component | Class |
|-----------|-------|
| MMX attribute split rules | **C** (reimplement from `NodeAdapter` / `MindMapMapModel`) |
| MMX XSLT join semantics | **C** (pure Kotlin ID overlay; no Saxon) |
| UTF-8 / entity policy | **C** |
| NanoXML `XMLElement` | **D** (do not port; replace) |
| `NodeAdapter` model | **C** (new Kotlin model) |
| Swing view/layout | **D** |
| PDA attribute round-trip idea | **B** (concept only) |
| Sample `.mm` fixtures | **A** (copy/adapt into Android tests) |
| Desktop actions/menus | **D** |
