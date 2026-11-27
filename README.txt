NTM Survival Energy installer hotfix

This replaces only NTM-Survival-Energy-Patch/INSTALL-PATCH.ps1. The original installer stopped at step 4 because it depended on exact old numeric literals. This version finds the six balance declarations by field name, validates all six first, supports CRLF/LF line endings, and then updates them as a group.

Install:
1. Extract this ZIP into the project root (the directory containing gradlew.bat), allowing overwrite.
2. From the project root run:
   .\NTM-Survival-Energy-Patch\INSTALL-PATCH.cmd

Your previous installer completed steps 1-3 before stopping. It may already have copied source/resources and added marked registry entries. The installer checks those markers to avoid duplicate registration. Backups are in .ntm-survival-expansion-backup.
