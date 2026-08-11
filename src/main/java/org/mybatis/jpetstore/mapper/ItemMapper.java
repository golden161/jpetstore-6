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
package org.mybatis.jpetstore.mapper;

import java.util.List;
import java.util.Map;

import org.mybatis.jpetstore.domain.Item;

/**
 * The Interface ItemMapper.
 *
 * @author Eduardo Macarron
 */
public interface ItemMapper {

  /**
   * Unconditionally decrements inventory, allowing the quantity to go negative.
   *
   * @param param
   *          map carrying {@code itemId} and {@code increment}
   *
   * @deprecated since B4. Prefer {@link #updateInventoryQuantityIfAvailable(Map)}, which refuses to oversell. Retained
   *             (with its characterization test) only until the follow-up milestone removes it per the B4 ticket, step
   *             4.
   */
  @Deprecated
  void updateInventoryQuantity(Map<String, Object> param);

  /**
   * Conditionally decrements inventory, only when enough stock is on hand.
   *
   * @param param
   *          map carrying {@code itemId} and {@code increment}
   *
   * @return {@code 1} when stock was sufficient and has been deducted; {@code 0} when insufficient, leaving the
   *         inventory row unchanged
   */
  int updateInventoryQuantityIfAvailable(Map<String, Object> param);

  /**
   * Get inventory quantity.
   *
   * @param itemId
   *          the item id
   *
   * @return the int
   */
  int getInventoryQuantity(String itemId);

  /**
   * Get item list by product.
   *
   * @param productId
   *          the product id
   *
   * @return the list
   */
  List<Item> getItemListByProduct(String productId);

  /**
   * Get item.
   *
   * @param itemId
   *          the item id
   *
   * @return the item
   */
  Item getItem(String itemId);

}
