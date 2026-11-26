# Code style

- Java 21. Records for packets and small value types. `final` utility classes with private constructors.
- Packages by feature: `leveling/`, `network/`, `client/`, later `jobs/`, `skills/`, `titles/`, `gates/`, `bosses/`. Each feature has:
  - a `*Service` class: the only API other features call,
  - a `*Rules` class: every balance number,
  - an `*Events` class: Forge event handlers.
- Anything that imports `net.minecraft.client` stays in `client/` and is only referenced from `Dist.CLIENT` subscribers. Packet handlers must not touch client classes directly; go through a cache class with no client imports (see `ClientStatsCache`).
- Use `ProjectZero.id("path")` for every `ResourceLocation`.
- All player-visible text goes through `Component.translatable` with keys in `assets/projectzero/lang/en_us.json`. Exception: short [System] chat lines may be literal until we add translations.
- Comments explain *why*, not what. Javadoc on every public `*Service` method.
- No reflection or mixins unless there is no event for it; if you add a mixin, explain why in the handoff.
