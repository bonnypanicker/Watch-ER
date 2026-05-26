package com.watch_er.data

data class QuietHours(
    val start: String = "23:00",
    val end: String = "07:00"
)

data class SourcePlan(
    val id: String,
    val type: String,
    val url: String? = null,
    val query: String? = null,
    val priority: Int
)

data class WatcherStrategy(
    val title: String,
    val sources: List<SourcePlan>,
    val verificationRules: List<String>,
    val fallbackStrategy: String,
    val frequency: String
)

data class Evidence(
    val title: String,
    val url: String?,
    val excerpt: String
)

data class SynthesisResult(
    val isTrue: Boolean,
    val confidence: Int,
    val reasoning: String,
    val evidence: List<Evidence>,
    val nextCheckAt: String
)

data class Watcher(
    val id: String,
    val intent: String,
    val strategy: WatcherStrategy,
    val mode: String,
    val status: String,
    val confidenceThreshold: Int,
    val frequency: String,
    val quietHours: QuietHours,
    val lastCheckedAt: String?,
    val nextCheckAt: String,
    val lastResult: SynthesisResult?
)
