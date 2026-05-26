export type AppConfig = {
  port: number;
  publicBaseUrl: string;
  geminiApiKey: string;
  strategistModel: string;
  synthesisModel: string;
  firebaseProjectId: string;
  androidPackageName: string;
  devInMemoryStore: boolean;
};

export function readConfig(env: NodeJS.ProcessEnv = process.env): AppConfig {
  return {
    port: Number(env.PORT ?? 8080),
    publicBaseUrl: env.PUBLIC_BASE_URL ?? "http://localhost:8080",
    geminiApiKey: env.GEMINI_API_KEY ?? "",
    strategistModel: env.GEMINI_STRATEGIST_MODEL ?? "gemini-2.5-flash",
    synthesisModel: env.GEMINI_SYNTHESIS_MODEL ?? "gemini-2.5-flash-lite",
    firebaseProjectId: env.FIREBASE_PROJECT_ID ?? "",
    androidPackageName: env.ANDROID_PACKAGE_NAME ?? "com.watch_er",
    devInMemoryStore: (env.DEV_IN_MEMORY_STORE ?? "true") === "true"
  };
}
