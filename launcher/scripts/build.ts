/**
 * Build Script
 * 
 * Creates a single native executable with the backend binary embedded.
 * The backend is extracted at runtime to the user's application data folder.
 * 
 * Usage: bun run scripts/build.ts [target]
 * Targets: windows, linux, macos, macos-arm (default: current platform)
 */

import { $ } from "bun";
import { existsSync, mkdirSync, cpSync, rmSync, statSync, writeFileSync, readFileSync } from "fs";
import { join } from "path";
import { patchWindowsExecutable } from "./patch-windows-exe";

// =============================================================================
// Configuration
// =============================================================================

const PATHS = {
  dist: "./dist",
  embedded: "./src/embedded",
  backendBuild: "../build/native/nativeCompile",
  appConfig: "../app.config.json",
  generatedConfig: "./src/config.generated.ts",
} as const;

// Load app config
interface AppConfig {
  name: string;
  id: string;
  version: string;
  description: string;
}

function loadAppConfig(): AppConfig {
  const configPath = join(process.cwd(), PATHS.appConfig);
  const content = readFileSync(configPath, "utf-8");
  return JSON.parse(content);
}

function generateConfigFile(config: AppConfig): void {
  const content = `// Auto-generated from app.config.json - DO NOT EDIT
export const AppConfig = {
  name: "${config.name}",
  id: "${config.id}",
  version: "${config.version}",
  description: "${config.description}",
} as const;
`;
  writeFileSync(PATHS.generatedConfig, content);
  console.log("Generated config file from app.config.json");
}

type Target = "windows" | "linux" | "macos" | "macos-arm";

interface TargetConfig {
  bunTarget: string;
  outputName: string;
  backendName: string;
}

const TARGET_CONFIGS: Record<Target, TargetConfig> = {
  windows: {
    bunTarget: "bun-windows-x64",
    outputName: "desktop-app.exe",
    backendName: "desktop-backend.exe",
  },
  linux: {
    bunTarget: "bun-linux-x64",
    outputName: "desktop-app",
    backendName: "desktop-backend",
  },
  macos: {
    bunTarget: "bun-darwin-x64",
    outputName: "desktop-app",
    backendName: "desktop-backend",
  },
  "macos-arm": {
    bunTarget: "bun-darwin-arm64",
    outputName: "desktop-app",
    backendName: "desktop-backend",
  },
};

// =============================================================================
// Build Functions
// =============================================================================

function getCurrentTarget(): Target {
  const p = process.platform;
  if (p === "win32") return "windows";
  if (p === "darwin") return "macos";
  return "linux";
}

function formatSize(bytes: number): string {
  const mb = bytes / (1024 * 1024);
  return `${mb.toFixed(2)} MB`;
}

function prepareEmbeddedDirectory(config: TargetConfig): boolean {
  if (!existsSync(PATHS.embedded)) {
    mkdirSync(PATHS.embedded, { recursive: true });
  }

  const sourcePath = join(PATHS.backendBuild, config.backendName);
  
  if (!existsSync(sourcePath)) {
    console.error(`Native backend not found: ${sourcePath}`);
    console.error("Run './gradlew nativeCompile' first");
    return false;
  }

  const destPath = join(PATHS.embedded, config.backendName);
  cpSync(sourcePath, destPath);
  
  console.log("Backend binary prepared for embedding");
  return true;
}

function cleanEmbeddedDirectory(): void {
  if (existsSync(PATHS.embedded)) {
    rmSync(PATHS.embedded, { recursive: true });
  }
}

async function compileExecutable(target: Target, outputPath: string): Promise<void> {
  const config = TARGET_CONFIGS[target];
  
  await $`bun build src/main.ts --compile --target=${config.bunTarget} --asset-naming=[name].[ext] --outfile=${outputPath}`;
}

async function buildTarget(target: Target): Promise<void> {
  const config = TARGET_CONFIGS[target];
  const outputDir = join(PATHS.dist, target);
  const outputPath = join(outputDir, config.outputName);

  console.log(`\nBuilding for ${target}...`);
  console.log("-".repeat(40));

  // Prepare output directory with retry logic
  if (existsSync(outputDir)) {
    for (let i = 0; i < 3; i++) {
      try {
        rmSync(outputDir, { recursive: true });
        break;
      } catch (e: any) {
        if (e.code === "EBUSY" && i < 2) {
          console.log("Directory locked, retrying in 1s...");
          await Bun.sleep(1000);
        } else {
          throw e;
        }
      }
    }
  }
  mkdirSync(outputDir, { recursive: true });

  // Prepare embedded backend
  if (!prepareEmbeddedDirectory(config)) {
    throw new Error("Failed to prepare backend binary");
  }

  // Compile executable
  console.log("Compiling executable...");
  await compileExecutable(target, outputPath);

  // Patch Windows executable
  if (target === "windows") {
    console.log("Patching PE header for Windows...");
    patchWindowsExecutable(outputPath);
  }

  // Report results
  const stats = statSync(outputPath);
  console.log(`\nBuild complete: ${outputPath}`);
  console.log(`Size: ${formatSize(stats.size)}`);
}

// =============================================================================
// Main
// =============================================================================

async function main(): Promise<void> {
  const args = process.argv.slice(2);
  const targetArg = args[0] || "current";

  console.log("Desktop App Build Script");
  console.log("=".repeat(40));

  // Load and generate config from app.config.json
  const appConfig = loadAppConfig();
  generateConfigFile(appConfig);

  if (!existsSync(PATHS.dist)) {
    mkdirSync(PATHS.dist, { recursive: true });
  }

  try {
    let target: Target;

    if (targetArg === "current" || targetArg === "all") {
      target = getCurrentTarget();
    } else if (targetArg in TARGET_CONFIGS) {
      target = targetArg as Target;
    } else {
      console.error(`Unknown target: ${targetArg}`);
      console.log("Available: windows, linux, macos, macos-arm, current");
      process.exit(1);
    }

    await buildTarget(target);
    console.log("\nBuild successful");
  } catch (error) {
    console.error("Build failed:", error);
    process.exit(1);
  } finally {
    cleanEmbeddedDirectory();
  }
}

main();
