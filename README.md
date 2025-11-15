
# NyaLoader - 多线程下载管理器

<div align="center">

![Android](https://img.shields.io/badge/Platform-Android-3DDC84?logo=android&logoColor=white)
![Kotlin](https://img.shields.io/badge/Language-Kotlin-7F52FF?logo=kotlin&logoColor=white)
![Jetpack Compose](https://img.shields.io/badge/UI-Jetpack%20Compose-4285F4?logo=jetpackcompose&logoColor=white)
![Material Design 3](https://img.shields.io/badge/Design-Material%203-6200EE)
[![License: AGPL v3](https://img.shields.io/badge/License-AGPL%20v3-blue.svg)](LICENSE)

一款现代化的 Android 多线程下载管理器，采用 Material Design 3 设计语言和 Jetpack Compose 构建。

[功能特性](#-功能特性) • [技术栈](#-技术栈) • [截图预览](#-截图预览) • [快速开始](#-快速开始) • [构建说明](#-构建说明)

</div>

---

## 📱 项目简介

NyaLoader 是一款功能强大、界面精美的 Android 下载管理器应用。它采用最新的 Android 开发技术栈，提供流畅的用户体验和丰富的自定义选项。无论是下载大文件还是批量管理下载任务，NyaLoader 都能轻松应对。

### 🎯 核心亮点

- **🚀 高性能下载引擎** - 支持最高 256 线程并发下载，充分利用网络带宽
- **🎨 现代化 UI 设计** - 基于 Material Design 3，支持动态取色和多种主题
- **💾 智能断点续传** - 即使中断也能从断点继续，不浪费已下载数据
- **📋 剪贴板智能监听** - 复制链接自动弹窗，一键开始下载
- **🎭 隐私优先** - Firebase 分析默认关闭，用户完全控制数据收集
- **🌈 高度可定制** - 主题、User-Agent、保存位置等多项个性化设置

---

## ✨ 功能特性

### 应用更新与维护

#### 🔄 自动更新检查 <sup>NEW</sup>
- **智能更新检测**：应用启动时自动检查 GitHub 上的新版本
- **频率控制**：每 24 小时最多检查一次,避免频繁请求
- **版本对比**：智能判断是否有新版本可用
- **可选更新**：支持"稍后再说"功能,用户自主选择
- **强制更新**：重要更新可设置为强制模式

#### 📱 智能架构适配 <sup>NEW</sup>
- **自动检测**：自动识别设备 CPU 架构（arm64-v8a、armeabi-v7a、x86、x86_64）
- **精准下载**：自动下载最适合设备的 APK 版本
- **智能回退**：不支持的架构自动回退到通用版本
- **节省流量**：只下载必需的架构,减少安装包大小

#### ⚙️ 手动检查更新 <sup>NEW</sup>
- **设置入口**：在设置界面随时手动检查更新
- **即时检查**：手动检查时忽略时间限制
- **状态提示**：明确显示是否有新版本
- **GitHub 链接**：一键跳转到项目仓库

#### 📂 文件管理增强 <sup>NEW</sup>
- **打开已下载文件**：点击已完成的任务直接打开文件
- **智能应用选择**：自动识别可打开文件的应用
- **MIME 类型检测**：准确识别文件类型
- **APK 安装支持**：完整的 APK 安装权限处理
- **详细日志**：完善的调试信息输出

### 核心下载功能

#### 🔥 多线程下载引擎
- **可配置线程数**：支持 1-256 个并发线程，默认 32 线程
- **智能分片下载**：自动将文件分割为多个分片并发下载
- **动态进度追踪**：实时显示每个分片的下载进度和速度
- **网络异常处理**：自动重试机制，网络波动也能稳定下载

#### 📥 批量下载支持 <sup>NEW</sup>
- **多链接添加**：一次性添加多个下载链接
- **动态管理**：可随时添加或删除输入框
- **批量提示**：显示已添加的链接数量
- **独立任务**：每个链接自动创建独立的下载任务

#### 💾 断点续传技术
- **Room 数据库持久化**：完整记录下载任务和分片进度
- **精确断点恢复**：支持到字节级别的断点续传
- **任务状态管理**：下载中、暂停、完成、失败等多种状态
- **失败自动恢复**：网络恢复后可一键继续所有任务
- **重新下载**：已完成的任务可以重新下载

#### 📂 灵活的存储方案
- **公共下载目录**：保存到系统标准 Download 文件夹
- **应用私有目录**：数据安全，卸载自动清理
- **自定义目录**：通过 SAF 选择任意目录，支持 SD 卡
- **自动文件命名**：智能从 URL 提取文件名，支持手动修改

### 用户界面与交互

#### 🎨 Material Design 3 UI
- **动态主题色**：从壁纸自动提取主题色（Android 12+）
- **预设主题**：蓝、绿、紫、橙、红、粉、青等多种配色
- **自定义主题**：支持自定义十六进制颜色值
- **深色模式**：浅色、深色、跟随系统三种模式
- **沉浸式状态栏** <sup>NEW</sup>：Edge-to-Edge 全面屏体验，状态栏和导航栏透明

#### 🔙 预测性返回手势 <sup>NEW</sup>
- **系统级支持**：Android 13+ 完整支持预测性返回手势
- **全局适配**：所有页面和组件都支持预测性返回
- **流畅动画**：返回前可预览动画效果

#### 📋 剪贴板智能监听
- **URL 自动识别**：复制链接后自动弹出下载对话框
- **智能去重**：避免同一链接重复弹窗
- **隐私保护**：可在设置中随时关闭监听
- **首次提示**：首次使用时显示隐私说明

#### 🎯 任务管理界面
- **实时进度显示**：百分比、速度、已下载/总大小一目了然
- **任务卡片设计**：优雅的卡片式布局，清晰展示任务信息
- **多状态展示**：不同状态用不同颜色和图标区分
- **批量操作**：支持暂停、继续、取消、删除等操作
- **搜索功能**：支持按文件名、URL、路径搜索任务

#### 🌍 多语言支持 <sup>NEW</sup>
- **简体中文** (zh-CN)
- **繁体中文** (zh-TW)
- **English** (en)
- **日本語** (ja)
- **自动跟随系统**：应用语言自动跟随系统设置
- **完整本地化**：所有界面文本都已翻译

### 高级设置选项

#### 🌐 User-Agent 管理
- **预设 UA 库**：Chrome、Firefox、Safari 等主流浏览器
- **自定义 UA**：支持添加和管理自定义 User-Agent
- **快速切换**：下载时可临时指定 UA，不影响默认设置
- **UA 预设列表**：保存常用 UA，方便重复使用

#### ⚙️ 下载参数配置
- **默认线程数**：全局设置默认并发线程数（1-256）
- **保存位置**：设置全局默认保存路径
- **创建任务时可覆盖**：每次下载都可临时修改参数

#### 📊 数据分析（可选）
- **Firebase 集成**：可选启用崩溃报告和使用分析
- **首次明确授权**：首次启动时弹窗询问，充分尊重隐私
- **随时可控**：在设置中一键开启/关闭
- **透明说明**：清晰告知数据收集目的和用途
---

## 🛠 技术栈

### 核心框架
- **Kotlin** - 100% Kotlin 编写，简洁高效
- **Jetpack Compose** - 声明式 UI，现代化界面开发
- **Material Design 3** - 最新设计规范，动态主题支持
- **Coroutines & Flow** - 异步编程，响应式数据流

### Android Jetpack 组件
- **Room Database** - 本地数据持久化，任务和进度管理
- **ViewModel** - MVVM 架构，UI 状态管理
- **Navigation Compose** - 页面导航和路由
- **DataStore/SharedPreferences** - 应用设置存储
- **WorkManager** - 后台任务调度（预留）

### 网络与存储
- **OkHttp** - HTTP 客户端，高效稳定的网络请求
- **Storage Access Framework (SAF)** - 安全的文件访问
- **Foreground Service** - 前台服务保证下载稳定性

### 第三方服务
- **Firebase Analytics** - 用户行为分析（可选）
- **Firebase Crashlytics** - 崩溃报告（可选）
- **Google Services** - Firebase 基础服务

### 开发工具
- **Kotlin DSL (build.gradle.kts)** - 类型安全的构建脚本
- **Version Catalog** - 依赖版本统一管理（可选）
- **ProGuard/R8** - 代码混淆和优化

---

## 🏗 项目架构

### MVVM 架构模式

```
┌─────────────────────────────────────────────────────────────┐
│                         UI Layer                             │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐      │
│  │DownloadScreen│  │SettingsScreen│  │LicensesScreen│      │
│  └──────────────┘  └──────────────┘  └──────────────┘      │
└────────────────────────┬────────────────────────────────────┘
                         │ observes StateFlow
┌────────────────────────▼────────────────────────────────────┐
│                    ViewModel Layer                           │
│  ┌───────────────────┐  ┌──────────────────┐               │
│  │ DownloadViewModel │  │ SettingsViewModel │               │
│  └───────────────────┘  └──────────────────┘               │
└────────────────────────┬────────────────────────────────────┘
                         │ calls Repository
┌────────────────────────▼────────────────────────────────────┐
│                   Repository Layer                           │
│  ┌──────────────────────────────────────────────────┐       │
│  │          DownloadRepository                       │       │
│  │  - createTask()  - startDownload()               │       │
│  │  - pauseDownload()  - deleteTask()               │       │
│  └──────────────────────────────────────────────────┘       │
└────────────────────────┬────────────────────────────────────┘
                         │ uses
┌────────────────────────▼────────────────────────────────────┐
│                     Data Layer                               │
│  ┌─────────────┐  ┌──────────────┐  ┌──────────────┐       │
│  │DownloadEngine│  │ Room Database│  │ AppPreferences│       │
│  │(多线程引擎) │  │(任务持久化)  │  │ (设置存储)   │       │
│  └─────────────┘  └──────────────┘  └──────────────┘       │
└─────────────────────────────────────────────────────────────┘
```

### 模块划分

```
app/
├── data/                       # 数据层
│   ├── database/              # Room 数据库
│   │   ├── AppDatabase.kt
│   │   ├── dao/               # DAO 接口
│   │   └── entity/            # 数据实体
│   ├── model/                 # 数据模型
│   └── preferences/           # 设置管理
│
├── download/                   # 下载引擎
│   └── DownloadEngine.kt      # 多线程下载核心
│
├── repository/                 # 仓库层
│   └── DownloadRepository.kt
│
├── service/                    # 服务
│   └── DownloadNotificationService.kt
│
├── ui/                         # UI 层
│   ├── screen/                # 页面
│   │   ├── DownloadScreen.kt
│   │   ├── SettingsScreen.kt
│   │   └── LicensesScreen.kt
│   └── theme/                 # 主题配置
│
├── util/                       # 工具类
│   ├── PermissionUtils.kt
│   ├── UrlValidator.kt
│   └── UserAgentHelper.kt
│
└── viewmodel/                  # ViewModel
    ├── DownloadViewModel.kt
    └── SettingsViewModel.kt
```

---

## 📸 截图预览

> 🚧 待补充：下载界面、设置界面、深色模式等截图

---

## 🚀 快速开始

### 系统要求

- **Android Studio**: Hedgehog (2023.1.1) 或更高版本
- **Android SDK**: API 24 (Android 7.0) 及以上
- **Kotlin**: 1.9.0+
- **JDK**: 17

### 克隆项目

```bash
git clone https://github.com/yourusername/NyaLoader.git
cd NyaLoader
```

### Firebase 配置（可选）

如果需要启用 Firebase 功能：

1. 访问 [Firebase Console](https://console.firebase.google.com/)
2. 创建新项目或选择现有项目
3. 添加 Android 应用，包名：`com.nyapass.loader`
4. 下载 `google-services.json` 文件
5. 将文件放置到 `app/` 目录下

> **注意**：即使不配置 Firebase，应用也能正常运行，只是分析功能不可用。

### 编译运行

```bash
# 同步依赖
./gradlew build

# 安装 Debug 版本
./gradlew installDebug

# 或直接在 Android Studio 中点击 Run
```

---

## 📦 构建说明

### 构建变体

- **Debug**: 开发版本，包含调试信息，未混淆
- **Release**: 发布版本，启用 R8 混淆和资源压缩

### 生成 Release APK

```bash
./gradlew assembleRelease
```

生成的 APK 位于：`app/build/outputs/apk/release/`

### 签名配置

在 `local.properties` 中配置签名信息（不要提交到 Git）：

```properties
keystore.path=/path/to/your/keystore.jks
keystore.password=your_keystore_password
key.alias=your_key_alias
key.password=your_key_password
```

---

## 🔒 权限说明

应用需要以下权限：

| 权限 | 用途 | 是否必需 |
|------|------|---------|
| `INTERNET` | 网络下载 | ✅ 必需 |
| `WRITE_EXTERNAL_STORAGE` | 写入文件（Android 10 以下） | ✅ 必需 |
| `READ_EXTERNAL_STORAGE` | 读取文件（Android 13 以下） | ✅ 必需 |
| `MANAGE_EXTERNAL_STORAGE` | 所有文件访问（Android 11+） | ⚠️ 自定义目录需要 |
| `FOREGROUND_SERVICE` | 前台服务 | ✅ 必需 |
| `FOREGROUND_SERVICE_DATA_SYNC` | 数据同步服务类型 | ✅ 必需 |
| `POST_NOTIFICATIONS` | 通知权限（Android 13+） | ✅ 必需 |
| `REQUEST_INSTALL_PACKAGES` | 安装 APK 权限（Android 8.0+） | ⚠️ 打开 APK 文件需要 |

---

## 🎯 使用说明

### 基本使用流程

1. **授予权限** - 首次启动时授予存储和通知权限
2. **Firebase 选择** - 选择是否启用数据分析（可随时更改）
3. **复制链接** - 复制下载链接，自动弹出下载对话框
4. **配置任务** - 设置文件名、线程数、保存位置等
5. **开始下载** - 点击开始，实时查看进度
6. **管理任务** - 暂停、继续、取消或删除任务

### 高级功能

#### 自定义 User-Agent
1. 进入**设置** → **默认 User-Agent**
2. 选择预设 UA 或添加自定义 UA
3. 保存后全局生效，创建任务时也可临时修改

#### 自定义保存目录
1. 进入**设置** → **默认保存位置** → **自定义目录**
2. 使用系统文件选择器选择目录
3. 授予持久化权限后即可使用

#### 调整线程数
1. 进入**设置** → **默认线程数**
2. 使用滑块调整（1-256）
3. 建议根据网速和服务器限制调整

---

## 🤝 贡献指南

欢迎提交 Issue 和 Pull Request！

### 开发规范

- 遵循 [Kotlin 官方代码风格](https://kotlinlang.org/docs/coding-conventions.html)
- 使用有意义的提交信息
- 为新功能添加注释和文档
- 保持代码简洁和可读性

### 提交流程

1. Fork 本仓库
2. 创建特性分支 (`git checkout -b feature/AmazingFeature`)
3. 提交更改 (`git commit -m 'Add some AmazingFeature'`)
4. 推送到分支 (`git push origin feature/AmazingFeature`)
5. 提交 Pull Request

---

## 📝 开源许可

本项目基于 **GNU Affero General Public License（AGPL）3.0** 开源。

### 使用的开源项目

- [Kotlin](https://kotlinlang.org/) - Apache 2.0
- [Jetpack Compose](https://developer.android.com/jetpack/compose) - Apache 2.0
- [OkHttp](https://square.github.io/okhttp/) - Apache 2.0
- [Room](https://developer.android.com/training/data-storage/room) - Apache 2.0
- [Firebase](https://firebase.google.com/) - Firebase Terms
- [Kotlin Coroutines](https://github.com/Kotlin/kotlinx.coroutines) - Apache 2.0
- [Material Design](https://m3.material.io/) - Apache 2.0

详细许可信息请查看应用内的 **关于与许可** 页面。

---

## 👨‍💻 作者

**小花生FMR**

- 当前版本：**v1.2.0**
- 邮箱：[2442198073@qq.com]
- GitHub：[@Jinsens]

---

## 📮 联系方式

- 提交 Issue: [GitHub Issues](https://github.com/Jinsens/NyaLoader/issues)
- 讨论区: [GitHub Discussions](https://github.com/Jinsens/NyaLoader/discussions)

---

## 🗺 路线图

### ✅ v1.2.0（已完成）
- [x] 自动更新检查功能
- [x] 智能架构检测和适配
- [x] 手动检查更新
- [x] 文件打开功能增强
- [x] APK 安装权限修复
- [x] GitHub 项目链接集成

### ✅ v1.1.0（已完成）
- [x] 支持多语言（简体中文、繁体中文、英语、日语）
- [x] 批量下载支持
- [x] 预测性返回手势
- [x] 状态栏沉浸式效果
- [x] 修复重新下载功能

### v1.3.0（计划中）
- [ ] 下载速度限制
- [ ] 文件分类管理
- [ ] 下载统计图表
- [ ] 下载队列管理
- [ ] 定时下载功能

### v1.4.0（未来规划）
- [ ] 📦 模块化架构重构
- [ ] ⚡ 性能全面优化
- [ ] 🏗️ 代码结构改进
- [ ] 🎯 可能推出正式版 (v2.0.0)

---

## ⭐ Star 历史

[![Star History Chart](https://api.star-history.com/svg?repos=Jinsens/NyaLoader&type=Date)](https://star-history.com/#Jinsens/NyaLoader&Date)

---

## 📄 更新日志

查看 [CHANGELOG.md](CHANGELOG.md) 了解详细更新记录。

---

## 🙏 致谢

感谢所有为本项目做出贡献的开发者和使用者！

如果觉得这个项目对你有帮助，请给个 ⭐️ 吧！

---

<div align="center">

**[⬆ 回到顶部](#nyaloader---多线程下载管理器)**

Made with ❤️ by 小花生FMR

</div>
