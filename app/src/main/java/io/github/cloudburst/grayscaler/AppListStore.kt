package io.github.cloudburst.grayscaler

import android.app.usage.UsageStats
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Context.USAGE_STATS_SERVICE
import android.graphics.drawable.Drawable
import kotlin.collections.mapNotNull

data class AppEntry(
    val packageName: String,
    val appName: String,
    val icon: Drawable
)

class AppListStore(
    val context: Context
) {

    val path = context.filesDir.resolve("apps.txt")
    var whitelist: Boolean = true
    var toggledApps: Set<String> = emptySet()

    var apps: List<Pair<AppEntry, Boolean>> = emptyList()
        get() {
            if (field.isEmpty()) {
                field = (toggledApps + listPackages()).mapNotNull { packageName ->
                    try {
                        val appInfo = context.packageManager.getApplicationInfo(packageName, 0)
                        val appName = context.packageManager.getApplicationLabel(appInfo).toString()
                        val icon = context.packageManager.getApplicationIcon(appInfo)
                        AppEntry(packageName, appName, icon) to toggledApps.contains(packageName)
                    } catch (e: Exception) {
                        null
                    }
                }
            }
            return field
        }
        private set

    fun save() {
        val stream = path.outputStream().writer()
        stream.write(if (whitelist) "w\n" else "b\n")
        toggledApps.forEach { stream.write("$it\n") }
        stream.close()
    }

    fun load() {
        if (!path.exists()) return
        val stream = path.inputStream().bufferedReader()
        whitelist = stream.readLine() != "b"
        toggledApps = stream.readLines().filter { it.isNotBlank() }.toSet()
        stream.close()
    }

    fun shouldGrayScale(packageName: String): Boolean {
        return if (whitelist) !toggledApps.contains(packageName) else toggledApps.contains(packageName)
    }

    fun toggleApp(packageName: String) {
        if (toggledApps.contains(packageName)) {
            toggledApps -= packageName
        } else {
            toggledApps += packageName
        }
    }

    fun invalidate() {
        apps = emptyList()
    }

    private fun listPackages() : Set<String> {
        val usageStatsManager = context.getSystemService(USAGE_STATS_SERVICE) as UsageStatsManager
        val result = usageStatsManager.queryUsageStats(
            UsageStatsManager.INTERVAL_DAILY,
            0,
            System.currentTimeMillis()
        )
            .filter { it.packageName != context.packageName && it.totalTime > 0 }
            .sortedBy { -it.lastTimeUsed }
            .map { it.packageName }
            .toSet()

        return result
    }
}

private val UsageStats.totalTime get() = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) totalTimeVisible else totalTimeInForeground