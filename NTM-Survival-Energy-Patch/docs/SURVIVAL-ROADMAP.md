# NTM-Continuum Survival Machine Roadmap

## Stage A — Starter infrastructure (Phase 1 patch)

- Photovoltaic Solar Panel: cheap, passive, daytime source.
- Wind Turbine: exposed-sky renewable source with altitude and weather variation.
- Shared HBM MK2 provider base with bounded buffers and transfer speed.
- Compact Create Shaft Turbine rebalanced to 200k SU.
- Simple vanilla + Create crafting recipes and HBM-style pixel textures / block models.

## Stage B — Operator equipment

- Shaft Turbine Linker: bind a turbine controller to one control panel with sneak + right-click.
- Control Panel GUI: Overview, Mechanical, Electrical, Fluids, Alarms, Configuration.
- Link validation on server, persistent controller location, unlink/replace support, stale-link recovery.

## Stage C — Industrial machines

- Tier II Industrial Shaft Turbine: 2m SU; moderate multiblock, dedicated intake/exhaust/cooling and service access.
- Tier III Large Shaft Turbine: 10m SU; larger rotor casing, separate fluid and power connections, heat management and maintenance requirements.
- Tier-specific recipes from vanilla, Create, and HBM materials; avoid recipe inflation that makes early survival grindy.

## Stage D — More survival utility machines

- Wind/solar battery controller and compact substation.
- Water pump and cooling equipment.
- Ore washing / crushing and resource recovery equipment.
- Small diesel or biomass generator upgrades using HBM's existing fuel and machine systems.
- Processing machines that improve survival automation without replacing the existing HBM progression.

## Visual direction

Use HBM's own machine language: industrial steel, painted panels, bolts, service hatches, exposed bearings, copper/brass fittings, restrained warning colors, and 16x/32x pixel textures. Avoid smooth sci-fi shells, random greebles, excessive neon, and repeated unmotivated vents. Animated parts should be separate from the static casing and rotate around their actual shaft pivot.
