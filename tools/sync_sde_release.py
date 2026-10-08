#!/usr/bin/env python3
"""
Download latest EstamelGG/EveSDE_2.0 release (sde.zip + icons.zip), refresh
D:\\Coding\\sde.

**Phase A (update check only):** Compare upstream ``EstamelGG/EveSDE_2.0`` release metadata to
**this repo's latest GitHub Release** (``latest.json`` on Pyerite Releases). Bundled
``app/src/main/assets/latest.txt`` is only a fallback when no Release exists yet.
No SQLite file diff; no comparing two DBs to decide whether to download.

**Phase B (after a real update):** Unzip upstream DB, normalize for Room, write
``app/src/main/assets/``, publish/update Pyerite GitHub Release, and ``--commit-assets``
pushes bundled files so the next app release ships the same SDE build.

Requires: public GitHub for downloads. Use --yes to allow wiping eve_sde root.
"""

from __future__ import annotations

import argparse
import json
import os
import re
import shutil
import subprocess
import tempfile
import time
import sys
import urllib.error
import urllib.parse
import urllib.request
import zipfile
from pathlib import Path

REPO_ROOT = Path(__file__).resolve().parents[1]
NORMALIZE_SCRIPT = REPO_ROOT / "tools" / "normalize_sqlite_boolean_to_integer.py"

DEFAULT_EVE_SDE_ROOT = Path(os.environ.get("EVE_SDE_ROOT", str(REPO_ROOT / ".eve_sde_cache")))
GITHUB_API_LATEST = "https://api.github.com/repos/EstamelGG/EveSDE_2.0/releases/latest"
GITHUB_HTML_LATEST = "https://github.com/EstamelGG/EveSDE_2.0/releases/latest"
USER_AGENT = "Pyerite-SDE-Sync/1.0 (github.com/EstamelGG/EveSDE_2.0 consumer)"

ASSET_SDE = "sde.zip"
ASSET_ICONS_PRIMARY = "icons.zip"
ASSET_ICONS_ALT = "icon.zip"
ASSET_METADATA = "metadata.json"

APP_ASSETS_DIR = REPO_ROOT / "app" / "src" / "main" / "assets"
APP_DB_DIR = APP_ASSETS_DIR / "db"
APP_ICONS_DIR = APP_ASSETS_DIR / "icons"
APP_DB_ZH = APP_DB_DIR / "item_db_zh.sqlite"
APP_DB_EN = APP_DB_DIR / "item_db_en.sqlite"
# Bundled release metadata for sync compares (JSON, same schema as upstream latest.txt / latest.log).
APP_LATEST = APP_ASSETS_DIR / "latest.txt"
APP_LATEST_JSON = APP_ASSETS_DIR / "latest.json"
# Older syncs wrote metadata next to the DB; still read for version compare until replaced.
LEGACY_APP_LATEST = APP_DB_DIR / "latest.txt"
APP_ICONS_ZIP = APP_ICONS_DIR / "icons.zip"

PYERITE_RELEASE_ASSET_DB_ZH = "item_db_zh.sqlite"
PYERITE_RELEASE_ASSET_DB_EN = "item_db_en.sqlite"
PYERITE_RELEASE_ASSET_ICONS = "icons.zip"
PYERITE_RELEASE_ASSET_META = "latest.json"


def _resolve_sde_db_root(eve: Path) -> Path:
    nested = eve / "sde" / "db"
    flat = eve / "db"
    if (nested / "item_db_zh.sqlite").is_file():
        return nested
    if (flat / "item_db_zh.sqlite").is_file():
        return flat
    names = sorted(p.name for p in eve.iterdir()) if eve.is_dir() else []
    raise SystemExit(
        "Missing item_db_zh.sqlite after unzip. "
        f"Tried {nested / 'item_db_zh.sqlite'} and {flat / 'item_db_zh.sqlite'}. "
        f"Top-level in {eve!r}: {names!r}"
    )


def find_remote_dbs_and_meta(eve: Path) -> tuple[Path, Path, Path]:
    """Paths to zh/en SQLite and build metadata after *sde.zip* is extracted into *eve*."""
    db_root = _resolve_sde_db_root(eve)
    zh_db = db_root / "item_db_zh.sqlite"
    en_db = db_root / "item_db_en.sqlite"
    if not en_db.is_file():
        raise SystemExit(f"Missing item_db_en.sqlite under {db_root} after unzip.")
    meta_root = db_root.parent
    for name in ("latest.txt", "latest.log"):
        p = meta_root / name
        if p.is_file():
            return zh_db, en_db, p
    raise SystemExit(f"No latest.txt or latest.log under {meta_root} after unzip.")


def _github_auth_headers() -> dict[str, str]:
    h = {
        "User-Agent": USER_AGENT,
        "Accept": "application/vnd.github+json",
    }
    token = os.environ.get("GITHUB_TOKEN") or os.environ.get("GH_TOKEN")
    if token:
        h["Authorization"] = f"Bearer {token}"
    return h


def http_json(url: str) -> dict:
    req = urllib.request.Request(url, headers=_github_auth_headers())
    with urllib.request.urlopen(req, timeout=120) as resp:
        return json.loads(resp.read().decode("utf-8"))


def resolve_tag_from_releases_page() -> str:
    """Follow github.com/releases/latest redirect; no REST API (avoids API rate limits)."""
    final: str | None = None
    urllib_err: Exception | None = None
    try:
        req = urllib.request.Request(GITHUB_HTML_LATEST, headers=_github_auth_headers())
        with urllib.request.urlopen(req, timeout=120) as resp:
            final = resp.geturl()
            with open(os.devnull, "wb") as sink:
                shutil.copyfileobj(resp, sink)
    except (urllib.error.URLError, TimeoutError, OSError) as e:
        urllib_err = e
        curl = shutil.which("curl")
        if not curl:
            raise SystemExit(
                f"Could not open {GITHUB_HTML_LATEST!r} ({e!r}); "
                "install curl on PATH or set GITHUB_TOKEN / GH_TOKEN and retry."
            ) from e
        curl_args = [curl]
        if sys.platform == "win32":
            curl_args.append("--ssl-no-revoke")
            curl_args.append("--http1.1")
        with tempfile.NamedTemporaryFile(delete=False) as sink:
            sink_path = sink.name
        try:
            curl_args += [
                "-sL",
                "--retry",
                "5",
                "--retry-all-errors",
                "--retry-delay",
                "2",
                "-o",
                sink_path,
                "-w",
                "%{url_effective}",
                "-A",
                USER_AGENT,
                GITHUB_HTML_LATEST,
            ]
            r = subprocess.run(
                curl_args,
                capture_output=True,
                text=True,
                timeout=120,
                check=False,
            )
        finally:
            try:
                os.unlink(sink_path)
            except OSError:
                pass
        if r.returncode != 0:
            raise SystemExit(
                f"curl failed ({r.returncode}) resolving latest release: {r.stderr.strip() or r.stdout!r}"
            ) from e
        final = (r.stdout or "").strip().splitlines()[-1].strip()
        print(f"[i] Resolved release tag via curl (urllib error: {urllib_err!r})", file=sys.stderr)

    assert final is not None
    marker = "/releases/tag/"
    if marker not in final:
        raise SystemExit(f"Could not parse release tag from redirect URL: {final}")
    tag = final.split(marker, 1)[1].split("?", 1)[0].split("#", 1)[0]
    tag = urllib.parse.unquote(tag)
    if not tag:
        raise SystemExit(f"Empty release tag parsed from URL: {final}")
    return tag


def direct_asset_urls(tag: str) -> tuple[str, str, str]:
    """Build browser_download-style URLs when the GitHub API is unavailable."""
    enc = urllib.parse.quote(tag, safe="")
    base = f"https://github.com/EstamelGG/EveSDE_2.0/releases/download/{enc}"
    sde_url = f"{base}/{ASSET_SDE}"
    primary = f"{base}/{ASSET_ICONS_PRIMARY}"
    alt = f"{base}/{ASSET_ICONS_ALT}"
    metadata_url = f"{base}/{ASSET_METADATA}"
    curl = shutil.which("curl")
    if not curl:
        return sde_url, primary, metadata_url
    curl_args = [curl]
    if sys.platform == "win32":
        curl_args.append("--ssl-no-revoke")
        curl_args.append("--http1.1")
    with tempfile.NamedTemporaryFile(delete=False) as hdr:
        hdr_path = hdr.name
    try:
        curl_args += [
            "-sI",
            "-L",
            "--retry",
            "5",
            "--retry-all-errors",
            "--retry-delay",
            "2",
            "-o",
            hdr_path,
            "-w",
            "%{http_code}",
            primary,
        ]
        r = subprocess.run(
            curl_args,
            capture_output=True,
            text=True,
            timeout=60,
            check=False,
        )
    finally:
        try:
            os.unlink(hdr_path)
        except OSError:
            pass
    code = (r.stdout or "").strip().splitlines()[-1].strip()
    if r.returncode == 0 and code == "404":
        return sde_url, alt, metadata_url
    return sde_url, primary, metadata_url


def _downloaded_zip_ok(dest: Path) -> bool:
    """urllib can finish with a truncated body on flaky TLS; reject bad .zip before unzip."""
    if dest.suffix.lower() != ".zip":
        return True
    return dest.is_file() and zipfile.is_zipfile(dest)


def download_file(url: str, dest: Path) -> None:
    dest.parent.mkdir(parents=True, exist_ok=True)
    headers = _github_auth_headers()
    req = urllib.request.Request(url, headers=headers)
    urllib_ok = False
    try:
        with urllib.request.urlopen(req, timeout=600) as resp:
            with dest.open("wb") as out:
                shutil.copyfileobj(resp, out)
        urllib_ok = True
    except (urllib.error.URLError, TimeoutError, OSError) as e:
        curl = shutil.which("curl")
        if not curl:
            raise SystemExit(
                f"Download failed ({e!r}) and curl was not found on PATH: {url}"
            ) from e
        print(f"[i] urllib TLS error; downloading via curl -> {dest.name}", file=sys.stderr)

    if urllib_ok and _downloaded_zip_ok(dest):
        return
    if urllib_ok and dest.suffix.lower() == ".zip":
        n = dest.stat().st_size if dest.is_file() else 0
        print(
            f"[i] urllib saved a corrupt or truncated zip ({n} bytes); "
            f"retrying via curl -> {dest.name}",
            file=sys.stderr,
        )
        try:
            dest.unlink()
        except OSError:
            pass

    curl = shutil.which("curl")
    if not curl:
        raise SystemExit(f"curl not on PATH (needed for zip retry or TLS fallback): {url}")

    args: list[str] = [curl]
    if sys.platform == "win32":
        args.append("--ssl-no-revoke")
        args.append("--http1.1")
    args += [
        "--retry",
        "5",
        "--retry-all-errors",
        "--retry-delay",
        "2",
        "--fail",
        "-sL",
        "-o",
        str(dest),
    ]
    token = os.environ.get("GITHUB_TOKEN") or os.environ.get("GH_TOKEN")
    if token:
        args += ["-H", f"Authorization: Bearer {token}"]
    args += ["-A", USER_AGENT, url]
    last_rc = -1
    last_r: subprocess.CompletedProcess[str] | None = None
    for attempt in range(6):
        r = subprocess.run(args, timeout=600, check=False, capture_output=True, text=True)
        last_r = r
        last_rc = r.returncode
        if r.returncode == 0 and _downloaded_zip_ok(dest):
            return
        if r.returncode == 0 and dest.suffix.lower() == ".zip":
            n = dest.stat().st_size if dest.is_file() else 0
            print(
                f"[i] curl wrote a corrupt or truncated zip ({n} bytes); "
                f"retrying ({attempt + 1}/6) ...",
                file=sys.stderr,
            )
            try:
                dest.unlink()
            except OSError:
                pass
            time.sleep(3)
            continue
        err = (r.stderr or "").strip()
        if attempt < 5:
            print(
                f"[i] curl download attempt {attempt + 1}/6 failed (exit {r.returncode}); "
                f"retrying in 3s ...{(' ' + err) if err else ''}",
                file=sys.stderr,
            )
            time.sleep(3)
    raise SystemExit(
        f"curl download failed after retries (exit {last_rc}): {url}\n"
        f"{((last_r.stderr or '').strip()) if last_r else ''}"
    )


def wipe_eve_sde_root(root: Path) -> None:
    if not root.exists():
        root.mkdir(parents=True)
        return
    for child in root.iterdir():
        if child.is_dir():
            shutil.rmtree(child)
        else:
            child.unlink()


def pick_asset_urls(release: dict) -> tuple[str, str, str | None]:
    assets = {a["name"]: a["browser_download_url"] for a in release.get("assets", []) if "browser_download_url" in a}
    if ASSET_SDE not in assets:
        raise SystemExit(f"Release missing {ASSET_SDE!r}. Available: {sorted(assets)}")
    icon_url = assets.get(ASSET_ICONS_PRIMARY) or assets.get(ASSET_ICONS_ALT)
    if not icon_url:
        raise SystemExit(
            f"Release missing {ASSET_ICONS_PRIMARY!r} or {ASSET_ICONS_ALT!r}. Available: {sorted(assets)}"
        )
    metadata_url = assets.get(ASSET_METADATA)
    return assets[ASSET_SDE], icon_url, metadata_url


def _extract_json_fence_from_body(body: str) -> dict | None:
    m = re.search(r"```json\s*([\s\S]*?)\s*```", body)
    if not m:
        return None
    try:
        return json.loads(m.group(1).strip())
    except json.JSONDecodeError:
        return None


def _tag_build_number(tag: str) -> str:
    if tag.startswith("sde-build-"):
        return tag.removeprefix("sde-build-").strip()
    return "0"


def flatten_release_meta(raw: dict, published_at: str = "") -> dict:
    """Normalize to version_key fields + optional icon/hash fields for bundled latest.txt."""
    bn_raw = raw.get("build_number")
    try:
        bn_int = int(bn_raw) if bn_raw is not None and str(bn_raw).strip() != "" else 0
    except (TypeError, ValueError):
        bn_int = 0
    bn_str = str(bn_int)
    rd = str(raw.get("release_date") or "").strip()
    ct = str(raw.get("completion_time") or "").strip()
    if not ct and rd:
        ct = rd
    if not rd and published_at:
        rd = published_at.strip()
    out: dict[str, object] = {
        "build_number": bn_str,
        "release_date": rd,
        "completion_time": ct,
    }
    for k in ("icon_version", "icon_sha256", "sde_sha256", "patch_number", "extra_db"):
        if k in raw and raw[k] is not None:
            out[k] = raw[k]
    return out


def metadata_from_release_body(release: dict) -> dict | None:
    """Parse 版本元数据 ```json ... ``` from GitHub release notes (same 构建信息 as the web UI)."""
    body = release.get("body") or ""
    if not isinstance(body, str) or not body.strip():
        return None
    j = _extract_json_fence_from_body(body)
    if not j:
        return None
    pub = str(release.get("published_at") or "")
    return flatten_release_meta(j, pub)


def try_download_json(url: str) -> dict | None:
    """Download a small JSON asset; return None on failure (never raises SystemExit)."""
    d = Path(tempfile.mkdtemp(prefix="sde-meta-"))
    p = d / "tmp.json"
    try:
        try:
            download_file(url, p)
        except SystemExit:
            return None
        try:
            return json.loads(p.read_text(encoding="utf-8"))
        except (OSError, json.JSONDecodeError):
            return None
    finally:
        shutil.rmtree(d, ignore_errors=True)


def remote_meta_for_compare(metadata_url: str | None, release: dict | None, tag: str) -> dict:
    """Build / icon / release fields for comparison **before** downloading ``sde.zip``."""
    if metadata_url:
        raw = try_download_json(metadata_url)
        if raw is not None:
            pub = str((release or {}).get("published_at") or "")
            return flatten_release_meta(raw, pub)
    if release:
        m = metadata_from_release_body(release)
        if m is not None:
            return m
    bn = _tag_build_number(tag)
    try:
        bn_int = int(bn)
    except ValueError:
        bn_int = 0
    return flatten_release_meta(
        {"build_number": bn_int, "release_date": "", "completion_time": ""},
        str((release or {}).get("published_at") or ""),
    )


def merge_meta_for_bundle(primary: dict, from_zip: dict | None) -> dict:
    """Prefer non-empty values from extracted ``latest.log`` / ``latest.txt`` over pre-download meta."""
    out = dict(primary)
    if from_zip:
        for k, v in from_zip.items():
            if v is not None and str(v).strip() != "":
                out[k] = v
    return out


def load_latest(path: Path) -> dict | None:
    if not path.is_file():
        return None
    try:
        return json.loads(path.read_text(encoding="utf-8"))
    except json.JSONDecodeError as e:
        raise SystemExit(f"Invalid JSON in {path}: {e}") from e


def version_key(d: dict) -> tuple[int, str, str]:
    try:
        bn = int(str(d.get("build_number") or "0").strip())
    except ValueError:
        bn = 0
    rd = str(d.get("release_date") or "")
    ct = str(d.get("completion_time") or "")
    return (bn, rd, ct)


def remote_is_newer(remote: dict, local: dict | None) -> bool:
    if local is None:
        return True
    return version_key(remote) > version_key(local)


def load_app_release_meta() -> dict | None:
    """Bundled fallback metadata shipped in the app (``app/src/main/assets``)."""
    return (
        load_latest(APP_LATEST)
        or load_latest(APP_LATEST_JSON)
        or load_latest(LEGACY_APP_LATEST)
    )


def fetch_pyerite_github_release() -> dict | None:
    url = f"{_github_repo_api_base()}/releases/latest"
    token = os.environ.get("GITHUB_TOKEN") or os.environ.get("GH_TOKEN")
    try:
        if token:
            rel = _github_get_json(url, token)
            return rel if isinstance(rel, dict) else None
        return http_json(url)
    except urllib.error.HTTPError as e:
        if e.code == 404:
            return None
        raise


def load_pyerite_release_latest_json() -> dict | None:
    """Raw ``latest.json`` from this repo's newest GitHub Release asset."""
    release = fetch_pyerite_github_release()
    if not release:
        return None
    for asset in release.get("assets") or []:
        if not isinstance(asset, dict):
            continue
        if asset.get("name") != PYERITE_RELEASE_ASSET_META:
            continue
        url = asset.get("browser_download_url")
        if url:
            return try_download_json(url)
    return None


def load_pyerite_github_release_meta() -> dict | None:
    """Build metadata for the SDE build already published on Pyerite Releases."""
    raw = load_pyerite_release_latest_json()
    if raw is not None:
        return flatten_release_meta(raw)
    release = fetch_pyerite_github_release()
    if not release:
        return None
    body_meta = metadata_from_release_body(release)
    if body_meta is not None:
        return body_meta
    tag = str(release.get("tag_name") or "")
    if tag.startswith("sde-build-"):
        return flatten_release_meta(
            {"build_number": _tag_build_number(tag)},
            str(release.get("published_at") or ""),
        )
    return None


def load_published_release_meta() -> dict | None:
    """Primary installed version: Pyerite GitHub Release, else bundled assets (first run)."""
    return load_pyerite_github_release_meta() or load_app_release_meta()


def bundled_behind_published(published: dict | None, bundled: dict | None) -> bool:
    if published is None:
        return False
    if bundled is None:
        return True
    if version_key(published) > version_key(bundled):
        return True
    if version_key(published) < version_key(bundled):
        return False
    for key in ("sde_sha256", "icon_sha256"):
        pv = published.get(key)
        bv = bundled.get(key)
        if pv and pv != bv:
            return True
    return False


def _asset_download_url(latest: dict, filename: str) -> str | None:
    assets = latest.get("assets")
    if isinstance(assets, dict):
        url = assets.get(filename)
        if url:
            return str(url)
    base = str(latest.get("assets_base_url") or "").strip().rstrip("/")
    if base:
        return f"{base}/{filename}"
    return None


def refresh_bundled_from_published_release() -> bool:
    """Copy DB/icons from this repo's GitHub Release into ``app/src/main/assets``."""
    latest = load_pyerite_release_latest_json()
    if not latest:
        print("[!] Could not load latest.json from Pyerite GitHub Release.", file=sys.stderr)
        return False

    download_dir = Path(tempfile.mkdtemp(prefix="pyerite-release-"))
    try:
        pairs = (
            (PYERITE_RELEASE_ASSET_DB_ZH, APP_DB_ZH),
            (PYERITE_RELEASE_ASSET_DB_EN, APP_DB_EN),
            (PYERITE_RELEASE_ASSET_ICONS, APP_ICONS_ZIP),
        )
        for asset_name, dest in pairs:
            url = _asset_download_url(latest, asset_name)
            if not url:
                print(f"[!] Missing download URL for {asset_name} in published latest.json", file=sys.stderr)
                return False
            tmp = download_dir / asset_name
            print(f"[+] Downloading published {asset_name} ...")
            download_file(url, tmp)
            replace_asset(tmp, dest)

        core = flatten_release_meta(latest)
        APP_ASSETS_DIR.mkdir(parents=True, exist_ok=True)
        APP_LATEST.write_text(json.dumps(core, indent=2) + "\n", encoding="utf-8")
        APP_LATEST_JSON.write_text(json.dumps(latest, indent=2) + "\n", encoding="utf-8")
        print(f"[+] Refreshed bundled assets from Pyerite Release (build {core.get('build_number')})")
        return True
    finally:
        shutil.rmtree(download_dir, ignore_errors=True)


def replace_asset(src: Path, dest: Path) -> None:
    """Remove any existing *dest* file, then copy *src* to *dest* (same filename)."""
    dest.parent.mkdir(parents=True, exist_ok=True)
    if dest.is_file():
        dest.unlink()
    shutil.copy2(src, dest)


def normalize_and_replace_db(remote_db: Path, app_db: Path) -> int:
    """Normalize upstream SQLite for Room and copy into app debug assets."""
    normalized = remote_db.with_name(f"{remote_db.stem}.normalized.sqlite")
    cmd = [
        sys.executable,
        str(NORMALIZE_SCRIPT),
        "--source",
        str(remote_db),
        "--output",
        str(normalized),
    ]
    print("Running:", " ".join(cmd))
    r = subprocess.run(cmd, cwd=str(REPO_ROOT))
    if r.returncode != 0:
        return r.returncode
    replace_asset(normalized, app_db)
    print(f"[+] Replaced {app_db}")
    return 0


def main() -> int:
    p = argparse.ArgumentParser(description=__doc__)
    p.add_argument(
        "--eve-sde-root",
        type=Path,
        default=DEFAULT_EVE_SDE_ROOT,
        help="Root folder to wipe, download zips into, and unzip sde.zip (default: D:\\Coding\\sde)",
    )
    p.add_argument(
        "--yes",
        action="store_true",
        help="Required to confirm wiping --eve-sde-root contents",
    )
    p.add_argument(
        "--force",
        action="store_true",
        help="Skip version compare; always normalize and copy into app assets",
    )
    p.add_argument(
        "--dry-run",
        action="store_true",
        help="Fetch release + compare metadata (upstream vs Pyerite Release vs bundled assets); no large downloads",
    )
    p.add_argument(
        "--publish-release",
        action="store_true",
        help="After updating bundled assets, create/update a Pyerite GitHub Release with DB/icons/latest.json",
    )
    p.add_argument(
        "--commit-assets",
        action="store_true",
        help="Commit app/src/main/assets to git and push (CI; requires GITHUB_TOKEN)",
    )
    args = p.parse_args()

    eve = args.eve_sde_root.resolve()

    if not args.dry_run and not args.yes:
        print("Refusing to modify disk without --yes (wipes eve_sde root per run).", file=sys.stderr)
        print("Re-run with:  python tools/sync_sde_release.py --yes", file=sys.stderr)
        return 2

    release: dict | None = None
    tag: str = "?"
    release_title = ""
    sde_url: str | None = None
    icons_url: str | None = None

    try:
        release = http_json(GITHUB_API_LATEST)
    except urllib.error.HTTPError as e:
        if e.code in (403, 429):
            print(
                f"[i] GitHub API HTTP {e.code} (rate limit). "
                f"Resolving tag via {GITHUB_HTML_LATEST!r} instead.",
                file=sys.stderr,
            )
        else:
            print(f"Failed to fetch GitHub latest release: HTTP {e.code}: {e}", file=sys.stderr)
            return 1
    except (urllib.error.URLError, TimeoutError, json.JSONDecodeError) as e:
        print(
            f"[i] GitHub API unreachable ({e!r}). "
            f"Resolving tag via {GITHUB_HTML_LATEST!r} instead.",
            file=sys.stderr,
        )

    if release is not None:
        tag = str(release.get("tag_name", "?"))
        release_title = str(release.get("name", "") or "")
        sde_url, icons_url, metadata_url = pick_asset_urls(release)
    else:
        try:
            tag = resolve_tag_from_releases_page()
            sde_url, icons_url, metadata_url = direct_asset_urls(tag)
        except SystemExit:
            raise
        except (urllib.error.URLError, TimeoutError, OSError) as e2:
            print(f"Failed to resolve latest release without API: {e2}", file=sys.stderr)
            print("Optional: set GITHUB_TOKEN or GH_TOKEN for higher GitHub API limits.", file=sys.stderr)
            return 1

    assert sde_url is not None and icons_url is not None

    print(f"[+] Latest release: {tag} ({release_title})")

    remote_cmp = remote_meta_for_compare(metadata_url, release, tag)
    published_meta = load_pyerite_github_release_meta()
    bundled_meta = load_app_release_meta()
    print(
        "[+] Upstream (EveSDE): "
        f"build_number={remote_cmp.get('build_number')!r} "
        f"icon_version={remote_cmp.get('icon_version', 'n/a')!r} "
        f"release_date={remote_cmp.get('release_date', '')!r}"
    )
    if published_meta:
        print(
            "[+] Pyerite Release: "
            f"build_number={published_meta.get('build_number')!r} "
            f"release_date={published_meta.get('release_date', '')!r}"
        )
    else:
        print("[+] Pyerite Release: (none yet)")
    if bundled_meta:
        print(
            "[+] Bundled assets:  "
            f"build_number={bundled_meta.get('build_number')!r} "
            f"release_date={bundled_meta.get('release_date', '')!r}"
        )
    else:
        print("[+] Bundled assets:  (missing)")

    if args.dry_run:
        print("[dry-run] Would wipe (if update):", eve)
        print("[dry-run] Would download:", ASSET_SDE, "and icons zip (only if update)")
        print("[dry-run] Would normalize + copy:", APP_DB_ZH.name, "and", APP_DB_EN.name)
        print("[dry-run] upstream (EveSDE):", remote_cmp)
        print("[dry-run] published (Pyerite Release):", published_meta)
        print("[dry-run] bundled (assets):", bundled_meta)
        print(
            "[dry-run] would_sync_upstream:",
            args.force or remote_is_newer(remote_cmp, published_meta),
        )
        print("[dry-run] would_refresh_bundled:", bundled_behind_published(published_meta, bundled_meta))
        return 0

    needs_upstream_sync = args.force or remote_is_newer(remote_cmp, published_meta)
    if not needs_upstream_sync:
        print("[=] Upstream is not newer than Pyerite GitHub Release. Skipping EveSDE download.")
        print("    Upstream:", version_key(remote_cmp), "Published:", version_key(published_meta or {}))
        if bundled_behind_published(published_meta, bundled_meta):
            print("[+] Bundled assets are behind published Release; refreshing from Pyerite Release ...")
            if not refresh_bundled_from_published_release():
                return 1
            if args.commit_assets and published_meta:
                commit_bundled_assets(str(published_meta.get("build_number", "?")))
        if args.publish_release or args.commit_assets:
            return finalize_from_existing_bundled(published_meta, bundled_meta, remote_cmp, args)
        return 0

    print("[+] Upstream newer than Pyerite Release (or --force); downloading SDE and icons ...")
    print(f"[+] Wiping {eve} ...")
    wipe_eve_sde_root(eve)

    sde_zip = eve / ASSET_SDE
    icons_zip = eve / ASSET_ICONS_PRIMARY
    print(f"[+] Downloading {ASSET_SDE} ...")
    download_file(sde_url, sde_zip)
    print(f"[+] Downloading icons ...")
    download_file(icons_url, icons_zip)

    print(f"[+] Extracting {ASSET_SDE} ...")
    with zipfile.ZipFile(sde_zip) as zf:
        zf.extractall(eve)

    remote_zh_db, remote_en_db, remote_latest = find_remote_dbs_and_meta(eve)

    remote_meta = load_latest(remote_latest)
    if not remote_meta:
        raise SystemExit(f"Empty or invalid: {remote_latest}")

    print("[+] Newer SDE detected; normalizing BOOLEAN -> INTEGER for Room ...")
    APP_DB_DIR.mkdir(parents=True, exist_ok=True)
    for remote_db, app_db in (
        (remote_zh_db, APP_DB_ZH),
        (remote_en_db, APP_DB_EN),
    ):
        r = normalize_and_replace_db(remote_db, app_db)
        if r != 0:
            return r

    APP_ASSETS_DIR.mkdir(parents=True, exist_ok=True)
    if LEGACY_APP_LATEST.is_file():
        LEGACY_APP_LATEST.unlink()
    merged = merge_meta_for_bundle(remote_cmp, remote_meta)
    latest_text = json.dumps(merged, indent=2) + "\n"
    APP_LATEST.write_text(latest_text, encoding="utf-8")
    print(f"[+] Wrote {APP_LATEST}")

    replace_asset(icons_zip, APP_ICONS_ZIP)
    print(f"[+] Replaced {APP_ICONS_ZIP}")

    publish_meta = build_pyerite_release_meta(merged)
    return finalize_publish_and_commit(publish_meta, args)


def bundled_assets_ready() -> bool:
    return all(
        path.is_file()
        for path in (APP_DB_ZH, APP_DB_EN, APP_ICONS_ZIP, APP_LATEST)
    )


def expected_release_asset_names() -> tuple[str, ...]:
    return (
        PYERITE_RELEASE_ASSET_META,
        PYERITE_RELEASE_ASSET_DB_ZH,
        PYERITE_RELEASE_ASSET_DB_EN,
        PYERITE_RELEASE_ASSET_ICONS,
    )


def release_has_all_assets(release: dict) -> bool:
    names = {
        a.get("name")
        for a in (release.get("assets") or [])
        if isinstance(a, dict)
    }
    return all(name in names for name in expected_release_asset_names())


def load_or_build_publish_meta(source_meta: dict) -> dict:
    if APP_LATEST_JSON.is_file():
        cached = load_latest(APP_LATEST_JSON)
        if cached and str(cached.get("build_number")) == str(source_meta.get("build_number")):
            return cached
    return build_pyerite_release_meta(source_meta)


def finalize_publish_and_commit(publish_meta: dict, args: argparse.Namespace) -> int:
    APP_LATEST_JSON.write_text(json.dumps(publish_meta, indent=2) + "\n", encoding="utf-8")
    print(f"[+] Wrote {APP_LATEST_JSON}")
    if args.publish_release:
        publish_pyerite_github_release(publish_meta)
    if args.commit_assets:
        commit_bundled_assets(str(publish_meta.get("build_number", "?")))
    return 0


def finalize_from_existing_bundled(
    published_meta: dict | None,
    bundled_meta: dict | None,
    remote_cmp: dict,
    args: argparse.Namespace,
) -> int:
    if not bundled_assets_ready():
        if published_meta and refresh_bundled_from_published_release():
            bundled_meta = load_app_release_meta()
        if not bundled_assets_ready():
            print(
                "[!] Bundled assets are incomplete under app/src/main/assets; "
                "cannot publish GitHub Release.",
                file=sys.stderr,
            )
            return 1
    source_meta = published_meta or bundled_meta or remote_cmp
    publish_meta = load_or_build_publish_meta(source_meta)
    print("[+] Publishing GitHub Release from current bundled assets ...")
    return finalize_publish_and_commit(publish_meta, args)


def pyerite_repo_slug() -> str:
    return os.environ.get("GITHUB_REPOSITORY", "Lalala-Marshall/Pyerite")


def pyerite_release_tag(build_number: str) -> str:
    return f"sde-build-{build_number}"


def build_pyerite_release_meta(merged: dict) -> dict:
    bn = str(merged.get("build_number") or "0")
    tag = pyerite_release_tag(bn)
    repo = pyerite_repo_slug()
    base = f"https://github.com/{repo}/releases/download/{urllib.parse.quote(tag, safe='')}"
    out = dict(merged)
    out["tag_name"] = tag
    out["assets_base_url"] = f"{base}/"
    out["assets"] = {
        PYERITE_RELEASE_ASSET_META: f"{base}/{PYERITE_RELEASE_ASSET_META}",
        PYERITE_RELEASE_ASSET_DB_ZH: f"{base}/{PYERITE_RELEASE_ASSET_DB_ZH}",
        PYERITE_RELEASE_ASSET_DB_EN: f"{base}/{PYERITE_RELEASE_ASSET_DB_EN}",
        PYERITE_RELEASE_ASSET_ICONS: f"{base}/{PYERITE_RELEASE_ASSET_ICONS}",
    }
    return out


def _github_repo_api_base() -> str:
    return f"https://api.github.com/repos/{pyerite_repo_slug()}"


def _require_github_token() -> str:
    token = os.environ.get("GITHUB_TOKEN") or os.environ.get("GH_TOKEN")
    if not token:
        raise SystemExit("GITHUB_TOKEN or GH_TOKEN is required for --publish-release / --commit-assets")
    return token


def _github_post_json(url: str, payload: dict, token: str) -> dict:
    data = json.dumps(payload).encode("utf-8")
    req = urllib.request.Request(
        url,
        data=data,
        headers={
            **_github_auth_headers(),
            "Authorization": f"Bearer {token}",
            "Content-Type": "application/json",
        },
        method="POST",
    )
    with urllib.request.urlopen(req, timeout=120) as resp:
        return json.loads(resp.read().decode("utf-8"))


def _github_get_json(url: str, token: str) -> dict | list | None:
    req = urllib.request.Request(
        url,
        headers={
            **_github_auth_headers(),
            "Authorization": f"Bearer {token}",
        },
    )
    try:
        with urllib.request.urlopen(req, timeout=120) as resp:
            return json.loads(resp.read().decode("utf-8"))
    except urllib.error.HTTPError as e:
        if e.code == 404:
            return None
        raise


def _github_upload_release_asset(upload_url_template: str, file_path: Path, token: str) -> None:
    upload_url = upload_url_template.split("{", 1)[0]
    name = urllib.parse.quote(file_path.name)
    url = f"{upload_url}?name={name}"
    body = file_path.read_bytes()
    req = urllib.request.Request(
        url,
        data=body,
        headers={
            "User-Agent": USER_AGENT,
            "Authorization": f"Bearer {token}",
            "Accept": "application/vnd.github+json",
            "Content-Type": "application/octet-stream",
            "Content-Length": str(len(body)),
        },
        method="POST",
    )
    try:
        with urllib.request.urlopen(req, timeout=1800) as resp:
            resp.read()
    except urllib.error.HTTPError as e:
        err_body = e.read().decode("utf-8", errors="replace")
        raise SystemExit(
            f"Upload failed for {file_path.name}: HTTP {e.code}: {err_body}"
        ) from e


def publish_pyerite_github_release(publish_meta: dict) -> None:
    token = _require_github_token()
    tag = str(publish_meta.get("tag_name") or pyerite_release_tag(str(publish_meta.get("build_number", "0"))))
    bn = str(publish_meta.get("build_number", "?"))
    api = _github_repo_api_base()

    release = _github_get_json(f"{api}/releases/tags/{urllib.parse.quote(tag, safe='')}", token)
    if release is None:
        body = (
            f"Processed EVE SDE build **{bn}** for Pyerite (Room-normalized SQLite + icons).\n\n"
            f"```json\n{json.dumps(publish_meta, indent=2)}\n```"
        )
        release = _github_post_json(
            f"{api}/releases",
            {
                "tag_name": tag,
                "name": f"SDE build {bn}",
                "body": body,
                "draft": False,
                "prerelease": False,
            },
            token,
        )
        print(f"[+] Created GitHub release {tag}")
    else:
        print(f"[=] GitHub release {tag} already exists; refreshing assets")

    release_id = release.get("id")
    if release_id:
        refreshed = _github_get_json(f"{api}/releases/{release_id}", token)
        if isinstance(refreshed, dict):
            release = refreshed

    if release_has_all_assets(release) and not os.environ.get("SDE_FORCE_REPUBLISH"):
        print(f"[=] GitHub release {tag} already has all assets; skipping upload")
        print("    Set SDE_FORCE_REPUBLISH=1 to replace assets anyway.")
        return

    upload_url = str(release.get("upload_url") or "")
    if not upload_url:
        raise SystemExit("GitHub release response missing upload_url")

    assets = {
        a.get("name"): a.get("id")
        for a in (release.get("assets") or [])
        if isinstance(a, dict)
    }
    for name, path in (
        (PYERITE_RELEASE_ASSET_META, APP_LATEST_JSON),
        (PYERITE_RELEASE_ASSET_DB_ZH, APP_DB_ZH),
        (PYERITE_RELEASE_ASSET_DB_EN, APP_DB_EN),
        (PYERITE_RELEASE_ASSET_ICONS, APP_ICONS_ZIP),
    ):
        if not path.is_file():
            raise SystemExit(f"Missing release asset file: {path}")
        asset_id = assets.get(name)
        if asset_id:
            delete_req = urllib.request.Request(
                f"{api}/releases/assets/{asset_id}",
                headers={
                    "User-Agent": USER_AGENT,
                    "Authorization": f"Bearer {token}",
                    "Accept": "application/vnd.github+json",
                },
                method="DELETE",
            )
            with urllib.request.urlopen(delete_req, timeout=120):
                pass
        print(f"[+] Uploading {name} ...")
        _github_upload_release_asset(upload_url, path, token)
    print(f"[+] Published assets to GitHub release {tag}")


def commit_bundled_assets(build_number: str) -> None:
    token = _require_github_token()
    repo = pyerite_repo_slug()
    branch = os.environ.get("GITHUB_REF_NAME", "main")

    subprocess.run(["git", "config", "user.name", "github-actions[bot]"], cwd=REPO_ROOT, check=True)
    subprocess.run(
        ["git", "config", "user.email", "github-actions[bot]@users.noreply.github.com"],
        cwd=REPO_ROOT,
        check=True,
    )
    subprocess.run(["git", "add", str(APP_ASSETS_DIR)], cwd=REPO_ROOT, check=True)
    diff = subprocess.run(["git", "diff", "--cached", "--quiet"], cwd=REPO_ROOT)
    if diff.returncode == 0:
        print("[=] No bundled asset changes to commit")
        return

    subprocess.run(
        ["git", "commit", "-m", f"chore(sde): bundle SDE build {build_number}"],
        cwd=REPO_ROOT,
        check=True,
    )
    remote = f"https://x-access-token:{token}@github.com/{repo}.git"
    subprocess.run(["git", "push", remote, f"HEAD:{branch}"], cwd=REPO_ROOT, check=True)
    print(f"[+] Committed and pushed bundled assets to {branch}")


if __name__ == "__main__":
    raise SystemExit(main())
