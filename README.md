# 飞牛管家 (fnOS Docker Manager)

用 **Compose Multiplatform (Kotlin)** 编写的 iOS 端 Docker 管理器，直连飞牛 NAS（fnOS）的 Docker Engine API，在手机上完成容器、镜像、仓库、网络、存储卷的管理，并支持**可视化创建 docker-compose 项目**一键部署。

## 功能

| 模块 | 能力 |
|---|---|
| 总览 | 服务器信息、容器/镜像/网络/卷统计、快捷操作 |
| 容器 | 列表/搜索、启动/停止/重启/暂停/删除、详情、实时 CPU/内存/网络统计、日志查看、一键运行新容器 |
| 镜像 | 本地镜像列表、拉取（含进度）、搜索、删除、详情（环境变量/端口/标签） |
| 仓库 | 私有仓库登录（等价 docker login）、跨仓库搜索、凭据管理 |
| 网络 | 创建（bridge/macvlan/ipvlan/overlay + 子网/网关）、删除、已连接容器、连接/断开 |
| 存储卷 | 列表、创建、删除、详情 |
| 编排 | **可视化创建 compose 项目**：服务（镜像/端口/存储挂载/环境变量/网络/重启策略/资源限制/依赖）、网络、卷；储存位置快捷选择（飞牛路径预设 + 最近使用）；实时 YAML 预览/复制；导入已有 YAML；一键部署（建卷 → 建网络 → 拉镜像 → 按依赖建容器 → 启动） |

## 技术栈

- Kotlin 2.2.10 · Compose Multiplatform 1.9.2 · Ktor 3.2.4 (Darwin) · kotlinx.serialization / coroutines / datetime · kaml (YAML 解析) · multiplatform-settings
- 架构：单模块 `composeApp`（commonMain 共享全部业务逻辑）+ `iosApp`（SwiftUI 壳）
- 部署器基于 Docker Engine API 实现（无需在 NAS 上安装 docker-compose）

## 目录结构

```
├── composeApp/
│   ├── src/commonMain/kotlin/com/fnos/dockermanager/
│   │   ├── docker/          # Docker Engine API 客户端与 DTO
│   │   ├── connection/      # 服务器配置与本地持久化
│   │   ├── composefile/     # 编排模型 + YAML 生成/解析 + API 部署器
│   │   ├── app/             # 全局状态与导航
│   │   └── ui/              # 全部界面（M3）
│   ├── src/iosMain/         # iOS 平台实现（Ktor Darwin、剪贴板、设置）
│   └── src/commonTest/      # YAML 生成/解析、日志解帧等单元测试
├── iosApp/                  # Xcode 壳工程
└── .github/workflows/ios-build.yml  # macOS Runner 自动出 IPA
```

## 在飞牛上开启 Docker 远程 API（一次性）

```bash
# SSH 登录飞牛后：
sudo tee /etc/docker/daemon.json <<'EOF'
{
  "hosts": ["unix:///var/run/docker.sock", "tcp://0.0.0.0:2375"]
}
EOF
sudo systemctl restart docker
```

> 若 daemon.json 已有内容，请合并 `hosts` 键。放行防火墙 2375（内网）。**公网暴露 2375 极不安全**，请务必改用 2376 + TLS，或仅通过 VPN/内网使用。自签名 TLS 证书需先安装并信任到 iPhone。

## 在 Mac 上构建（一条命令出包）

前置：Xcode 15+、JDK 17、网络。

```bash
# 1. 首次：生成 gradle wrapper（已有则跳过）
gradle wrapper --gradle-version 8.14.3

# 2. 打开工程
open iosApp/iosApp.xcodeproj
# 在 Build Settings 里填 DEVELOPMENT_TEAM（或编辑 iosApp/Configuration/Config.xcconfig 的 TEAM_ID）
# 选择自己的签名 Team 后 Cmd+R 即可运行到真机

# 3. 纯命令行出 IPA（无签名 / 模拟器）
xcodebuild -project iosApp/iosApp.xcodeproj -scheme iosApp -configuration Debug \
  -sdk iphonesimulator -destination 'generic/platform=iOS Simulator' \
  CODE_SIGNING_ALLOWED=NO build
```

## 自动出 IPA（GitHub Actions 备用通道）

1. 把仓库推到 GitHub；
2. Settings → Secrets 添加 `TEAM_ID`（开发者 Team ID）；
3. Actions → **iOS Build (IPA)** → Run workflow（Release 配置会打真机包）；
4. 产物在 Artifacts 下载，打 tag `v*` 会自动发布到 GitHub Release。

## 验证

- 单元测试（JVM）：`./gradlew :composeApp:jvmTest` —— 覆盖 YAML 生成/解析、标量转义、日志多路复用解帧、模型校验。
- iOS 编译验证（Linux 主机也可执行，Kotlin/Native 交叉编译）：
  `./gradlew :composeApp:compileKotlinIosSimulatorArm64 :composeApp:compileKotlinIosX64`

## 已知限制

- 打包 IPA 需要 macOS/Xcode（iOS 工具链限制），本仓库在 Linux 上已验证共享代码与 iOS framework 编译。
- 注册表密码存于应用本地设置（未用 Keychain），仅建议可信设备使用。
- 编排部署通过 Docker Engine API 实现，支持常用 compose 语义（卷/网络/端口/环境/依赖/重启策略/资源限制）；健康检查、configs、secrets 等高级字段暂未覆盖。
- 储存位置选择基于常见路径预设 + 最近使用（Docker API 不提供文件系统浏览）。
