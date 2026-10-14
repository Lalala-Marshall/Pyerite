# Pyerite

![Pyerite](img.png)

[中文](Readme.md) | [English](Readme.en.md)

# 简介

Pyerite 是 EVE Online 的 Android 伴侣应用。通过 EVE SSO 登录后，可以查看人物、军团、技能、邮件、市场与工业等数据。

界面与交互已获得三钛合金作者 EstamelGG 的授权。

# SDE 更新

自动同步流程见 [SDE 自动同步说明](docs/sde-automation.md)。

1. 上游为 `EstamelGG/EveSDE_2.0` 的最新 Release
2. 本仓库 GitHub Release 提供 `latest.json`、中英 SQLite 与图标包
3. 每天北京时间 22:00 由 Actions 检查上游；有新 build 时发布 Release，并更新 `app/src/main/assets/` 兜底数据
4. 应用启动时用 APK 内置版本升级本地库；首页也可检查并下载远端更新

本地只更新 assets、不发布 Release：

```bash
python tools/sync_sde_release.py --yes
```

# Android Studio

使用 Android Studio 编译（AGP 9.2.1，Kotlin 2.4.10，minSdk 24，compileSdk 37）。

# 第三方依赖

- **AndroidX**（Jetpack Compose、Navigation、Room、Lifecycle、AppCompat）: https://github.com/androidx/androidx
- **Kotlin**: https://github.com/JetBrains/kotlin
- **kotlinx.serialization**: https://github.com/Kotlin/kotlinx.serialization
- **Koin**: https://github.com/InsertKoinIO/koin
- **Retrofit**: https://github.com/square/retrofit
- **OkHttp**: https://github.com/square/okhttp
- **Coil**: https://github.com/coil-kt/coil
- **Vico**: https://github.com/patrykandpatrick/vico

# 获取应用

Android：

- [Pyerite on GitHub](https://github.com/Lalala-Marshall/Pyerite)
