# 不变量清单（红线）—— 由 B0 考古从真实代码提炼

> 行号锚定提交 54f13af。触碰前停下询问。

| # | 不变量 | 证据（文件:行） | 下沉状态 |
|---|--------|----------------|----------|
| 1 | 业务主键一律经 SEQUENCE 表 `getNextId()` 发号，禁用数据库自增 | OrderService.java:121-130；schema 的 `sequence` 表 | 未下沉（评审把守）；候选：ArchUnit 规则 |
| 2 | 金额一律 BigDecimal，禁 double/float 参与 | Order.java:50 `private BigDecimal totalPrice` 等 | 未下沉（评审把守）；候选：checkstyle 正则 |
| 3 | 下单与扣库存必须同一事务 | OrderService.java:59 `@Transactional` 覆盖 insertOrder 全程 | 已下沉（Spring 事务） |
| 4 | Cart 由会话承载，服务层不得持会话状态 | service/ 三个类均无状态字段 | 评审把守 |

## 怪行为登记（现状 ≠ 正确，改动需走工单决策）
- **INVENTORY.QTY 可被扣减为负**：`ItemMapper.xml:76-80` 的 UPDATE 无下限条件、无锁；
  `inventory` 表 DDL 仅 `qty int not null`，无 CHECK。
  已由特征测试锁定（characterization/InventoryCharacterizationTest.java）。
  处置：工单 B4 将其改为"缺货下单失败"，特征测试随之按流程退役。
