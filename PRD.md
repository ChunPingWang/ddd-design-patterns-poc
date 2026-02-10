# PRD: 汽車製造業領域驅動設計 PoC

## Product Requirements Document

**專案名稱**: AutoMFG — 汽車製造業訂單與生產管理系統 PoC  
**版本**: 1.0  
**日期**: 2025-02-11  
**目標讀者**: 業務分析師、產品經理、領域專家、架構師

---

## 1. 業務背景與目標

### 1.1 業務背景

某汽車製造商希望建構新一代的「訂單到交車」(Order-to-Delivery) 數位化平台，涵蓋從經銷商下單、生產排程、組裝製造、品質檢驗到物流交車的完整流程。現有系統為單體式架構，面臨以下痛點：

- 訂單變更（如客製化配備）需跨多個子系統手動同步，耗時且易出錯
- 生產排程無法即時反映物料供應狀態，導致產線停工
- 品質追溯困難，無法快速定位缺陷批次
- 各工廠系統各自為政，難以統一管理

### 1.2 PoC 目標

透過 Domain-Driven Design (DDD) 戰術設計模式，建構一個可驗證的原型系統，證明：

1. **領域模型的可表達性** — 業務規則封裝在 Domain Model 中，非散落在 Service 或 Controller
2. **邊界清晰的聚合設計** — Aggregate 確保交易一致性，降低並發衝突
3. **架構可演進性** — 六角形架構讓核心領域不依賴基礎設施，可獨立測試與替換技術棧
4. **事件驅動的跨界互動** — Domain Event 實現 Bounded Context 間的鬆耦合

### 1.3 成功指標

| 指標 | 目標 |
|------|------|
| 領域模型覆蓋率 | 核心業務規則 100% 封裝在 Domain Layer |
| 單元測試覆蓋率 | Domain Layer ≥ 90% |
| 架構合規性 | 零違規：Domain Layer 不依賴 Infrastructure |
| 業務場景驗證 | 完成 5 個核心 Use Case 的端到端驗證 |

---

## 2. 領域分析

### 2.1 核心領域 (Core Domain)

**生產製造管理 (Manufacturing Management)** — 這是汽車製造商的核心競爭力所在，包含生產工單管理、組裝流程控制、品質檢驗等，需要高度客製化且頻繁迭代。

### 2.2 支撐子領域 (Supporting Subdomain)

- **車輛配置管理 (Vehicle Configuration)** — 管理車型、配備選項、相容性規則
- **物料管理 (Material Management)** — BOM 管理、庫存查詢、物料預留

### 2.3 通用子領域 (Generic Subdomain)

- **訂單管理 (Order Management)** — 經銷商訂單 CRUD、訂單狀態追蹤
- **物流配送 (Logistics)** — 運輸排程、交車追蹤

### 2.4 Bounded Context Map

```
┌─────────────────────────────────────────────────────────────────┐
│                        AutoMFG Platform                         │
│                                                                 │
│  ┌──────────────┐    Domain Event     ┌──────────────────────┐  │
│  │    Order      │ ──────────────────> │   Manufacturing      │  │
│  │  Management   │  OrderPlaced        │   Management         │  │
│  │   Context     │  OrderChanged       │   Context            │  │
│  └──────────────┘                     └──────────────────────┘  │
│         │                                       │               │
│         │ ACL                          Domain Event              │
│         ▼                                       ▼               │
│  ┌──────────────┐                     ┌──────────────────────┐  │
│  │   Vehicle     │ <── Conformist ──> │   Material           │  │
│  │ Configuration │                    │   Management         │  │
│  │   Context     │                    │   Context            │  │
│  └──────────────┘                     └──────────────────────┘  │
│                                                 │               │
│                                          Domain Event           │
│                                                 ▼               │
│                                       ┌──────────────────────┐  │
│                                       │   Logistics          │  │
│                                       │   Context            │  │
│                                       └──────────────────────┘  │
└─────────────────────────────────────────────────────────────────┘
```

**Context 間關係**:

- **Order → Manufacturing**: Customer/Supplier — 訂單下達後透過 `OrderPlacedEvent` 觸發生產工單建立
- **Order → Vehicle Configuration**: ACL (Anti-Corruption Layer) — 訂單引用車輛配置，但透過 ACL 轉換語言
- **Manufacturing → Material**: Conformist — 製造依賴物料的 BOM 結構
- **Manufacturing → Logistics**: Domain Event — 車輛完工後透過 `VehicleCompletedEvent` 觸發物流排程

---

## 3. 核心 Use Case

### UC-01: 經銷商下單 (Place Order)

**Actor**: 經銷商  
**前置條件**: 經銷商已登入系統並選定車型與配備  
**主要流程**:

1. 經銷商選擇車型 (Model)、車色 (Color)、配備套件 (Option Package)
2. 系統驗證配備相容性（例如：選配天窗不可搭配敞篷車型）
3. 系統計算預估交車日期與報價
4. 經銷商確認下單
5. 系統建立訂單，狀態為 `PLACED`
6. 系統發佈 `OrderPlacedEvent`

**業務規則**:
- BR-01: 同一經銷商同一車型最多可有 50 張未完成訂單
- BR-02: 配備選項必須通過相容性檢查
- BR-03: 預估交車日不得早於下單日 + 45 天

### UC-02: 建立生產工單 (Create Production Order)

**Actor**: 系統（事件驅動）  
**觸發**: 收到 `OrderPlacedEvent`  
**主要流程**:

1. 系統接收訂單事件，解析車輛配置
2. 系統展開 BOM (Bill of Materials)，確認所有物料
3. 系統檢查物料可用性
4. 若物料充足，建立生產工單，狀態為 `SCHEDULED`
5. 若物料不足，建立生產工單，狀態為 `MATERIAL_PENDING`，並發佈 `MaterialShortageEvent`

**業務規則**:
- BR-04: 一張客戶訂單對應一張生產工單（1:1）
- BR-05: 生產工單必須包含完整 BOM 展開結果
- BR-06: 物料不足時不得排入生產排程

### UC-03: 組裝流程推進 (Advance Assembly)

**Actor**: 產線操作員  
**前置條件**: 生產工單狀態為 `IN_PRODUCTION`  
**主要流程**:

1. 操作員掃描工單條碼進入工站
2. 系統顯示該工站的組裝任務清單
3. 操作員逐項完成組裝並回報
4. 系統記錄組裝紀錄（操作員、時間、使用物料批號）
5. 若該工站所有任務完成，自動推進至下一工站
6. 若為最終工站，工單狀態轉為 `ASSEMBLY_COMPLETED`

**業務規則**:
- BR-07: 工站必須按順序推進，不可跳站
- BR-08: 每個組裝步驟必須記錄物料批號（品質追溯）
- BR-09: 單一工站操作時間超過標準工時 150% 時發出告警

### UC-04: 品質檢驗 (Quality Inspection)

**Actor**: 品檢員  
**前置條件**: 生產工單狀態為 `ASSEMBLY_COMPLETED`  
**主要流程**:

1. 品檢員接收待檢車輛
2. 系統載入該車型的檢驗清單 (Inspection Checklist)
3. 品檢員逐項檢驗並記錄結果（合格/不合格/有條件通過）
4. 若全部合格，車輛狀態轉為 `INSPECTION_PASSED`，發佈 `VehicleCompletedEvent`
5. 若有不合格項，車輛狀態轉為 `INSPECTION_FAILED`，建立返工單

**業務規則**:
- BR-10: 安全相關項目不合格即判定整車不合格
- BR-11: 非安全項目允許最多 3 項「有條件通過」
- BR-12: 品檢結果須由不同品檢員複核（四眼原則）

### UC-05: 訂單變更 (Change Order)

**Actor**: 經銷商  
**前置條件**: 訂單狀態為 `PLACED` 或 `SCHEDULED`  
**主要流程**:

1. 經銷商提交變更請求（如變更車色或配備）
2. 系統檢查變更是否可行（基於當前生產狀態）
3. 若生產尚未開始，允許變更，更新訂單與生產工單
4. 若生產已開始，拒絕變更或提供替代方案
5. 系統發佈 `OrderChangedEvent`

**業務規則**:
- BR-13: 生產狀態為 `IN_PRODUCTION` 以後不可變更
- BR-14: 變更車型視為取消原單 + 新建訂單
- BR-15: 每張訂單最多允許 3 次配備變更

---

## 4. 領域術語表 (Ubiquitous Language)

| 中文術語 | 英文術語 | 定義 |
|----------|----------|------|
| 車型 | Vehicle Model | 車輛的基本型號，如 Model-X Sedan |
| 配備套件 | Option Package | 一組可選配的設備組合，如豪華內裝套件 |
| 車輛識別碼 | VIN (Vehicle Identification Number) | 每輛車的唯一識別碼，17 位 |
| 物料清單 | BOM (Bill of Materials) | 組裝一輛車所需的完整零件清單 |
| 生產工單 | Production Order | 指示工廠生產特定車輛的工作指令 |
| 工站 | Work Station | 組裝產線上的一個作業位置 |
| 組裝步驟 | Assembly Step | 在特定工站執行的一個組裝動作 |
| 品質檢驗 | Quality Inspection | 車輛組裝完成後的品質查驗流程 |
| 檢驗清單 | Inspection Checklist | 品檢時需逐項確認的檢查項目 |
| 返工單 | Rework Order | 品檢不合格時建立的修復工作指令 |
| 經銷商 | Dealer | 銷售車輛的合作通路商 |
| 交車 | Vehicle Delivery | 完成車輛交付至經銷商或最終客戶 |

---

## 5. 業務流程概覽

```
經銷商下單          生產排程           組裝製造          品質檢驗         物流交車
   │                  │                 │                 │                │
   ▼                  ▼                 ▼                 ▼                ▼
┌───────┐      ┌───────────┐     ┌───────────┐    ┌───────────┐   ┌──────────┐
│ Order │      │Production │     │ Assembly  │    │ Quality   │   │Logistics │
│Placed │─────>│  Order    │────>│  Process  │───>│Inspection │──>│ Delivery │
│       │ Evt  │ Created   │     │           │    │           │   │          │
└───────┘      └───────────┘     └───────────┘    └───────────┘   └──────────┘
   │                │                 │                 │
   │           物料不足？          工站推進           不合格？
   │              │                  │                 │
   ▼              ▼                  ▼                 ▼
 變更/取消    物料等待/           記錄批號           建立返工單
             採購通知            品質追溯
```

---

## 6. 非功能性需求（業務觀點）

| 面向 | 需求 |
|------|------|
| 可用性 | 生產線系統 99.9% 可用，計劃外停機 < 8 小時/年 |
| 回應時間 | 訂單操作 < 2 秒回應；工站掃碼 < 500ms |
| 資料保留 | 品質追溯資料保留 15 年（法規要求） |
| 多廠支援 | 設計須支援多工廠部署，各廠獨立生產排程 |
| 合規性 | 符合 ISO 9001 品質管理 / IATF 16949 汽車業品質標準 |

---

## 7. 範圍與限制

### PoC 範圍內

- Order Management Context（簡化版）
- Manufacturing Management Context（核心）
- Vehicle Configuration Context（簡化版）
- Material Management Context（Mock）

### PoC 範圍外

- Logistics Context（以 Event Stub 代替）
- 使用者認證與授權
- 報表與 BI
- 實際 MES / ERP 整合

---

## 8. 利害關係人

| 角色 | 關注點 |
|------|--------|
| 產品經理 | 業務流程正確性、交車時程可預測性 |
| 工廠廠長 | 產線效率、停工率降低 |
| 品質經理 | 缺陷追溯能力、檢驗流程標準化 |
| IT 架構師 | 系統可維護性、技術債控制、架構合規 |
| 經銷商 | 下單便利性、訂單狀態可見度 |
