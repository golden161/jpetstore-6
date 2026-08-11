/*
 *    Copyright 2010-2026 the original author or authors.
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *       https://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */
/*
 * 教学材料（场景 B2）：特征测试 —— 现状即规格。
 * 放置：src/test/java/org/mybatis/jpetstore/mapper/InventoryCharacterizationTest.java
 * 运行：./mvnw test -Dtest=InventoryCharacterizationTest -D"license.skip=true"
 *
 * 它锁定的是系统"今天的真实行为"（库存可为负），不代表该行为正确。
 * 在工单 B4 把行为改为"缺货下单失败"之前，本测试必须保持绿色；
 * B4 合并时按其第 4 步流程显式退役——静默改写本测试会被监理拦下（见 B5 样例）。
 */
package org.mybatis.jpetstore.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.transaction.annotation.Transactional;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = MapperTestContext.class)
@Transactional
class InventoryCharacterizationTest {

  @Autowired
  private ItemMapper mapper;

  @Test
  void currentBehavior_inventoryCanGoNegative() {
    // given：EST-1 的当前库存（来自仓库自带种子 jpetstore-hsqldb-dataload.sql）
    int before = mapper.getInventoryQuantity("EST-1");
    assertThat(before).isPositive();

    // when：一次性扣减"超过现有库存 5 个"的数量
    Map<String, Object> param = new HashMap<>(2);
    param.put("itemId", "EST-1");
    param.put("increment", before + 5);
    mapper.updateInventoryQuantity(param);

    // then：现状——扣减照常成功，库存变成 -5（ItemMapper.xml:76-80 无下限保护）
    assertThat(mapper.getInventoryQuantity("EST-1")).isEqualTo(-5);
  }
}
