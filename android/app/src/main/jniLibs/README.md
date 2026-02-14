## SunnyNet native libraries (jniLibs)

将 SunnyNet 的 `.so` 放到以下目录（按 ABI）：

- `android/app/src/main/jniLibs/arm64-v8a/libsunnynet.so`

MVP 默认以 `arm64-v8a` 为主（见 `AGENTS.md` / Native 约束）。

> 注意：
> - `.so` 属于二进制大文件，且可能包含授权内容，默认 **不要提交到仓库**。
> - 本仓库的 `SunnyNetJniBridge` 默认尝试 `System.loadLibrary("sunnynet")`；
>   如果没有找到库，会自动回退到 Fake 引擎（不影响 UI/管线验证）。

