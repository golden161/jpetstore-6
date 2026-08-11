# 工单 B0：考古时间盒（半天）

- 委托级别：高（纯阅读，零风险） · 时间盒：4 小时，到点收口

## 目标
产出四件套草案：地图 v0 / 不变量草案（每条带 文件:行 证据）/ 术语地雷 / 黄金与隔离区普查。

## 范围
**In：** 只读全仓库 + 运行一次构建与测试。 **Out：** 任何代码修改。

## 验收（C 级探索型 + 证据）
1. 地图 v0：分层结构、各包职责、一句话数据流（下单路径）。
2. ≥3 条不变量草案，逐条给出 文件:行。
3. ≥2 颗术语地雷（含 Product vs Item）。
4. 黄金层/隔离区清单及理由。
5. 标注"怪行为"：与直觉相悖但真实存在的行为，至少 1 条（提示：往库存方向看）。

## 建议命令
find src -name "*.java" | wc -l · ./mvnw test -D"license.skip=true" ·
grep -rn "@Transactional" src/main · grep -n "QTY" src/main/resources/org/mybatis/jpetstore/mapper/ItemMapper.xml
