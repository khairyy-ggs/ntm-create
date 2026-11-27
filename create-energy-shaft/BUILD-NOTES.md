# Building with the Create Energy Shaft pack applied

This tree is NTM-Continuum (base 0c0fb44) with the Create Energy Shaft pack and every edit from PATCHES.md applied.

## Open with GitHub Desktop
1. On GitHub, fork `Warfactory-Official/NTM-Continuum` and `Warfactory-Official/Golem-Core`.
2. In GitHub Desktop: File > Clone repository, clone both forks into the SAME parent folder:
   `<parent>/NTM-Continuum` and `<parent>/Golem-Core` (the build includes `../Golem-Core`).
3. Copy the contents of `create-energy-shaft-changed-files.zip` over your `NTM-Continuum` clone (same paths).
   Or, in a terminal inside the clone: `git apply create-energy-shaft.patch`.
4. GitHub Desktop now lists the changed files. Commit to a branch and push.
5. Build: `./gradlew :1.21.1-neoforge:build` (JDK 21). Upstream's `.github/workflows` check out both repos side by side;
   for your fork's CI, do the same (Golem-Core as a sibling).

## What was changed beyond PATCHES.md
- `src/create/java` is a new source root (same pattern as upstream's `src/sable`), added only on nodes that set
  `feature.create_shaft=true` (only `versions/1.21.1-neoforge`). Other nodes never see the Create classes.
- `build.neoforge.gradle.kts`: Create is `compileOnly` via `maven.modrinth:create:<deps.create_run>` (6.0.10+mc1.21.1).
- `neoforge.mods.toml`: Create is an optional dependency.
- `HbmCreate` / `CreateRegistration` register the Shaft Output block, item and block entity type only when Create is loaded.
- `TileEntityShaftCable` (new), and `BlockShaftCable.newBlockEntity`, so the red cable's block entity type stays red-only.
- A loot table for `shaft_output` (not generated, since it is registered outside the Golem registry).
- The Hot Water fluid is appended last (after TITANIUM_TETRACHLORIDE) because ids are positional.

## Not verified (no Gradle run was possible)
- Nothing here has been compiled: the build hosts (Maven, Gradle plugins) were unreachable from the sandbox.
- `IdsGameTests` / `FluidPropertyGameTests` list every fluid id; they will fail until HOTWATER is added to their lists.
- Create's compile-time classpath: the Modrinth Create jar nests Flywheel/Ponder/Registrate, so the Shaft Output classes
  may need those as extra `compileOnly` lines if javac reports missing supertypes.
- Create 6.x method names in `ShaftOutputBlockEntity`, item/tab entry for the Shaft Output, and everything in README "Verified".
- The pack code lives in `src/main`, so the 26.1 and 1.20.1 nodes also compile it; those nodes were not checked.
