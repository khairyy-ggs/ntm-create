NTM Continuum - DirectionProperty cross-version compile hotfix

Reason:
The renewable block source is shared by Stonecutter targets. The submitted
build log shows :1.21.1-neoforge:compileJava completed, while
:26.1-neoforge:compileJava failed because DirectionProperty is not found in
net.minecraft.world.level.block.state.properties for the 26.1 target.

Fix:
Remove the version-specific DirectionProperty field and use
BlockStateProperties.HORIZONTAL_FACING directly at each BlockState call site.
The default facing, placement facing, rotate/mirror behavior, and blockstate
property remain the same.

Files replaced:
- src/main/java/com/hbm/blocks/machine/MachineSolarPanel.java
- src/main/java/com/hbm/blocks/machine/MachineWindTurbine.java

Install:
Extract this archive into the project root and allow overwrite of the two
.java files, OR run INSTALL-HOTFIX.cmd from this extracted folder while your
PowerShell current directory is the project root.

Project root expected:
D:\\khairy\\java mod\\mc-dev\\NTM-Continuum-create

Then run:
.\\gradlew.bat --configure-on-demand :1.21.1-neoforge:build

This hotfix targets the DirectionProperty error only. If another 26.1 API
incompatibility appears afterwards, handle that separately. Do not treat a
full build as verified until Gradle finishes with BUILD SUCCESSFUL.
