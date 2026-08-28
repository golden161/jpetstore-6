# 不变量清单（红线）—— 由 B0 考古从真实代码提炼

> 行号锚定提交 54f13af（下表）。触碰前停下询问。
> 「怪行为登记」中标注 B4 的条目已重锚到 B4 第 4 步完结的当前代码。

| # | 不变量 | 证据（文件:行） | 下沉状态 |
|---|--------|----------------|----------|
| 1 | 业务主键一律经 SEQUENCE 表 `getNextId()` 发号，禁用数据库自增 | OrderService.java:121-130；schema 的 `sequence` 表 | 未下沉（评审把守）；候选：ArchUnit 规则 |
| 2 | 金额一律 BigDecimal，禁 double/float 参与 | Order.java:50 `private BigDecimal totalPrice` 等 | 未下沉（评审把守）；候选：checkstyle 正则 |
| 3 | 下单与扣库存必须同一事务 | OrderService.java:59 `@Transactional` 覆盖 insertOrder 全程 | 已下沉（Spring 事务） |
| 4 | Cart 由会话承载，服务层不得持会话状态 | service/ 三个类均无状态字段 | 评审把守 |

## 怪行为登记（现状 ≠ 正确，改动需走工单决策）
- ~~**INVENTORY.QTY 可被扣减为负**~~ —— **已消除，登记退役（工单 B4 第 4 步，2026-08-25）**
  - 原状：唯一扣减 SQL 无下限条件（`ItemMapper.xml` 旧 `updateInventoryQuantity`），
    `inventory` 表 DDL 仅 `qty int not null`、无 CHECK；由特征测试
    `mapper/InventoryCharacterizationTest.java` 锁定"扣超变负"。
  - 现状：扣减改为条件更新 `updateInventoryQuantityIfAvailable`
    （`ItemMapper.xml:76-81`，带 `AND QTY >= #{increment}`，单条语句原子生效），
    受影响行数为 0 即抛 `OutOfStockException` 整单回滚（`OrderService.java:84-87`，
    回滚由不变量 #3 保证）。
  - **本次显式声明删除**：旧方法 `ItemMapper.updateInventoryQuantity`（接口 + XML +
    `ItemMapperTest.updateInventoryQuantity`）与特征测试文件
    `InventoryCharacterizationTest.java` 一并删除——它锁定的怪行为已不可达，安全网使命完成。
  - 残留（**未消除**，另单）：DDL 仍无 CHECK 约束，"不为负"由那一条 SQL 与评审把守，
    而非数据库强制；任何绕过该 mapper 方法的新写路径可再次扣成负数。
