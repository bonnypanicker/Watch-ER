import type { AppConfig } from "./config.js";
import { fetchSources } from "./fetcher.js";
import { GeminiClient } from "./gemini.js";
import { newId } from "./ids.js";
import type { Check, QuietHours, Watcher, WatcherMode, WatcherStrategy } from "./models.js";
import { type Notifier, shouldNotify } from "./notifications.js";
import { nextCheckFromFrequency, type Scheduler } from "./scheduler.js";
import type { WatchStore } from "./store.js";

export type CreateWatcherInput = {
  intent: string;
  mode?: WatcherMode;
  confidenceThreshold?: number;
  quietHours?: QuietHours;
  strategy?: WatcherStrategy;
};

export class WatcherService {
  private readonly gemini: GeminiClient;

  constructor(
    private readonly config: AppConfig,
    private readonly store: WatchStore,
    private readonly scheduler: Scheduler,
    private readonly notifier: Notifier
  ) {
    this.gemini = new GeminiClient(config);
  }

  async createWatcher(userId: string, input: CreateWatcherInput): Promise<Watcher> {
    const intent = input.intent.trim();
    this.validateIntent(intent);

    const strategy = input.strategy ?? await this.gemini.createStrategy(intent);
    const now = new Date();
    const watcher: Watcher = {
      id: newId("watcher"),
      userId,
      intent,
      strategy,
      mode: input.mode ?? "oneshot",
      status: "active",
      confidenceThreshold: input.confidenceThreshold ?? 90,
      frequency: strategy.frequency,
      quietHours: input.quietHours ?? { start: "23:00", end: "07:00" },
      nextCheckAt: nextCheckFromFrequency(strategy.frequency, now),
      createdAt: now.toISOString()
    };

    await this.store.createWatcher(watcher);
    await this.scheduler.scheduleCheck(watcher.id, watcher.nextCheckAt);
    return watcher;
  }

  async previewStrategy(intentInput: string): Promise<WatcherStrategy> {
    const intent = intentInput.trim();
    this.validateIntent(intent);
    return this.gemini.createStrategy(intent);
  }

  async listWatchers(userId: string): Promise<Watcher[]> {
    return this.store.listWatchers(userId);
  }

  private validateIntent(intent: string): void {
    if (intent.length < 8) {
      throw new Error("Intent must be at least 8 characters.");
    }
    if (intent.length > 500) {
      throw new Error("Intent must be 500 characters or less.");
    }
  }

  async getWatcher(userId: string, watcherId: string): Promise<Watcher> {
    const watcher = await this.store.getWatcher(watcherId);
    if (!watcher || watcher.userId !== userId) {
      throw new Error("Watcher not found.");
    }
    return watcher;
  }

  async setPaused(userId: string, watcherId: string, paused: boolean): Promise<Watcher> {
    const watcher = await this.getWatcher(userId, watcherId);
    const updated = { ...watcher, status: paused ? "paused" as const : "active" as const };
    await this.store.updateWatcher(updated);
    if (!paused) {
      await this.scheduler.scheduleCheck(updated.id, updated.nextCheckAt);
    }
    return updated;
  }

  async runCheck(watcherId: string): Promise<Check> {
    const watcher = await this.store.getWatcher(watcherId);
    if (!watcher) {
      throw new Error("Watcher not found.");
    }
    if (watcher.status !== "active") {
      throw new Error(`Watcher is ${watcher.status}.`);
    }

    const startedAt = new Date().toISOString();
    await this.store.updateWatcher({ ...watcher, status: "checking" });

    const fetchResults = await fetchSources(watcher.strategy.sources);
    const synthesis = await this.gemini.synthesize({
      intent: watcher.intent,
      strategy: watcher.strategy,
      fetchResults,
      previousResult: watcher.lastResult
    });

    const check: Check = {
      id: newId("check"),
      watcherId: watcher.id,
      userId: watcher.userId,
      startedAt,
      finishedAt: new Date().toISOString(),
      strategyUsed: watcher.strategy,
      fetchResults,
      synthesis,
      notificationSent: false,
      costEstimate: estimateCost(fetchResults.length)
    };

    const savedCheck = await this.store.createCheck(check);
    let updatedWatcher: Watcher = {
      ...watcher,
      status: "active",
      lastCheckedAt: savedCheck.finishedAt,
      lastResult: synthesis,
      nextCheckAt: synthesis.nextCheckAt || nextCheckFromFrequency(watcher.frequency)
    };

    if (shouldNotify(watcher, savedCheck)) {
      const notification = await this.notifier.sendWatcherTrue(watcher, savedCheck);
      await this.store.createNotification(notification);
      savedCheck.notificationSent = true;
      if (watcher.mode === "oneshot") {
        updatedWatcher = {
          ...updatedWatcher,
          status: "completed",
          completedAt: new Date().toISOString()
        };
      }
    }

    await this.store.updateWatcher(updatedWatcher);
    if (updatedWatcher.status === "active") {
      await this.scheduler.scheduleCheck(updatedWatcher.id, updatedWatcher.nextCheckAt);
    }

    return savedCheck;
  }
}

function estimateCost(fetchCount: number): number {
  return Number((0.0002 + fetchCount * 0.0001).toFixed(6));
}
