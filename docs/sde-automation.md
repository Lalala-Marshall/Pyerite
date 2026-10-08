# SDE 自动同步（GitHub Releases + App 兜底）

## 三层数据

| 层级 | 位置 | 作用 |
|------|------|------|
| 手机本地 | `databases/` + `files/icons/` | 用户实际使用的数据 |
| APK 内置 | `app/src/main/assets/` | 安装/升级后的兜底版本 |
| GitHub Release | `Lalala-Marshall/Pyerite` Releases | 远端增量更新源 |

## CI 如何判断「有新 build」

比较的是 **两份 GitHub Release 的元数据**，不是 assets：

| 一端 | 来源 |
|------|------|
| **上游** | `EstamelGG/EveSDE_2.0` 最新 Release（`metadata.json` / release body / tag） |
| **已发布** | `Lalala-Marshall/Pyerite` 最新 Release 的 `latest.json` |

上游 `build_number` 更大 → 下载、normalize、发 Pyerite Release、**commit & push `main/assets`**。

若上游未更新，但 **assets 落后于 Pyerite Release** → 从 Release 下载文件刷新 assets 并 commit（保证下次发版 APK 兜底是最新的）。

`app/src/main/assets/` 仅在 **尚无 Pyerite Release** 时作为首次比较的兜底。

## 一次性设置

1. **推送本仓库到 GitHub**（若尚未推送 workflow 与脚本变更）。
2. 打开 **Actions → Sync SDE → Run workflow**，手动跑一次。
   - 若 bundled 已是最新，也会从 `app/src/main/assets/` **发布 GitHub Release**（首次必需）
   - 检测 `EstamelGG/EveSDE_2.0` 是否有新版本
   - 下载、normalize、写入 `app/src/main/assets/`
   - 创建/更新 GitHub Release（`sde-build-<build_number>`）
   - 自动 commit + push 兜底 assets 到 `main`
3. 确认 Releases 页出现 `latest.json`、`item_db_zh.sqlite`、`item_db_en.sqlite`、`icons.zip`。

之后每天 **北京时间 22:00**（UTC 14:00）会自动检查上游；有新 SDE 时重复上述步骤，无需手动操作。

## 本地手动同步（开发）

```bash
python tools/sync_sde_release.py --yes
```

仅更新本机 `app/src/main/assets/`，不发布 Release。

发布 Release 并 push 兜底（需 `GITHUB_TOKEN`）：

```bash
set GITHUB_TOKEN=ghp_xxx
set GITHUB_REPOSITORY=Lalala-Marshall/Pyerite
set GITHUB_REF_NAME=main
python tools/sync_sde_release.py --yes --publish-release --commit-assets
```

## App 行为

- 启动时：`BundledSdeUpdater` 用 APK 内置 `latest.txt` / `latest.json` 升级手机本地库（若 bundled 更新）。
- 主页右上角：检查 `https://github.com/Lalala-Marshall/Pyerite/releases/latest/download/latest.json`。
- 有更新时显示角标；点击下载 ~260MB，应用后自动关闭 Room 缓存、重建图标索引，**无需清缓存或重启**。

## 注意

- 单个 sqlite 约 90MB，Git 历史会变大；用户量大时可考虑 Git LFS。
- Workflow 仅在 `schedule` / `workflow_dispatch` 触发，**不要在 push 时触发**，避免 commit 循环。
- 兜底资源仅维护在 `app/src/main/assets/`（Debug / Release 共用）。
