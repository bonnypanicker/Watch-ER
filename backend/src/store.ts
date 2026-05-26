import type { Check, NotificationRecord, Watcher } from "./models.js";

export interface WatchStore {
  createWatcher(watcher: Watcher): Promise<Watcher>;
  updateWatcher(watcher: Watcher): Promise<Watcher>;
  listWatchers(userId: string): Promise<Watcher[]>;
  getWatcher(watcherId: string): Promise<Watcher | undefined>;
  createCheck(check: Check): Promise<Check>;
  listChecks(watcherId: string): Promise<Check[]>;
  createNotification(notification: NotificationRecord): Promise<NotificationRecord>;
}

export class InMemoryWatchStore implements WatchStore {
  private watchers = new Map<string, Watcher>();
  private checks = new Map<string, Check[]>();
  private notifications = new Map<string, NotificationRecord[]>();

  async createWatcher(watcher: Watcher): Promise<Watcher> {
    this.watchers.set(watcher.id, watcher);
    return watcher;
  }

  async updateWatcher(watcher: Watcher): Promise<Watcher> {
    this.watchers.set(watcher.id, watcher);
    return watcher;
  }

  async listWatchers(userId: string): Promise<Watcher[]> {
    return [...this.watchers.values()]
      .filter((watcher) => watcher.userId === userId)
      .sort((a, b) => a.createdAt.localeCompare(b.createdAt));
  }

  async getWatcher(watcherId: string): Promise<Watcher | undefined> {
    return this.watchers.get(watcherId);
  }

  async createCheck(check: Check): Promise<Check> {
    const existing = this.checks.get(check.watcherId) ?? [];
    this.checks.set(check.watcherId, [...existing, check]);
    return check;
  }

  async listChecks(watcherId: string): Promise<Check[]> {
    return this.checks.get(watcherId) ?? [];
  }

  async createNotification(notification: NotificationRecord): Promise<NotificationRecord> {
    const existing = this.notifications.get(notification.watcherId) ?? [];
    this.notifications.set(notification.watcherId, [...existing, notification]);
    return notification;
  }
}
