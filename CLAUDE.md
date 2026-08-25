# jpetstore-6 协作宪法（CLAUDE.md）—— 存量项目版

## 这是什么项目
MyBatis 官方示例宠物店：Java 21 · MyBatis 3.5 · Spring 事务 · Stripes 1.6（已停维护的
Web 框架）· HSQLDB。本仓库被本团队用作存量改造演练场。

## 六条宪法（同 LibraryHub，违反 = 阻断）
工单先行 / 验收对照举证 / 一里程碑一提交 ≤400 行 / 不变量是红线 /
如实未完成是合法产出 / 变更必落单。

## 黄金层与隔离区（手册 6.5）
- **黄金层（照此写）**：`service/`（构造注入 + @Transactional）、`domain/`（POJO +
  BigDecimal 金额）、`mapper/`（接口 + XML 分离）；测试照 `mapper/*MapperTest` 与
  `service/*ServiceTest` 的既有模式。
- **隔离区（只许阅读与最小修补，禁止模仿，禁止新增同类）**：
  `web/actions/`（Stripes ActionBean：@SessionScope 会话状态、JSP 路径字符串常量、
  transient @SpringBean 字段注入）与 `src/main/webapp/WEB-INF/jsp/`。
  UI 演进路线见绞杀者决策（手册 6.6），本期不动。

## 一张地图
不变量：docs/invariants.md · 术语：docs/glossary.md · 工单：docs/tasks/
数据库脚本（真实 schema/种子）：src/main/resources/database/

## 仲裁序
任务工单 > docs/invariants.md > 本文件 > 代码现状。
**特别提醒**：隔离区代码量大且自洽，频率引力很强——它不是范本，是病灶（手册 1.4）。
