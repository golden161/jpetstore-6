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
4. 下一里程碑删除旧方法与旧特征测试（删除需在工单变更记录中显式声明）。

## 验收标准（A 级）
1. 库存充足：下单成功，各行项目库存正确扣减（既有行为不回归）。
2. 任一行项目缺货：抛 OutOfStockException，订单未落库，**所有**库存不变。
3. 新增测试覆盖 1/2；既有测试全绿：`./mvnw test -D"license.skip=true"`。
4. B2 特征测试按第 4 步流程退役，diff 中附说明（监理会查，见 B5）。

## 假设与默认值
- A1 "缺多少"不在错误信息中暴露给前端（避免超卖探测）——仅记录 itemId。
