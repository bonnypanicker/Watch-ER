import { getApps, initializeApp } from "firebase-admin/app";
import { getMessaging } from "firebase-admin/messaging";
import { newId } from "./ids.js";
import type { Check, NotificationRecord, Watcher } from "./models.js";
import type { Notifier } from "./notifications.js";

export class FcmNotifier implements Notifier {
  constructor() {
    if (getApps().length === 0) {
      initializeApp();
    }
  }

  async sendWatcherTrue(watcher: Watcher, check: Check): Promise<NotificationRecord> {
    const record: NotificationRecord = {
      id: newId("notification"),
      watcherId: watcher.id,
      checkId: check.id,
      userId: watcher.userId,
      channel: "push",
      title: `${watcher.strategy.title} is ready`,
      summary: check.synthesis.reasoning,
      evidenceLinks: check.synthesis.evidence
        .map((item) => item.url)
        .filter((url): url is string => Boolean(url)),
      sentAt: new Date().toISOString()
    };

    const topic = `user_${watcher.userId}`;
    await getMessaging().send({
      topic,
      notification: {
        title: record.title,
        body: record.summary
      },
      data: {
        watcherId: watcher.id,
        checkId: check.id,
        notificationId: record.id
      },
      android: {
        notification: {
          channelId: "watcher_alerts"
        }
      }
    });

    return record;
  }
}
