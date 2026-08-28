# 承重墙地图 v0 —— 由 B0 考古产出

> 行号锚定提交 `54f13af`。链路与不变量的红线以 [invariants.md](invariants.md) 为准，
> 术语口径以 [glossary.md](glossary.md) 为准；本文件只画拓扑，不重复红线正文（避免双份漂移）。
>
> 漂移提示：`web/actions/CartActionBean.java` 工作区有未提交改动，但仅第 2 行版权头一换一
> （不增删行），故下文所有行号与 HEAD 一致。
>
> **B4 更新（第 2/4 节，2026-08-25）**：库存扣减链路已随工单 B4 改造完成（新增条件扣减 →
> 切换调用 → 旧方法与 B2 特征测试第 4 步退役删除），本文件第 2/4 节及构件清单的
> **扣减相关行号已重锚到 B4 完结的当前代码**；其余章节仍锚 `54f13af`。

## 1. 分层结构与各包职责

四层，自上而下调用；`web/actions` 是隔离区，其余为黄金层（手册 6.5）。

| 层 | 包 | 职责 | 写法特征 | 归属 |
|----|----|------|----------|------|
| Web | `web/actions/` | Stripes ActionBean：会话态、JSP 路径常量、请求编排 | `@SessionScope` + `transient @SpringBean` 字段注入 + JSP 路径 String 常量 | **隔离区**（只读/最小修补，禁模仿禁新增同类） |
| Service | `service/` | 事务边界、业务编排 | 构造注入 + `@Transactional` + 无会话状态字段 | **黄金层** |
| Mapper | `mapper/` (+ `resources/.../mapper/*.xml`) | 数据访问 | 接口与 XML 分离 | **黄金层** |
| Domain | `domain/` | 业务对象 | POJO + 金额用 `BigDecimal` | **黄金层** |
| DB | `resources/database/*.sql` | 真实 schema/种子（HSQLDB） | DDL/DML | 事实源 |

一句话数据流（下单路径）：**购物车（会话态）→ `Order.initOrder` 拷成 `LineItem` 列表 →
`OrderService.insertOrder` 在单个事务内先按每行「购买数量」做「够才扣」的条件扣减 `INVENTORY.QTY`
（任一行不足即抛 `OutOfStockException`，整单回滚、库存分毫不动），
再落订单/订单状态/行项目 → web 层清空购物车。**

## 2. 承重墙地图：库存扣减链路

从「点击」到「SQL」跨三层。**全仓库真正扣库存的只有一处**：`ItemMapper.xml:76-81`
（B4 后为带下限条件的更新；旧无条件 UPDATE 已于 B4 第 4 步删除）。

```
[隔离区/web]  加购  CartActionBean.addItemToCart()            web/actions/CartActionBean.java:68
                     ├─ 实时查库存 isItemInStock(workingItemId) :81   ← 唯一的"库存检查"（只判 >0）
                     └─ cart.addItem(item, isInStock)           :83  → domain/Cart.java:67

[隔离区/web]  下单表单 OrderActionBean.newOrderForm()          web/actions/OrderActionBean.java:119
                     └─ order.initOrder(account, cart)          :129 → domain/Order.java:286
                          └─ 遍历购物车 → addLineItem()          Order.java:318-329
                               └─ new LineItem(n, cartItem)      domain/LineItem.java:50-57  (quantity=购买数)

[隔离区/web]  确认下单 OrderActionBean.newOrder()              web/actions/OrderActionBean.java:142
                     ├─ 已确认时 orderService.insertOrder(order) :152
                     └─ 成功后 cartBean.clear()                  :155

[黄金层/service] OrderService.insertOrder(Order)  @Transactional  service/OrderService.java:75-76
                     ├─ getNextId("ordernum") 发订单号           :77  → getNextId() :140-149 (SEQUENCE)
                     ├─ for each lineItem:                       :78-88
                     │     increment = lineItem.getQuantity()    :80  ← 购买数量
                     │     updateInventoryQuantityIfAvailable()  :84  ★★★ 扣库存（返回受影响行数）
                     │     updated == 0 → OutOfStockException    :85-87 ← 缺货即整单回滚
                     ├─ orderMapper.insertOrder(order)           :90
                     ├─ orderMapper.insertOrderStatus(order)     :91
                     └─ for each lineItem: insertLineItem()      :92-95

[黄金层/mapper] ItemMapper.updateInventoryQuantityIfAvailable(Map)  mapper/ItemMapper.java:39

[SQL]    UPDATE INVENTORY SET QTY = QTY - #{increment}
         WHERE ITEMID = #{itemId}
           AND QTY >= #{increment}                               mapper/ItemMapper.xml:76-81  ★★★

[数据]   INVENTORY 表: qty int not null, PK=itemid             database/jpetstore-hsqldb-schema.sql:154-158
         种子 EST-1 = 10000                                     database/jpetstore-hsqldb-data.sql:240
```

读侧旁路（同一张 INVENTORY 表，供检查/展示，**不参与扣减**）：

- 加购实时检查：`CatalogService.isItemInStock` → `getInventoryQuantity(itemId) > 0`
  `service/CatalogService.java:87-88` → SQL `mapper/ItemMapper.xml:70-74`
- 下单后展示回读：`OrderService.getOrder` 里 `item.setQuantity(getInventoryQuantity(...))`
  `service/OrderService.java:113`

### 构件清单（file:line 出处）

| 角色 | 构件 | 出处 |
|------|------|------|
| 入口·加购 | `CartActionBean.addItemToCart()` | src/main/java/org/mybatis/jpetstore/web/actions/CartActionBean.java:68 |
| 库存检查 | `isItemInStock`（只判 `>0`） | src/main/java/org/mybatis/jpetstore/service/CatalogService.java:87-88 |
| 入口·下单 | `OrderActionBean.newOrder()` → `insertOrder` | src/main/java/org/mybatis/jpetstore/web/actions/OrderActionBean.java:142,152 |
| 购物车→行项目 | `Order.initOrder` / `LineItem(int,CartItem)` | src/main/java/org/mybatis/jpetstore/domain/Order.java:286；.../domain/LineItem.java:50-57 |
| **扣减编排** | `OrderService.insertOrder` `@Transactional` | src/main/java/org/mybatis/jpetstore/service/OrderService.java:75-88 |
| **扣减入口** | `ItemMapper.updateInventoryQuantityIfAvailable` | src/main/java/org/mybatis/jpetstore/mapper/ItemMapper.java:39 |
| **扣减 SQL** | `UPDATE ... QTY = QTY - #{increment} ... AND QTY >= #{increment}` | src/main/resources/org/mybatis/jpetstore/mapper/ItemMapper.xml:76-81 |
| **缺货信号** | `OutOfStockException`（unchecked，只带 itemId） | src/main/java/org/mybatis/jpetstore/service/OutOfStockException.java |
| 库存表 | `INVENTORY(itemid, qty int not null)` 无 CHECK | src/main/resources/database/jpetstore-hsqldb-schema.sql:154-158 |

调用面（封闭，故 B4 得以安全动这条 SQL）：全 main 中 `updateInventoryQuantityIfAvailable` 仅
`OrderService.java:84` 一个调用方；`OrderService.insertOrder` 仅 `OrderActionBean.java:152`
一个调用方。旧的无条件方法 `updateInventoryQuantity` 已在 B4 第 4 步删除，全仓库零调用方。

## 3. 这条链上的承重墙（不变量，详见 invariants.md）

| 不变量 | 关键证据 | 断了会怎样 |
|--------|----------|------------|
| #3 扣库存与落单同一事务 | `OrderService.java:75` `@Transactional` 覆盖 76-96 全程 | 部分扣减/落单不一致；B4 的缺货回滚保证依赖它 |
| #1 主键经 SEQUENCE 发号 | 扣减前 `OrderService.java:77` 调 `getNextId`（读改写 :140-149） | 与自增混用会撞号 |
| #2 金额一律 BigDecimal | `LineItem.java:35,37`；`Order.java:50` | 浮点误差污染金额 |

## 4. 怪行为（现状 ≠ 正确，详见 invariants.md「怪行为登记」）

1. ~~**库存可被扣成负数**~~ —— **已消除，登记退役**（工单 [B4](tasks/B4-inventory-guard.md)
   第 4 步，2026-08-25）：扣减 SQL 现带 `AND QTY >= #{increment}`（`ItemMapper.xml:76-81`），
   受影响行数为 0 即抛 `OutOfStockException` 整单回滚（`OrderService.java:84-87`）。
   旧无条件方法与锁定它的特征测试 `InventoryCharacterizationTest.java` 本次一并删除
   （显式声明见 invariants.md 怪行为登记）。**残留**：表 DDL `schema.sql:154-158` 仍无 CHECK，
   「不为负」由这条 SQL 与评审把守，而非数据库强制。
2. **检查与扣减脱节（TOCTOU）**：全链路唯一库存检查仍是加购时的
   `CartActionBean.java:81` `isItemInStock`，且只判「有没有（`>0`）」不判「够不够（`>=购买数量`）」
   （`CatalogService.java:88`）；到 `OrderService.java:84` 真正扣减之间从不重新检查。
   B4 后果已变：(a) 加购到结账之间被买光 → 结账时整单失败（不再照扣）；
   (b) 一单买 10000+ 个 EST-1 → 整单失败（不再变负）；(c) 并发两单靠单条条件 UPDATE 的原子性
   互斥，不再丢失更新。**仍未解**：用户要到结账那一刻才知道买不到（体验问题，属隔离区文案另单），
   且加购检查的口径（`>0`）与扣减口径（`>=购买数量`）依然不一致。

## 5. 术语地雷（详见 glossary.md）

- **Product vs Item**：扣减键在 **itemId（SKU）** 上（`INVENTORY.itemid` / `WHERE ITEMID`）；
  **Product（款式）无库存**。要"给商品加库存校验"却说成 Product，必改错聚合。
- **quantity 同名反义（就撞在扣减这一行上）**：`LineItem.quantity`（`LineItem.java:33`）=购买数量，
  在 `OrderService.java:64` 化作 `increment`（被减数）；`Item.quantity`（`Item.java:42`）=库存余量，
  读时由 `getItem` 的 `QTY AS quantity` 从 INVENTORY join 填入（`ItemMapper.xml:63`）。同词，站在减法两边。
- **INVENTORY ≠ ITEM**：库存单列在 INVENTORY 表；ITEM DDL（`schema.sql:133-151`）无 qty 列。

---

仲裁序：任务工单 > invariants.md > CLAUDE.md > 代码现状。触碰承重墙前停下询问。
未决：本图 v0.1 —— B4（含第 4 步退役）已同步进第 2/4 节与构件清单；
其余章节行号仍锚 `54f13af`，待下一次考古统一重锚。
