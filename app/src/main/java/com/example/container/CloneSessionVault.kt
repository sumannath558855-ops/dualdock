package com.example.container

import android.content.Context
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.webkit.CookieManager
import android.webkit.WebStorage
import android.webkit.WebView
import org.json.JSONObject
import java.io.File
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors

/**
 * Provides persistent per-clone cookie & localStorage snapshots for unlimited clones
 * sharing the dynamic `:slot_unlimited` process beyond the 8 pinned OS process slots.
 * All disk I/O runs asynchronously on a dedicated background thread with atomic file writes
 * so WebView frames never stutter and session files never corrupt.
 */
class CloneSessionVault(context: Context) {

    private val vaultDir = File(context.filesDir, "dualdock_session_vault").apply {
        runCatching { if (!exists()) mkdirs() }
    }
    private val prefs = context.getSharedPreferences("dualdock_unlimited_pool", Context.MODE_PRIVATE)
    private val ioExecutor = Executors.newSingleThreadExecutor { runnable ->
        Thread(runnable, "DualDockSessionVaultIO").apply { isDaemon = true }
    }
    private val mainHandler = Handler(Looper.getMainLooper())
    private val memoryCache = ConcurrentHashMap<Long, JSONObject>()

    fun getActiveDynamicCloneId(): Long = prefs.getLong(KEY_ACTIVE_CLONE_ID, -1L)

    fun setActiveDynamicCloneId(cloneId: Long, hostUrl: String) {
        prefs.edit()
            .putLong(KEY_ACTIVE_CLONE_ID, cloneId)
            .putString(KEY_ACTIVE_CLONE_URL, hostUrl)
            .apply()
    }

    fun getActiveDynamicCloneUrl(): String = prefs.getString(KEY_ACTIVE_CLONE_URL, "").orEmpty()

    fun saveCookiesForClone(cloneId: Long, targetUrl: String) {
        if (cloneId <= 0L) return
        val hosts = extractCandidateOrigins(targetUrl)
        if (hosts.isEmpty()) return

        val captured = LinkedHashMap<String, String>(hosts.size)
        runCatching {
            val cookieManager = CookieManager.getInstance()
            hosts.forEach { origin ->
                val rawCookie = cookieManager.getCookie(origin)
                if (!rawCookie.isNullOrBlank()) {
                    captured[origin] = rawCookie
                }
            }
        }
        if (captured.isEmpty()) return

        ioExecutor.execute {
            runCatching {
                val file = sessionFile(cloneId)
                val root = loadJson(cloneId, file)
                val cookiesObj = root.optJSONObject("cookies") ?: JSONObject()
                captured.forEach { (origin, rawCookie) ->
                    cookiesObj.put(origin, rawCookie)
                }
                root.put("cookies", cookiesObj)
                root.put("updatedAt", System.currentTimeMillis())
                memoryCache[cloneId] = root
                writeJsonAtomically(file, root)
            }
        }
    }

    fun saveLocalStorageSnapshot(cloneId: Long, storageJson: String?) {
        if (cloneId <= 0L || storageJson.isNullOrBlank() || storageJson == "null") return
        ioExecutor.execute {
            runCatching {
                val file = sessionFile(cloneId)
                val root = loadJson(cloneId, file)
                root.put("localStorage", storageJson)
                root.put("updatedAt", System.currentTimeMillis())
                memoryCache[cloneId] = root
                writeJsonAtomically(file, root)
            }
        }
    }

    fun prepareSlotForClone(
        cloneId: Long,
        targetUrl: String,
        forceClearTarget: Boolean,
        onReady: () -> Unit
    ) {
        val currentActive = getActiveDynamicCloneId()
        val currentUrl = getActiveDynamicCloneUrl()

        if (currentActive > 0L && currentActive != cloneId && currentUrl.isNotBlank()) {
            saveCookiesForClone(currentActive, currentUrl)
        }

        if (forceClearTarget) {
            deleteSession(cloneId)
        }

        val needsSwap = forceClearTarget || (currentActive != cloneId)
        if (!needsSwap) {
            onReady()
            return
        }

        val cookieManager = runCatching { CookieManager.getInstance() }.getOrNull()
        if (cookieManager == null) {
            setActiveDynamicCloneId(cloneId, targetUrl)
            onReady()
            return
        }

        runCatching {
            cookieManager.removeAllCookies {
                runCatching { WebStorage.getInstance().deleteAllData() }
                ioExecutor.execute {
                    val cookiesToRestore = runCatching {
                        val root = loadJson(cloneId, sessionFile(cloneId))
                        extractCookiePairs(root)
                    }.getOrDefault(emptyList())

                    mainHandler.post {
                        runCatching {
                            cookiesToRestore.forEach { (origin, cookie) ->
                                cookieManager.setCookie(origin, cookie)
                            }
                            cookieManager.flush()
                        }
                        setActiveDynamicCloneId(cloneId, targetUrl)
                        onReady()
                    }
                }
            }
        }.onFailure {
            setActiveDynamicCloneId(cloneId, targetUrl)
            onReady()
        }
    }

    fun restoreLocalStorageIntoWebView(cloneId: Long, webView: WebView?) {
        if (cloneId <= 0L || webView == null) return
        ioExecutor.execute {
            val serialized = runCatching {
                loadJson(cloneId, sessionFile(cloneId)).optString("localStorage", "")
            }.getOrDefault("")
            if (serialized.isBlank()) return@execute

            val safeJsArg = JSONObject.quote(serialized)
            val script = """
                (function() {
                  try {
                    var data = JSON.parse($safeJsArg);
                    if (data && typeof data === 'object') {
                      Object.keys(data).forEach(function(k) {
                        window.localStorage.setItem(k, data[k]);
                      });
                    }
                  } catch (e) {}
                })();
            """.trimIndent()
            mainHandler.post {
                runCatching { webView.evaluateJavascript(script, null) }
            }
        }
    }

    fun captureLocalStorageFromWebView(cloneId: Long, webView: WebView?) {
        if (cloneId <= 0L || webView == null) return
        val script = """
            (function() {
              try {
                var out = {};
                for (var i = 0; i < window.localStorage.length; i++) {
                  var k = window.localStorage.key(i);
                  out[k] = window.localStorage.getItem(k);
                }
                return JSON.stringify(out);
              } catch (e) { return ""; }
            })();
        """.trimIndent()
        runCatching {
            webView.evaluateJavascript(script) { rawResult ->
                if (!rawResult.isNullOrBlank() && rawResult != "null" && rawResult != "\"\"") {
                    val decoded = runCatching {
                        JSONObject("{\"v\":$rawResult}").optString("v", "")
                    }.getOrDefault("")
                    if (decoded.startsWith("{") && decoded.length > 2) {
                        saveLocalStorageSnapshot(cloneId, decoded)
                    }
                }
            }
        }
    }

    fun deleteSession(cloneId: Long) {
        memoryCache.remove(cloneId)
        if (getActiveDynamicCloneId() == cloneId) {
            prefs.edit().remove(KEY_ACTIVE_CLONE_ID).remove(KEY_ACTIVE_CLONE_URL).apply()
        }
        ioExecutor.execute {
            runCatching {
                val file = sessionFile(cloneId)
                if (file.exists()) {
                    file.delete()
                }
            }
        }
    }

    private fun extractCookiePairs(root: JSONObject): List<Pair<String, String>> {
        val cookiesObj = root.optJSONObject("cookies") ?: return emptyList()
        val pairs = ArrayList<Pair<String, String>>()
        val keys = cookiesObj.keys()
        while (keys.hasNext()) {
            val origin = keys.next()
            val rawCookieHeader = cookiesObj.optString(origin, "")
            rawCookieHeader.split(";").forEach { part ->
                val trimmed = part.trim()
                if (trimmed.isNotEmpty()) {
                    pairs.add(origin to "$trimmed; Path=/; Secure")
                }
            }
        }
        return pairs
    }

    private fun extractCandidateOrigins(url: String): List<String> {
        val parsed = runCatching { Uri.parse(url) }.getOrNull() ?: return emptyList()
        val scheme = parsed.scheme ?: "https"
        val host = parsed.host ?: return emptyList()
        val origins = linkedSetOf("$scheme://$host")
        val parts = host.split(".")
        if (parts.size >= 2) {
            val apex = parts.takeLast(2).joinToString(".")
            origins.add("$scheme://$apex")
            origins.add("$scheme://www.$apex")
        }
        return origins.toList()
    }

    private fun sessionFile(cloneId: Long): File = File(vaultDir, "clone_$cloneId.json")

    private fun loadJson(cloneId: Long, file: File): JSONObject {
        memoryCache[cloneId]?.let { cached ->
            return runCatching { JSONObject(cached.toString()) }.getOrDefault(JSONObject())
        }
        if (!file.exists()) return JSONObject()
        return try {
            JSONObject(file.readText()).also { memoryCache[cloneId] = it }
        } catch (_: Exception) {
            JSONObject()
        }
    }

    private fun writeJsonAtomically(file: File, json: JSONObject) {
        try {
            if (!vaultDir.exists()) vaultDir.mkdirs()
            val tmpFile = File(vaultDir, "${file.name}.tmp")
            tmpFile.writeText(json.toString())
            if (!tmpFile.renameTo(file)) {
                file.writeText(json.toString())
                tmpFile.delete()
            }
        } catch (_: Exception) {
            // Safe ignore on storage errors.
        }
    }

    companion object {
        private const val KEY_ACTIVE_CLONE_ID = "active_dynamic_clone_id"
        private const val KEY_ACTIVE_CLONE_URL = "active_dynamic_clone_url"
    }
}
