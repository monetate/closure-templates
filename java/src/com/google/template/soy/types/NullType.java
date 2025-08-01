/*
 * Copyright 2013 Google Inc.
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

package com.google.template.soy.types;

import com.google.template.soy.soytree.SoyTypeP;

/**
 * The "null" type.
 */
public final class NullType extends PrimitiveType {

  private static final NullType INSTANCE = new NullType();

  // Not constructible - use getInstance().
  private NullType() {}

  @Override
  public Kind getKind() {
    return Kind.NULL;
  }

  @Override
  public String toString() {
    return "null";
  }

  @Override
  protected void doToProto(SoyTypeP.Builder builder) {
    builder.setPrimitive(SoyTypeP.PrimitiveTypeP.NULL);
  }

  /** Return the single instance of this type. */
  public static NullType getInstance() {
    return INSTANCE;
  }
}
