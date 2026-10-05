# Shortcuts Panel（快捷面板）

[English](README_EN.md) | 简体中文

一个快捷键面板，可以搜索并触发mod注册的按键绑定，支持筛选未绑定键、即时绑定、收藏条目、历史记录、排除结果。
支持masa系列快捷键(tweakeroo\minihud\litematica\itemscroller等)。

- 默认按 **K** 键打开。

![面板截图](assets/screenshot.jpg)

- 搜索栏条目可用右键排除。

- 收藏栏条目可用右键移除，支持拖拽排序。

搜索匹配 **绑定项名**（权重 3）、**内部键名** `key.xxx`（权重 2）、**类别名**（权重 1）；
评分顺序为 精确 > 前缀 > 包含 > 子序列，中文名、英文键名均可搜。

## 构建

```bash
./gradlew buildAll
```
