# 工单 B4：缺货下单应失败（防负库存）★ 主线生产任务

- 委托级别：中（黄金层内改造，但触及核心下单路径 → 验收按隐蔽工程级全审）
- 前提：B2 特征测试已合并且为绿（安全网先于改造存在）

## 背景与目标
现状（怪行为登记 #1）：库存可被扣成负数。馆…商家口径：**任一行项目库存不足时，
整单失败，库存分毫不动**。

## 范围
**In：** ItemMapper.java / ItemMapper.xml / OrderService.java / 新增 OutOfStockException /
对应测试。
**Out：** web/actions 与 JSP 的提示文案（另单）、Stripes 升级、并发锁方案重设计、
立即删除旧 mapper 方法（按微型绞杀者分步走）。

## 前置阅读
docs/invariants.md（#1/#3 与怪行为登记）→ docs/glossary.md（Item vs Product）→
现有 OrderServiceTest 的 Mockito 模式。

## 实施路线：微型绞杀者（手册 6.6 的方法用在一条 SQL 上）
1. **新增**条件扣减 `updateInventoryQuantityIfAvailable`（`AND QTY >= #{increment}`，
   返回受影响行数）——旧方法原样保留。
2. OrderService.insertOrder 切换到新方法；`updated == 0` 时抛 OutOfStockException，
   事务回滚（不变量 #3 保证已扣行项目一并回滚）。
3. 旧方法标 `@Deprecated`，全仓库确认无其他调用方。
4. 下一里程碑删除旧方法与旧特征测试（删除需在工单变更记录中显式声明）。**✅ 2026-08-25 完成，见文末《变更记录》。**

## 验收标准（A 级）
1. 库存充足：下单成功，各行项目库存正确扣减（既有行为不回归）。
2. 任一行项目缺货：抛 OutOfStockException，订单未落库，**所有**库存不变。
3. 新增测试覆盖 1/2；既有测试全绿：`./mvnw test -D"license.skip=true"`。
4. B2 特征测试按第 4 步流程退役，diff 中附说明（监理会查，见 B5）。

## 假设与默认值
- A1 "缺多少"不在错误信息中暴露给前端（避免超卖探测）——仅记录 itemId。

---

## 变更记录

### 2026-08-25 · 里程碑 1（步骤 1-3）—— 提交 `9769397`（PR #2 已合并）
新增 `updateInventoryQuantityIfAvailable`（`AND QTY >= #{increment}`，返回受影响行数）+
`OutOfStockException`；`OrderService.insertOrder` 切换调用，`updated == 0` 抛异常整单回滚；
旧 `updateInventoryQuantity` 原样保留并标 `@Deprecated`。测试 88 绿。

### 2026-08-25 · 里程碑 2（步骤 4）—— 退役旧路径【**本记录即删除的显式声明**】

**显式声明删除以下四处**（微型绞杀者收口，非静默改写）：

| # | 删除对象 | 位置 | 删除理由 |
|---|----------|------|----------|
| 1 | `@Deprecated void updateInventoryQuantity(Map)` + 其 Javadoc | `mapper/ItemMapper.java` | 无条件扣减入口；全仓库零调用方（main 已于步骤 2 切走） |
| 2 | `<update id="updateInventoryQuantity">` 语句块 | `resources/.../mapper/ItemMapper.xml` | 上者的 SQL 实现，随之失效 |
| 3 | 测试方法 `ItemMapperTest.updateInventoryQuantity()` | `test/.../mapper/ItemMapperTest.java` | 只为验证被删方法而存在 |
| 4 | **整个文件** `InventoryCharacterizationTest.java`（B2 特征测试） | `test/.../mapper/` | **按 B4 步骤 4 流程退役**：它锁定的「扣超变负」怪行为只能经第 1 项触发，行为已消除、断言不再可达。安全网使命完成，非因碍事而删 |

**保留不动**：`updateInventoryQuantityIfAvailable`（接口 + XML）、`OutOfStockException`、
`OrderService.insertOrder` 的守卫路径、`OrderServiceTest` 的 2 个新用例、
`ItemMapperTest.updateInventoryQuantityIfAvailable*` 2 个用例（缺货保护的新安全网）；
`getInventoryQuantity` / `CatalogService.isItemInStock` 与本单无关，未触碰。

**举证**：
- `grep -rn "updateInventoryQuantity\b" src/main src/test | grep -v IfAvailable` → 零命中。
- 全量测试绿：`Tests run: 86, Failures: 0, Errors: 0`（88 → 86，正为删掉的 2 个用例；
  命令 `JAVA_HOME=…/21.0.5-tem ./mvnw clean test -Dlicense.skip=true`；
  首跑需 `clean`，否则 surefire 会跑 `target/` 里的旧特征测试 `.class`）。
- 验收标准 4「B2 特征测试按第 4 步流程退役，diff 中附说明」→ 本节即说明。

**文档同步**：`docs/invariants.md` 怪行为登记 #1 标记为「已消除·登记退役」并记残留；
`docs/map.md` 第 2/4 节、构件清单、调用面重锚到当前代码。

**如实未完成（本次不做，建议另单）**：`src/site/{,ja/,ko/,es/}xdoc/index.xml` 四份上游站点
教程仍引用已删除的 `updateInventoryQuantity` 作示例代码——属上游文档、不在本单范围（Out），
且四语同改会撑爆本里程碑的单一提交。数据库 DDL 仍无 `CHECK (QTY >= 0)`，并发与
「加购只判 >0」的 TOCTOU 口径不一致亦未处理（见 map.md 第 4 节残留）。
