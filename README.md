<div align="center">

![Grayscaler](./app/src/main/play_store_512.png)

</div>

# Grayscaler

Grayscaler is an Android app designed to help users stay focused by keeping their phone mostly monochrome.
However, it allows specific apps, like the Camera or Gallery, to remain in full color when needed.
This can help reduce distractions, encourage mindful phone usage, and promote digital well-being.

## Features
- **Whitelist Mode**: Select apps that should remain in color while the rest of the phone stays grayscale.
- **Blacklist Mode**: Choose specific apps to be grayscale while keeping the rest in color.
- **Accessibility Service**: Detects the currently open app and adjusts the screen colors accordingly.
- **Shizuku Integration**: Uses Shizuku to grant required permissions for editing system settings.

## Why Use Grayscaler?
- Reduce distractions and improve focus.
- Enhance digital well-being by minimizing excessive screen time.
- Maintain color in essential apps like Camera and Gallery while keeping others monochrome.

## Requirements
- Android device with Shizuku installed and set up.
- Accessibility permissions enabled for Grayscaler.

## Installation
1. Install Shizuku and start the service.
2. Download and install Grayscaler.
3. Grant Accessibility permissions when prompted.
4. Select your preferred mode (Whitelist or Blacklist) and configure your app list.

## Usage
1. Ensure Shizuku is running in the background. (Required first time only)
2. Open Grayscaler and select your mode.
3. Add apps to the whitelist or blacklist as needed.

## Usage without Shizuku

You can use Grayscaler without Shizuku by manually granting the required permissions using ADB.

```shell
adb shell pm grant io.github.cloudburst.grayscaler android.permission.WRITE_SECURE_SETTINGS
adb shell pm grant io.github.cloudburst.grayscaler android.permission.PACKAGE_USAGE_STATS
adb shell pm grant io.github.cloudburst.grayscaler android.permission.QUERY_ALL_PACKAGES
```

## License


---

Feel free to contribute or provide feedback to improve Grayscaler!

