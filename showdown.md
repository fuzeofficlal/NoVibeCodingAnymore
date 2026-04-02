# 🚀 NoVibe Terminal: 终极演示剧本

这份文档是您的完整、分步演示脚本，旨在全面展示 NoVibe Financial Copilot 项目的强大功能。它的设计逻辑能够流畅地贯穿并展示所有微服务（Go网关、Java核心引擎、Python量化引擎以及Spring AI大模型），全部通过前端 UI 直观呈现。

---

## 🕒 了解警报推送机制 (背景知识)

在开始演示之前，解释**服务器是如何、以及何时推送价格警报**是非常重要的。

根据核心引擎中的 `AlertSchedulerService.java`：
1. **定时扫描**：后端运行着一个完全自动化的 Cron 定时器，**每整整 20 秒**执行一次。
2. **触发逻辑**：它会将您数据库中 `price_alerts` 表的每一行与 `market_prices` 表的最新实时价格进行大盘比对。
   - 如果设置了 `TAKE_PROFIT` (止盈)，当 `当前价 >= 目标价` 时立即触发。
   - 如果设置了 `STOP_LOSS` (止损)，当 `当前价 <= 目标价` 时立即触发。
3. **AI 接管 (AI Event)**：一旦阈值被突破，Java Core 会永久删除该触发器以防止垃圾信息轰炸，并立即向 `advisor-service` 发送一个 Webhook `POST` 请求。随后，AI Copilot 会强行通过服务器发送事件 (SSE) 流劫持您的全局 UI，并在前端页面的任何位置弹出一个主动防御警报框（Toast / Modal），警告您市场发生了剧变！

---

## 🎬 5分钟巅峰演示流程

这是您完美展示所有功能的精确点击操作顺序。

### 第一幕：冷启动与数据入库 (Home & Dashboard)
**目标：** 展示极其迅捷的注册绑定流程以及 Python Yahoo Finance 的数据爬取引擎。
1. **导航至** `http://localhost:8080/` (首页)。
2. 查看底部的表单，在 **New Portfolio Name** (投资组合名称) 下方输入：`Alpha Fund`。
3. 在 **Initial Deposit** (初始入金) 下方输入：`1000000` (100万美元)。
4. 点击 **Create New Portfolio**。您将看到一个成功弹窗，并被瞬间传送到 Dashboard 仪表盘。
5. **Dashboard 演示：** 您会看到拥有 100 万美元的现金，但仓位是 0。重点指出通过 WebSockets 实时跳动的上方市场报价行情网格。
6. 点击右上角最新加入的 **"Sync Market"** 按钮。
   * *解说词：* “这个操作会强制后台的 Python 量化微服务去死磕 Yahoo Finance 获取硬核的最新收盘数据并落库。”

### 第二幕：执行台与技术指标 (Trade)
**目标：** 证明与量化引擎的深度集成并展示超高频的数据库账本交易。
1. 点击左侧边栏的 **Trade** (02) 页面。
2. 在 "Execution Ticket" (交易单) 下，将 **Asset Type** 更改为 `Stock`。
3. 点击 **Ticker** (代码) 搜索框。注意现在这是一个原生的高级搜索下拉列表。输入 `NV` 并选择 `NVDA`。
4. **等待 1 秒钟**。右侧的 **Live Execution Price** (实时执行价) 面板将通过 REST API 被自动填充。
5. 转移视线到下方新增的 **SMA Baseline** (简单移动平均线) 面板。将下拉菜单从 `50-Day` 更改为 `20-Day`。观察 Python 量化引擎如何实时重新计算并传回全新的技术指标。
6. 在 **Quantity** (数量) 框中输入 `1000`。观察 "Estimated Notional" (预估名义价值) 的动态刷新。
7. 点击 **Submit Order** (提交订单)。
   * *解说词：* “此时 Java Spring 后端刚刚扣除了我们的虚拟现金，并将一笔真实的资产交割单录入到了后台 SQL 账本中。”

### 第三幕：量化宽客图表 (Compare)
**目标：** 通过 Pandas 处理的历史大数组，展示前端极限的数据可视化折线图。
1. 点击左侧边栏的 **Compare** (03) 页面。
2. 在 **Asset A** 下方，输入 `NVDA`。
3. 在 **Asset B (Optional)** 下方，输入 `AAPL`。
4. 点击 **Month** (月) 时间维度的过滤按钮，然后点击 **Compare Assets**。
5. 将鼠标悬停在精美的双色 Chart.js 坐标系图表上查看两股历史走势叠加。
6. **单点透视测试：** 将输入框里的 `AAPL` 完全清空（保留空白）。再次点击 **Compare Assets**。系统会非常优雅地降级为一根孤立的金棕色单股走势曲线。

### 第四幕：风控策略盘 (Watchlist)
**目标：** 展示 Yahoo Finance 外部新闻爬取和作用域严格管控的警报过滤逻辑。
1. 点击 **Watchlist** (04) 页面。
2. 在 **Search Ticker** 下方输入 `TSLA` (特斯拉) 并点击 **Add Symbol** 关注它。
3. **向下滚动：** 这里将揭示由 Python 后台动态拼接产生的 "Latest Market News" (本地金融新闻流) 卡片。
4. 来到上方的 **Price Alerts** (价格预警) 模块。
5. 点击 **Asset** 下拉搜索框。
   * *解说词：* “这个下拉框只包含了我们资金盘里的 `NVDA` 和自选池的 `TSLA`。”
6. **实机指令（为了确保触发）：** 选择 `NVDA`，条件选 `STOP_LOSS` (止损)，目标价设为：`999.00` (一个绝对高于当前真实股价的值)。点击 **Set Alert**。
   * *解说词：* “现在我故意设一个荒唐的极速倒挂指标，模拟一次恐怖的大跌熔断！我们在接下来的 20 秒内随时会遭遇系统的最高级别的全区广播拦截。”
   * **(等待最多 20 秒)：** 此时您页面的正中央会突然强行弹出一个附带**关闭 (Close)** 按钮的巨大红色警告窗 (AI Copilot Alert)，里面的内容是由大模型极速生成的抛售警告！
   * 点击 `Close` 关闭弹窗，继续演示。

### 第五幕：金融机长出舱 (AI Desk)
**目标：** 展示 Spring AI 框架下大模型 Agent 的自主 Function-Calling (工具调用) 能力。
1. 点击 **AI Desk** (05) 页面。
2. **实机指令 1：** 输入框输入：`"Give me a full summary of my portfolio and tell me how my cash balance looks."` (给我一份完整的投资组合摘要，并告诉我现金余额情况)。
   * *效果展示：* 大语言模型将自主判断并调用底层的 `getPortfolioSummary` (获取组合摘要) 的 Java API 来为您总结资金状况。
3. **实机指令 2：** 输入框输入：`"Check the latest news for Apple."` (帮我查一下苹果最新发生的新闻)。
   * *效果展示：* 模型会自行切入调用 `getMarketNews` Python 接口。
4. **最后高光实机指令 3：** 输入框输入：`"My NVDA holdings are looking good. Buy me 50 shares of AAPL right now."` (我的英伟达长势喜人，现在立刻帮我买入 50 股苹果公司股票)。
   * *效果展示：* 终极测试触发！大模型会未经您的界面干预，物理级别地在后台代您发起一笔 `BUY` (买入) 入账确认，并直接扣除您的剩余现金余额！

**演示完美落幕。** 🎉
