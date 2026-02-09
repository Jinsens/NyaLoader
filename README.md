# NyaLoader

<div align="center">

![Android](https://img.shields.io/badge/Android-27+-3DDC84?logo=android&logoColor=white)
![Kotlin](https://img.shields.io/badge/Kotlin-2.3-7F52FF?logo=kotlin&logoColor=white)
![Compose](https://img.shields.io/badge/Jetpack%20Compose-Material3-4285F4)
[![License: AGPL v3](https://img.shields.io/badge/License-AGPL%20v3-blue.svg)](LICENSE)

**Android 多线程下载管理器** · Material Design 3 · Jetpack Compose

[下载](#-下载) • [功能](#-功能) • [构建](#-构建) • [许可](#-许可)

</div>

---

## 📥 下载

从 [Releases](https://github.com/Jinsens/NyaLoader/releases) 下载最新版本。

---

## ✨ 功能

### 核心
- **多线程下载** - 最高 256 线程并发
- **断点续传** - 精确到字节级恢复
- **内置浏览器** - 自动捕获下载链接

### 界面
- **Material Design 3** - 动态主题、深色模式
- **剪贴板监听** - 复制链接自动弹窗
- **多语言** - 中/英/日

### 更多
- 下载限速、下载统计、批量下载
- 自定义 User-Agent、保存目录
- 自动更新检查

---

## 🛠 技术栈

| 类别 | 技术 |
|------|------|
| UI | Jetpack Compose, Material 3 |
| 架构 | MVVM, Hilt, Room |
| 网络 | OkHttp, Coroutines |
| 其他 | Firebase (可选), KSP |

---

## 🚀 构建

```bash
git clone https://github.com/Jinsens/NyaLoader.git
cd NyaLoader
./gradlew assembleDebug
```

**要求**: Android Studio, JDK 17, Android SDK 27+

**Firebase (可选)**: 放置 `google-services.json` 到 `app/` 目录

---

## 📝 许可

[AGPL-3.0](LICENSE)

---

## ⭐ Star 历史

[![Star History Chart](https://api.star-history.com/svg?repos=Jinsens/NyaLoader&type=Date)](https://star-history.com/#Jinsens/NyaLoader&Date)

---

## 👨‍💻 作者

**小花生FMR** · [@Jinsens](https://github.com/Jinsens)

---

<div align="center">

⭐ 觉得有用？请给个 Star！

</div>
