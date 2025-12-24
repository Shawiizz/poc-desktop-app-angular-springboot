/**
 * Desktop Application Launcher
 * 
 * Single executable that embeds and manages a Spring Boot backend
 * with a native WebView frontend.
 */

import { Webview, SizeHint } from "webview-bun";
import { spawn, type ChildProcess } from "child_process";
import { existsSync, mkdirSync, chmodSync } from "fs";
import { join } from "path";
import { platform, homedir } from "os";
import { AppConfig } from "./config.generated";

// @ts-ignore - Embedded binary import
import embeddedBackend from "./embedded/desktop-backend.exe" with { type: "file" };

// =============================================================================
// Configuration
// =============================================================================

const Config = {
  app: {
    name: AppConfig.name,
    id: AppConfig.id,
    version: AppConfig.version,
  },
  frontend: {
    // Remote URL for the frontend. If set, the WebView will point to this URL
    // instead of the local backend. The backend still starts for API calls.
    // Example: "https://app.example.com"
    remoteUrl: "",
  },
  backend: {
    port: 8080,
    healthEndpoint: "/actuator/health",
    startupTimeout: 30000,
    healthCheckInterval: 500,
  },
  window: {
    width: 1200,
    height: 800,
  },
} as const;

// =============================================================================
// Platform Utilities
// =============================================================================

type Platform = "windows" | "linux" | "darwin";

function getPlatform(): Platform {
  const p = platform();
  if (p === "win32") return "windows";
  if (p === "linux") return "linux";
  return "darwin";
}

function getAppDataDirectory(): string {
  const p = getPlatform();
  const appId = Config.app.id;

  switch (p) {
    case "windows":
      return join(process.env.APPDATA || join(homedir(), "AppData", "Roaming"), appId);
    case "darwin":
      return join(homedir(), "Library", "Application Support", appId);
    default:
      return join(homedir(), ".local", "share", appId);
  }
}

function getBackendExecutableName(): string {
  return getPlatform() === "windows" ? "desktop-backend.exe" : "desktop-backend";
}

// =============================================================================
// Backend Manager
// =============================================================================

class BackendManager {
  private process: ChildProcess | null = null;
  private readonly binaryPath: string;
  private readonly hashPath: string;

  constructor() {
    const binDir = join(getAppDataDirectory(), "bin");
    this.binaryPath = join(binDir, getBackendExecutableName());
    this.hashPath = join(binDir, ".hash");
  }

  async start(): Promise<boolean> {
    try {
      await this.extractIfNeeded();
      this.spawnProcess();
      return await this.waitForHealthy();
    } catch (error) {
      console.error("Failed to start backend:", error);
      return false;
    }
  }

  stop(): void {
    if (this.process) {
      this.process.kill();
      this.process = null;
    }
  }

  private async extractIfNeeded(): Promise<void> {
    const binDir = join(getAppDataDirectory(), "bin");
    
    if (!existsSync(binDir)) {
      mkdirSync(binDir, { recursive: true });
    }

    // Calculate hash of embedded binary
    const binaryBlob = Bun.file(embeddedBackend);
    const binaryBuffer = await binaryBlob.arrayBuffer();
    const hasher = new Bun.CryptoHasher("md5");
    hasher.update(new Uint8Array(binaryBuffer));
    const currentHash = hasher.digest("hex");

    // Check if extraction is needed
    const installedHash = await this.getInstalledHash();
    
    if (!existsSync(this.binaryPath) || installedHash !== currentHash) {
      await Bun.write(this.binaryPath, binaryBuffer);

      if (getPlatform() !== "windows") {
        chmodSync(this.binaryPath, 0o755);
      }

      await Bun.write(this.hashPath, currentHash);
    }
  }

  private async getInstalledHash(): Promise<string> {
    if (!existsSync(this.hashPath)) {
      return "";
    }
    
    try {
      return (await Bun.file(this.hashPath).text()).trim();
    } catch {
      return "";
    }
  }

  private spawnProcess(): void {
    this.process = spawn(this.binaryPath, [], {
      env: {
        ...process.env,
        SERVER_PORT: String(Config.backend.port),
        SPRING_PROFILES_ACTIVE: "prod",
      },
      stdio: "ignore",
      detached: true,
      windowsHide: true,
    });

    this.process.unref();
  }

  private async waitForHealthy(): Promise<boolean> {
    const { startupTimeout, healthCheckInterval, port, healthEndpoint } = Config.backend;
    const maxAttempts = Math.ceil(startupTimeout / healthCheckInterval);
    const healthUrl = `http://localhost:${port}${healthEndpoint}`;

    for (let attempt = 1; attempt <= maxAttempts; attempt++) {
      if (this.process?.killed) {
        return false;
      }

      try {
        const response = await fetch(healthUrl, {
          signal: AbortSignal.timeout(1000),
        });
        
        if (response.ok) {
          return true;
        }
      } catch {
        // Server not ready yet
      }

      await Bun.sleep(healthCheckInterval);
    }

    return false;
  }
}

// =============================================================================
// Application
// =============================================================================

class Application {
  private readonly backend: BackendManager;

  constructor() {
    this.backend = new BackendManager();
  }

  async run(): Promise<void> {
    this.registerSignalHandlers();

    // Start backend and wait for it to be healthy
    const isReady = await this.backend.start();

    if (!isReady) {
      console.error("Backend failed to start");
      process.exit(1);
    }

    // Determine frontend URL (remote or local)
    const frontendUrl = Config.frontend.remoteUrl || `http://localhost:${Config.backend.port}`;

    // Open webview
    const webview = new Webview(false, {
      width: Config.window.width,
      height: Config.window.height,
      hint: SizeHint.NONE,
    });

    webview.title = Config.app.name;
    webview.navigate(frontendUrl);
    webview.run();

    // Cleanup when window closes
    this.backend.stop();
  }

  private registerSignalHandlers(): void {
    const shutdown = () => {
      this.backend.stop();
      process.exit(0);
    };

    process.on("SIGINT", shutdown);
    process.on("SIGTERM", shutdown);
  }
}

// =============================================================================
// Entry Point
// =============================================================================

const app = new Application();

app.run().catch((error) => {
  console.error("Application error:", error);
  process.exit(1);
});
