---
name: forge-mod-dev
description: Write, fix or build Java/Gradle code for the Project Zero Forge 1.21.1 mod. Use for any code change, compile error or API question.
---

# Forge 1.21.1 mod development (Project Zero)

## Setup facts
- Minecraft 1.21.1, Forge 52.1.0, Java 21, ForgeGradle 6, official mappings, `reobf = false`.
- Commands (Windows: `gradlew.bat`): `gradlew build`, `gradlew runClient`, `gradlew runServer`.
- First build downloads Minecraft + Forge (a few minutes). If Gradle complains about Java, install a JDK 21 (e.g. Temurin) or let the foojay toolchain download it.

## 1.21.1 API gotchas (check these first when something does not compile)
- `ResourceLocation` constructors are private: use `ResourceLocation.fromNamespaceAndPath(ns, path)` or `ProjectZero.id(path)`.
- Attributes are `Holder<Attribute>` (`Attributes.MAX_HEALTH`). `AttributeModifier` is a record `(ResourceLocation id, double amount, Operation op)`; operations are `ADD_VALUE`, `ADD_MULTIPLIED_BASE`, `ADD_MULTIPLIED_TOTAL`. Remove with `instance.removeModifier(id)`.
- Networking: `ChannelBuilder.named(id).networkProtocolVersion(int).simpleChannel()`, then `channel.messageBuilder(Type.class, NetworkDirection.PLAY_TO_CLIENT).encoder(...).decoder(...).consumerMainThread((msg, ctx) -> ...).add()`. Send with `channel.send(msg, PacketDistributor.PLAYER.with(player))` or `PacketDistributor.SERVER.noArg()`. Context type: `CustomPayloadEvent.Context`.
- Tags folders are singular in 1.21: `data/<ns>/tags/entity_type/`, `tags/item/`, `tags/block/`.
- `Screen.render` already calls `renderBackground`; calling it twice crashes ("Can only blur once per frame"). Draw panels by overriding `renderBackground` and calling super first.
- Tick events have `Pre`/`Post` subclasses in 52.x. Prefer `InputEvent.Key` for key handling.
- `@Mod.EventBusSubscriber(bus = Bus.MOD)` for registration events (keys, registries, config); `Bus.FORGE` for gameplay events. Add `value = Dist.CLIENT` for client-only subscribers.
- Registries: use `DeferredRegister.create(ForgeRegistries.ITEMS, MOD_ID)` and register it on the mod bus in the mod constructor.

## Project patterns
- Feature package = `*Service` (public API), `*Rules` (numbers), `*Events` (handlers), data class with `save()/load()` NBT and `write()/read()` buffer methods.
- Player data: persistent NBT under `projectzero:<feature>`, copied in `PlayerEvent.Clone`, synced to client on login/respawn/dimension change and after every change.
- Client receives data into a `Client*Cache` class with no client imports.
- New packet: record + static `encode/decode/handle`, register in `ModNetwork.register()`, bump `PROTOCOL_VERSION` if an existing packet's format changed.

## Done means
- `gradlew build` passes.
- For gameplay/UI: tested in `runClient` and the test steps are written in the handoff session log.
