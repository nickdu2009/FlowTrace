# Android 工程说明

**状态**：Gradle 构建系统已就绪（MVP 接口与模块骨架）

## 模块结构

- `core/domain`：领域模型与端口（纯 Kotlin，不依赖 Android SDK）
- `core/application`：用例编排（纯 Kotlin）
- `core/infra/sunnynet`：SunnyNet JNI 适配层（Android Library）
- `core/infra/storage`：Room + 文件 body 存储（Android Library）
- `app`：UI（Compose）+ DI（Hilt）+ 权限/服务编排（Android Application）

## 前置条件（必须）

### Java 17

当前系统使用 Java 25，但 Gradle 8.5 内嵌的 Kotlin 无法识别。**必须安装 Java 17**：

```bash
# macOS (Homebrew)
brew install --cask temurin@17

# 验证安装
/usr/libexec/java_home -V

# 设置 JAVA_HOME（临时，针对本次构建）
export JAVA_HOME=`/usr/libexec/java_home -v 17`

# 或在 ~/.zshrc 中永久设置：
export JAVA_HOME=`/usr/libexec/java_home -v 17`
```

##构建与运行

```bash
# 验证构建系统
./gradlew tasks

# 编译所有模块
./gradlew build

# 编译并安装 Debug APK
./gradlew installDebug

# 清理构建产物
./gradlew clean
```

## 技术栈（已配置）

- **Kotlin**: 2.0.21
- **Gradle**: 8.5
- **AGP**: 8.7.3
- **Compose BOM**: 2024.12.01
- **Hilt**: 2.54
- **Room**: 2.6.1
- **Coroutines**: 1.10.1
- **Min SDK**: 26 (Android 8.0)
- **Target/Compile SDK**: 34

## 下一步（实现里程碑 m2-m5）

### M2：Shared Core (KMP) 初始化
- [ ] 将 `core/domain` + `core/application` 迁移到 `shared/` KMP 模块
- [ ] 配置 KMP targets（jvm / iosArm64 / iosSimulatorArm64）

### M3：Android VpnService 骨架
- [ ] 实现 `VpnService` 子类与生命周期管理
- [ ] 前台服务通知 + 权限申请流程
- [ ] VPN 建立 TUN fd

### M4：SunnyNet JNI Bridge 实现
- [ ] 补齐 JNI native 方法声明（`.so` 加载与调用）
- [ ] SunnyNet 回调适配为 Domain `CaptureEvent`
- [ ] 事件进入有界队列（背压策略）

### M5：事件管线打通
- [ ] SessionAggregator 实现（幂等合并、乱序容忍）
- [ ] 批处理落盘 Worker（Room + Body 文件）
- [ ] UI 观察会话摘要流（Compose 列表）

详见：`doc/MVP_IMPLEMENTATION_PLAN.md`

## 工程约束（必须遵守）

参见根目录 `AGENTS.md` 与 `doc/`：
- Clean Architecture 分层边界（Domain 不依赖 Android/JNI）
- 默认脱敏（入库 & 导出）
- JNI 回调不做重活（只进队列）
- Body 分离存储（DB 只存 `BodyRef`）

## 疑难排查

### Java 版本错误

**现象**：`./gradlew` 报错 `25.0.1` 或 `IllegalArgumentException`

**原因**：Gradle 内嵌 Kotlin 不识别 Java 25

**解决**：安装并切换到 Java 17（见"前置条件"）

### Gradle Daemon 卡死

```bash
./gradlew --stop
rm -rf ~/.gradle/caches
./gradlew --refresh-dependencies
```

### 模块依赖冲突

检查 `libs.versions.toml` 版本号一致性，重新同步：

```bash
./gradlew --refresh-dependencies build
```
