#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
SDK_ROOT="${ANDROID_SDK_ROOT:-${ANDROID_HOME:-$HOME/Library/Android/sdk}}"
ANDROID_API="${ANDROID_API:-26}" # minSdk=26 for MVP (Android 8.0)

resolve_ndk_dir() {
  # 1) Explicit env var (recommended)
  for v in ANDROID_NDK_ROOT ANDROID_NDK_HOME NDK_HOME NDK_ROOT; do
    local p="${!v:-}"
    if [[ -n "${p}" && -d "${p}" ]]; then
      echo "${p}"
      return 0
    fi
  done

  # 2) Typical SDK layout: $ANDROID_SDK_ROOT/ndk/<version>
  if [[ -d "${SDK_ROOT}/ndk" ]]; then
    local best=""
    local d=""
    # shellcheck disable=SC2012
    for d in "${SDK_ROOT}/ndk/"*; do
      [[ -d "${d}" ]] || continue
      [[ -d "${d}/toolchains/llvm/prebuilt" ]] || continue
      best="${best}"$'\n'"${d}"
    done
    if [[ -n "${best//$'\n'/}" ]]; then
      # pick the last directory by version-like sort
      echo "${best}" | sed '/^$/d' | sort -V | tail -n 1
      return 0
    fi
  fi

  return 1
}

NDK_DIR="$(resolve_ndk_dir || true)"
if [[ -z "${NDK_DIR}" || ! -d "${NDK_DIR}" ]]; then
  echo "Android NDK not found."
  echo "Please install Android NDK manually, then set one of:"
  echo "  - ANDROID_NDK_ROOT=/path/to/ndk"
  echo "  - ANDROID_NDK_HOME=/path/to/ndk"
  echo "  - NDK_HOME=/path/to/ndk"
  echo "Or install via Android SDK Manager so it's under: ${SDK_ROOT}/ndk/<version>"
  exit 1
fi

TOOLCHAIN_BIN="$(ls -d "${NDK_DIR}/toolchains/llvm/prebuilt/"*/bin 2>/dev/null | head -n 1 || true)"
CC="${TOOLCHAIN_BIN}/aarch64-linux-android${ANDROID_API}-clang"
CXX="${TOOLCHAIN_BIN}/aarch64-linux-android${ANDROID_API}-clang++"

if [[ ! -x "${CC}" ]]; then
  echo "clang not found: ${CC}"
  exit 1
fi

SRC_DIR="${ROOT_DIR}/third_party/sunnynet-src"
OUT_DIR="${ROOT_DIR}/android/app/src/main/jniLibs/arm64-v8a"
OUT_SO="${OUT_DIR}/libsunnynet.so"

mkdir -p "${OUT_DIR}"
mkdir -p "${ROOT_DIR}/third_party"

if [[ ! -d "${SRC_DIR}/.git" ]]; then
  echo "Cloning SunnyNet source (depth=1)..."
  git clone --depth 1 "https://github.com/qtgolang/SunnyNet.git" "${SRC_DIR}"
else
  echo "Updating SunnyNet source..."
  (cd "${SRC_DIR}" && git fetch --depth 1 origin main && git reset --hard origin/main)
fi

echo "Building libsunnynet.so (android/arm64, minSdk=26)..."
(cd "${SRC_DIR}" && \
  env \
    CGO_ENABLED=1 \
    GOOS=android \
    GOARCH=arm64 \
    CC="${CC}" \
    CXX="${CXX}" \
    CGO_LDFLAGS="-llog" \
    go build -mod=mod -trimpath -buildmode=c-shared -o "${OUT_SO}" \
)

echo "Built: ${OUT_SO}"
ls -la "${OUT_SO}"

