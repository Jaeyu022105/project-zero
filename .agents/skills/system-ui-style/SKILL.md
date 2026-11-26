---
name: system-ui-style
description: Build or restyle any Project Zero screen, HUD element, title or [System] message so it matches the System look.
---

# The System look

Reference implementation: `src/main/java/com/projectzero/client/StatusScreen.java`.

## Palette (ARGB)
- Panel `0xE00A1220` (dark navy, slightly translucent), outer glow `0x553FD0FF`
- Border `0xFF3FD0FF` (cyan), dividers `0x803FD0FF`
- Labels `0xFF7FE8FF` (light cyan, ALL CAPS), values `0xFFFFFFFF`
- Muted `0xFF8090A0`, attention `0xFFFFD34D` (unspent points, new quest)
- Bars: background `0xFF1A2A3A`, fill cyan for XP, blue for mana, red for HP

## Layout
- Centered window, 1px cyan border plus 2px glow, header centered in caps, a divider under it.
- Two-column label: value rows for identity info. Stats as rows with a + button on the right.
- Hover shows a tooltip with what the thing does in plain words.
- Screens do not pause the game (`isPauseScreen` false). The same key that opens a window closes it.

## Messages
- Chat: `[System]` prefix in aqua, message in white, rewards in yellow. Short, a little cold: "You have leveled up." not "Congrats!!".
- Big moments (level up, job change, title earned) use an on-screen title + subtitle and a sound.
- Action bar for small, frequent things (+XP, Evaded!).

## Do not
- Use vanilla-looking gray buttons for new System windows if a custom style exists; keep everything in the palette.
- Copy art, fonts or exact UI from the anime or manhwa.
