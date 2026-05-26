package com.watch_er.data

class WatcherRepository(
    private val api: WatchErApi = WatchErApi()
) {
    suspend fun loadWatchers(): List<Watcher> = api.listWatchers()

    suspend fun previewStrategy(intent: String): WatcherStrategy = api.previewStrategy(intent)

    suspend fun createWatcher(
        intent: String,
        mode: String,
        threshold: Int,
        strategy: WatcherStrategy?
    ): Watcher {
        return api.createWatcher(intent, mode, threshold, strategy)
    }

    suspend fun pauseWatcher(id: String): Watcher = api.pauseWatcher(id)

    suspend fun resumeWatcher(id: String): Watcher = api.resumeWatcher(id)
}
