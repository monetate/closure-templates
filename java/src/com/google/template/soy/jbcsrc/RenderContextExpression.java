/*
 * Copyright 2017 Google Inc.
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

package com.google.template.soy.jbcsrc;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.template.soy.jbcsrc.restricted.BytecodeUtils.constant;

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.google.template.soy.data.LoggingAdvisingAppendable;
import com.google.template.soy.data.RecordProperty;
import com.google.template.soy.data.SoyValue;
import com.google.template.soy.data.internal.ParamStore;
import com.google.template.soy.jbcsrc.restricted.BytecodeUtils;
import com.google.template.soy.jbcsrc.restricted.CodeBuilder;
import com.google.template.soy.jbcsrc.restricted.Expression;
import com.google.template.soy.jbcsrc.restricted.JbcSrcPluginContext;
import com.google.template.soy.jbcsrc.restricted.MethodRef;
import com.google.template.soy.jbcsrc.restricted.SoyExpression;
import com.google.template.soy.jbcsrc.restricted.Statement;
import com.google.template.soy.jbcsrc.shared.ExtraConstantBootstraps;
import com.google.template.soy.jbcsrc.shared.RenderContext;
import com.google.template.soy.jbcsrc.shared.StackFrame;
import com.google.template.soy.msgs.restricted.SoyMsgRawParts;
import java.lang.invoke.MethodHandles;
import javax.annotation.Nullable;
import org.objectweb.asm.ConstantDynamic;
import org.objectweb.asm.Handle;
import org.objectweb.asm.Type;

/** An expression for a {@link RenderContext} object. */
final class RenderContextExpression extends Expression implements JbcSrcPluginContext {

  private static final MethodRef GET_DELTEMPLATE =
      MethodRef.createNonPure(RenderContext.class, "getDelTemplate", String.class, String.class);

  private static final MethodRef RENDER_MODIFIABLE =
      MethodRef.createNonPure(
          RenderContext.class,
          "renderModifiable",
          String.class,
          StackFrame.class,
          ParamStore.class,
          LoggingAdvisingAppendable.class);

  private static final MethodRef GET_PLUGIN_INSTANCE =
      MethodRef.createNonPure(RenderContext.class, "getPluginInstance", String.class);

  private static final MethodRef GET_LOCALE =
      MethodRef.createNonPure(RenderContext.class, "getLocale");

  private static final MethodRef GET_PRINT_DIRECTIVE =
      MethodRef.createNonPure(RenderContext.class, "getPrintDirective", String.class);

  private static final MethodRef GET_SOY_MSG_PARTS =
      MethodRef.createNonPure(
              RenderContext.class, "getSoyMsgParts", long.class, SoyMsgRawParts.class)
          .asNonJavaNullable();

  private static final MethodRef GET_SOY_MSG_PARTS_NO_DEFAULT =
      MethodRef.createNonPure(RenderContext.class, "getSoyMsgParts", long.class)
          .asNonJavaNullable();

  private static final MethodRef GET_SOY_MSG_PARTS_WITH_ALTERNATE_ID =
      MethodRef.createNonPure(
              RenderContext.class,
              "getSoyMsgPartsWithAlternateId",
              long.class,
              SoyMsgRawParts.class,
              long.class)
          .asNonJavaNullable();

  private static final MethodRef GET_SOY_MSG_PARTS_WITH_ALTERNATE_ID_NO_DEFAULT =
      MethodRef.createNonPure(
              RenderContext.class, "getSoyMsgPartsWithAlternateId", long.class, long.class)
          .asNonJavaNullable();

  private static final MethodRef GET_BASIC_SOY_MSG_PART =
      MethodRef.createNonPure(RenderContext.class, "getBasicSoyMsgPart", long.class, String.class)
          .asNonJavaNullable();

  private static final MethodRef GET_BASIC_SOY_MSG_PART_NO_DEFAULT =
      MethodRef.createNonPure(RenderContext.class, "getBasicSoyMsgPart", long.class)
          .asNonJavaNullable();

  private static final MethodRef GET_BASIC_SOY_MSG_PART_WITH_ALTERNATE_ID =
      MethodRef.createNonPure(
              RenderContext.class,
              "getBasicSoyMsgPartWithAlternateId",
              long.class,
              String.class,
              long.class)
          .asNonJavaNullable();

  private static final MethodRef GET_BASIC_SOY_MSG_PART_NO_DEFAULT_WITH_ALTERNATE_ID =
      MethodRef.createNonPure(
              RenderContext.class, "getBasicSoyMsgPartWithAlternateId", long.class, long.class)
          .asNonJavaNullable();

  private static final MethodRef RENAME_CSS_SELECTOR =
      MethodRef.createNonPure(RenderContext.class, "renameCssSelector", String.class)
          .asNonJavaNullable();

  private static final MethodRef RECORD_JS_ID =
      MethodRef.createNonPure(RenderContext.class, "recordJsId", String.class);

  private static final MethodRef RENAME_XID_AND_RECORD_JS_ID =
      MethodRef.createNonPure(RenderContext.class, "renameXidAndRecordJsId", String.class);

  private static final MethodRef EVAL_TOGGLE =
      MethodRef.createNonPure(RenderContext.class, "evalToggle", String.class);

  private static final MethodRef RENAME_XID =
      MethodRef.createNonPure(RenderContext.class, "renameXid", String.class).asNonJavaNullable();

  private static final MethodRef USE_PRIMARY_MSG_IF_FALLBACK =
      MethodRef.createNonPure(
          RenderContext.class, "usePrimaryMsgIfFallback", long.class, long.class);

  private static final MethodRef USE_PRIMARY_OR_ALTERNATE_IF_FALLBACK =
      MethodRef.createNonPure(
          RenderContext.class,
          "usePrimaryOrAlternateIfFallback",
          long.class,
          long.class,
          long.class);

  private static final MethodRef USE_PRIMARY_IF_FALLBACK_OR_FALLBACK_ALTERNATE =
      MethodRef.createNonPure(
          RenderContext.class,
          "usePrimaryIfFallbackOrFallbackAlternate",
          long.class,
          long.class,
          long.class);

  private static final MethodRef USE_PRIMARY_OR_ALTERNATE_IF_FALLBACK_OR_FALLBACK_ALTERNATE =
      MethodRef.createNonPure(
          RenderContext.class,
          "usePrimaryOrAlternateIfFallbackOrFallbackAlternate",
          long.class,
          long.class,
          long.class,
          long.class);

  private static final MethodRef GET_DEBUG_SOY_TEMPLATE_INFO =
      MethodRef.createNonPure(RenderContext.class, "getDebugSoyTemplateInfo");

  private static final MethodRef GET_BIDI_GLOBAL_DIR =
      MethodRef.createNonPure(RenderContext.class, "getBidiGlobalDir");

  private static final MethodRef GET_BIDI_GLOBAL_DIR_DIR =
      MethodRef.createNonPure(RenderContext.class, "getBidiGlobalDirDir");

  private static final MethodRef GET_ALL_REQUIRED_CSS_NAMESPACES =
      MethodRef.createNonPure(RenderContext.class, "getAllRequiredCssNamespaces", String.class);

  private static final MethodRef GET_ALL_REQUIRED_CSS_PATHS =
      MethodRef.createNonPure(RenderContext.class, "getAllRequiredCssPaths", String.class);

  private static final MethodRef GET_ESCAPING_DIRECTIVE_AS_FUNCTION =
      MethodRef.createNonPure(RenderContext.class, "getEscapingDirectiveAsFunction", String.class);

  private static final MethodRef GET_RENDER_CSS_HELPER =
      MethodRef.createNonPure(RenderContext.class, "getRenderCssHelper");

  private static final MethodRef GET_CONST =
      MethodRef.createNonPure(RenderContext.class, "getConst", String.class);

  private static final MethodRef STORE_CONST =
      MethodRef.createNonPure(RenderContext.class, "storeConst", String.class, Object.class);

  private static final MethodRef TRACK_REQUIRED_CSS =
      MethodRef.createNonPure(
          RenderContext.class, "trackRequiredCss", RenderContext.CssToTrack.class);

  private static final MethodRef GET_INJECTED_PARAMETER =
      MethodRef.createPure(RenderContext.class, "getInjectedValue", RecordProperty.class);

  private static final MethodRef GET_INJECTED_PARAMETER_DEFAULT =
      MethodRef.createPure(
          RenderContext.class, "getInjectedValue", RecordProperty.class, SoyValue.class);

  private static final Type CSS_TO_TRACK_TYPE = Type.getType(RenderContext.CssToTrack.class);
  private static final Handle CSS_TO_TRACK_HANDLE =
      MethodRef.createPure(
              ExtraConstantBootstraps.class,
              "cssToTrack",
              MethodHandles.Lookup.class,
              String.class,
              Class.class,
              String.class)
          .asHandle();

  private final Expression delegate;

  RenderContextExpression(Expression renderContext) {
    super(renderContext.resultType(), renderContext.features());
    this.delegate = renderContext;
  }

  @Override
  protected void doGen(CodeBuilder adapter) {
    delegate.gen(adapter);
  }

  @Override
  public Expression getBidiGlobalDir() {
    return delegate.invoke(GET_BIDI_GLOBAL_DIR);
  }

  public Expression getBidiGlobalDirDir() {
    return delegate.invoke(GET_BIDI_GLOBAL_DIR_DIR);
  }

  @Override
  public Expression getAllRequiredCssNamespaces(SoyExpression template) {
    return delegate.invoke(GET_ALL_REQUIRED_CSS_NAMESPACES, template.unboxAsStringUnchecked());
  }

  @Override
  public Expression getAllRequiredCssPaths(SoyExpression template) {
    return delegate.invoke(GET_ALL_REQUIRED_CSS_PATHS, template.unboxAsStringUnchecked());
  }

  Expression getRenderCssHelper() {
    return delegate.invoke(GET_RENDER_CSS_HELPER);
  }

  Expression getDebugSoyTemplateInfo() {
    return delegate.invoke(GET_DEBUG_SOY_TEMPLATE_INFO);
  }

  Expression getPluginInstance(String pluginName) {
    return delegate.invoke(GET_PLUGIN_INSTANCE, constant(pluginName));
  }

  Expression renameXid(String value) {
    return delegate.invoke(RENAME_XID, constant(value));
  }

  Expression renameCss(String value) {
    return delegate.invoke(RENAME_CSS_SELECTOR, constant(value));
  }

  Expression recordJsId(SoyExpression value) {
    return delegate.invoke(RECORD_JS_ID, value);
  }

  Expression renameXidAndRecordJsId(SoyExpression value) {
    return delegate.invoke(RENAME_XID_AND_RECORD_JS_ID, value);
  }

  Expression evalToggle(String toggleName) {
    return delegate.invoke(EVAL_TOGGLE, constant(toggleName));
  }

  Expression getDeltemplate(String delCalleeName, Expression variantExpr) {
    return delegate.invoke(GET_DELTEMPLATE, constant(delCalleeName), variantExpr);
  }

  Expression renderModifiable(
      String delCalleeName,
      Expression stackFrame,
      Expression params,
      Expression appendableExpression) {
    return delegate.invoke(
        RENDER_MODIFIABLE, constant(delCalleeName), stackFrame, params, appendableExpression);
  }

  @Override
  public Expression getULocale() {
    return delegate.invoke(GET_LOCALE);
  }

  Expression getSoyMsgParts(long id, @Nullable Expression defaultParts) {
    return defaultParts == null
        ? delegate.invoke(GET_SOY_MSG_PARTS_NO_DEFAULT, constant(id))
        : delegate.invoke(GET_SOY_MSG_PARTS, constant(id), defaultParts);
  }

  Expression getSoyMsgPartsWithAlternateId(
      long id, @Nullable Expression defaultParts, long alternateId) {
    return defaultParts == null
        ? delegate.invoke(
            GET_SOY_MSG_PARTS_WITH_ALTERNATE_ID_NO_DEFAULT, constant(id), constant(alternateId))
        : delegate.invoke(
            GET_SOY_MSG_PARTS_WITH_ALTERNATE_ID, constant(id), defaultParts, constant(alternateId));
  }

  Expression getBasicSoyMsgPart(long id, @Nullable Expression defaultPart) {
    return defaultPart == null
        ? delegate.invoke(GET_BASIC_SOY_MSG_PART_NO_DEFAULT, constant(id))
        : delegate.invoke(GET_BASIC_SOY_MSG_PART, constant(id), defaultPart);
  }

  Expression getBasicSoyMsgPartWithAlternateId(
      long id, @Nullable Expression defaultPart, long alternateId) {
    return defaultPart == null
        ? delegate.invoke(
            GET_BASIC_SOY_MSG_PART_NO_DEFAULT_WITH_ALTERNATE_ID,
            constant(id),
            constant(alternateId))
        : delegate.invoke(
            GET_BASIC_SOY_MSG_PART_WITH_ALTERNATE_ID,
            constant(id),
            defaultPart,
            constant(alternateId));
  }

  Expression getPrintDirective(String name) {
    return delegate.invoke(GET_PRINT_DIRECTIVE, constant(name));
  }

  Expression getEscapingDirectiveAsFunction(String name) {
    return delegate.invoke(GET_ESCAPING_DIRECTIVE_AS_FUNCTION, constant(name));
  }

  Expression getConst(String name) {
    return delegate.invoke(GET_CONST, constant(name));
  }

  Statement storeConst(String name, Expression value) {
    return delegate.invokeVoid(STORE_CONST, constant(name), value);
  }

  Statement trackRequiredCss(ImmutableList<String> cssPaths, ImmutableList<String> cssNamespaces) {
    checkArgument(!cssPaths.isEmpty() || !cssNamespaces.isEmpty());
    // Concatenate the paths and namespaces into a single string, adding the number of paths
    // as the initial value so we can tell where paths end and namespaces begin.
    // This is parsed in `ExtraConstantBootstraps.cssToTrack`.  Alternatively, we could pass an
    // array of these values but having a single string is useful since we also use it as a key for
    // interning.
    StringBuilder sb = new StringBuilder();
    sb.append(cssPaths.size()).append(",");
    Joiner.on(',').appendTo(sb, Iterables.concat(cssPaths, cssNamespaces));
    return delegate.invokeVoid(
        TRACK_REQUIRED_CSS,
        constant(
            CSS_TO_TRACK_TYPE,
            new ConstantDynamic(
                "cssToTrack",
                CSS_TO_TRACK_TYPE.getDescriptor(),
                CSS_TO_TRACK_HANDLE,
                constant(sb.toString()).constantBytecodeValue()),
            Features.of(Feature.NON_JAVA_NULLABLE)));
  }

  Expression usePrimaryMsgIfFallback(long msgId, long fallbackId) {
    return delegate.invoke(USE_PRIMARY_MSG_IF_FALLBACK, constant(msgId), constant(fallbackId));
  }

  Expression usePrimaryOrAlternateIfFallback(long msgId, long alternateId, long fallbackId) {
    return delegate.invoke(
        USE_PRIMARY_OR_ALTERNATE_IF_FALLBACK,
        constant(msgId),
        constant(alternateId),
        constant(fallbackId));
  }

  Expression usePrimaryIfFallbackOrFallbackAlternate(
      long msgId, long fallbackId, long fallbackAlternateId) {
    return delegate.invoke(
        USE_PRIMARY_IF_FALLBACK_OR_FALLBACK_ALTERNATE,
        constant(msgId),
        constant(fallbackId),
        constant(fallbackAlternateId));
  }

  Expression usePrimaryOrAlternateIfFallbackOrFallbackAlternate(
      long msgId, long alternateId, long fallbackId, long fallbackAlternateId) {
    return delegate.invoke(
        USE_PRIMARY_OR_ALTERNATE_IF_FALLBACK_OR_FALLBACK_ALTERNATE,
        constant(msgId),
        constant(alternateId),
        constant(fallbackId),
        constant(fallbackAlternateId));
  }

  public Expression getInjectedValue(String property, @Nullable SoyExpression value) {
    var prop = BytecodeUtils.constantRecordProperty(property);
    if (value == null) {
      return delegate.invoke(GET_INJECTED_PARAMETER, prop);
    }
    return delegate.invoke(GET_INJECTED_PARAMETER_DEFAULT, prop, value);
  }
}
