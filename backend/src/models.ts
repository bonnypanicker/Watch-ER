export type WatcherMode = "oneshot" | "persistent";
export type WatcherStatus = "active" | "checking" | "paused" | "completed" | "error";
export type SourceType = "url" | "api" | "rss" | "search";

export type QuietHours = {
  start: string;
  end: string;
};

export type SourcePlan = {
  id: string;
  type: SourceType;
  url?: string;
  query?: string;
  method?: "GET" | "POST";
  priority: number;
};

export type WatcherStrategy = {
  title: string;
  sources: SourcePlan[];
  verificationRules: string[];
  fallbackStrategy: string;
  frequency: string;
};

export type SynthesisResult = {
  isTrue: boolean;
  confidence: number;
  reasoning: string;
  evidence: Array<{
    title: string;
    url?: string;
    excerpt: string;
  }>;
  nextCheckAt: string;
};

export type Watcher = {
  id: string;
  userId: string;
  intent: string;
  strategy: WatcherStrategy;
  mode: WatcherMode;
  status: WatcherStatus;
  confidenceThreshold: number;
  frequency: string;
  quietHours: QuietHours;
  lastCheckedAt?: string;
  nextCheckAt: string;
  lastResult?: SynthesisResult;
  createdAt: string;
  completedAt?: string;
};

export type FetchResult = {
  sourceId: string;
  ok: boolean;
  status?: number;
  url?: string;
  bodyPreview: string;
  error?: string;
};

export type Check = {
  id: string;
  watcherId: string;
  userId: string;
  startedAt: string;
  finishedAt: string;
  strategyUsed: WatcherStrategy;
  fetchResults: FetchResult[];
  synthesis: SynthesisResult;
  notificationSent: boolean;
  costEstimate: number;
};

export type NotificationRecord = {
  id: string;
  watcherId: string;
  checkId: string;
  userId: string;
  channel: "push";
  title: string;
  summary: string;
  evidenceLinks: string[];
  sentAt: string;
};
