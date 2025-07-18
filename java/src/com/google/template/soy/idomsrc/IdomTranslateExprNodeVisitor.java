/*
 * Copyright 2019 Google Inc.
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

import static com.google.common.collect.ImmutableSet.toImmutableSet;
import static com.google.template.soy.idomsrc.IdomRuntime.INCREMENTAL_DOM_EVAL_LOG_FN;
import static com.google.template.soy.idomsrc.IdomRuntime.SOY_IDOM_EMPTY_TO_UNDEFINED;
import static com.google.template.soy.idomsrc.IdomRuntime.SOY_IDOM_HAS_CONTENT;
import static com.google.template.soy.idomsrc.IdomRuntime.SOY_IDOM_IS_TRUTHY;
import static com.google.template.soy.idomsrc.IdomRuntime.SOY_IDOM_IS_TRUTHY_NON_EMPTY;
import static com.google.template.soy.idomsrc.IdomRuntime.STATE_PREFIX;
import static com.google.template.soy.idomsrc.IdomRuntime.STATE_VAR_PREFIX;
import static com.google.template.soy.jssrc.dsl.Expressions.id;
import static com.google.template.soy.jssrc.internal.JsRuntime.BIND_TEMPLATE_PARAMS_FOR_IDOM;
import static com.google.template.soy.jssrc.internal.JsRuntime.XID;

import com.google.common.base.Ascii;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.google.common.collect.Sets;
import com.google.protobuf.Descriptors.Descriptor;
import com.google.template.soy.base.SoyBackendKind;
import com.google.template.soy.base.internal.SanitizedContentKind;
import com.google.template.soy.error.ErrorReporter;
import com.google.template.soy.exprtree.FunctionNode;
import com.google.template.soy.exprtree.ProtoEnumValueNode;
import com.google.template.soy.jssrc.dsl.Expression;
import com.google.template.soy.jssrc.dsl.Expressions;
import com.google.template.soy.jssrc.dsl.GoogRequire;
import com.google.template.soy.jssrc.dsl.SourceMapHelper;
import com.google.template.soy.jssrc.internal.GenJsCodeVisitor.ScopedJsTypeRegistry;
import com.google.template.soy.jssrc.internal.JavaScriptValueFactoryImpl;
import com.google.template.soy.jssrc.internal.JsType;
import com.google.template.soy.jssrc.internal.TemplateAliases;
import com.google.template.soy.jssrc.internal.TranslateExprNodeVisitor;
import com.google.template.soy.jssrc.internal.TranslationContext;
import com.google.template.soy.logging.LoggingFunction;
import com.google.template.soy.shared.internal.BuiltinFunction;
import com.google.template.soy.soytree.defn.TemplateStateVar;
import com.google.template.soy.types.SoyType;
import com.google.template.soy.types.SoyType.Kind;
import com.google.template.soy.types.SoyTypes;
import com.google.template.soy.types.TemplateType;

/** Translates expressions, overriding methods for special-case idom behavior. */
final class IdomTranslateExprNodeVisitor extends TranslateExprNodeVisitor {
  public IdomTranslateExprNodeVisitor(
      JavaScriptValueFactoryImpl javaScriptValueFactory,
      TranslationContext translationContext,
      TemplateAliases templateAliases,
      ErrorReporter errorReporter,
      ScopedJsTypeRegistry jsTypeRegistry,
      SourceMapHelper sourceMapHelper) {
    super(
        javaScriptValueFactory,
        translationContext,
        templateAliases,
        errorReporter,
        jsTypeRegistry,
        sourceMapHelper);
  }

  @Override
  protected Expression genCodeForStateAccess(String paramName, TemplateStateVar stateVar) {
    return id(STATE_VAR_PREFIX + STATE_PREFIX + paramName);
  }

  @Override
  protected Expression sanitizedContentToProtoConverterFunction(Descriptor messageType) {
    return IdomRuntime.IDOM_JS_TO_PROTO_PACK_FN.get(messageType.getFullName());
  }

  @Override
  protected Expression visitFunctionNode(FunctionNode node) {
    Object soyFunction = node.getSoyFunction();

    if (soyFunction instanceof LoggingFunction) {
      LoggingFunction loggingNode = (LoggingFunction) soyFunction;
      return INCREMENTAL_DOM_EVAL_LOG_FN.call(
          XID.call(Expressions.stringLiteral(node.getStaticFunctionName())),
          Expressions.arrayLiteral(visitChildren(node)),
          Expressions.stringLiteral(loggingNode.getPlaceholder()));
    }
    // Use module syntax when generating toggle code for IDOM
    if (soyFunction instanceof BuiltinFunction) {
      if (soyFunction == BuiltinFunction.EVAL_TOGGLE) {
        return super.visitToggleFunction(node, true);
      }
    }
    return super.visitFunctionNode(node);
  }

  @Override
  protected Expression visitEmptyToUndefinedFunction(FunctionNode node) {
    return SOY_IDOM_EMPTY_TO_UNDEFINED.call(visit(node.getParam(0)));
  }

  @Override
  protected Expression genCodeForBind(
      Expression template, Expression paramRecord, SoyType templateType) {
    // Unions are enforced to have the same content kind in CheckTemplateCallsPass.
    SanitizedContentKind kind =
        Iterables.getOnlyElement(
                SoyTypes.flattenUnion(templateType)
                    .map(type -> ((TemplateType) type).getContentKind())
                    .collect(toImmutableSet()))
            .getSanitizedContentKind();
    if (kind.isHtml() || kind == SanitizedContentKind.ATTRIBUTES) {
      return BIND_TEMPLATE_PARAMS_FOR_IDOM.call(template, paramRecord);
    } else {
      return super.genCodeForBind(template, paramRecord, templateType);
    }
  }

  /** Types that might possibly be idom function callbacks, which always need custom truthiness. */
  private static final ImmutableSet<Kind> FUNCTION_TYPES =
      Sets.immutableEnumSet(Kind.HTML, Kind.ELEMENT, Kind.ATTRIBUTES, Kind.UNKNOWN, Kind.ANY);

  @Override
  protected Expression maybeCoerceToBoolean(SoyType type, Expression chunk, boolean force) {
    if (SoyTypes.containsKinds(type, FUNCTION_TYPES)) {
      return SOY_IDOM_IS_TRUTHY.call(chunk);
    }

    return super.maybeCoerceToBoolean(type, chunk, force);
  }

  @Override
  protected Expression isTruthyNonEmpty(Expression chunk) {
    return SOY_IDOM_IS_TRUTHY_NON_EMPTY.call(chunk);
  }

  @Override
  protected Expression hasContent(Expression chunk) {
    return SOY_IDOM_HAS_CONTENT.call(chunk);
  }

  @Override
  protected JsType jsTypeForStrict(SoyType type) {
    return JsType.forIdomSrcState().get(type);
  }

  @Override
  protected JsType jsTypeFor(SoyType type) {
    return JsType.forIdomSrc().get(type);
  }

  @Override
  protected Expression visitProtoEnumValueNode(ProtoEnumValueNode node) {
    return GoogRequire.create(node.getType().getNameForBackend(SoyBackendKind.JS_SRC))
        .googModuleGet()
        .dotAccess(Ascii.toUpperCase(node.getEnumValueDescriptor().getName()));
  }
}
