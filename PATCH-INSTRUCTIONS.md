# NTM-Continuum Create Energy Shaft patch

Target: NTM-Continuum, Minecraft 1.21.1 NeoForge, Create 6.x.

## Files in this patch

- `src/create/java/com/hbm/compat/create/ShaftOutputBlock.java`
  - Adds an explicit block-entity ticker. Create's `IBE` default ticker only ticks `SmartBlockEntity`; this block entity extends `GeneratingKineticBlockEntity`, so it needs its own ticker.
  - Treats the shaft as a two-ended axial connection.
- `src/create/java/com/hbm/compat/create/ShaftOutputBlockEntity.java`
  - Uses Create's direction conversion for generated speed.
  - Continues to publish the turbine's SU/RPM as Create stress capacity/speed.
- `src/main/java/com/hbm/tileentity/machine/ShaftTurbineModel.java`
  - Lowers the startup threshold and buffer/full-output values for early-game.
  - Lowers energy cost by 200x from 0.2 HE/SU to 0.001 HE/SU.
- Four shaped crafting recipes under `src/main/resources/data/hbm/recipe/`.

## Balance values

- Startup threshold: 1,000,000 HE (was 10,000,000)
- Stored power for full output: 20,000,000 HE (was 300,000,000)
- Maximum stored power: 50,000,000 HE (was 600,000,000)
- Energy cost: 0.001 HE per SU per tick (was 0.2)
- At 5,000,000 SU, energy draw is 5,000 HE/t without steam.
- Steam remains optional and continues to act as a booster.

## Apply

1. Extract this ZIP into the project root `NTM-Continuum-create`, allowing these three Java files to overwrite the existing files.
2. Copy the four `data/hbm/recipe/*.json` files into `src/main/resources/data/hbm/recipe/`.
3. Run `./INSTALL-PATCH.ps1` from the project root. It adds the four recipe paths to `gradle/dejson-authored.txt` if that file exists, and creates a `.bak` backup before changing it.
4. Rebuild with:
   `./gradlew.bat --configure-on-demand :1.21.1-neoforge:build`
5. Test in a new creative test world first. Place Shaft Output adjacent to the turbine's output/front side, make its FACING point away from the turbine, then connect an ordinary Create shaft on the outward side, on the same axis. The output block must be rebuilt/replaced after updating the JAR in an existing world.

## Recipe contents

- Shaft Cable x4: copper ingots + redstone + Create shaft.
- Shaft Turbine x1: iron ingots + Andesite Casing + Andesite Alloy + Create shaft.
- Shaft Output x1: iron ingots + Andesite Alloy + Create shaft.
- Vertical Heat Exchanger x1: copper ingots + glass + Create fluid pipes + iron ingot.

## Verification limits

The uploaded source bundles contained the specific Java files but not the complete project, all dependencies, or the root Gradle wrapper/build scripts. The pure Java turbine model and recipe JSON can be checked independently, but the entire NeoForge/Create project build must be run in the user's full checkout before treating the patch as a verified JAR.
