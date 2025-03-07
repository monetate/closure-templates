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

/**
 * External interface for a node visitor. A visitor is basically a function implemented for some or
 * all node classes, where the implementation can be different for each specific node class.
 *
 * @param <N> A more specific subinterface of Node, or just Node if not applicable.
 * @param <R> The return type of this visitor.
 */
public interface NodeVisitor<N extends Node, R> {

  /**
   * Executes the function defined by this visitor.
   *
   * @param node The node to execute the function on.
   */
  R exec(N node);
}
