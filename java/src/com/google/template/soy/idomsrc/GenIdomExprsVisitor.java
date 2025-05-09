/*
 * Copyright 2016 Google Inc.
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

package com.google.template.soy.idomsrc;

import com.google.template.soy.base.internal.SanitizedContentKind;
import com.google.template.soy.error.ErrorReporter;
import com.google.template.soy.jssrc.dsl.Expression;
import com.google.template.soy.jssrc.dsl.SourceMapHelper;
import com.google.template.soy.jssrc.internal.GenCallCodeUtils;
import com.google.template.soy.jssrc.internal.GenJsCodeVisitor.ScopedJsTypeRegistry;
import com.google.template.soy.jssrc.internal.GenJsExprsVisitor;
import com.google.template.soy.jssrc.internal.IsComputableAsJsExprsVisitor;
import com.google.template.soy.jssrc.internal.TemplateAliases;
import com.google.template.soy.jssrc.internal.TranslationContext;
import com.google.template.soy.jssrc.internal.VisitorsState;
import com.google.template.soy.soytree.PrintNode;
import com.google.template.soy.soytree.SoyNode;
import com.google.template.soy.soytree.SoyNode.RenderUnitNode;
import java.util.ArrayList;
import java.util.List;

/**
 * Overrides the base class to provide the correct helpers classes.
 *
 * <p>Also does not wrap function arguments as sanitized content, which is used to prevent
 * re-escaping of safe content. The Incremental DOM code generation use DOM APIs for creating
 * Elements, Text and attributes rather than relying on innerHTML
 */
public final class GenIdomExprsVisitor extends GenJsExprsVisitor {

  public GenIdomExprsVisitor(
      VisitorsState state,
      GenCallCodeUtils genCallCodeUtils,
      IsComputableAsJsExprsVisitor isComputableAsJsExprsVisitor,
      TranslationContext translationContext,
      ErrorReporter errorReporter,
      TemplateAliases templateAliases,
      ScopedJsTypeRegistry jsTypeRegistry,
      SourceMapHelper sourceMapHelper) {
    super(
        state,
        genCallCodeUtils,
        isComputableAsJsExprsVisitor,
        translationContext,
        errorReporter,
        templateAliases,
        jsTypeRegistry,
        sourceMapHelper);
  }

  @Override
  public List<Expression> exec(SoyNode node) {
    chunks = new ArrayList<>();
    visit(node);
    return chunks;
  }

  @Override
  protected Expression maybeAddNodeBuilder(PrintNode node, Expression expr) {
    return expr;
  }

  /** Never wrap contents as SanitizedContent if HTML or ATTRIBUTES. */
  @Override
  protected Expression maybeWrapContent(RenderUnitNode node, Expression content) {
    SanitizedContentKind kind = node.getContentKind();
    if (kind.isHtml() || kind == SanitizedContentKind.ATTRIBUTES) {
      return content;
    }
    return super.maybeWrapContent(node, content);
  }
}
