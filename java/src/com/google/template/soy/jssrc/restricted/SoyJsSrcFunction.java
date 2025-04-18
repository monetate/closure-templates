/*
 * Copyright 2009 Google Inc.
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

package com.google.template.soy.jssrc.restricted;

import com.google.template.soy.shared.restricted.SoyFunction;
import java.util.List;

/**
 * Interface for a Soy function implemented for the JS Source backend.
 *
 * @deprecated Switch to using SoyJavaScriptSourceFunction. It has the ability to depend on {@code
 *     goog.module} symbols and is a safer, more typesafe API.
 */
@Deprecated
public interface SoyJsSrcFunction extends SoyFunction {

  /**
   * Computes this function on the given arguments for the JS Source backend.
   *
   * @param args The function arguments.
   * @return The computed result of this function.
   */
  JsExpr computeForJsSrc(List<JsExpr> args);
}
