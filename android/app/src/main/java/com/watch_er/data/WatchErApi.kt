package com.watch_er.data

import com.watch_er.core.WatchErConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

class WatchErApi(
    private val baseUrl: String = WatchErConfig.BackendBaseUrl
) {
    suspend fun listWatchers(): List<Watcher> = withContext(Dispatchers.IO) {
        val body = request("GET", "/v1/watchers")
        body.getJSONArray("watchers").toWatcherList()
    }

    suspend fun previewStrategy(intent: String): WatcherStrategy = withContext(Dispatchers.IO) {
        val payload = JSONObject().put("intent", intent)
        request("POST", "/v1/strategies/preview", payload).getJSONObject("strategy").toStrategy()
    }

    suspend fun createWatcher(
        intent: String,
        mode: String,
        threshold: Int,
        strategy: WatcherStrategy?
    ): Watcher = withContext(Dispatchers.IO) {
        val payload = JSONObject()
            .put("intent", intent)
            .put("mode", mode)
            .put("confidenceThreshold", threshold)
            .put("quietHours", JSONObject().put("start", "23:00").put("end", "07:00"))
        if (strategy != null) {
            payload.put("strategy", strategy.toJson())
        }
        request("POST", "/v1/watchers", payload).getJSONObject("watcher").toWatcher()
    }

    suspend fun pauseWatcher(id: String): Watcher = withContext(Dispatchers.IO) {
        request("POST", "/v1/watchers/$id/pause").getJSONObject("watcher").toWatcher()
    }

    suspend fun resumeWatcher(id: String): Watcher = withContext(Dispatchers.IO) {
        request("POST", "/v1/watchers/$id/resume").getJSONObject("watcher").toWatcher()
    }

    private fun request(method: String, path: String, payload: JSONObject? = null): JSONObject {
        val connection = (URL("$baseUrl$path").openConnection() as HttpURLConnection).apply {
            requestMethod = method
            connectTimeout = 15_000
            readTimeout = 20_000
            setRequestProperty("Content-Type", "application/json")
            setRequestProperty("x-dev-user-id", "android_dev")
            if (payload != null) {
                doOutput = true
                OutputStreamWriter(outputStream).use { it.write(payload.toString()) }
            }
        }

        val stream = if (connection.responseCode in 200..299) {
            connection.inputStream
        } else {
            connection.errorStream
        }
        val text = stream.bufferedReader().use { it.readText() }
        if (connection.responseCode !in 200..299) {
            throw IllegalStateException(JSONObject(text).optString("error", text))
        }
        return JSONObject(text)
    }
}

private fun JSONArray.toWatcherList(): List<Watcher> {
    return (0 until length()).map { index -> getJSONObject(index).toWatcher() }
}

private fun WatcherStrategy.toJson(): JSONObject {
    return JSONObject()
        .put("title", title)
        .put("frequency", frequency)
        .put("fallbackStrategy", fallbackStrategy)
        .put("sources", JSONArray().also { array -> sources.forEach { array.put(it.toJson()) } })
        .put("verificationRules", JSONArray().also { array -> verificationRules.forEach { array.put(it) } })
}

private fun SourcePlan.toJson(): JSONObject {
    return JSONObject()
        .put("id", id)
        .put("type", type)
        .put("url", url)
        .put("query", query)
        .put("priority", priority)
}

private fun JSONObject.toWatcher(): Watcher {
    val strategyJson = getJSONObject("strategy")
    val quietJson = optJSONObject("quietHours") ?: JSONObject()
    val lastResultJson = optJSONObject("lastResult")
    return Watcher(
        id = getString("id"),
        intent = getString("intent"),
        strategy = strategyJson.toStrategy(),
        mode = getString("mode"),
        status = getString("status"),
        confidenceThreshold = getInt("confidenceThreshold"),
        frequency = getString("frequency"),
        quietHours = QuietHours(
            start = quietJson.optString("start", "23:00"),
            end = quietJson.optString("end", "07:00")
        ),
        lastCheckedAt = optStringOrNull("lastCheckedAt"),
        nextCheckAt = getString("nextCheckAt"),
        lastResult = lastResultJson?.toSynthesis()
    )
}

private fun JSONObject.toStrategy(): WatcherStrategy {
    return WatcherStrategy(
        title = getString("title"),
        sources = getJSONArray("sources").let { sources ->
            (0 until sources.length()).map { sources.getJSONObject(it).toSourcePlan() }
        },
        verificationRules = getJSONArray("verificationRules").toStringList(),
        fallbackStrategy = optString("fallbackStrategy"),
        frequency = getString("frequency")
    )
}

private fun JSONObject.toSourcePlan(): SourcePlan {
    return SourcePlan(
        id = getString("id"),
        type = getString("type"),
        url = optStringOrNull("url"),
        query = optStringOrNull("query"),
        priority = optInt("priority", 1)
    )
}

private fun JSONObject.toSynthesis(): SynthesisResult {
    val evidence = optJSONArray("evidence") ?: JSONArray()
    return SynthesisResult(
        isTrue = optBoolean("isTrue"),
        confidence = optInt("confidence"),
        reasoning = optString("reasoning"),
        evidence = (0 until evidence.length()).map { evidence.getJSONObject(it).toEvidence() },
        nextCheckAt = optString("nextCheckAt")
    )
}

private fun JSONObject.toEvidence(): Evidence {
    return Evidence(
        title = optString("title"),
        url = optStringOrNull("url"),
        excerpt = optString("excerpt")
    )
}

private fun JSONArray.toStringList(): List<String> {
    return (0 until length()).map { getString(it) }
}

private fun JSONObject.optStringOrNull(key: String): String? {
    return if (has(key) && !isNull(key)) optString(key) else null
}
