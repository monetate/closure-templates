/*
 * Copyright 2021 Google Inc.
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

package com.google.template.soy.soytree;

import static com.google.common.collect.Sets.union;

import com.google.template.soy.base.SourceFilePath;
import com.google.template.soy.base.internal.SoyFileKind;
import java.util.Set;

/** Metadata about a soy file that is available as soon as its AST is parsed. */
public interface PartialFileMetadata {

  SourceFilePath getPath();

  String getNamespace();

  Set<String> getTemplateNames();

  default boolean hasSymbol(String symbolName) {
    return hasTemplate(symbolName)
        || hasConstant(symbolName)
        || hasExtern(symbolName)
        || hasTypeDef(symbolName);
  }

  default Set<String> allSymbolNames() {
    return union(
        getTemplateNames(), union(getConstantNames(), union(getExternNames(), getTypeDefNames())));
  }

  default boolean hasTemplate(String shortName) {
    return getTemplateNames().contains(shortName);
  }

  Set<String> getConstantNames();

  default boolean hasConstant(String shortName) {
    return getConstantNames().contains(shortName);
  }

  Set<String> getExternNames();

  default boolean hasExtern(String shortName) {
    return getExternNames().contains(shortName);
  }

  boolean isOverloadedExtern(String shortName);

  Set<String> getTypeDefNames();

  default boolean hasTypeDef(String shortName) {
    return getTypeDefNames().contains(shortName);
  }

  SoyFileKind getSoyFileKind();
}
