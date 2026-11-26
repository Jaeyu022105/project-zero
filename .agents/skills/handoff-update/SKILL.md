---
name: handoff-update
description: Update HANDOFF.md (and DESIGN-DOCUMENT.md when design changed) at the end of any Project Zero task.
---

# Updating the handoff

1. Append a session log at the **bottom** of `HANDOFF.md`:
   ```
   ### Session Log — YYYY-MM-DD (Short Title)

   **Context & Direction Check**
   - What the owner asked, any redirection and why.

   **Changes Made**
   - What changed, with file paths.

   **Verification Executed**
   - Exact commands and in-game tests, and their results. Say plainly what was not run.

   **Remaining Recommendations**
   1. Next steps, most important first.

   ---
   ```
2. Fix any **reference section** above "Operating Guidance For Future AI" that your work made wrong: Current App State, Architecture Notes, Data Model Notes, Known Gaps.
3. Update the "Last caught up" line at the top.
4. If the design changed, update `DESIGN-DOCUMENT.md` and its status tags in the same commit.
5. Never rewrite old session logs.
