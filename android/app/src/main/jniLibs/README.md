## SunnyNet native libraries (jniLibs)

将 SunnyNet 的 `.so` 放到以下目录（按 ABI）：

- `android/app/src/main/jniLibs/arm64-v8a/libsunnynet.so`

MVP 默认以 `arm64-v8a` 为主（见 `AGENTS.md` / Native 约束）。

推荐使用仓库脚本生成（从官方仓库拉取源码构建；**NDK/SKD 需要你自行安装**）：

```bash
# 推荐：显式指定 NDK 路径
export ANDROID_NDK_ROOT="$HOME/Library/Android/sdk/ndk/<your-ndk-version>"

bash scripts/build_sunnynet_android_arm64.sh
```

> 注意：
> - `.so` 属于二进制大文件，且可能包含授权内容，默认 **不要提交到仓库**。
> - 本仓库的 `SunnyNetJniBridge` 默认尝试 `System.loadLibrary("sunnynet")`；
>   如果没有找到库，会自动回退到 Fake 引擎（不影响 UI/管线验证）。

