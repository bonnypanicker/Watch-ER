import { Firestore } from "@google-cloud/firestore";
import type { Check, NotificationRecord, Watcher } from "./models.js";
import type { WatchStore } from "./store.js";

export class FirestoreWatchStore implements WatchStore {
  private readonly db = new Firestore();

  async createWatcher(watcher: Watcher): Promise<Watcher> {
    await this.db.collection("watchers").doc(watcher.id).set(watcher);
    return watcher;
  }

  async updateWatcher(watcher: Watcher): Promise<Watcher> {
    await this.db.collection("watchers").doc(watcher.id).set(watcher, { merge: true });
    return watcher;
  }

  async listWatchers(userId: string): Promise<Watcher[]> {
    const snapshot = await this.db
      .collection("watchers")
      .where("userId", "==", userId)
      .get();
    return snapshot.docs
      .map((doc) => doc.data() as Watcher)
      .sort((a, b) => b.createdAt.localeCompare(a.createdAt));
  }

  async getWatcher(watcherId: string): Promise<Watcher | undefined> {
    const doc = await this.db.collection("watchers").doc(watcherId).get();
    return doc.exists ? doc.data() as Watcher : undefined;
  }

  async createCheck(check: Check): Promise<Check> {
    await this.db.collection("checks").doc(check.id).set(check);
    return check;
  }

  async listChecks(watcherId: string): Promise<Check[]> {
    const snapshot = await this.db
      .collection("checks")
      .where("watcherId", "==", watcherId)
      .get();
    return snapshot.docs
      .map((doc) => doc.data() as Check)
      .sort((a, b) => b.startedAt.localeCompare(a.startedAt))
      .slice(0, 25);
  }

  async createNotification(notification: NotificationRecord): Promise<NotificationRecord> {
    await this.db.collection("notifications").doc(notification.id).set(notification);
    return notification;
  }
}
