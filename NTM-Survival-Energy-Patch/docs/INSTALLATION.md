# Installation (Windows)

1. Back up the project or commit it to Git first.
2. Extract the `NTM-Survival-Energy-Patch` folder beside `gradlew.bat` in the project root.
3. From the project root, run:

   ```powershell
   .\NTM-Survival-Energy-Patch\INSTALL-PATCH.cmd
   ```

   The CMD wrapper starts PowerShell with a process-only execution-policy bypass. It does not change the machine's permanent execution policy.

4. Build the 1.21.1 NeoForge target:

   ```powershell
   .\gradlew.bat --configure-on-demand :1.21.1-neoforge:build
   ```

5. The patch copies source and assets, registers the two renewable blocks and their block entities, appends authored JSON paths to `gradle/dejson-authored.txt`, and changes the existing compact shaft turbine balance.

The installer creates `.ntm-survival-expansion-backup` in the project root before replacing existing files. It is idempotent for its registration markers and balance values.
