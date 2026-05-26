import type { AppConfig } from "./config.js";

export interface Scheduler {
  scheduleCheck(watcherId: string, runAtIso: string): Promise<void>;
}

export class LoggingScheduler implements Scheduler {
  constructor(private readonly config: AppConfig) {}

  async scheduleCheck(watcherId: string, runAtIso: string): Promise<void> {
    console.log("schedule_check", {
      watcherId,
      runAtIso,
      callback: `${this.config.publicBaseUrl}/v1/tasks/check`
    });
  }
}

export function nextCheckFromFrequency(frequency: string, now = new Date()): string {
  const match = frequency.match(/^(\d+)(m|h|d)$/);
  if (!match) {
    return new Date(now.getTime() + 30 * 60 * 1000).toISOString();
  }
  const amount = Number(match[1]);
  const unit = match[2];
  const multiplier = unit === "m" ? 60_000 : unit === "h" ? 3_600_000 : 86_400_000;
  return new Date(now.getTime() + amount * multiplier).toISOString();
}
