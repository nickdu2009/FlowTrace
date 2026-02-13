# Android 工程骨架说明

本目录仅包含 **MVP 的接口与模块骨架（纯文本源码）**，暂未引入 Gradle Wrapper（二进制 jar 不适合在当前阶段生成）。

## 模块结构（预期）

- `core/domain`：领域模型与端口（不依赖 Android）
- `core/application`：用例（不依赖 Android）
- `core/infra/sunnynet`：SunnyNet JNI 适配层（Android 相关）
- `core/infra/storage`：Room + 文件 body 存储（Android 相关）
- `app`：UI（Compose）与 DI、权限/服务编排

## 下一步（当你准备开始跑起来）

- 初始化 Android Gradle 工程（Android Studio 或手工）
- 将各模块接入 `settings.gradle(.kts)` 并配置依赖
- 在 `core/infra/sunnynet` 中补齐 JNI binding（调用 SunnyNet 的 Java API）
- 在 `app` 中实现 `VpnService` 与前台服务通知

