/*
 * Copyright 2008 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.template.soy.exprtree;

import com.google.template.soy.base.SourceLocation;
import com.google.template.soy.basetree.CopyState;
import com.google.template.soy.exprtree.ExprNode.PrimitiveNode;

/** Abstract implementation of a PrimitiveNode. */
abstract class AbstractPrimitiveNode extends AbstractExprNode implements PrimitiveNode {

  protected AbstractPrimitiveNode(SourceLocation sourceLocation) {
    super(sourceLocation);
  }

  /**
   * Copy constructor.
   *
   * @param orig The node to copy.
   */
  protected AbstractPrimitiveNode(AbstractPrimitiveNode orig, CopyState copyState) {
    super(orig, copyState);
  }
}
