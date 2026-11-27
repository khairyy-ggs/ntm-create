# NTM Survival Energy Expansion - Phase 1

This is a source patch for NTM-Continuum, not a ready-to-use JAR. It adds two early-game HBM energy sources and rebalances the existing compact Create Shaft Turbine.

## Included

- **Photovoltaic Solar Panel**: up to 1,500 HE/t in daytime under open sky. Rain and thunder reduce output. Buffer: 250,000 HE.
- **Industrial Wind Turbine**: open-sky source affected by altitude, deterministic gust variation and weather. Approx. 1,090-2,830 HE/t. Buffer: 400,000 HE.
- Both generators implement HBM `IEnergyProviderMK2` and use the existing `PowerNetwork`; no second electrical distribution system is created.
- Each generator has one rear-facing output port. It can connect to either the ordinary red-cable network or the separate Create Shaft Cable network, but not both at once.
- Hand-authored Minecraft block models, blockstates, item models, 16x/32x pixel textures, recipes and loot tables.
- The existing compact Create Shaft Turbine is rebalanced to 200,000 SU, 256 RPM, 5,000,000 HE buffer, 1,000,000 HE full-output threshold and 0.005 HE/SU/t (about 1,000 HE/t at full 200k SU). Steam remains optional.
- `ShaftTurbineTier` stores the intended capacity profiles for compact, industrial and large tiers.

## Not included yet

The following are **planned**, not complete in this Phase 1 patch: Tier II and Tier III multiblock turbines, animated wind rotor, Shaft Turbine Linker, Control Panel block/entity/GUI, per-tier fluid ports and alarms. See `docs/SURVIVAL-ROADMAP.md`.

## Install

1. Back up or commit the full project.
2. Extract the `NTM-Survival-Energy-Patch` folder beside `gradlew.bat`.
3. From the project root, run `NTM-Survival-Energy-Patch\INSTALL-PATCH.cmd`. The wrapper uses a process-only PowerShell execution-policy bypass; it does not permanently change Windows policy.
4. Build with `.\gradlew.bat --configure-on-demand :1.21.1-neoforge:build`.

The installer backs up existing modified files under `.ntm-survival-expansion-backup`. It adds the two blocks and block entities to the existing registries, adds the authored JSON declarations to `gradle/dejson-authored.txt`, adds English names and updates the compact turbine constants.

## Verification status

Pure Java balance tests pass. All included JSON files parse, and all block-model texture references resolve. The full Gradle build has **not** been run here because the uploaded material did not contain the complete project and Gradle dependency environment. Build and test this patch on the actual project before using it in a survival world.
