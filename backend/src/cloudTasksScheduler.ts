import { CloudTasksClient } from "@google-cloud/tasks";
import type { AppConfig } from "./config.js";
import type { Scheduler } from "./scheduler.js";

export class CloudTasksScheduler implements Scheduler {
  private readonly client = new CloudTasksClient();

  constructor(private readonly config: AppConfig) {}

  async scheduleCheck(watcherId: string, runAtIso: string): Promise<void> {
    const project = this.config.firebaseProjectId;
    const location = process.env.CLOUD_TASKS_LOCATION ?? "us-central1";
    const queue = process.env.CLOUD_TASKS_QUEUE ?? "watcher-checks";
    const parent = this.client.queuePath(project, location, queue);
    const runAt = new Date(runAtIso);

    await this.client.createTask({
      parent,
      task: {
        scheduleTime: {
          seconds: Math.floor(runAt.getTime() / 1000)
        },
        httpRequest: {
          httpMethod: "POST",
          url: `${this.config.publicBaseUrl}/v1/tasks/check`,
          headers: {
            "Content-Type": "application/json"
          },
          body: Buffer.from(JSON.stringify({ watcherId })).toString("base64")
        }
      }
    });
  }
}
