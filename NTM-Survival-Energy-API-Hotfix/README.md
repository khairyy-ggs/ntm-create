# NTM Survival Energy API Hotfix

Fixes the shared renewable-generator sources for the 1.21.1 and 26.1 NeoForge targets.

## What changed

- Replaced direct `Level.isClientSide` field access with `Level.isClientSide()` in the four renewable-generator source files. `LevelReader.isClientSide()` is available in both target versions.
- Replaced `Level.getDayTime()` with `Level.getSkyDarken() < 4` for the solar day/night check. `getDayTime()` was replaced by world-clock APIs in 26.1; `getSkyDarken()` is common to both supported targets.
- No recipes, registrations, energy values, models, or textures are changed by this hotfix.

## Install

1. Extract this folder to the project root, beside `gradlew.bat`.
2. From the project root, run:

   ` .\NTM-Survival-Energy-API-Hotfix\INSTALL-HOTFIX.cmd `

3. Build the requested target(s):

   ` .\gradlew.bat --configure-on-demand :1.21.1-neoforge:build `

   ` .\gradlew.bat --configure-on-demand :26.1-neoforge:build `

The installer backs up the four replaced files under `.ntm-renewable-api-backup-<timestamp>`.

## Compatibility note

The solar panel still requires open sky and checks sky light/weather. The day/night gate uses the shared sky-darkening API, which may behave slightly differently around dawn/dusk than a direct time-of-day comparison.
