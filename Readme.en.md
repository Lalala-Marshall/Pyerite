# Pyerite

![Pyerite](img.png)

[中文](Readme.md) | [English](Readme.en.md)

# About

Pyerite is an Android companion for EVE Online. After signing in with EVE SSO, it shows character, corporation, skills, mail, market, and industry data.

The interface and interactions are used with authorization from EstamelGG, author of Tritanium.

# SDE Updates

See [SDE sync](docs/sde-automation.md).

1. Upstream is the latest Release of `EstamelGG/EveSDE_3.0`, projected into the existing Chinese and English databases
2. GitHub Releases and Gitee (`lalala-marshall/pyerite-sde-source`) publish `latest.json`, gzipped databases, and the icon pack. The app tries Gitee first
3. GitHub Actions checks upstream daily at 22:00 Beijing time, and updates the bundled copy under `app/src/main/assets/` when a new build is published
4. On launch, the app upgrades the local database from the copy bundled in the APK. The home screen can also download a remote update

Update local assets without publishing a Release:

```bash
python tools/sync_sde_release.py --yes
```

# Android Studio

Compiled with Android Studio (AGP 9.2.1, Kotlin 2.4.10, minSdk 24, compileSdk 37).

# Third-party Dependencies

- **AndroidX** (Jetpack Compose, Navigation, Room, Lifecycle, AppCompat): https://github.com/androidx/androidx
- **Kotlin**: https://github.com/JetBrains/kotlin
- **kotlinx.serialization**: https://github.com/Kotlin/kotlinx.serialization
- **Koin**: https://github.com/InsertKoinIO/koin
- **Retrofit**: https://github.com/square/retrofit
- **OkHttp**: https://github.com/square/okhttp
- **Coil**: https://github.com/coil-kt/coil
- **Vico**: https://github.com/patrykandpatrick/vico

# Get App

Download and install the Android package.

- [Pyerite 1.0.1 APK](https://github.com/Lalala-Marshall/Pyerite/releases/download/app-v1.0.1/pyerite-1.0.1.apk)

Quark Cloud Download
- [Pyerite 1.0.1 APK_Quark](https://pan.quark.cn/s/0dc8e9012d98)
