# Workflow

1. Read `HANDOFF.md` and the relevant design doc section.
2. Load the matching skill in `.agents/skills/`.
3. Make the change in small steps. Keep the mod compiling after each step.
4. Verify: `gradlew build` always; `gradlew runClient` and an in-game test for anything gameplay or UI. Write what you tested in the session log. If you could not run it, say so plainly.
5. Update `HANDOFF.md` (session log + stale reference sections) and `DESIGN-DOCUMENT.md` if the design changed.
6. Commit with a conventional message (`feat(leveling): ...`, `fix(network): ...`, `docs: ...`). No co-author or AI lines.
7. Uncommitted experiments happen in the Downloads clone; only commit what the owner is happy with.
