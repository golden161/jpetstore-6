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
package org.mybatis.jpetstore.service;

/**
 * Thrown when an order cannot be fulfilled because a line item's inventory is insufficient.
 * <p>
 * Deliberately unchecked so that it triggers Spring's default transaction rollback (invariant #3: the whole order —
 * including any line items already deducted in the same transaction — is rolled back), and so that it does not force
 * the isolation-zone web layer to change method signatures (that user-facing message is handled by a separate ticket).
 * Per B4 assumption A1 only the offending {@code itemId} is carried; the shortfall amount is not, to avoid leaking
 * stock levels to oversell probes.
 *
 * @author Claude
 */
public class OutOfStockException extends RuntimeException {

  private static final long serialVersionUID = 1L;

  private final String itemId;

  /**
   * Instantiates a new out-of-stock exception.
   *
   * @param itemId
   *          the id of the item that was out of stock
   */
  public OutOfStockException(String itemId) {
    super("Item " + itemId + " is out of stock.");
    this.itemId = itemId;
  }

  /**
   * Gets the id of the item that could not be fulfilled.
   *
   * @return the item id
   */
  public String getItemId() {
    return itemId;
  }

}
