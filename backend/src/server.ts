import express from "express";
import { CloudTasksScheduler } from "./cloudTasksScheduler.js";
import { readConfig } from "./config.js";
import { FcmNotifier } from "./fcmNotifier.js";
import { FirestoreWatchStore } from "./firestoreStore.js";
import { getAuth } from "firebase-admin/auth";
import { LoggingNotifier } from "./notifications.js";
import { LoggingScheduler } from "./scheduler.js";
import { WatcherService } from "./service.js";
import { InMemoryWatchStore } from "./store.js";

const config = readConfig();
const store = config.devInMemoryStore ? new InMemoryWatchStore() : new FirestoreWatchStore();
const scheduler = config.devInMemoryStore ? new LoggingScheduler(config) : new CloudTasksScheduler(config);
const notifier = config.devInMemoryStore ? new LoggingNotifier() : new FcmNotifier();
const service = new WatcherService(config, store, scheduler, notifier);

const app = express();
app.use(express.json({ limit: "1mb" }));

app.get("/healthz", (_req, res) => {
  res.json({ ok: true });
});

app.post("/v1/strategies/preview", async (req, res) => {
  try {
    await userIdFromRequest(req);
    const strategy = await service.previewStrategy(String(req.body.intent ?? ""));
    res.json({ strategy });
  } catch (error) {
    sendError(res, error);
  }
});

app.post("/v1/watchers", async (req, res) => {
  try {
    const watcher = await service.createWatcher(await userIdFromRequest(req), req.body);
    res.status(201).json({ watcher });
  } catch (error) {
    sendError(res, error);
  }
});

app.get("/v1/watchers", async (req, res) => {
  try {
    const watchers = await service.listWatchers(await userIdFromRequest(req));
    res.json({ watchers });
  } catch (error) {
    sendError(res, error);
  }
});

app.get("/v1/watchers/:id", async (req, res) => {
  try {
    const watcher = await service.getWatcher(await userIdFromRequest(req), req.params.id);
    const checks = await store.listChecks(watcher.id);
    res.json({ watcher, checks });
  } catch (error) {
    sendError(res, error);
  }
});

app.post("/v1/watchers/:id/pause", async (req, res) => {
  try {
    const watcher = await service.setPaused(await userIdFromRequest(req), req.params.id, true);
    res.json({ watcher });
  } catch (error) {
    sendError(res, error);
  }
});

app.post("/v1/watchers/:id/resume", async (req, res) => {
  try {
    const watcher = await service.setPaused(await userIdFromRequest(req), req.params.id, false);
    res.json({ watcher });
  } catch (error) {
    sendError(res, error);
  }
});

app.post("/v1/tasks/check", async (req, res) => {
  try {
    const check = await service.runCheck(String(req.body.watcherId ?? ""));
    res.json({ check });
  } catch (error) {
    sendError(res, error);
  }
});

app.listen(config.port, () => {
  console.log(`Watch-ER backend listening on :${config.port}`);
});

async function userIdFromRequest(req: express.Request): Promise<string> {
  if (config.devInMemoryStore) {
    return String(req.header("x-dev-user-id") ?? "dev_user");
  }

  const header = req.header("authorization") ?? "";
  const match = header.match(/^Bearer\s+(.+)$/i);
  if (!match) {
    throw new Error("Missing Firebase ID token.");
  }

  const decoded = await getAuth().verifyIdToken(match[1]);
  return decoded.uid;
}

function sendError(res: express.Response, error: unknown): void {
  const message = error instanceof Error ? error.message : "Unknown error";
  const status = message.includes("ID token") ? 401 : message.includes("not found") ? 404 : 400;
  res.status(status).json({ error: message });
}
