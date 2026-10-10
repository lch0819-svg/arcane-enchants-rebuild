# 奧術附魔 26.3 重建專案（開發中）

## 目標
- Minecraft Java 26.3
- Fabric Loader 0.19.5
- Fabric Loom 1.17.1、Gradle 9.6、Java 25
- 保留 alpha JAR 中的附魔 JSON 與繁體中文語言資源
- 新增「脈絡挖掘」事件處理原型

## 重要狀態
這是**尚待建置與遊戲內驗證的原始碼專案**，不是已驗證可用的 JAR。
目前已把原始 JAR 中的 80 個附魔 JSON 與 zh_tw 語言檔複製到專案，並新增 `PlayerBlockBreakEvents.AFTER` 事件原型。

### 3×3 挖掘原型的限制
- 需要實際以 Java 25、Gradle 9.6 與 Minecraft 26.3 對應的 Fabric API 建置，確認 callback 與映射 API。
- 額外方塊會透過伺服器世界的 `destroyBlock(..., true, player)` 破壞並保留掉落物。
- **額外方塊的工具耐久度尚未逐塊扣除**，因此此版本不能視為完整功能。
- 目前使用玩家視線主方向決定平面；不同工具、方塊保護、領地模組、精準採集與幸運附魔的相容性仍需測試。
- 80 個 JSON 只是定義資料，不代表 80 種附魔效果都已實作。

## 建置方式
需要安裝 JDK 25 與 Gradle 9.6。於專案根目錄執行：

```bash
gradle --version
gradle clean build
```

若建置成功，產物會位於 `build/libs/`。若失敗，請提供完整錯誤輸出，尤其是第一個 `What went wrong` 區塊。

## 安裝測試
1. 先備份世界存檔。
2. 使用 Minecraft Java 26.3 與相符版本的 Fabric Loader/API。
3. 將建置產物放入 `mods` 資料夾。
4. 先在新建的測試世界測試，勿直接用主要存檔驗證。

## 上游依據
請以 Fabric 官方 26.3 發布說明與 Develop 頁面確認實際可用的 Loader/API 版本：
- https://www.fabricmc.net/2026/09/15/263.html
- https://fabricmc.net/develop/

## 自動編譯（不用在自己的電腦安裝 Java / Gradle）

專案加入了 GitHub Actions 工作流程 `.github/workflows/build.yml`。把整個專案上傳到自己的 GitHub repository 後，在 GitHub 的 **Actions** 頁面執行 **Build Arcane Enchants JAR**，成功時可在該次執行頁面底部的 **Artifacts** 下載 `arcane-enchants-minecraft-26.3`，解壓後會得到 `.jar`。

注意：此流程尚未在本環境執行；第一次遠端編譯可能會暴露版本解析或 API 編譯錯誤，需要再根據 Actions 的日誌修正。成功編譯也不等於已完成 Minecraft 內實際測試。不要把目前 ZIP 或未驗證的 JAR 當成可用成品。
