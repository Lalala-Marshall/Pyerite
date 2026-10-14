# SDE 自动同步（Gitee + GitHub Releases）

## 三层数据

| 层级 | 位置 | 作用 |
|------|------|------|
| 手机本地 | `databases/` + `files/icons/` | 用户实际使用的数据 |
| APK 内置 | `app/src/main/assets/` | 安装/升级后的兜底版本。数据库是未压缩的 sqlite |
| Gitee Release | `lalala-marshall/pyerite-sde-source` | 国内下载。数据库是 `.sqlite.gz` |
| GitHub Release | `Lalala-Marshall/Pyerite` | 国外下载。图标只有一个 `icons.zip` |

## CI 如何判断「有新 build」

比较的是 **两份 Release 的元数据**，不是文件内容：

| 一端 | 来源 |
|------|------|
| **上游** | `EstamelGG/EveSDE_3.0` 最新 Release（`metadata.json` / release body / tag） |
| **已发布** | `Lalala-Marshall/Pyerite` 最新 Release 的 `latest.json` |

上游 `build_number` 更大时：

1. 下载 `sde.zip`（单个 `db/item_db.sqlite` + `texts.zip`）和 `icons.zip`。
2. 投影成 Room 仍在使用的 `item_db_zh.sqlite` / `item_db_en.sqlite`。缺了 App 要读的列就停止，不发布。
3. 把 `BOOLEAN` 收成 `INTEGER`。
4. 未压缩的 sqlite 写入 `app/src/main/assets/`，并 commit。
5. 发布 gzip 后的数据库、`latest.json` 和单个 `icons.zip` 到 GitHub。Gitee 不传整包图标：美国 runner 到 Gitee 单连接太慢，整包会超过上传时限。图标切成约 8MB 的 `icons.zip.partNN`，外加一行块数的 `icons.zip.parts`，只出现在 Gitee，由多台 runner 并行上传。这些分块不是 GitHub Release 的附件。Gitee 只保留最近 4 个 `sde-build-*` Release。单个文件超过 100MB 时跳过 Gitee，GitHub 仍然发布。

`latest.json` 里的 `item_db_zh_sha256` / `item_db_en_sha256` 是解压后 sqlite 的 SHA256，`icon_sha256` 是 `icons.zip` 的 SHA256。

## 一次性设置

1. GitHub Actions secret 名为 `GITEE_TOKEN`（Gitee 私人令牌，权限包含 projects）。不要把令牌写进仓库。
2. 推送 workflow 与脚本。
3. 打开 **Actions → Sync SDE → Run workflow**，手动跑一次。
4. 确认 GitHub 最新 Release 只有 `latest.json`、两个 `.sqlite.gz` 和一个 `icons.zip`。Gitee 上有两个 `.sqlite.gz`、`icons.zip.parts` 和全部分块，没有整包 `icons.zip`。

之后每天 **北京时间 22:00**（UTC 14:00）自动检查上游。

## 本地手动同步（开发）

```bash
python tools/sync_sde_release.py --yes
```

只更新本机 `app/src/main/assets/`，不发布 Release。

发布到 GitHub 和 Gitee，并 push 兜底：

```bash
set GITHUB_TOKEN=ghp_xxx
set GITEE_TOKEN=gitee_xxx
set GITHUB_REPOSITORY=Lalala-Marshall/Pyerite
set GITHUB_REF_NAME=main
python tools/sync_sde_release.py --yes --publish-release --commit-assets
```

## App 行为

- 启动时：`BundledSdeUpdater` 用 APK 内置版本升级手机本地库。
- 检查更新：先 Gitee 的 `latest.json`，失败再试 GitHub。下载地址按仓库和 tag 自己拼，不使用 `latest.json` 里的任意主机。
- 数据库先下 `.sqlite.gz`，没有再下旧的 `.sqlite`。解压后核对 SHA256，再交给 Room。
- 图标先从 Gitee 按 `icons.zip.parts` 的块数逐块下载，拼回 `icons.zip` 后核对 SHA256。分块失败，或没有分块清单时，改下整包 `icons.zip`（在 GitHub Release 上）。
- 下载顺序是 Gitee，失败再 GitHub。中断后同一地址用 Range 续传。

## 注意

- Workflow 只在 `schedule` / `workflow_dispatch` 触发，不要在 push 时触发。
- 兜底资源只在 `app/src/main/assets/`（Debug / Release 共用），里面仍是未压缩 sqlite。
