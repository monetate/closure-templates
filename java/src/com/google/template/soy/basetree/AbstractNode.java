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

package com.google.template.soy.basetree;

/** Abstract implementation of a Node. */
public abstract class AbstractNode implements Node {

  /** The parent of this node. */
  private ParentNode<?> parent;

  protected AbstractNode() {}

  /**
   * Copy constructor.
   *
   * @param orig The node to copy.
   */
  protected AbstractNode(AbstractNode orig, CopyState copyState) {
    // important: should not copy parent pointer
  }

  @Override
  public void setParent(ParentNode<?> parent) {
    this.parent = parent;
  }

  @Override
  public ParentNode<?> getParent() {
    return parent;
  }

  @Override
  public final boolean hasAncestor(Class<? extends Node> ancestorClass) {
    return getNearestAncestor(ancestorClass) != null;
  }

  @Override
  public <N extends Node> N getNearestAncestor(Class<N> ancestorClass) {
    for (Node node = this; node != null; node = node.getParent()) {
      if (ancestorClass.isInstance(node)) {
        return ancestorClass.cast(node);
      }
    }
    return null;
  }

  @Override
  public final int hashCode() {
    return super.hashCode();
  }

  @Override
  public final boolean equals(Object other) {
    return super.equals(other);
  }

  @Override
  public String toString() {
    String sourceString = toSourceString();
    sourceString =
        sourceString.length() > 30 ? sourceString.substring(0, 30) + "..." : sourceString;
    return this.getClass().getSimpleName() + "<" + sourceString + ">";
  }
}
