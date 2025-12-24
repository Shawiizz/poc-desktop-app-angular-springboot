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

// @ts-ignore - Embedded binary import
import embeddedBackend from "./embedded/desktop-backend.exe" with { type: "file" };

// =============================================================================
// Configuration
// =============================================================================

const Config = {
  app: {
    name: "Desktop App",
    id: "desktop-app",
    version: "1.0.0",
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
  private readonly versionPath: string;

  constructor() {
    const binDir = join(getAppDataDirectory(), "bin");
    this.binaryPath = join(binDir, getBackendExecutableName());
    this.versionPath = join(binDir, ".version");
  }

  async start(): Promise<boolean> {
    try {
      await this.extractIfNeeded();
      return await this.spawnProcess();
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

    const installedVersion = await this.getInstalledVersion();
    
    if (!existsSync(this.binaryPath) || installedVersion !== Config.app.version) {
      console.log("Extracting backend binary...");
      
      const binaryBlob = Bun.file(embeddedBackend);
      await Bun.write(this.binaryPath, binaryBlob);

      if (getPlatform() !== "windows") {
        chmodSync(this.binaryPath, 0o755);
      }

      await Bun.write(this.versionPath, Config.app.version);
      console.log("Backend extracted successfully");
    }
  }

  private async getInstalledVersion(): Promise<string> {
    if (!existsSync(this.versionPath)) {
      return "";
    }
    
    try {
      return (await Bun.file(this.versionPath).text()).trim();
    } catch {
      return "";
    }
  }

  private async spawnProcess(): Promise<boolean> {
    console.log("Starting backend server...");

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

    const isReady = await this.waitForHealthy();
    
    if (isReady) {
      console.log("Backend server is ready");
    } else {
      console.error("Backend server failed to start");
    }

    return isReady;
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
// Window Manager
// =============================================================================

class WindowManager {
  private readonly backendUrl: string;

  constructor() {
    this.backendUrl = `http://localhost:${Config.backend.port}`;
  }

  show(): void {
    const { width, height } = Config.window;

    const webview = new Webview(false, {
      width,
      height,
      hint: SizeHint.NONE,
    });

    webview.title = Config.app.name;
    webview.navigate(this.backendUrl);
    webview.run();
  }
}

// =============================================================================
// Application
// =============================================================================

class Application {
  private readonly backend: BackendManager;
  private readonly window: WindowManager;

  constructor() {
    this.backend = new BackendManager();
    this.window = new WindowManager();
  }

  async run(): Promise<void> {
    this.registerSignalHandlers();

    const started = await this.backend.start();
    
    if (!started) {
      process.exit(1);
    }

    try {
      this.window.show();
    } finally {
      this.backend.stop();
    }
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
