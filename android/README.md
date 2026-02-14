# Android 工程说明

**状态**：Gradle 构建系统已就绪（MVP 接口与模块骨架）

## 模块结构

- `core/domain`：领域模型与端口（纯 Kotlin，不依赖 Android SDK）
- `core/application`：用例编排（纯 Kotlin）
- `core/infra/sunnynet`：SunnyNet JNI 适配层（Android Library）
- `core/infra/storage`：Room + 文件 body 存储（Android Library）
- `app`：UI（Compose）+ DI（Hilt）+ 权限/服务编排（Android Application）

## 前置条件（必须）

### JDK（建议 17+，当前已验证 25）

本工程默认以 **JDK 17+** 为基线（Android/Gradle 生态最常见）。

当前仓库已完成对 **JDK 25（Java 25.0.1）** 的构建验证：通过 **Gradle 9.1 + Kotlin 2.3.0** 配置即可正常编译。

```bash
# 查看本机已安装 JDK
/usr/libexec/java_home -V

# 设置 JAVA_HOME（临时，针对本次构建）
export JAVA_HOME=`/usr/libexec/java_home -v 17`

# 或在 ~/.zshrc 中永久设置：
export JAVA_HOME=`/usr/libexec/java_home -v 17`
```

> 备注：如果你希望继续使用系统自带/已安装的 **JDK 25**，无需切换 JAVA_HOME。

## 构建与运行

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

- **Kotlin**: 2.3.0
- **Gradle Wrapper**: 9.1.0
- **AGP**: 8.7.3
- **Compose BOM**: 2024.12.01
- **Hilt**: 2.57.2
- **Room**: 2.6.1
- **Coroutines**: 1.10.1
- **Min SDK**: 26 (Android 8.0)
- **Target SDK**: 34
- **Compile SDK**: 35

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

**现象**：`./gradlew` 报错 `IllegalArgumentException: 25.0.1`

**原因**：在较旧 Gradle/Kotlin 组合下，Kotlin 编译器（daemon）会在解析 Java 25 版本字符串时异常。

**解决**：
- 本仓库当前已升级到 **Gradle 9.1 + Kotlin 2.3.0**，并通过 `kotlin.compiler.execution.strategy=in-process` 规避 daemon 启动问题
- 若你回退到旧版本（例如 Gradle 8.5），请切换到 **JDK 17**（见“前置条件”）

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
