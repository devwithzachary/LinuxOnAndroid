#!/usr/bin/env python3
"""
Headless Docker-based 1-Click Package & Base Image Verification Matrix.
Reads app/build/package_matrix.json (exported from DistroCatalog) and tests
first-launch setup + all 9 1-click packages across all 7 distributions.
"""

import argparse
import json
import os
import re
import shutil
import subprocess
import sys
import tempfile
import urllib.error
import urllib.request
from pathlib import Path

ROOT_DIR = Path(__file__).resolve().parent.parent
MATRIX_PATH = ROOT_DIR / "app" / "build" / "package_matrix.json"
CACHE_DIR = Path("/tmp/loa_rootfs_cache")

BINARY_ALIASES = {
    "usr/bin/vncserver": ["usr/bin/vncserver", "usr/bin/tigervncserver", "usr/bin/Xvnc", "usr/bin/Xtigervnc"],
    "usr/sbin/sshd": ["usr/sbin/sshd", "usr/bin/sshd"],
    "usr/bin/sshd": ["usr/bin/sshd", "usr/sbin/sshd"],
    "usr/sbin/nginx": ["usr/sbin/nginx", "usr/bin/nginx"],
    "usr/bin/nginx": ["usr/bin/nginx", "usr/sbin/nginx"],
    "usr/bin/code-server": ["usr/bin/code-server", "usr/local/bin/code-server", "usr/lib/code-server/bin/code-server"],
    "usr/bin/ttyd": ["usr/bin/ttyd", "usr/local/bin/ttyd"],
    "usr/bin/docker": ["usr/bin/docker", "usr/local/bin/docker", "usr/local/bin/udocker", "usr/bin/udocker"],
    "usr/local/bin/docker": ["usr/bin/docker", "usr/local/bin/docker", "usr/local/bin/udocker", "usr/bin/udocker"],
    "usr/bin/redis-server": ["usr/bin/redis-server", "usr/sbin/redis-server"],
    "usr/bin/go": ["usr/bin/go", "usr/local/go/bin/go", "usr/lib/go/bin/go"],
    "usr/bin/zsh": ["usr/bin/zsh", "bin/zsh"],
    "usr/bin/iperf3": ["usr/bin/iperf3", "usr/sbin/iperf3"],
    "usr/bin/firefox": ["usr/bin/firefox", "usr/bin/firefox-esr"],
    "usr/bin/mariadb": ["usr/bin/mariadb", "usr/bin/mysql"],
}


def run(cmd, check=True, capture=False):
    return subprocess.run(
        cmd,
        check=check,
        text=True,
        stdout=subprocess.PIPE if capture else None,
        stderr=subprocess.STDOUT if capture else None,
    )


def ensure_matrix():
    if not MATRIX_PATH.exists():
        print("Exporting package_matrix.json from DistroCatalog...")
        env = os.environ.copy()
        env["EXPORT_PACKAGE_MATRIX"] = "true"
        subprocess.run(
            ["./gradlew", "testDebugUnitTest", "--tests", "*.PackageMatrixExporterTest", "--rerun-tasks"],
            cwd=ROOT_DIR,
            env=env,
            check=True,
        )
    return json.loads(MATRIX_PATH.read_text())


def download_rootfs(distro_id: str, url: str) -> Path:
    CACHE_DIR.mkdir(parents=True, exist_ok=True)
    ext = ".tar.gz"
    if url.endswith(".tar.xz"):
        ext = ".tar.xz"
    elif url.endswith(".tar.zst"):
        ext = ".tar.zst"
    elif url.endswith(".wsl"):
        ext = ".wsl"
    dest = CACHE_DIR / f"{distro_id}-amd64{ext}"
    if dest.exists() and dest.stat().st_size > 0:
        return dest
    print(f"  [DL] Downloading {url} -> {dest}")
    req = urllib.request.Request(url, headers={"User-Agent": "LinuxOnAndroid-Test/1.0"})
    with urllib.request.urlopen(req, timeout=120) as resp, open(dest, "wb") as out:
        shutil.copyfileobj(resp, out)
    return dest


def build_base_image(distro: dict) -> str:
    distro_id = distro["id"]
    image_tag = f"loa-test-{distro_id}:latest"
    archive = download_rootfs(distro_id, distro["amd64Url"])

    with tempfile.TemporaryDirectory(prefix=f"loa_{distro_id}_") as tmp:
        rootfs = Path(tmp) / "rootfs"
        rootfs.mkdir()
        # Extract inside a root Docker container to preserve all rootfs permissions/symlinks cleanly
        if str(archive).endswith(".tar.zst"):
            extract_cmd = "apk add --no-cache tar zstd >/dev/null 2>&1 && tar --zstd -xf /archive -C /rootfs"
        elif str(archive).endswith(".tar.xz"):
            extract_cmd = "apk add --no-cache tar xz >/dev/null 2>&1 && tar -xJf /archive -C /rootfs"
        else:
            extract_cmd = "tar -xzf /archive -C /rootfs"

        prep_cmd = (
            f"{extract_cmd} && "
            "if [ -d /rootfs/root.x86_64 ]; then mv /rootfs/root.x86_64/* /rootfs/ 2>/dev/null || true; rmdir /rootfs/root.x86_64 2>/dev/null || true; fi && "
            "for kd in /rootfs/kali-*; do if [ -d \"$kd\" ]; then mv \"$kd\"/* /rootfs/ 2>/dev/null || true; rmdir \"$kd\" 2>/dev/null || true; fi; done && "
            "mkdir -p /rootfs/etc /rootfs/tmp && "
            "printf 'nameserver 1.1.1.1\\nnameserver 8.8.8.8\\n' > /rootfs/etc/resolv.conf && "
            "tar -C /rootfs -c . > /tmp_out/rootfs.tar"
        )
        out_dir = Path(tmp) / "out"
        out_dir.mkdir()
        subprocess.run(
            [
                "docker", "run", "--rm", "--platform", "linux/amd64",
                "-v", f"{archive}:/archive:ro",
                "-v", f"{out_dir}:/tmp_out",
                "alpine:latest", "sh", "-c",
                f"mkdir -p /rootfs && {prep_cmd}",
            ],
            check=True,
        )
        with open(out_dir / "rootfs.tar", "rb") as tf:
            subprocess.run(["docker", "import", "-", image_tag], stdin=tf, check=True, stdout=subprocess.DEVNULL)

    # Run first-launch script and commit as post-setup base image
    setup_tag = f"loa-test-{distro_id}:setup"
    cname = f"loa-setup-{distro_id}"
    subprocess.run(["docker", "rm", "-f", cname], stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL)
    shell = "/bin/sh" if distro_id.startswith("alpine") else "/bin/bash"
    setup_cmd = distro["firstLaunchScript"]
    if setup_cmd.strip():
        res = subprocess.run(
            ["docker", "run", "--name", cname, image_tag, shell, "-c", setup_cmd],
            text=True,
            stdout=subprocess.PIPE,
            stderr=subprocess.STDOUT,
        )
        if res.returncode != 0:
            print(f"  [WARN] firstLaunchScript exited {res.returncode} for {distro_id}:\n{res.stdout[-800:]}")
        subprocess.run(["docker", "commit", cname, setup_tag], check=True, stdout=subprocess.DEVNULL)
        subprocess.run(["docker", "rm", "-f", cname], stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL)
        return setup_tag
    return image_tag


def verify_arch_urls(pkg: dict) -> tuple[bool, str]:
    """
    Verify that any architecture-branched download URLs in installCommand
    resolve cleanly (HTTP 200/302) for aarch64/arm64 as well as x86_64, even
    when Docker tests run on an x86_64 host without QEMU arm64 emulation.
    """
    install_cmd = pkg.get("installCommand", "")
    if "uname -m" not in install_cmd and "aarch64" not in install_cmd:
        return True, ""

    # Extract case "$ARCH" in ... esac blocks and evaluate with ARCH=aarch64
    case_matches = re.findall(r'case\s+"?\$ARCH"?\s+in\s+(.*?)\s+esac', install_cmd)
    if not case_matches:
        return True, ""

    urls_to_check = []
    # Find curl/wget URLs that reference shell variables set in the case block
    raw_urls = re.findall(r'https://[^\s"\'<>]+', install_cmd)
    for arch_val in ("aarch64", "x86_64"):
        env_setup = f'ARCH="{arch_val}"; ' + "; ".join(f'case "$ARCH" in {c} esac' for c in case_matches)
        for raw_url in raw_urls:
            if "$" not in raw_url:
                continue
            resolved = subprocess.run(
                ["sh", "-c", f'{env_setup}; echo "{raw_url}"'],
                text=True,
                stdout=subprocess.PIPE,
                stderr=subprocess.PIPE,
            )
            if resolved.returncode == 0:
                url = resolved.stdout.strip()
                if url.startswith("https://") and "$" not in url:
                    urls_to_check.append((arch_val, url))

    for arch_val, url in urls_to_check:
        try:
            req = urllib.request.Request(url, method="HEAD", headers={"User-Agent": "LinuxOnAndroid-Test/1.0"})
            with urllib.request.urlopen(req, timeout=20) as resp:
                if resp.status >= 400:
                    return False, f"Arch URL check failed ({arch_val}): HTTP {resp.status} for {url}"
        except urllib.error.HTTPError as e:
            return False, f"Arch URL check failed ({arch_val}): HTTP {e.code} for {url}"
        except Exception as e:
            return False, f"Arch URL check failed ({arch_val}): {e} for {url}"
    return True, f"Verified {len(urls_to_check)} arch URL(s)"


def test_package(distro: dict, base_image: str, pkg: dict, test_uninstall: bool = False, verify_urls: bool = True) -> tuple[bool, str]:
    distro_id = distro["id"]
    pkg_id = pkg["id"]
    shell = "/bin/sh" if distro_id.startswith("alpine") else "/bin/bash"
    install_cmd = pkg["installCommand"]
    uninstall_cmd = pkg.get("uninstallCommand", "")

    if verify_urls:
        url_ok, url_msg = verify_arch_urls(pkg)
        if not url_ok:
            return False, url_msg

    # Build binary check snippet
    present_checks = []
    removed_checks = []
    for rel_bin in pkg["expectedBinaries"]:
        candidates = BINARY_ALIASES.get(rel_bin, [rel_bin])
        cond = " -o ".join(f"-e /{c} -o -L /{c}" for c in candidates)
        present_checks.append(f"if [ ! \\( {cond} \\) ]; then echo 'MISSING_BIN:{rel_bin}'; exit 42; fi")
        removed_checks.append(f"if [ {cond} ]; then echo 'STILL_PRESENT_BIN:{rel_bin}'; exit 43; fi")
    verify_present_cmd = "; ".join(present_checks)
    verify_removed_cmd = "; ".join(removed_checks)

    if test_uninstall and uninstall_cmd:
        full_cmd = f"( {install_cmd} ) && ( {verify_present_cmd} ) && ( {uninstall_cmd} ) && ( {verify_removed_cmd} )"
    else:
        full_cmd = f"( {install_cmd} ) && ( {verify_present_cmd} )"
    res = subprocess.run(
        ["docker", "run", "--rm", base_image, shell, "-c", full_cmd],
        text=True,
        stdout=subprocess.PIPE,
        stderr=subprocess.STDOUT,
    )
    if res.returncode == 0:
        suffix = " + uninstall verified" if (test_uninstall and uninstall_cmd) else ""
        return True, f"All {len(pkg['expectedBinaries'])} binaries verified{suffix}"
    tail = "\n".join(res.stdout.strip().splitlines()[-12:])
    return False, f"Exit {res.returncode}: {tail}"


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--distro", default="all", help="Distro ID or 'all'")
    parser.add_argument("--package", default="all", help="Package ID or 'all'")
    parser.add_argument("--test-uninstall", action="store_true", help="Also run uninstallCommand and verify binaries are removed")
    parser.add_argument("--skip-arch-urls", action="store_true", help="Skip live HTTP verification of aarch64/x86_64 download URLs")
    args = parser.parse_args()

    matrix = ensure_matrix()
    wanted = {d.strip() for d in args.distro.split(",")}
    distros = [d for d in matrix if args.distro == "all" or d["id"] in wanted]
    if not distros:
        print(f"Unknown distro: {args.distro}")
        sys.exit(1)

    results = []
    for distro in distros:
        print(f"\n=== Preparing {distro['name']} ({distro['id']}) ===")
        base_image = build_base_image(distro)
        pkgs = [p for p in distro["packages"] if args.package == "all" or p["id"] == args.package]
        for pkg in pkgs:
            print(f"  -> Testing {distro['id']} / {pkg['id']}...", flush=True)
            ok, msg = test_package(
                distro,
                base_image,
                pkg,
                test_uninstall=args.test_uninstall,
                verify_urls=not args.skip_arch_urls,
            )
            status = "PASS" if ok else "FAIL"
            print(f"     [{status}] {distro['id']} | {pkg['id']} | {msg if ok else msg.splitlines()[0]}")
            if not ok:
                print(f"     --- Failure tail ---\n{msg}\n     --------------------")
            results.append((distro["id"], pkg["id"], ok, msg))

    passed = sum(1 for _, _, ok, _ in results if ok)
    failed = len(results) - passed
    print(f"\n================ SUMMARY ================")
    print(f"Total: {len(results)} | Passed: {passed} | Failed: {failed}")
    if failed > 0:
        print("\nFailed combinations:")
        for did, pid, ok, _ in results:
            if not ok:
                print(f"  - {did} / {pid}")
        sys.exit(1)


if __name__ == "__main__":
    main()

