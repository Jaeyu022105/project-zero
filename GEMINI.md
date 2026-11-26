# GEMINI.md

Read `AGENTS.md` first; everything there applies. Then `HANDOFF.md` and `DESIGN-DOCUMENT.md`.

Gemini-specific notes:

- Skills live in `.agents/skills/<name>/SKILL.md`. To use them as Gemini CLI skills, copy (or symlink) that folder to `.gemini/skills/` in this repo, or to `~/.gemini/skills/` to have them in every project. On Windows PowerShell, from the repo root:
  `New-Item -ItemType Directory -Force .gemini; Copy-Item -Recurse -Force .agents\skills .gemini\skills`
- If your Gemini setup does not load skills automatically, read the matching `SKILL.md` yourself before starting a task.
- No co-author or AI attribution lines in commits.
