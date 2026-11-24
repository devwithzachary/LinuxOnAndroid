#!/usr/bin/env python3
"""
Real Android Emulator (x86_64 KVM) & Physical Device (ARM64) E2E Test Runner.
Builds debug + androidTest APKs, installs them onto the target, and runs
DistroAndPackageE2ETest (fresh uncached distro download, 1-click install,
CLI smoke check, XFCE Desktop menu + VNC GUI launch, and uninstall).
"""

import argparse
import os
import shutil
import subprocess
import sys
import time
from pathlib import Path

ROOT_DIR = Path(__file__).resolve().parent.parent
SDK_DIR = Path(os.environ.get("ANDROID_HOME", "/home/zachary/Android/Sdk"))
ADB_BIN = shutil.which("adb") or str(SDK_DIR / "platform-tools" / "adb")
EMULATOR_BIN = shutil.which("emulator") or str(SDK_DIR / "emulator" / "emulator")
DEFAULT_AVD = "LoA_x86_64_E2E"
TEST_PACKAGE = "com.devwithzachary.completelinuxinstaller.test"
TEST_CLASS = "com.devwithzachary.completelinuxinstaller.e2e.DistroAndPackageE2ETest"


def run(cmd, check=True, capture=False):
    return subprocess.run(
        cmd,
        cwd=ROOT_DIR,
        check=check,
        text=True,
        stdout=subprocess.PIPE if capture else None,
        stderr=subprocess.STDOUT if capture else None,
    )


def list_adb_devices() -> list[tuple[str, str]]:
    res = run([ADB_BIN, "devices"], capture=True)
    devices = []
    for line in res.stdout.splitlines()[1:]:
        parts = line.strip().split()
        if len(parts) >= 2:
            devices.append((parts[0], parts[1]))
    return devices


def wait_for_emulator_boot(serial: str, timeout_s: int = 180):
    print(f"Waiting for {serial} to finish booting...")
    deadline = time.time() + timeout_s
    run([ADB_BIN, "-s", serial, "wait-for-device"], check=False)
    while time.time() < deadline:
        res = run([ADB_BIN, "-s", serial, "shell", "getprop", "sys.boot_completed"], check=False, capture=True)
        if res.returncode == 0 and res.stdout.strip() == "1":
            print(f"Emulator {serial} booted cleanly.")
            return
        time.sleep(2)
    raise TimeoutError(f"Emulator {serial} did not boot within {timeout_s}s")


def ensure_target_serial(target: str, avd: str, headless: bool) -> str:
    devices = list_adb_devices()
    if target == "device":
        physical = [s for s, st in devices if not s.startswith("emulator-") and st == "device"]
        if not physical:
            print("ERROR: No physical Android device found via adb.", file=sys.stderr)
            sys.exit(1)
        return physical[0]

    emus = [s for s, st in devices if s.startswith("emulator-") and st == "device"]
    for emu in emus:
        name_res = run([ADB_BIN, "-s", emu, "emu", "avd", "name"], check=False, capture=True)
        running_avd = name_res.stdout.splitlines()[0].strip() if name_res.stdout else ""
        if running_avd == avd:
            return emu
        print(f"Stopping wrong AVD '{running_avd}' on {emu} (need '{avd}')...")
        run([ADB_BIN, "-s", emu, "emu", "kill"], check=False)
        time.sleep(6)

    print(f"Starting x86_64 KVM emulator AVD '{avd}'...")
    cmd = [EMULATOR_BIN, "-avd", avd, "-no-audio", "-no-boot-anim", "-gpu", "swiftshader_indirect"]
    if headless:
        cmd.append("-no-window")
    subprocess.Popen(cmd, stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL, start_new_session=True)

    deadline = time.time() + 120
    while time.time() < deadline:
        emus = [s for s, st in list_adb_devices() if s.startswith("emulator-")]
        if emus:
            wait_for_emulator_boot(emus[0])
            return emus[0]
        time.sleep(2)
    raise TimeoutError("Timed out waiting for emulator to appear in adb devices")


def main():
    parser = argparse.ArgumentParser(description="Run real Android E2E tests on x86_64 emulator or ARM64 device")
    parser.add_argument("--target", choices=["emulator", "device", "both"], default="emulator")
    parser.add_argument("--avd", default=DEFAULT_AVD, help=f"AVD name (default: {DEFAULT_AVD})")
    parser.add_argument("--headless", action="store_true", help="Boot emulator with -no-window")
    parser.add_argument("--distro", default="ubuntu_26_04", help="Distro ID, comma-separated IDs, or 'all'")
    parser.add_argument("--package", default="all", help="Package ID, comma-separated IDs, or 'all'")
    parser.add_argument("--no-gui", action="store_true", help="Skip XFCE Desktop / VNC GUI app verification")
    parser.add_argument("--reuse-container", action="store_true", help="Reuse existing e2e_<distro> container if present")
    parser.add_argument("--skip-uninstall", action="store_true", help="Keep container and installed packages after test for manual testing")
    parser.add_argument("--skip-build", action="store_true", help="Skip ./gradlew assembleDebug assembleDebugAndroidTest")
    args = parser.parse_args()

    if not args.skip_build:
        print("=== Building Debug & AndroidTest APKs ===")
        run(["./gradlew", "assembleDebug", "assembleDebugAndroidTest"])

    targets = ["emulator", "device"] if args.target == "both" else [args.target]
    debug_apk = ROOT_DIR / "app" / "build" / "outputs" / "apk" / "debug" / "app-debug.apk"
    test_apk = ROOT_DIR / "app" / "build" / "outputs" / "apk" / "androidTest" / "debug" / "app-debug-androidTest.apk"
    gui_flag = "false" if args.no_gui else "true"

    all_passed = True
    for tgt in targets:
        serial = ensure_target_serial(tgt, args.avd, args.headless)
        arch = run([ADB_BIN, "-s", serial, "shell", "getprop", "ro.product.cpu.abi"], capture=True).stdout.strip()
        print(f"\n=== Target: {serial} (mode={tgt}, ABI={arch}) ===")

        run([ADB_BIN, "-s", serial, "install", "-r", str(debug_apk)])
        run([ADB_BIN, "-s", serial, "install", "-r", str(test_apk)])

        print(f"=== Running E2E Instrumentation (distro={args.distro}, pkg={args.package}, gui={gui_flag}) ===")
        run([ADB_BIN, "-s", serial, "shell", "svc", "power", "stayon", "true"], check=False)
        run([ADB_BIN, "-s", serial, "shell", "input", "keyevent", "KEYCODE_WAKEUP"], check=False)
        run([ADB_BIN, "-s", serial, "logcat", "-c"], check=False)
        logcat_proc = subprocess.Popen(
            [ADB_BIN, "-s", serial, "logcat", "-v", "time", "-s",
             "DistroAndPackageE2E:*", "RootfsManager:*", "SoftwareInstaller:*", "TestRunner:*"],
            cwd=ROOT_DIR,
            text=True,
        )
        instr_cmd = [
            ADB_BIN, "-s", serial, "shell", "am", "instrument", "-w",
            "-e", "distro", args.distro,
            "-e", "pkg", args.package,
            "-e", "gui", gui_flag,
            "-e", "reuseContainer", "true" if args.reuse_container else "false",
            "-e", "skipUninstall", "true" if args.skip_uninstall else "false",
            "-e", "class", TEST_CLASS,
            f"{TEST_PACKAGE}/androidx.test.runner.AndroidJUnitRunner",
        ]
        try:
            res = run(instr_cmd, check=False, capture=True)
        finally:
            logcat_proc.terminate()
            logcat_proc.wait(timeout=5)
            run([ADB_BIN, "-s", serial, "shell", "svc", "power", "stayon", "false"], check=False)
        print(res.stdout)
        passed = ("OK (" in res.stdout) and ("FAILURES!!!" not in res.stdout) and ("Process crashed" not in res.stdout)
        if not passed:
            all_passed = False
            print(f"=== Logcat Failure Diagnostics ({serial}) ===")
            log_res = run([ADB_BIN, "-s", serial, "logcat", "-d", "-s", "AndroidRuntime:E", "DistroAndPackageE2E:*", "TestRunner:*"], check=False, capture=True)
            print("\n".join(log_res.stdout.splitlines()[-60:]))
    sys.exit(0 if all_passed else 1)


if __name__ == "__main__":
    main()

