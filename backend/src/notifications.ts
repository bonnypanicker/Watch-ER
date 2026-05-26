import type { Check, NotificationRecord, Watcher } from "./models.js";
import { newId } from "./ids.js";

export interface Notifier {
  sendWatcherTrue(watcher: Watcher, check: Check): Promise<NotificationRecord>;
}

export class LoggingNotifier implements Notifier {
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
    console.log("send_push", record);
    return record;
  }
}

export function shouldNotify(watcher: Watcher, check: Check): boolean {
  const lastWasTrue = watcher.lastResult?.isTrue === true;
  return check.synthesis.isTrue
    && check.synthesis.confidence >= watcher.confidenceThreshold
    && !lastWasTrue
    && watcher.status !== "completed";
}
