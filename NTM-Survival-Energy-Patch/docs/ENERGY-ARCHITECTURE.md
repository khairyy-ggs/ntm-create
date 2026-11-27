# NTM Survival Energy Expansion — Architecture

## Scope of Phase 1

This patch adds two starter sources to HBM's existing MK2 energy system:

- `machine_solar_panel`: daylight and sky-light dependent.
- `machine_wind_turbine`: open-sky wind source, affected by altitude and weather.

Both extend one shared `TileEntityRenewableGenerator` base and implement `IEnergyProviderMK2`. They use finite stored HE buffers and a per-tick provider limit. The existing `PowerNetwork` remains the only distributor; this patch does not create a second energy network.

## Cable compatibility and isolation

Each renewable generator has exactly one output port, on the rear face opposite its visual front (the block must be connected to a cable on that rear face). This is intentional: a generator cannot be attached to two separate cable graphs at once and accidentally supply both graphs in parallel. The one output can connect to either the ordinary HBM red cable or the dedicated Create Shaft Cable.

The network still separates receivers by type: ordinary receivers are served on red-cable graphs; `IShaftNetOnly` receivers such as the Create Shaft Turbine are served on shaft-cable graphs. A renewable generator is a source, not a shaft-only receiver, so it can supply either graph through its one physical output.

## Balance — starter generators

| Source | Generation | Buffer | Notes |
|---|---:|---:|---|
| Solar Panel | up to 1,500 HE/t | 250,000 HE | Daylight, open sky, weather penalty |
| Wind Turbine | about 1,090–2,830 HE/t depending on altitude, gust and weather | 400,000 HE | Open sky, altitude, deterministic gust, weather |

Solar reaches 1,500 HE/t in full sky light, 1,020 HE/t in rain and 630 HE/t in thunder. Wind is approximately 1,090–2,830 HE/t. Values are initial progression targets and should be play-tested. Existing HBM batteries remain the storage tier; renewable machines do not silently create an infinite buffer.

## Compact turbine rebalance

The existing Create Energy Shaft turbine is configured as Tier I:

- 200,000 SU ceiling
- 256 RPM ceiling
- 5,000,000 HE internal buffer
- 1,000,000 HE full-output threshold
- 25,000 HE startup threshold
- 0.005 HE per SU per tick (about 1,000 HE/t at 200,000 SU)

Steam remains optional and acts as a boost. It is not required for base output.

## Phase 2 and Phase 3 — not included in this patch

The tier profiles are written into `ShaftTurbineTier`, but the following gameplay systems still need implementation and in-game testing:

- Industrial Tier II multiblock (2,000,000 SU)
- Large Tier III multiblock (10,000,000 SU)
- Shaft Turbine Linker item (sneak + right-click turbine controller, then panel)
- Shaft Turbine Control Panel block entity, server-authoritative link storage, and multi-page GUI
- Per-tier input/output ports, fluid routing, structure validation, alarms, and failure states

They are deliberately not represented as finished features in Phase 1. The controller should store a dimension + controller BlockPos link, validate the target before opening, and read metrics from the server-side turbine entity. It must not trust client-submitted coordinates or duplicate energy buffers.
