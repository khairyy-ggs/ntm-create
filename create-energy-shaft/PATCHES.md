# Edits to existing NTM-Continuum files

Target: **NeoForge 1.21.1 only** (Create 6.x). New files in this pack drop into the repo at the same paths.
Files marked REPLACE overwrite the repo's file. Everything below is an edit to a file that stays.
Nothing here has been compiled (see README, "Verified").

Stonecutter note: the repo is one source tree for several loader/version nodes. Keep `compat/create/**` out of every
node except `1.21.1-neoforge` (use the project's per-node source exclusion), or those nodes will not compile.

## REPLACE
- `api/energymk2/CableData.java`, `api/energymk2/PowerGraphProvider.java`

## 1. `api/energymk2/PowerNetwork.java` — the shaft filter (in `round`)
Inside `for (GraphNode<CableData> node : faces) {` add `boolean shaftNet = node.data.shaft();` and change the
`receive` condition so a machine receives only on the kind of net it belongs to (providers stay unfiltered,
so any generator or battery can feed a shaft net):

```java
boolean receive = handler instanceof IEnergyReceiverMK2
        && shaftNet == (handler instanceof IShaftNetOnly)      // NEW: shaft turbine <-> shaft cables only
        && MachineCaps.accepts(be, side, CapRole.POWER_IN)
        && !gather.seenReceiving.contains(handler)
        && Math.min(handler.getMaxPower() - handler.getPower(), handler.getReceiverSpeed()) > 0L
        && gather.seenReceiving.add(handler);
```
Known gap: machines connected through power ports (`offerPort`) skip this loop. The shaft turbine has
power ports; see plan step 4 and gametest `shaft_ported_leak`.

## 2. `blocks/network/BlockCable.java` — make the connection test overridable
Replace `private static boolean canConnectTo(LevelReader, BlockPos, Direction)` with
`protected boolean connectsTo(LevelReader level, BlockPos pos, Direction dir)` (instance method) and update its
three callers (`getStateForPlacement`, `onUpdateShape`). Add at its top, so red cables never draw an arm to the turbine:

```java
var nb = level.getBlockState(pos.relative(dir)).getBlock();
if (nb instanceof IShaftNetOnly || level.getBlockEntity(pos.relative(dir)) instanceof IShaftNetOnly) return false;
if (nb instanceof BlockShaftCable) return false;
```

## 3. `tileentity/network/TileEntityCableBaseNT.java`
Add a second constructor `protected TileEntityCableBaseNT(BlockEntityType<?> type, BlockPos pos, BlockState state)`
and a subclass `TileEntityShaftCable` using `ModBlockEntities.SHAFT_CABLE`. `BlockShaftCable` overrides
`newBlockEntity` to return it. (The red cable's type only accepts the red cable block.)

## 4. `blocks/machine/ModMachineBlocks.java` (next to `red_cable` / `machine_large_turbine`)
```java
public static final RegistryObject<BlockShaftCable> shaft_cable = REG.<BlockShaftCable>block(
        "shaft_cable", BlockShaftCable::new,
        p -> p.requiresCorrectToolForDrops().strength(5.0F, 6.0F).sound(SoundType.METAL).noOcclusion())
        .loot(Loot.self())
        .andThen(h -> States.multipart(h.namespace(), h.path())
                .part(new Variant("hbm:block/machine_dummy_empty")).register())
        .tag(BlockTag.PICKAXE);

public static final RegistryObject<MachineShaftTurbine> machine_shaft_turbine = REG.<MachineShaftTurbine>block(
        "machine_shaft_turbine", MachineShaftTurbine::new,
        p -> dummyable(p.strength(5.0F, 6.0F).sound(SoundType.METAL).requiresCorrectToolForDrops().noOcclusion()))
        .model(d -> d.models(Models.parented("minecraft:block/air", Slots.of("particle", "hbm:blocks/block_steel"))).states(Variants.simple()))
        .loot(Loot.self())
        .tag(BlockTag.PICKAXE);
```
```java
public static final RegistryObject<MachineVerticalHeatExchanger> machine_vertical_heat_exchanger =
        REG.<MachineVerticalHeatExchanger>block("machine_vertical_heat_exchanger", MachineVerticalHeatExchanger::new,
        p -> dummyable(p.strength(5.0F, 6.0F).sound(SoundType.METAL).requiresCorrectToolForDrops().noOcclusion()))
        .model(d -> d.models(Models.parented("minecraft:block/air", Slots.of("particle", "hbm:blocks/block_steel"))).states(Variants.simple()))
        .loot(Loot.self())
        .tag(BlockTag.PICKAXE);
```
and `tabItem(shaft_cable); tabItem(machine_shaft_turbine); tabItem(machine_vertical_heat_exchanger);` in the tab list. The shaft cable's look comes from `CableModel` (section 10), not from this JSON placeholder, exactly like the red cable.

## 5. `tileentity/ModBlockEntities.java`
```java
public static final RegistryObject<BlockEntityType<TileEntityShaftCable>> SHAFT_CABLE =
        REG.blockEntity("shaft_cable", () -> BlockEntities.type(TileEntityShaftCable::new, ModMachineBlocks.shaft_cable.get()));
public static final RegistryObject<BlockEntityType<TileEntityMachineShaftTurbine>> MACHINE_SHAFT_TURBINE =
        REG.blockEntity("machine_shaft_turbine", () -> BlockEntities.ticking(TileEntityMachineShaftTurbine::new,
                TileEntityMachineShaftTurbine::clientTick, TileEntityMachineShaftTurbine::serverTick,
                ModMachineBlocks.machine_shaft_turbine.get()));
```
Also add the exchanger's type:
```java
public static final RegistryObject<BlockEntityType<TileEntityMachineVerticalHeatExchanger>> MACHINE_VERTICAL_HEAT_EXCHANGER =
        REG.blockEntity("machine_vertical_heat_exchanger", () -> BlockEntities.ticking(TileEntityMachineVerticalHeatExchanger::new,
                TileEntityMachineVerticalHeatExchanger::clientTick, TileEntityMachineVerticalHeatExchanger::serverTick,
                ModMachineBlocks.machine_vertical_heat_exchanger.get()));
```
## 6. `inventory/ModMenus.java` and `client/HbmClientRegistrations.java`
```java
public static final RegistryObject<MenuType<ContainerMachineShaftTurbine>> MACHINE_SHAFT_TURBINE =
        REG.menu("machine_shaft_turbine", () -> Menus.typeWithPos(ContainerMachineShaftTurbine::new));
public static final RegistryObject<MenuType<ContainerMachineVerticalHeatExchanger>> MACHINE_VERTICAL_HEAT_EXCHANGER =
        REG.menu("machine_vertical_heat_exchanger", () -> Menus.typeWithPos(ContainerMachineVerticalHeatExchanger::new));
// client:
sink.register(ModMenus.MACHINE_SHAFT_TURBINE.get(), GUIMachineShaftTurbine::new);
sink.register(ModMenus.MACHINE_VERTICAL_HEAT_EXCHANGER.get(), GUIMachineVerticalHeatExchanger::new);
```
## 7. `inventory/fluid/Fluids.java` — Hot Water
Declare `public static FluidType HOTWATER;`, create it in `init()` as `new FluidType("HOTWATER", 0x6C8FD6)` with
temperature 90 and a coolable step back to WATER in the fluid's property data (follow how an existing hot fluid's
property file is authored), add it to `metaOrder`, and add the lang line from the snippet.

## 8. Create bridge registration (NeoForge 1.21.1 only)
Register `shaft_output` (`ShaftOutputBlock`) and its block entity type, and assign `CreateBridge.SHAFT_OUTPUT_TYPE`,
only when Create is present (`ModList.get().isLoaded("create")`). Add Create 6.x as `compileOnly` for that node and an
optional dependency in `neoforge.mods.toml`. Create's Maven coordinates and the late-registration hook in Golem
Core's `REG` are unconfirmed (plan step 7). Fallback: make Create a required dependency for this node.

## 8b. Create pumps and pipes
No code needed. `ForeignFluids.registerMachines` already exposes every HBM block entity that is a fluid user to
NeoForge fluid handlers, so Create pumps and pipes reach the turbine and the exchanger. Water maps to vanilla water
through `Fluids.toVanilla`. Confirm with a pump on the exchanger's upper inlet in the gametest (plan step 6b).

## 9. Language
Merge `src/main/resources/assets/hbm/lang/en_us.shaft.json.snippet` into `en_us.json`.

## 10. Models, renderers and items (new in this pack)
All art is generated at 16 px per block by `tools/` (see README). New files: `models/machines/shaft_turbine.obj`,
`models/machines/vertical_heat_exchanger.obj` and their textures under `textures/models/machines/`,
`textures/models/blocks/cable_shaft.png`, `textures/blocks/shaft_output.png`, `models/block/shaft_output.json`,
`blockstates/shaft_output.json`, and the renderer classes under `client/render/`.

**`client/render/HbmBlockEntityRenderers.java`**, next to the large turbine line:
```java
regGem(sink, ModBlockEntities.MACHINE_SHAFT_TURBINE.get(), ShaftTurbineRenderer::new);
regGem(sink, ModBlockEntities.MACHINE_VERTICAL_HEAT_EXCHANGER.get(), VerticalHeatExchangerRenderer::new);
```
**`client/render/CableModel.java`**: the sprite is a constant. Make it a field so the shaft cable can use its own texture on the
same `cable_neo.obj` (the UVs are shared, `cable_shaft.png` is `cable_neo.png` with the accent pixels recoloured to brass):
```java
private final String sprite;
public CableModel() { this("hbm:models/blocks/cable_neo"); }
public CableModel(String sprite) { this.sprite = sprite; }
// line 43:  out.add(model().part(part), sprite, -1, CENTRE);   (drop the SPRITE constant)
```
**`client/ChunkModelRegistrations.java`**, below the red cable line:
```java
Disguises.register(ModMachineBlocks.shaft_cable, new CableModel("hbm:models/blocks/cable_shaft"));
```
**`items/ObjItemDeclarations.java`** (inventory and hand models, same pattern as the large turbine and the red cable):
```java
Dejson.item(NS, "machine_shaft_turbine")
        .definition(MachineObjModel.of("hbm:models/machines/shaft_turbine.obj", "hbm:textures/models/machines/shaft_turbine.png")
                .scale(1.0).ty(0.0).yaw(90.0).guiScale(2.5).rawLegacyTransform()
                .itemTranslate(0.0, 0.0, 0.0).itemPreTranslate(0.0, 0.0, 0.0).guiTranslate(0.0, -1.0, 0.0)).oversizedInGui().register();
Dejson.item(NS, "machine_vertical_heat_exchanger")
        .definition(MachineObjModel.of("hbm:models/machines/vertical_heat_exchanger.obj", "hbm:textures/models/machines/vertical_heat_exchanger.png")
                .scale(1.0).ty(0.0).yaw(0.0).guiScale(2.4).rawLegacyTransform()
                .itemTranslate(0.0, 0.0, 0.0).itemPreTranslate(0.0, 0.0, 0.0).guiTranslate(0.0, -1.5, 0.0)).oversizedInGui().register();
Dejson.item(NS, "shaft_cable")
        .definition(MachineObjModel.of("hbm:models/blocks/cable_neo.obj", "hbm:textures/models/blocks/cable_shaft.png")
                .scale(1.25).ty(0.0).yaw(180.0).frame("block").rawLegacyTransform().exclude("posY", "negY", "CX", "CY", "CZ")
                .itemTranslate(0.0, 0.0, 0.0).itemPreTranslate(0.0, 0.0, 0.0)).oversizedInGui().register();
```
The turbine entry copies the large turbine's numbers because the OBJ has the same envelope and centring. The exchanger's
gui scale and translate are a first guess: tune them in game.

The `shaft_output` block ships a plain JSON model and blockstate (facing north is the model, the other five are rotations); make
sure its registration does not also generate a blockstate.
