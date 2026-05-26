import type { AppConfig } from "./config.js";
import type { FetchResult, SynthesisResult, WatcherStrategy } from "./models.js";

const strategySchema = {
  type: "object",
  properties: {
    title: { type: "string" },
    frequency: { type: "string" },
    fallbackStrategy: { type: "string" },
    sources: {
      type: "array",
      items: {
        type: "object",
        properties: {
          id: { type: "string" },
          type: { type: "string", enum: ["url", "api", "rss", "search"] },
          url: { type: ["string", "null"] },
          query: { type: ["string", "null"] },
          method: { type: ["string", "null"], enum: ["GET", "POST", null] },
          priority: { type: "integer" }
        },
        required: ["id", "type", "priority"]
      }
    },
    verificationRules: {
      type: "array",
      items: { type: "string" }
    }
  },
  required: ["title", "frequency", "fallbackStrategy", "sources", "verificationRules"]
};

const synthesisSchema = {
  type: "object",
  properties: {
    isTrue: { type: "boolean" },
    confidence: { type: "integer", minimum: 0, maximum: 100 },
    reasoning: { type: "string" },
    evidence: {
      type: "array",
      items: {
        type: "object",
        properties: {
          title: { type: "string" },
          url: { type: ["string", "null"] },
          excerpt: { type: "string" }
        },
        required: ["title", "excerpt"]
      }
    },
    nextCheckAt: { type: "string", format: "date-time" }
  },
  required: ["isTrue", "confidence", "reasoning", "evidence", "nextCheckAt"]
};

export class GeminiClient {
  constructor(private readonly config: AppConfig) {}

  async createStrategy(intent: string): Promise<WatcherStrategy> {
    if (!this.config.geminiApiKey) {
      return this.fallbackStrategy(intent);
    }

    const prompt = [
      "You are Watch-ER's strategist agent.",
      "Create a public web/API/search monitoring strategy for the user's intent.",
      "Prefer deterministic public APIs and direct URLs. Avoid authenticated sources.",
      "Use a practical frequency such as 15m, 30m, 1h, or 1d.",
      `User intent: ${intent}`
    ].join("\n");

    const json = await this.generateJson(this.config.strategistModel, prompt, strategySchema);
    return this.normalizeStrategy(json as WatcherStrategy, intent);
  }

  async synthesize(args: {
    intent: string;
    strategy: WatcherStrategy;
    fetchResults: FetchResult[];
    previousResult?: SynthesisResult;
  }): Promise<SynthesisResult> {
    if (!this.config.geminiApiKey) {
      return this.fallbackSynthesis(args.fetchResults);
    }

    const prompt = [
      "You are Watch-ER's synthesis agent.",
      "Decide if the user's watched condition is now true.",
      "Be conservative. Only set isTrue when evidence satisfies the verification rules.",
      "Return concise reasoning and cite evidence from fetch results.",
      `Intent: ${args.intent}`,
      `Strategy: ${JSON.stringify(args.strategy)}`,
      `Previous result: ${JSON.stringify(args.previousResult ?? null)}`,
      `Fetch results: ${JSON.stringify(args.fetchResults)}`
    ].join("\n");

    const usesSearch = args.strategy.sources.some((source) => source.type === "search");
    const json = await this.generateJson(this.config.synthesisModel, prompt, synthesisSchema, usesSearch);
    return this.normalizeSynthesis(json as SynthesisResult);
  }

  private async generateJson(
    model: string,
    prompt: string,
    schema: object,
    useGoogleSearch = false
  ): Promise<unknown> {
    const response = await fetch(
      `https://generativelanguage.googleapis.com/v1beta/models/${model}:generateContent`,
      {
        method: "POST",
        headers: {
          "Content-Type": "application/json",
          "x-goog-api-key": this.config.geminiApiKey
        },
        body: JSON.stringify({
          contents: [{ parts: [{ text: prompt }] }],
          tools: useGoogleSearch ? [{ googleSearch: {} }] : undefined,
          generationConfig: {
            responseMimeType: "application/json",
            responseJsonSchema: schema
          }
        })
      }
    );

    if (!response.ok) {
      throw new Error(`Gemini request failed: ${response.status}`);
    }

    const body = await response.json() as {
      candidates?: Array<{ content?: { parts?: Array<{ text?: string }> } }>;
    };
    const text = body.candidates?.[0]?.content?.parts?.[0]?.text;
    if (!text) {
      throw new Error("Gemini returned no JSON text");
    }
    return JSON.parse(text);
  }

  private fallbackStrategy(intent: string): WatcherStrategy {
    const urlMatch = intent.match(/https?:\/\/\S+/i)?.[0];
    return {
      title: intent.slice(0, 64),
      frequency: "30m",
      fallbackStrategy: "If direct fetch fails, try a grounded search query using the user's original intent.",
      sources: [
        urlMatch
          ? { id: "direct_url", type: "url", url: urlMatch, method: "GET", priority: 1 }
          : { id: "web_search", type: "search", query: intent, priority: 1 }
      ],
      verificationRules: [
        "The fetched evidence must directly support the user's requested condition.",
        "Do not notify on vague or partial matches."
      ]
    };
  }

  private fallbackSynthesis(fetchResults: FetchResult[]): SynthesisResult {
    const okResults = fetchResults.filter((result) => result.ok);
    return {
      isTrue: false,
      confidence: okResults.length > 0 ? 20 : 0,
      reasoning: "Gemini is not configured, so Watch-ER fetched sources but did not make a truth decision.",
      evidence: okResults.slice(0, 3).map((result) => ({
        title: result.sourceId,
        url: result.url,
        excerpt: result.bodyPreview.slice(0, 240)
      })),
      nextCheckAt: new Date(Date.now() + 30 * 60 * 1000).toISOString()
    };
  }

  private normalizeStrategy(strategy: WatcherStrategy, intent: string): WatcherStrategy {
    return {
      title: strategy.title || intent.slice(0, 64),
      frequency: strategy.frequency || "30m",
      fallbackStrategy: strategy.fallbackStrategy || "Retry the primary source, then use search.",
      sources: strategy.sources?.length ? strategy.sources : this.fallbackStrategy(intent).sources,
      verificationRules: strategy.verificationRules?.length
        ? strategy.verificationRules
        : ["Evidence must directly satisfy the intent."]
    };
  }

  private normalizeSynthesis(result: SynthesisResult): SynthesisResult {
    return {
      isTrue: Boolean(result.isTrue),
      confidence: Math.max(0, Math.min(100, Number(result.confidence ?? 0))),
      reasoning: result.reasoning || "No reasoning returned.",
      evidence: result.evidence ?? [],
      nextCheckAt: result.nextCheckAt || new Date(Date.now() + 30 * 60 * 1000).toISOString()
    };
  }
}
