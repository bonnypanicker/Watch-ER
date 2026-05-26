import type { FetchResult, SourcePlan } from "./models.js";

export async function fetchSources(sources: SourcePlan[]): Promise<FetchResult[]> {
  const ordered = [...sources].sort((a, b) => a.priority - b.priority).slice(0, 3);
  return Promise.all(ordered.map(fetchSource));
}

async function fetchSource(source: SourcePlan): Promise<FetchResult> {
  if (source.type === "search") {
    return {
      sourceId: source.id,
      ok: true,
      bodyPreview: `Search source queued for Gemini grounding: ${source.query ?? ""}`
    };
  }

  if (!source.url) {
    return {
      sourceId: source.id,
      ok: false,
      bodyPreview: "",
      error: "Source has no URL"
    };
  }

  try {
    const response = await fetch(source.url, {
      method: source.method ?? "GET",
      headers: {
        "User-Agent": "Watch-ER/0.1 (+https://watch-er.local)"
      }
    });
    const text = await response.text();
    return {
      sourceId: source.id,
      ok: response.ok,
      status: response.status,
      url: source.url,
      bodyPreview: text.replace(/\s+/g, " ").slice(0, 4000)
    };
  } catch (error) {
    return {
      sourceId: source.id,
      ok: false,
      url: source.url,
      bodyPreview: "",
      error: error instanceof Error ? error.message : "Unknown fetch error"
    };
  }
}
