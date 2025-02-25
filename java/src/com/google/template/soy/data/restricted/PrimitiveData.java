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

package com.google.template.soy.data.restricted;

import com.google.template.soy.data.LoggingAdvisingAppendable;
import com.google.template.soy.data.SoyValue;
import java.io.IOException;

/**
 * Abstract superclass for a node in a Soy data tree that represents a piece of primitive data (i.e.
 * a leaf node).
 */
public abstract class PrimitiveData extends SoyValue {
  @Override
  public void render(LoggingAdvisingAppendable appendable) throws IOException {
    // PrimitiveData instances can't really benefit from any incremental approach anyway.
    appendable.append(coerceToString());
  }
}
