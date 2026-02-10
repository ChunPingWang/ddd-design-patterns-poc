# TECH: 汽車製造業 DDD 戰術設計技術文件

## Technical Design Document

**專案名稱**: AutoMFG — 汽車製造業訂單與生產管理系統 PoC  
**版本**: 1.0  
**日期**: 2025-02-11  
**目標讀者**: 軟體架構師、後端開發工程師、Tech Lead

---

## 1. 架構總覽

### 1.1 六角形架構 (Hexagonal Architecture)

本系統採用六角形架構（又稱 Ports and Adapters），確保 Domain Layer 完全獨立於技術框架與基礎設施。

```
                        ┌─────────────────────────────┐
                        │        Driving Side          │
                        │  (Primary / Input Adapters)  │
                        │                              │
                        │  ┌─────────┐ ┌────────────┐ │
                        │  │  REST   │ │  Event     │ │
                        │  │  API    │ │  Consumer  │ │
                        │  └────┬────┘ └─────┬──────┘ │
                        │       │             │        │
                        └───────┼─────────────┼────────┘
                                │             │
                        ┌───────▼─────────────▼────────┐
                        │      Input Ports              │
                        │  (Use Case Interfaces)        │
                        │                               │
                        │  ┌──────────────────────────┐ │
                        │  │   Application Layer       │ │
                        │  │   (Use Case Impl /        │ │
                        │  │    Application Services)  │ │
                        │  └────────────┬─────────────┘ │
                        │               │               │
                        │  ┌────────────▼─────────────┐ │
                        │  │     Domain Layer          │ │
                        │  │  ┌─────────────────────┐  │ │
                        │  │  │  Aggregates         │  │ │
                        │  │  │  Entities           │  │ │
                        │  │  │  Value Objects      │  │ │
                        │  │  │  Domain Events      │  │ │
                        │  │  │  Domain Services    │  │ │
                        │  │  │  Repository Ports   │  │ │
                        │  │  └─────────────────────┘  │ │
                        │  └────────────┬─────────────┘ │
                        │               │               │
                        │      Output Ports             │
                        │  (Repository / Gateway Intf)  │
                        └───────┬─────────────┬─────────┘
                                │             │
                        ┌───────▼─────────────▼────────┐
                        │       Driven Side             │
                        │  (Secondary / Output Adapters)│
                        │                               │
                        │  ┌──────────┐ ┌────────────┐  │
                        │  │   JPA    │ │  Message   │  │
                        │  │  Repos   │ │  Publisher │  │
                        │  └──────────┘ └────────────┘  │
                        └───────────────────────────────┘
```

### 1.2 分層依賴規則

```
依賴方向（由外向內，內層不知道外層）:

Infrastructure Layer  ──depends on──>  Application Layer  ──depends on──>  Domain Layer
     (Adapters)                        (Use Cases)                       (Core Model)

Domain Layer: 零外部依賴，純 Java (no Spring, no JPA annotations)
Application Layer: 僅依賴 Domain Layer
Infrastructure Layer: 依賴 Application + Domain，實作 Ports
```

### 1.3 SOLID 原則在架構中的體現

| 原則 | 體現方式 |
|------|----------|
| **S** — Single Responsibility | 每個 Aggregate 只負責一個業務聚合邊界內的不變條件 (invariants) |
| **O** — Open/Closed | 新增品檢類型透過擴展 `InspectionPolicy` 介面，不修改現有邏輯 |
| **L** — Liskov Substitution | `JpaProductionOrderRepository` 可完全替換為 `InMemoryProductionOrderRepository` |
| **I** — Interface Segregation | Repository Port 拆分為 `ProductionOrderReader` 與 `ProductionOrderWriter` |
| **D** — Dependency Inversion | Domain 定義 Port (interface)，Infrastructure 提供 Adapter (implementation) |

---

## 2. Module 結構

### 2.1 Maven Multi-Module

```
auto-mfg/
├── pom.xml                              # Parent POM
│
├── manufacturing-domain/                # Domain Layer (pure Java)
│   └── src/main/java/
│       └── com.automfg.manufacturing.domain/
│           ├── model/                   # Aggregates, Entities, VOs
│           ├── event/                   # Domain Events
│           ├── service/                 # Domain Services
│           └── port/                    # Repository & Gateway Ports
│
├── manufacturing-application/           # Application Layer
│   └── src/main/java/
│       └── com.automfg.manufacturing.application/
│           ├── usecase/                 # Use Case implementations
│           ├── port/                    # Input Ports (use case interfaces)
│           └── dto/                     # Command & Query DTOs
│
├── manufacturing-infrastructure/        # Infrastructure Layer
│   └── src/main/java/
│       └── com.automfg.manufacturing.infrastructure/
│           ├── adapter/
│           │   ├── inbound/            # REST Controllers, Event Consumers
│           │   └── outbound/           # JPA Repos, Message Publishers
│           ├── persistence/            # JPA Entities, Mappers
│           └── config/                 # Spring Configuration
│
└── manufacturing-bootstrap/             # 啟動模組 (Spring Boot Main)
    └── src/main/java/
        └── com.automfg.manufacturing/
            └── AutoMfgApplication.java
```

### 2.2 Maven 依賴約束

```xml
<!-- manufacturing-domain/pom.xml — 零 Spring 依賴 -->
<dependencies>
    <!-- 僅允許 Java 標準庫 + 少量工具庫 -->
    <dependency>
        <groupId>org.apache.commons</groupId>
        <artifactId>commons-lang3</artifactId>
    </dependency>
</dependencies>

<!-- manufacturing-application/pom.xml -->
<dependencies>
    <dependency>
        <groupId>com.automfg</groupId>
        <artifactId>manufacturing-domain</artifactId>
    </dependency>
    <!-- 不依賴 Spring，不依賴 Infrastructure -->
</dependencies>

<!-- manufacturing-infrastructure/pom.xml -->
<dependencies>
    <dependency>
        <groupId>com.automfg</groupId>
        <artifactId>manufacturing-application</artifactId>
    </dependency>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-data-jpa</artifactId>
    </dependency>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-web</artifactId>
    </dependency>
</dependencies>
```

---

## 3. DDD 戰術設計 — Manufacturing Context

### 3.1 Aggregate 識別與邊界

```
┌─────────────────────────────────────────────────────────────────┐
│                    Manufacturing Context                        │
│                                                                 │
│  ┌─────────────────────────┐   ┌─────────────────────────────┐  │
│  │  «Aggregate»            │   │  «Aggregate»                │  │
│  │  ProductionOrder        │   │  QualityInspection           │  │
│  │  ─────────────────────  │   │  ─────────────────────────── │  │
│  │  ProductionOrder (Root) │   │  QualityInspection (Root)    │  │
│  │    ├─ AssemblyProcess   │   │    ├─ InspectionItem         │  │
│  │    │   ├─ AssemblyStep  │   │    └─ InspectionResult       │  │
│  │    └─ BomSnapshot       │   │                              │  │
│  │        └─ BomLineItem   │   │                              │  │
│  └─────────────────────────┘   └─────────────────────────────┘  │
│                                                                 │
│  ┌─────────────────────────┐   ┌─────────────────────────────┐  │
│  │  «Aggregate»            │   │  «Aggregate»                │  │
│  │  VehicleConfiguration   │   │  MaterialStock              │  │
│  │  ─────────────────────  │   │  ──────────────────         │  │
│  │  VehicleConfig (Root)   │   │  MaterialStock (Root)       │  │
│  │    ├─ ModelSpec         │   │    └─ StockMovement         │  │
│  │    └─ OptionItem        │   │                              │  │
│  └─────────────────────────┘   └─────────────────────────────┘  │
└─────────────────────────────────────────────────────────────────┘
```

**Aggregate 設計決策**:

| 決策 | 理由 |
|------|------|
| `ProductionOrder` 包含 `AssemblyProcess` | 組裝進度必須與工單同步變更，共享交易一致性 |
| `QualityInspection` 獨立於 `ProductionOrder` | 品檢由不同人員執行，避免大 Aggregate 鎖競爭 |
| `BomSnapshot` 嵌入 `ProductionOrder` | 建單時快照 BOM，後續 BOM 變更不影響已建工單 |
| Aggregate 間透過 ID 引用 | 例如 `QualityInspection` 持有 `ProductionOrderId`，非物件引用 |

### 3.2 核心 Domain Model（Java 程式碼）

#### 3.2.1 Value Objects

```java
// ========================
// Value Objects — 不可變、無身份、語義豐富
// ========================

/**
 * 車輛識別碼 — 17 位國際標準格式
 * 體現 DDD Value Object: immutable, self-validating, equality by value
 */
public record VIN(String value) {
    public VIN {
        Objects.requireNonNull(value, "VIN must not be null");
        if (!value.matches("[A-HJ-NPR-Z0-9]{17}")) {
            throw new IllegalArgumentException(
                "Invalid VIN format: must be 17 alphanumeric chars excluding I, O, Q");
        }
    }
}

/**
 * 生產工單編號 — 格式: PO-{工廠代碼}-{年月}-{序號}
 */
public record ProductionOrderNumber(String value) {
    public ProductionOrderNumber {
        Objects.requireNonNull(value);
        if (!value.matches("PO-[A-Z]{2}-\\d{6}-\\d{5}")) {
            throw new IllegalArgumentException("Invalid ProductionOrderNumber: " + value);
        }
    }

    public String factoryCode() {
        return value.substring(3, 5);
    }
}

/**
 * 物料批號 — 用於品質追溯
 */
public record MaterialBatchId(String value) {
    public MaterialBatchId {
        Objects.requireNonNull(value);
        if (value.isBlank()) {
            throw new IllegalArgumentException("MaterialBatchId must not be blank");
        }
    }
}

/**
 * 工時 — 以分鐘為單位的度量
 * 體現 Quantity Pattern
 */
public record Duration(int minutes) {
    public Duration {
        if (minutes < 0) {
            throw new IllegalArgumentException("Duration cannot be negative");
        }
    }

    public boolean exceeds(Duration other) {
        return this.minutes > other.minutes;
    }

    public Duration multipliedBy(double factor) {
        return new Duration((int) (minutes * factor));
    }
}

/**
 * 工站識別 — 包含工站代碼與順序
 */
public record WorkStationId(String code, int sequence) {
    public WorkStationId {
        Objects.requireNonNull(code);
        if (sequence < 1) {
            throw new IllegalArgumentException("Sequence must be positive");
        }
    }

    public boolean isNextOf(WorkStationId previous) {
        return this.sequence == previous.sequence + 1;
    }
}
```

#### 3.2.2 Entity — AssemblyStep

```java
/**
 * 組裝步驟 — 有身份的領域物件
 * 屬於 AssemblyProcess (間接屬於 ProductionOrder Aggregate)
 * 體現 Entity: identity by id, mutable state, lifecycle within aggregate
 */
public class AssemblyStep {

    private final AssemblyStepId id;
    private final WorkStationId workStation;
    private final String taskDescription;
    private final Duration standardTime;
    private AssemblyStepStatus status;
    private MaterialBatchId materialBatchId;  // 品質追溯
    private String operatorId;
    private LocalDateTime completedAt;
    private Duration actualTime;

    // 由 AssemblyProcess 內部建立，外部不可直接 new
    AssemblyStep(AssemblyStepId id, WorkStationId workStation,
                 String taskDescription, Duration standardTime) {
        this.id = Objects.requireNonNull(id);
        this.workStation = Objects.requireNonNull(workStation);
        this.taskDescription = Objects.requireNonNull(taskDescription);
        this.standardTime = Objects.requireNonNull(standardTime);
        this.status = AssemblyStepStatus.PENDING;
    }

    /**
     * 完成此組裝步驟
     * BR-08: 必須記錄物料批號
     */
    public void complete(String operatorId, MaterialBatchId batchId, Duration actualTime) {
        if (this.status != AssemblyStepStatus.IN_PROGRESS) {
            throw new IllegalStateException(
                "Cannot complete step in status: " + this.status);
        }
        Objects.requireNonNull(batchId, "Material batch ID is required for traceability");
        this.operatorId = operatorId;
        this.materialBatchId = batchId;
        this.actualTime = actualTime;
        this.completedAt = LocalDateTime.now();
        this.status = AssemblyStepStatus.COMPLETED;
    }

    /**
     * BR-09: 檢查是否超過標準工時 150%
     */
    public boolean isOvertime() {
        if (actualTime == null) return false;
        return actualTime.exceeds(standardTime.multipliedBy(1.5));
    }

    void start() {
        if (this.status != AssemblyStepStatus.PENDING) {
            throw new IllegalStateException("Cannot start step in status: " + this.status);
        }
        this.status = AssemblyStepStatus.IN_PROGRESS;
    }

    // Getters (no setters — state change through behavior methods only)
    public AssemblyStepId getId() { return id; }
    public WorkStationId getWorkStation() { return workStation; }
    public AssemblyStepStatus getStatus() { return status; }
    public boolean isCompleted() { return status == AssemblyStepStatus.COMPLETED; }
}

public enum AssemblyStepStatus {
    PENDING, IN_PROGRESS, COMPLETED, SKIPPED
}
```

#### 3.2.3 Entity — AssemblyProcess

```java
/**
 * 組裝流程 — 管理一輛車的整個組裝過程
 * 負責維護工站順序的不變條件 (invariant)
 * 屬於 ProductionOrder Aggregate 內部
 */
public class AssemblyProcess {

    private final AssemblyProcessId id;
    private final List<AssemblyStep> steps;
    private WorkStationId currentStation;
    private AssemblyProcessStatus status;

    AssemblyProcess(AssemblyProcessId id, List<AssemblyStep> steps) {
        this.id = Objects.requireNonNull(id);
        if (steps == null || steps.isEmpty()) {
            throw new IllegalArgumentException("Assembly process must have at least one step");
        }
        this.steps = new ArrayList<>(steps);
        this.status = AssemblyProcessStatus.NOT_STARTED;
    }

    /**
     * 開始組裝 — 進入第一個工站
     */
    public void start() {
        if (status != AssemblyProcessStatus.NOT_STARTED) {
            throw new IllegalStateException("Assembly already started");
        }
        this.currentStation = steps.get(0).getWorkStation();
        steps.get(0).start();
        this.status = AssemblyProcessStatus.IN_PROGRESS;
    }

    /**
     * 完成當前工站的指定步驟
     * BR-07: 工站必須按順序推進
     * BR-08: 每步必須記錄物料批號
     */
    public AssemblyStepCompletionResult completeStep(
            AssemblyStepId stepId,
            String operatorId,
            MaterialBatchId batchId,
            Duration actualTime) {

        AssemblyStep step = findStep(stepId);

        if (!step.getWorkStation().equals(currentStation)) {
            throw new AssemblySequenceViolationException(
                "Step belongs to station " + step.getWorkStation()
                + " but current station is " + currentStation);
        }

        step.complete(operatorId, batchId, actualTime);

        // 檢查是否超時告警 (BR-09)
        boolean overtimeAlert = step.isOvertime();

        // 檢查當前工站是否全部完成
        boolean stationCompleted = stepsAtStation(currentStation)
                .allMatch(AssemblyStep::isCompleted);

        if (stationCompleted) {
            advanceToNextStation();
        }

        return new AssemblyStepCompletionResult(overtimeAlert, stationCompleted, isCompleted());
    }

    /**
     * BR-07: 按順序推進至下一工站
     */
    private void advanceToNextStation() {
        Optional<AssemblyStep> nextStep = steps.stream()
                .filter(s -> s.getStatus() == AssemblyStepStatus.PENDING)
                .findFirst();

        if (nextStep.isPresent()) {
            currentStation = nextStep.get().getWorkStation();
            stepsAtStation(currentStation).forEach(AssemblyStep::start);
        } else {
            this.status = AssemblyProcessStatus.COMPLETED;
        }
    }

    public boolean isCompleted() {
        return status == AssemblyProcessStatus.COMPLETED;
    }

    private Stream<AssemblyStep> stepsAtStation(WorkStationId station) {
        return steps.stream().filter(s -> s.getWorkStation().equals(station));
    }

    private AssemblyStep findStep(AssemblyStepId stepId) {
        return steps.stream()
                .filter(s -> s.getId().equals(stepId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Step not found: " + stepId));
    }
}

/**
 * 步驟完成結果 — Value Object，封裝完成後的狀態資訊
 */
public record AssemblyStepCompletionResult(
    boolean overtimeAlert,
    boolean stationCompleted,
    boolean assemblyCompleted
) {}
```

#### 3.2.4 Aggregate Root — ProductionOrder

```java
/**
 * 生產工單 — Aggregate Root
 *
 * 設計原則:
 * 1. 所有狀態變更必須透過 Aggregate Root 的方法
 * 2. 外部不可直接操作內部 Entity (AssemblyProcess, AssemblyStep)
 * 3. 不變條件 (invariants) 由 Aggregate Root 維護
 * 4. 產生 Domain Events 作為與外部溝通的方式
 * 5. 透過 ID 引用其他 Aggregate (OrderId, VIN)
 */
public class ProductionOrder {

    // === Identity ===
    private final ProductionOrderId id;
    private final ProductionOrderNumber orderNumber;

    // === 跨 Aggregate 引用（by ID, not by object reference）===
    private final OrderId sourceOrderId;
    private final VIN vin;

    // === 內部 Entities ===
    private final AssemblyProcess assemblyProcess;
    private final BomSnapshot bomSnapshot;

    // === State ===
    private ProductionOrderStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime scheduledStartDate;

    // === Domain Events（暫存，由 Repository 發佈）===
    private final List<DomainEvent> domainEvents = new ArrayList<>();

    // === Factory Method — 建立生產工單 ===
    public static ProductionOrder create(
            ProductionOrderId id,
            ProductionOrderNumber orderNumber,
            OrderId sourceOrderId,
            VIN vin,
            BomSnapshot bomSnapshot,
            List<AssemblyStep> assemblySteps,
            boolean materialAvailable) {

        // BR-05: 必須包含完整 BOM
        Objects.requireNonNull(bomSnapshot, "BOM snapshot is required");
        if (bomSnapshot.getLineItems().isEmpty()) {
            throw new IllegalArgumentException("BOM must not be empty");
        }

        ProductionOrder order = new ProductionOrder(
                id, orderNumber, sourceOrderId, vin, bomSnapshot, assemblySteps);

        // BR-06: 物料不足不得排入排程
        if (materialAvailable) {
            order.status = ProductionOrderStatus.SCHEDULED;
            order.domainEvents.add(new ProductionOrderScheduledEvent(
                    id, orderNumber, vin, LocalDateTime.now()));
        } else {
            order.status = ProductionOrderStatus.MATERIAL_PENDING;
            order.domainEvents.add(new MaterialShortageEvent(
                    id, orderNumber, bomSnapshot.getMissingMaterials()));
        }

        return order;
    }

    private ProductionOrder(
            ProductionOrderId id,
            ProductionOrderNumber orderNumber,
            OrderId sourceOrderId,
            VIN vin,
            BomSnapshot bomSnapshot,
            List<AssemblyStep> assemblySteps) {
        this.id = Objects.requireNonNull(id);
        this.orderNumber = Objects.requireNonNull(orderNumber);
        this.sourceOrderId = Objects.requireNonNull(sourceOrderId);
        this.vin = Objects.requireNonNull(vin);
        this.bomSnapshot = bomSnapshot;
        this.assemblyProcess = new AssemblyProcess(
                new AssemblyProcessId(UUID.randomUUID()), assemblySteps);
        this.createdAt = LocalDateTime.now();
    }

    // === Behavior Methods ===

    /**
     * 開始生產
     */
    public void startProduction() {
        if (status != ProductionOrderStatus.SCHEDULED) {
            throw new ProductionOrderStateException(
                "Cannot start production in status: " + status);
        }
        this.status = ProductionOrderStatus.IN_PRODUCTION;
        this.assemblyProcess.start();
        domainEvents.add(new ProductionStartedEvent(id, vin, LocalDateTime.now()));
    }

    /**
     * 完成組裝步驟
     * 委派給 AssemblyProcess，但由 Aggregate Root 控制狀態轉換
     */
    public void completeAssemblyStep(
            AssemblyStepId stepId,
            String operatorId,
            MaterialBatchId batchId,
            Duration actualTime) {

        if (status != ProductionOrderStatus.IN_PRODUCTION) {
            throw new ProductionOrderStateException(
                "Cannot perform assembly in status: " + status);
        }

        AssemblyStepCompletionResult result =
                assemblyProcess.completeStep(stepId, operatorId, batchId, actualTime);

        // BR-09: 超時告警
        if (result.overtimeAlert()) {
            domainEvents.add(new AssemblyOvertimeAlertEvent(
                    id, stepId, actualTime));
        }

        // 組裝全部完成 → 轉入待檢
        if (result.assemblyCompleted()) {
            this.status = ProductionOrderStatus.ASSEMBLY_COMPLETED;
            domainEvents.add(new AssemblyCompletedEvent(id, vin, LocalDateTime.now()));
        }
    }

    /**
     * 記錄品檢通過
     * 由 Application Service 在 QualityInspection 判定合格後呼叫
     */
    public void markInspectionPassed() {
        if (status != ProductionOrderStatus.ASSEMBLY_COMPLETED) {
            throw new ProductionOrderStateException(
                "Inspection only after assembly completed");
        }
        this.status = ProductionOrderStatus.INSPECTION_PASSED;
        domainEvents.add(new VehicleCompletedEvent(id, vin, sourceOrderId, LocalDateTime.now()));
    }

    /**
     * 記錄品檢失敗
     */
    public void markInspectionFailed(String reason) {
        if (status != ProductionOrderStatus.ASSEMBLY_COMPLETED) {
            throw new ProductionOrderStateException(
                "Inspection only after assembly completed");
        }
        this.status = ProductionOrderStatus.INSPECTION_FAILED;
        domainEvents.add(new InspectionFailedEvent(id, vin, reason));
    }

    /**
     * BR-13: 判斷是否可變更
     */
    public boolean isModifiable() {
        return status == ProductionOrderStatus.SCHEDULED
            || status == ProductionOrderStatus.MATERIAL_PENDING;
    }

    // === Domain Event 收集 ===
    public List<DomainEvent> getDomainEvents() {
        return Collections.unmodifiableList(domainEvents);
    }

    public void clearDomainEvents() {
        domainEvents.clear();
    }

    // === Getters (no setters) ===
    public ProductionOrderId getId() { return id; }
    public ProductionOrderNumber getOrderNumber() { return orderNumber; }
    public VIN getVin() { return vin; }
    public ProductionOrderStatus getStatus() { return status; }
}

public enum ProductionOrderStatus {
    MATERIAL_PENDING,
    SCHEDULED,
    IN_PRODUCTION,
    ASSEMBLY_COMPLETED,
    INSPECTION_PASSED,
    INSPECTION_FAILED,
    REWORK_IN_PROGRESS
}
```

#### 3.2.5 Aggregate Root — QualityInspection

```java
/**
 * 品質檢驗 — 獨立 Aggregate
 *
 * 為什麼獨立於 ProductionOrder？
 * 1. 不同的業務生命週期（品檢可重做多次）
 * 2. 不同的操作者（品檢員 vs 產線操作員）
 * 3. 避免 ProductionOrder Aggregate 過大，減少鎖競爭
 * 4. 透過 ProductionOrderId 引用，最終一致性
 */
public class QualityInspection {

    private final QualityInspectionId id;
    private final ProductionOrderId productionOrderId;  // 引用，非包含
    private final VIN vin;
    private final List<InspectionItem> items;
    private InspectionResult result;
    private String inspectorId;
    private String reviewerId;  // BR-12: 四眼原則
    private LocalDateTime inspectedAt;
    private LocalDateTime reviewedAt;

    public static QualityInspection create(
            QualityInspectionId id,
            ProductionOrderId productionOrderId,
            VIN vin,
            List<InspectionChecklistEntry> checklist) {

        List<InspectionItem> items = checklist.stream()
                .map(entry -> new InspectionItem(
                        new InspectionItemId(UUID.randomUUID()),
                        entry.description(),
                        entry.isSafetyRelated()))
                .toList();

        return new QualityInspection(id, productionOrderId, vin, items);
    }

    /**
     * 記錄單項檢驗結果
     */
    public void recordItemResult(
            InspectionItemId itemId,
            InspectionItemStatus status,
            String notes) {

        InspectionItem item = findItem(itemId);
        item.recordResult(status, notes);
    }

    /**
     * 完成檢驗 — 評估整體結果
     * BR-10: 安全項目不合格即判定整車不合格
     * BR-11: 非安全項目最多 3 項有條件通過
     */
    public void complete(String inspectorId) {
        if (!allItemsInspected()) {
            throw new IllegalStateException("Not all items have been inspected");
        }

        this.inspectorId = inspectorId;
        this.inspectedAt = LocalDateTime.now();

        // BR-10
        boolean safetyFailed = items.stream()
                .anyMatch(item -> item.isSafetyRelated()
                        && item.getStatus() == InspectionItemStatus.FAILED);

        if (safetyFailed) {
            this.result = InspectionResult.FAILED;
            return;
        }

        // BR-11
        long conditionalCount = items.stream()
                .filter(item -> item.getStatus() == InspectionItemStatus.CONDITIONAL)
                .count();

        boolean anyFailed = items.stream()
                .anyMatch(item -> item.getStatus() == InspectionItemStatus.FAILED);

        if (anyFailed || conditionalCount > 3) {
            this.result = InspectionResult.FAILED;
        } else if (conditionalCount > 0) {
            this.result = InspectionResult.CONDITIONAL_PASS;
        } else {
            this.result = InspectionResult.PASSED;
        }
    }

    /**
     * BR-12: 複核（四眼原則）
     */
    public void review(String reviewerId) {
        if (this.result == null) {
            throw new IllegalStateException("Inspection must be completed before review");
        }
        if (reviewerId.equals(this.inspectorId)) {
            throw new IllegalArgumentException(
                "Reviewer must be different from inspector (four-eyes principle)");
        }
        this.reviewerId = reviewerId;
        this.reviewedAt = LocalDateTime.now();
    }

    public boolean isReviewed() {
        return reviewerId != null;
    }

    public boolean isPassed() {
        return isReviewed()
            && (result == InspectionResult.PASSED
                || result == InspectionResult.CONDITIONAL_PASS);
    }

    private boolean allItemsInspected() {
        return items.stream().allMatch(InspectionItem::isInspected);
    }

    private InspectionItem findItem(InspectionItemId itemId) {
        return items.stream()
                .filter(i -> i.getId().equals(itemId))
                .findFirst()
                .orElseThrow();
    }
}

public enum InspectionResult {
    PASSED, CONDITIONAL_PASS, FAILED
}
```

### 3.3 Domain Events

```java
/**
 * Domain Event 基底 — 所有事件共用的元資料
 */
public abstract class DomainEvent {
    private final UUID eventId;
    private final LocalDateTime occurredAt;

    protected DomainEvent() {
        this.eventId = UUID.randomUUID();
        this.occurredAt = LocalDateTime.now();
    }

    public UUID getEventId() { return eventId; }
    public LocalDateTime getOccurredAt() { return occurredAt; }
}

/**
 * 車輛完工事件 — 跨 Bounded Context 通訊
 * Manufacturing → Logistics / Order Management
 */
public class VehicleCompletedEvent extends DomainEvent {
    private final ProductionOrderId productionOrderId;
    private final VIN vin;
    private final OrderId sourceOrderId;
    private final LocalDateTime completedAt;

    // constructor, getters...
}

/**
 * 組裝超時告警事件 — 通知生產監控
 */
public class AssemblyOvertimeAlertEvent extends DomainEvent {
    private final ProductionOrderId productionOrderId;
    private final AssemblyStepId stepId;
    private final Duration actualTime;

    // constructor, getters...
}
```

### 3.4 Domain Service

```java
/**
 * Domain Service — 跨 Aggregate 的業務邏輯
 *
 * 使用時機: 當業務邏輯不自然屬於任何單一 Aggregate 時
 * 此服務協調 ProductionOrder 與 QualityInspection 兩個 Aggregate
 */
public class InspectionCompletionService {

    /**
     * 完成品檢流程 — 需同時更新兩個 Aggregate
     * 這個邏輯不屬於 ProductionOrder 也不屬於 QualityInspection
     * 因此放在 Domain Service
     */
    public InspectionCompletionResult completeInspection(
            ProductionOrder productionOrder,
            QualityInspection inspection) {

        if (!inspection.isReviewed()) {
            throw new IllegalStateException(
                "Inspection must be reviewed before completion");
        }

        if (inspection.isPassed()) {
            productionOrder.markInspectionPassed();
            return InspectionCompletionResult.passed(productionOrder.getVin());
        } else {
            productionOrder.markInspectionFailed(
                "Inspection failed: see inspection " + inspection.getId());
            return InspectionCompletionResult.failed(
                productionOrder.getVin(), inspection.getFailedItems());
        }
    }
}
```

---

## 4. Ports（六角形架構介面層）

### 4.1 Output Ports（Domain Layer 定義）

```java
/**
 * Repository Port — 由 Domain Layer 定義介面
 * 體現 Dependency Inversion: Domain 定義契約，Infrastructure 實作
 *
 * ISP 原則: 拆分為 Reader 與 Writer
 */
public interface ProductionOrderRepository {

    Optional<ProductionOrder> findById(ProductionOrderId id);

    Optional<ProductionOrder> findByOrderNumber(ProductionOrderNumber orderNumber);

    ProductionOrder save(ProductionOrder order);

    List<ProductionOrder> findByStatus(ProductionOrderStatus status);
}

/**
 * 事件發佈 Port
 */
public interface DomainEventPublisher {
    void publish(DomainEvent event);
    void publishAll(List<DomainEvent> events);
}

/**
 * 物料查詢 Gateway Port — 跨 Bounded Context
 */
public interface MaterialAvailabilityGateway {
    MaterialAvailabilityResult checkAvailability(BomSnapshot bom);
}
```

### 4.2 Input Ports（Application Layer 定義）

```java
/**
 * Input Port — Use Case 介面
 * Driving Adapter (REST Controller) 呼叫此介面
 */
public interface CreateProductionOrderUseCase {
    ProductionOrderCreatedDto execute(CreateProductionOrderCommand command);
}

public interface CompleteAssemblyStepUseCase {
    AssemblyStepResultDto execute(CompleteAssemblyStepCommand command);
}

public interface CompleteInspectionUseCase {
    InspectionResultDto execute(CompleteInspectionCommand command);
}
```

---

## 5. Application Layer（Use Case 實作）

```java
/**
 * Application Service — 編排 Use Case
 *
 * 職責:
 * 1. 接收 Command DTO
 * 2. 載入 Aggregate
 * 3. 呼叫 Domain Model 行為方法
 * 4. 儲存 Aggregate
 * 5. 發佈 Domain Events
 *
 * 不包含業務邏輯！業務邏輯在 Domain Layer
 */
@RequiredArgsConstructor
public class CreateProductionOrderUseCaseImpl
        implements CreateProductionOrderUseCase {

    private final ProductionOrderRepository repository;
    private final MaterialAvailabilityGateway materialGateway;
    private final BomExpansionService bomExpansionService;  // Domain Service
    private final DomainEventPublisher eventPublisher;
    private final ProductionOrderNumberGenerator numberGenerator;

    @Override
    public ProductionOrderCreatedDto execute(CreateProductionOrderCommand command) {
        // 1. 展開 BOM
        BomSnapshot bom = bomExpansionService.expand(
                command.vehicleModelCode(), command.selectedOptions());

        // 2. 檢查物料可用性
        MaterialAvailabilityResult availability =
                materialGateway.checkAvailability(bom);

        // 3. 建立 Aggregate（業務邏輯封裝在 Factory Method 中）
        ProductionOrder order = ProductionOrder.create(
                new ProductionOrderId(UUID.randomUUID()),
                numberGenerator.generate(command.factoryCode()),
                new OrderId(command.sourceOrderId()),
                new VIN(command.vin()),
                bom,
                buildAssemblySteps(command.vehicleModelCode()),
                availability.isFullyAvailable());

        // 4. 儲存
        repository.save(order);

        // 5. 發佈 Domain Events
        eventPublisher.publishAll(order.getDomainEvents());
        order.clearDomainEvents();

        return ProductionOrderCreatedDto.from(order);
    }
}
```

---

## 6. Infrastructure Layer（Adapters）

### 6.1 Inbound Adapter — REST Controller

```java
/**
 * Inbound Adapter (Driving Adapter)
 * 將 HTTP 請求轉換為 Use Case Command
 * 不包含任何業務邏輯
 */
@RestController
@RequestMapping("/api/v1/production-orders")
@RequiredArgsConstructor
public class ProductionOrderController {

    private final CreateProductionOrderUseCase createUseCase;
    private final CompleteAssemblyStepUseCase completeStepUseCase;

    @PostMapping
    public ResponseEntity<ProductionOrderCreatedDto> create(
            @RequestBody @Valid CreateProductionOrderRequest request) {

        // 僅做 DTO 轉換，不含業務邏輯
        CreateProductionOrderCommand command = request.toCommand();
        ProductionOrderCreatedDto result = createUseCase.execute(command);

        return ResponseEntity
                .created(URI.create("/api/v1/production-orders/" + result.id()))
                .body(result);
    }

    @PostMapping("/{orderId}/assembly-steps/{stepId}/complete")
    public ResponseEntity<AssemblyStepResultDto> completeStep(
            @PathVariable UUID orderId,
            @PathVariable UUID stepId,
            @RequestBody @Valid CompleteAssemblyStepRequest request) {

        CompleteAssemblyStepCommand command = new CompleteAssemblyStepCommand(
                orderId, stepId,
                request.operatorId(),
                request.materialBatchId(),
                request.actualMinutes());

        return ResponseEntity.ok(completeStepUseCase.execute(command));
    }
}
```

### 6.2 Outbound Adapter — JPA Repository

```java
/**
 * Outbound Adapter (Driven Adapter)
 * 實作 Domain Layer 定義的 Repository Port
 *
 * 注意: JPA Entity (ProductionOrderJpaEntity) ≠ Domain Entity (ProductionOrder)
 * 必須透過 Mapper 轉換，避免 JPA 註解污染 Domain Layer
 */
@Repository
@RequiredArgsConstructor
public class JpaProductionOrderRepository implements ProductionOrderRepository {

    private final ProductionOrderJpaRepository jpaRepository;
    private final ProductionOrderMapper mapper;

    @Override
    public Optional<ProductionOrder> findById(ProductionOrderId id) {
        return jpaRepository.findById(id.value())
                .map(mapper::toDomain);
    }

    @Override
    public ProductionOrder save(ProductionOrder order) {
        ProductionOrderJpaEntity entity = mapper.toJpa(order);
        ProductionOrderJpaEntity saved = jpaRepository.save(entity);
        return mapper.toDomain(saved);
    }

    @Override
    public List<ProductionOrder> findByStatus(ProductionOrderStatus status) {
        return jpaRepository.findByStatus(status.name())
                .stream()
                .map(mapper::toDomain)
                .toList();
    }
}

/**
 * JPA Entity — 純粹的持久化模型，與 Domain Model 分離
 */
@Entity
@Table(name = "production_orders")
public class ProductionOrderJpaEntity {

    @Id
    private UUID id;

    @Column(name = "order_number", unique = true)
    private String orderNumber;

    @Column(name = "vin")
    private String vin;

    @Column(name = "status")
    @Enumerated(EnumType.STRING)
    private String status;

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "production_order_id")
    private List<AssemblyStepJpaEntity> assemblySteps;

    // JPA requires default constructor
    protected ProductionOrderJpaEntity() {}
}
```

### 6.3 Outbound Adapter — Event Publisher

```java
/**
 * Event Publisher Adapter — 將 Domain Events 發佈到 Message Broker
 */
@Component
@RequiredArgsConstructor
public class KafkaDomainEventPublisher implements DomainEventPublisher {

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final ObjectMapper objectMapper;

    @Override
    public void publish(DomainEvent event) {
        String topic = resolveTopicFor(event);
        kafkaTemplate.send(topic, event.getEventId().toString(), event);
    }

    @Override
    public void publishAll(List<DomainEvent> events) {
        events.forEach(this::publish);
    }

    private String resolveTopicFor(DomainEvent event) {
        return switch (event) {
            case VehicleCompletedEvent e -> "manufacturing.vehicle-completed";
            case MaterialShortageEvent e -> "manufacturing.material-shortage";
            case AssemblyOvertimeAlertEvent e -> "manufacturing.assembly-alert";
            default -> "manufacturing.domain-events";
        };
    }
}
```

### 6.4 Inbound Adapter — Event Consumer

```java
/**
 * Event Consumer Adapter — 監聽來自其他 Bounded Context 的事件
 */
@Component
@RequiredArgsConstructor
public class OrderEventConsumer {

    private final CreateProductionOrderUseCase createProductionOrderUseCase;

    @KafkaListener(topics = "order.order-placed")
    public void onOrderPlaced(OrderPlacedExternalEvent event) {
        // ACL: 將外部事件轉換為內部 Command
        CreateProductionOrderCommand command = new CreateProductionOrderCommand(
                event.getOrderId(),
                event.getVin(),
                event.getModelCode(),
                event.getSelectedOptions(),
                event.getFactoryCode()
        );
        createProductionOrderUseCase.execute(command);
    }
}
```

---

## 7. 測試策略

### 7.1 測試金字塔

```
         ╱  E2E Tests  ╲           少量 — 驗證完整流程
        ╱───────────────╲
       ╱ Integration Tests╲        中量 — 驗證 Adapter 正確性
      ╱─────────────────────╲
     ╱    Unit Tests          ╲    大量 — 驗證 Domain Logic
    ╱──────────────────────────╲
   ╱   Architecture Tests        ╲  ArchUnit — 驗證依賴規則
  ╱────────────────────────────────╲
```

### 7.2 Domain Layer 單元測試（無 Spring Context）

```java
/**
 * Aggregate Root 單元測試
 * 注意: 純 Java 測試，無 Spring、無 Mock framework
 */
class ProductionOrderTest {

    @Test
    void should_create_order_as_scheduled_when_material_available() {
        ProductionOrder order = ProductionOrder.create(
                new ProductionOrderId(UUID.randomUUID()),
                new ProductionOrderNumber("PO-SH-202502-00001"),
                new OrderId(UUID.randomUUID()),
                new VIN("1HGCM82633A004352"),
                createSampleBom(),
                createSampleSteps(),
                true  // material available
        );

        assertThat(order.getStatus()).isEqualTo(ProductionOrderStatus.SCHEDULED);
        assertThat(order.getDomainEvents())
                .hasSize(1)
                .first()
                .isInstanceOf(ProductionOrderScheduledEvent.class);
    }

    @Test
    void should_create_order_as_material_pending_when_shortage() {
        ProductionOrder order = ProductionOrder.create(
                aProductionOrderId(), aProductionOrderNumber(),
                anOrderId(), aVin(),
                createSampleBom(), createSampleSteps(),
                false  // material NOT available
        );

        assertThat(order.getStatus())
                .isEqualTo(ProductionOrderStatus.MATERIAL_PENDING);
        assertThat(order.getDomainEvents())
                .extracting(DomainEvent::getClass)
                .contains(MaterialShortageEvent.class);
    }

    @Test
    void should_reject_assembly_when_not_in_production() {
        ProductionOrder order = createScheduledOrder();
        // 未開始生產就嘗試完成組裝步驟

        assertThatThrownBy(() ->
            order.completeAssemblyStep(
                aStepId(), "OP001",
                new MaterialBatchId("BAT-001"),
                new Duration(30)))
            .isInstanceOf(ProductionOrderStateException.class);
    }

    @Test
    void should_transition_to_assembly_completed_when_all_steps_done() {
        ProductionOrder order = createInProductionOrder();

        // 依序完成所有組裝步驟
        completeAllSteps(order);

        assertThat(order.getStatus())
                .isEqualTo(ProductionOrderStatus.ASSEMBLY_COMPLETED);
        assertThat(order.getDomainEvents())
                .extracting(DomainEvent::getClass)
                .contains(AssemblyCompletedEvent.class);
    }
}

/**
 * 品質檢驗業務規則測試
 */
class QualityInspectionTest {

    @Test
    void should_fail_when_safety_item_fails() {
        // BR-10: 安全項目不合格即判定整車不合格
        QualityInspection inspection = createInspectionWithItems(
                safetyItem("Brake System"),
                normalItem("Paint Quality")
        );

        inspection.recordItemResult(
                brakeSystemId, InspectionItemStatus.FAILED, "Brake pad worn");
        inspection.recordItemResult(
                paintQualityId, InspectionItemStatus.PASSED, "OK");

        inspection.complete("QC001");

        assertThat(inspection.getResult()).isEqualTo(InspectionResult.FAILED);
    }

    @Test
    void should_enforce_four_eyes_principle() {
        // BR-12: 品檢與複核不可為同一人
        QualityInspection inspection = createCompletedInspection("QC001");

        assertThatThrownBy(() -> inspection.review("QC001"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("four-eyes");
    }
}
```

### 7.3 Architecture Tests (ArchUnit)

```java
/**
 * 架構合規測試 — 確保六角形架構的依賴規則不被破壞
 */
class ArchitectureTest {

    private final JavaClasses classes = new ClassFileImporter()
            .importPackages("com.automfg.manufacturing");

    @Test
    void domain_should_not_depend_on_spring() {
        noClasses()
                .that().resideInAPackage("..domain..")
                .should().dependOnClassesThat()
                .resideInAnyPackage(
                        "org.springframework..",
                        "javax.persistence..",
                        "jakarta.persistence..")
                .check(classes);
    }

    @Test
    void domain_should_not_depend_on_infrastructure() {
        noClasses()
                .that().resideInAPackage("..domain..")
                .should().dependOnClassesThat()
                .resideInAPackage("..infrastructure..")
                .check(classes);
    }

    @Test
    void application_should_not_depend_on_infrastructure() {
        noClasses()
                .that().resideInAPackage("..application..")
                .should().dependOnClassesThat()
                .resideInAPackage("..infrastructure..")
                .check(classes);
    }

    @Test
    void infrastructure_adapters_should_implement_ports() {
        classes()
                .that().resideInAPackage("..adapter.outbound..")
                .should().implement(resideInAPackage("..domain.port.."))
                .check(classes);
    }
}
```

---

## 8. 技術棧

| 層級 | 技術 | 版本 |
|------|------|------|
| Language | Java | 21 (LTS) |
| Framework | Spring Boot | 3.3.x |
| Build | Maven (Multi-Module) | 3.9.x |
| Persistence | Spring Data JPA + PostgreSQL | — |
| Messaging | Apache Kafka | 3.7.x |
| Testing | JUnit 5 + AssertJ + ArchUnit | — |
| API Doc | SpringDoc OpenAPI | 2.x |
| Containerization | Docker + Docker Compose | — |

---

## 9. 設計決策紀錄 (ADR 摘要)

| ADR | 決策 | 理由 |
|-----|------|------|
| ADR-001 | Domain Model 使用 Java Record 作為 Value Object | Immutable by default，減少 boilerplate |
| ADR-002 | JPA Entity 與 Domain Entity 分離 | 避免 JPA 註解污染 Domain Layer，符合 DIP |
| ADR-003 | Aggregate 間使用 Domain Event 通訊 | 最終一致性，降低耦合，支援 Saga Pattern |
| ADR-004 | BOM 建單時快照 | 避免事後 BOM 變更影響已排程工單 |
| ADR-005 | QualityInspection 獨立 Aggregate | 避免 ProductionOrder 過大，不同操作者的並發寫入 |
| ADR-006 | ArchUnit 守護架構規則 | 自動化檢測，防止架構腐化 |

---

## 10. 狀態機

### 10.1 Production Order 狀態轉換

```
                              ┌──────────────────┐
                              │ MATERIAL_PENDING  │
                              │                  │◄── 物料到齊 ──┐
                              └────────┬─────────┘              │
                                       │                        │
          ┌────────────────────────────▼──────────┐             │
          │              SCHEDULED                 │─────────────┘
          └────────────────────────────┬──────────┘
                                       │ startProduction()
                                       ▼
          ┌───────────────────────────────────────┐
          │            IN_PRODUCTION               │
          │  (AssemblyProcess 推進中)               │
          └────────────────────────────┬──────────┘
                                       │ 所有步驟完成
                                       ▼
          ┌───────────────────────────────────────┐
          │        ASSEMBLY_COMPLETED              │
          └──────────┬─────────────────┬──────────┘
                     │                 │
          品檢通過    │                 │  品檢失敗
                     ▼                 ▼
    ┌──────────────────┐    ┌─────────────────────┐
    │ INSPECTION_PASSED │    │  INSPECTION_FAILED   │
    │                  │    │                     │
    │ → VehicleCompleted│    │ → 建立返工單          │
    │   Event 發佈      │    │                     │
    └──────────────────┘    └─────────────────────┘
```

---

## 11. PoC 驗證範圍

| 驗證項目 | 驗證方式 |
|----------|----------|
| Aggregate 不變條件 | Domain Unit Test — 測試所有 BR 規則 |
| 六角形架構依賴方向 | ArchUnit Test — 自動化架構守護 |
| Domain Event 流轉 | Integration Test — Embedded Kafka |
| Repository 正確性 | Integration Test — Testcontainers + PostgreSQL |
| 端到端流程 | E2E Test — 從下單到完工的完整 Happy Path |
