package io.github.cloudburst.grayscaler

import android.content.ComponentName
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toBitmap
import io.github.cloudburst.grayscaler.ShizukuRunner.CommandResultListener
import io.github.cloudburst.grayscaler.ShizukuRunner.Companion.command
import io.github.cloudburst.grayscaler.ShizukuRunner.Companion.shizukuEnabled
import io.github.cloudburst.grayscaler.ui.theme.GrayscalerTheme
import kotlinx.coroutines.sync.Semaphore
import java.util.concurrent.CyclicBarrier
import kotlin.concurrent.thread

class MainActivity : ComponentActivity() {
    lateinit var store: AppListStore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (!hasAllPermissions()) {
            if (shizukuEnabled(this)) {
                thread {
                    val barrier = CyclicBarrier(if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) 4 else 3)
                    command("pm grant ${packageName} android.permission.WRITE_SECURE_SETTINGS") { msg, done, error ->
                        if (error)
                            Toast.makeText(this, msg, Toast.LENGTH_LONG).show()
                        barrier.await()
                    }
                    command("pm grant ${packageName} android.permission.PACKAGE_USAGE_STATS") { msg, done, error ->
                        if (error)
                            Toast.makeText(this, msg, Toast.LENGTH_LONG).show()
                        barrier.await()
                    }
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R)
                        command("pm grant ${packageName} android.permission.QUERY_ALL_PACKAGES") { msg, done, error ->
                            if (error)
                                Toast.makeText(this, msg, Toast.LENGTH_LONG).show()
                            barrier.await()
                        }
                    barrier.await()
                    runOnUiThread {
                        finish()
                        startActivity(Intent(this, MainActivity::class.java))
                    }
                }

            } else {
                val packageManager = packageManager
                val intent = packageManager.getLaunchIntentForPackage("moe.shizuku.privileged.api")
                if (intent != null) {
                    startActivity(intent)
                } else {
                  startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://shizuku.rikka.app/")))
                }
                finish()
            }
        }

        store = AppListStore(this)
        store.load()
        //enableEdgeToEdge()

        setContent {
            MaterialTheme {
                App(store)
            }
        }
    }

    override fun onPause() {
        super.onPause()
        store.save()
    }

    private fun hasAllPermissions(): Boolean {
        if (ContextCompat.checkSelfPermission(
                this,
                android.Manifest.permission.WRITE_SECURE_SETTINGS
            ) != PackageManager.PERMISSION_GRANTED
        )
            return false
        if (ContextCompat.checkSelfPermission(
                this,
                android.Manifest.permission.PACKAGE_USAGE_STATS
            ) != PackageManager.PERMISSION_GRANTED
        )
            return false
        if (ContextCompat.checkSelfPermission(
                this,
                android.Manifest.permission.QUERY_ALL_PACKAGES
            ) != PackageManager.PERMISSION_GRANTED && android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R
        )
            return false
        return true
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun App(store: AppListStore) {
    val (apps, setApps) = remember { mutableStateOf(store.apps) }
    val (whitelist, setWhitelist) = remember { mutableStateOf(store.whitelist) }
    GrayscalerTheme {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            topBar = {
                TopAppBar(
                    title = { Text(text = "Grayscaler") },
                    actions = {
                        AccessibilityToggle(
                            serviceComponentName = ComponentName(
                                store.context,
                                MainService::class.java
                            ),
                            context = LocalContext.current
                        )
                    }
                )
            }
        ) { innerPadding ->
            Column(modifier = Modifier.padding(innerPadding)) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(text = if (whitelist) "Whitelist" else "Blacklist")
                    Switch(
                        checked = whitelist,
                        onCheckedChange = {
                            store.whitelist = it
                            store.toggledApps = emptySet()
                            store.invalidate()
                            setWhitelist(it)
                            setApps(store.apps)
                        }
                    )
                }
                LazyColumn(
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(apps.size) { appId ->
                        val (app, enabled) = apps[appId]
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Image(
                                    bitmap = app.icon.current.toBitmap().asImageBitmap(),
                                    contentDescription = null,
                                    modifier = Modifier
                                        .size(40.dp)
                                )
                                Column(
                                    modifier = Modifier
                                        .weight(1f)
                                        .padding(start = 16.dp)
                                ) {
                                    Text(text = app.appName)
                                    Text(
                                        text = app.packageName,
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }
                                Switch(
                                    checked = enabled,
                                    onCheckedChange = { isChecked ->
                                        store.toggleApp(app.packageName)
                                        store.invalidate()
                                        setApps(store.apps)
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
