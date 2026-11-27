# Create Energy Shaft for NTM-Continuum (NeoForge 1.21.1)

A turbine that turns stored power into Create stress (up to 5,000,000 SU), a dedicated cable for it, and a
Vertical Heat Exchanger that cools its water. Original code, written against the study of NTM-Continuum;
nothing is copied (the source is NonCommercial/GPL, study only).

## How it plays
```
 generators / batteries --(Shaft Cable)--> [ Create Energy Shaft ] --front--> [Shaft Output] --> Create shafts
                                                |   ^
                                    hot water out   cold water in
                                                v   |
 pump --> water in --> [ Vertical Heat Exchanger ] --> pre-heated water out --> tank / pipe / anything that needs water
```
- **Shaft Cable** joins only other shaft cables, the turbine and anything that outputs power. It never merges with the
  red-cable grid, and the net it forms feeds only the turbine.
- **Turbine** output follows the power it holds (linear up to 300M HE stored), scaled by throttle. Speed tops out at 256 RPM.
  Create is told at most once per second, in 1% steps, because each change rebuilds the kinetic network.
- **Minimum water temperature**: you set it in the panel. The setting that balances the turbine moves with the load
  (about 45 C at idle to 90 C at full load, with steam pushing it up). Adjust until the readout says STABLE.
- **Steam limit**: how much steam may be fed in. More steam gives more stress for the same stored power (up to 1.8x) and
  draws less power per SU, but it makes the turbine less stable. Fed steam while unstable, the turbine goes
  WARNING, then CRITICAL with a ten second countdown, then explodes. Cutting the steam or retuning the water in time saves it.
  Without steam a badly tuned turbine only loses output; it never explodes.
- **Vertical Heat Exchanger**: lower level takes the turbine's hot water and gives back cold water; upper level takes pumped
  water and gives out pre-heated water. With no pumped water nothing is cooled and the turbine stalls when its hot tank fills.
  Create pumps and pipes work through the mod's existing fluid exposure.

## Models (1:1 with NTM)
| Asset | Size / scale | Check |
|---|---|---|
| `shaft_turbine.obj` + png | 3 x 4 footprint, 1.97 high; atlas 256x180 | body and rotor both median **16.0 px per block**, NTM's own turbines measure 15.8 to 16.0 |
| `vertical_heat_exchanger.obj` + png | 3 x 3 x 3; atlas 256x135 | body median **16.0 px per block** |
| `cable_shaft.png` | 16x16, NTM's `cable_neo.png` with brass accents | same UVs as the red cable |
| `shaft_output.json` + png | 16-unit block model, 32x32 sheet | every face samples a region its own size |

Conventions copied from NTM: OBJ in block units with `o` groups (`Body`, `Rotor`), triangulated `v/vt/vn`, the turbine centred on
its midpoint (z -2.5..2.5) and shifted by the renderer, `Gem` renderers with the same yaw and spin set-up as the large turbine, and the
palette sampled from NTM's turbine sheets (olive panels, vent slits, orange stripes, steel noise, rivets). Create's brass and the
exchanger's copper and teal flanges are the only additions. The turbine's rotor spins with the published RPM; the front flywheel
ends flush at the block face where the Shaft Output attaches.

Blender (5.2) was used to check the geometry from the same generator code: exact footprint bounds (x -1.5..1.5, z -3.5..1.5), outward winding
(positive signed volume), no degenerate faces, face counts matching. Its viewport screenshot timed out, so the textured renders here come from
the preview tool in `previews/`.

Regenerate everything: `python3 tools/gen_shaft_turbine.py` and `python3 tools/gen_vhe.py` (needs Pillow).

## Verified and not verified
| Piece | Status |
|---|---|
| Turbine control model and heat exchange model (`ShaftTurbineModel`, `HeatExchangerModel`) | compiled and run here, 31 checks pass (`ShaftTurbineModelTest.java`) |
| GUI textures, model textures, previews | generated and viewed |
| Model scale and texel density | measured: 16 px per block, exact footprint bounds, outward winding (also checked in Blender) |
| Everything else (blocks, block entities, cables, GUIs, renderers, Create bridge) | written against the real APIs by reading them, **never compiled or seen in game** |
| Item framing of the exchanger and shaft cable | first guess, tune in game |
| Create 6.x method names in `ShaftOutputBlockEntity` | from an older Create commit, confirm against 1.21.1 |
| Shaft filter for machines connected through power ports | known gap, gametest in the plan |

## A decision you should know about
`CableData` carries nothing in the original on purpose: its comment says a tier field once silently partitioned a world's
power grid. The shaft cable partitions the grid deliberately with one flag, a distinct save byte (0x53, so no old save
changes meaning) and the filter in `PowerNetwork`. If upstream would rather not have that, the fallback in the plan is a
second graph named `shaft_power_graph` with its own provider.

## Assumptions to confirm
- Balance numbers (0.2 HE per SU, 600M buffer, ramps, explosion radius 4 to 10) are placeholders.
- The Hot Water fluid and its cooling back to water need the fluid property data written (PATCHES.md step 7).
- The exchanger's two circuits rely on `IFluidMultiUserMK2` giving each level its own user.

See `PATCHES.md` for every edit to existing files.
