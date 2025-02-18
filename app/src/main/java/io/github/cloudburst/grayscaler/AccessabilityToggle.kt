package io.github.cloudburst.grayscaler

import android.accessibilityservice.AccessibilityServiceInfo
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.provider.Settings
import android.view.accessibility.AccessibilityManager
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver

@Composable
fun AccessibilityToggle(
    serviceComponentName: ComponentName,
    modifier: Modifier = Modifier,
    context: Context = LocalContext.current
) {
    val (isServiceEnabled, setIsEnabled) = remember {
        mutableStateOf(
            checkAccessibilityServiceEnabled(context, serviceComponentName)
        )
    }

    val lifecycleOwner = LocalLifecycleOwner.current

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                setIsEnabled(checkAccessibilityServiceEnabled(context, serviceComponentName))
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }


    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .clickable {
                context.startActivity(
                    Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                )
            }
            .padding(16.dp)
    ) {
        Text(
            text = "Accessibility Service",
            modifier = Modifier.weight(1f)
        )
        Switch(
            checked = isServiceEnabled,
            onCheckedChange = {
                context.startActivity(
                    Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                )
            }
        )
    }
}

fun checkAccessibilityServiceEnabled(
    context: Context,
    serviceComponentName: ComponentName
): Boolean {
    val accessibilityManager =
        context.getSystemService(Context.ACCESSIBILITY_SERVICE) as AccessibilityManager
    val enabledServices =
        accessibilityManager.getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_ALL_MASK)
    return enabledServices.any { serviceInfo ->
        val info = serviceInfo.resolveInfo.serviceInfo
        info.packageName == serviceComponentName.packageName &&
                info.name == serviceComponentName.className
    }
}