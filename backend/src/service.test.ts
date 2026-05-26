import { describe, expect, it } from "vitest";
import { readConfig } from "./config.js";
import { LoggingNotifier } from "./notifications.js";
import { LoggingScheduler } from "./scheduler.js";
import { WatcherService } from "./service.js";
import { InMemoryWatchStore } from "./store.js";

describe("WatcherService", () => {
  it("creates a watcher with defaults and schedules the first check", async () => {
    const config = readConfig({ DEV_IN_MEMORY_STORE: "true" });
    const store = new InMemoryWatchStore();
    const service = new WatcherService(
      config,
      store,
      new LoggingScheduler(config),
      new LoggingNotifier()
    );

    const watcher = await service.createWatcher("user_1", {
      intent: "Tell me when https://example.com mentions MobileCLIP2-S2-int8"
    });

    expect(watcher.mode).toBe("oneshot");
    expect(watcher.status).toBe("active");
    expect(watcher.confidenceThreshold).toBe(90);
    expect(watcher.strategy.sources[0].type).toBe("url");
  });
});
