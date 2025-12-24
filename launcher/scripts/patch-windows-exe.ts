/**
 * Windows PE Header Patcher
 * 
 * Modifies the PE header to change SUBSYSTEM from CONSOLE to WINDOWS,
 * which prevents the console window from appearing on startup.
 */

import { openSync, readSync, writeSync, closeSync } from "fs";

// PE Constants
const PE_SIGNATURE_OFFSET = 0x3C;
const OPTIONAL_HEADER_SUBSYSTEM_OFFSET = 0x5C;
const SUBSYSTEM_WINDOWS_GUI = 2;

/**
 * Patches a Windows PE executable to use the WINDOWS subsystem.
 * This hides the console window when the application starts.
 */
export function patchWindowsExecutable(filePath: string): void {
  const fd = openSync(filePath, "r+");
  
  try {
    // Read PE header offset from DOS header
    const offsetBuffer = Buffer.alloc(4);
    readSync(fd, offsetBuffer, 0, 4, PE_SIGNATURE_OFFSET);
    const peHeaderOffset = offsetBuffer.readUInt32LE(0);

    // Calculate subsystem field position
    const subsystemPosition = peHeaderOffset + OPTIONAL_HEADER_SUBSYSTEM_OFFSET;

    // Write new subsystem value
    const subsystemBuffer = Buffer.alloc(2);
    subsystemBuffer.writeUInt16LE(SUBSYSTEM_WINDOWS_GUI, 0);
    writeSync(fd, subsystemBuffer, 0, 2, subsystemPosition);

    console.log(`Patched executable: ${filePath}`);
  } finally {
    closeSync(fd);
  }
}
