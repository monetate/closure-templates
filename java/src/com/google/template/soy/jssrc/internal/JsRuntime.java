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

package com.google.template.soy.jssrc.internal;

import static com.google.template.soy.jssrc.dsl.Expressions.dottedIdNoRequire;
import static com.google.template.soy.jssrc.dsl.Expressions.id;

import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.html.types.SafeHtmlProto;
import com.google.common.html.types.SafeScriptProto;
import com.google.common.html.types.SafeStyleProto;
import com.google.common.html.types.SafeStyleSheetProto;
import com.google.common.html.types.SafeUrlProto;
import com.google.common.html.types.TrustedResourceUrlProto;
import com.google.protobuf.Descriptors.Descriptor;
import com.google.protobuf.Descriptors.FieldDescriptor;
import com.google.template.soy.base.internal.SanitizedContentKind;
import com.google.template.soy.data.internalutils.NodeContentKinds;
import com.google.template.soy.internal.proto.ProtoUtils;
import com.google.template.soy.jssrc.dsl.CodeChunk;
import com.google.template.soy.jssrc.dsl.Expression;
import com.google.template.soy.jssrc.dsl.Expressions;
import com.google.template.soy.jssrc.dsl.GoogRequire;
import com.google.template.soy.types.SoyProtoType;

/**
 * Constants for commonly used js runtime functions and objects.
 *
 * <p>Unlike {@code JsExprUtils}, this is only intended for use by the compiler itself and deals
 * exclusively with the {@link CodeChunk} api.
 */
public final class JsRuntime {
  private static final GoogRequire GOOG_ARRAY = GoogRequire.create("goog.array");
  private static final GoogRequire GOOG_ASSERTS = GoogRequire.create("goog.asserts");
  private static final GoogRequire GOOG_STRING = GoogRequire.create("goog.string");

  public static final GoogRequire SOY = GoogRequire.create("soy");
  private static final GoogRequire SOY_NEWMAPS = GoogRequire.create("soy.newmaps");
  public static final GoogRequire SOY_VELOG = GoogRequire.create("soy.velog");
  public static final GoogRequire GOOG_SOY_ALIAS =
      GoogRequire.createWithAlias("goog.soy", "$googSoy");

  private static final GoogRequire SOY_TEMPLATES = GoogRequire.create("soy.templates");

  public static final GoogRequire GOOG_SOY = GoogRequire.create("goog.soy");

  public static final Expression IS_RECORD = SOY.dotAccess("$$isRecord");
  public static final Expression IS_JS = SOY.dotAccess("$$isJS");
  public static final Expression IS_HTML = SOY.dotAccess("$$isHtml");
  public static final Expression IS_CSS = SOY.dotAccess("$$isCss");
  public static final Expression IS_ATTRIBUTE = SOY.dotAccess("$$isAttribute");
  public static final Expression IS_TRUSTED_RESOURCE_URI = SOY.dotAccess("$$isTrustedResourceURI");
  public static final Expression IS_URI = SOY.dotAccess("$$isURI");
  public static final Expression IS_READONLY = SOY.dotAccess("$$isReadonly");

  public static final Expression SOY_STUBS_MAP = SOY.dotAccess("$$stubsMap");

  private static final GoogRequire XID_REQUIRE = GoogRequire.create("xid");

  private JsRuntime() {}

  public static final Expression GOOG_ARRAY_MAP = GOOG_ARRAY.reference().dotAccess("map");

  public static final Expression GOOG_ASSERTS_ASSERT = GOOG_ASSERTS.reference().dotAccess("assert");

  public static final Expression GOOG_DEBUG = dottedIdNoRequire("goog.DEBUG");

  public static final Expression GOOG_GET_CSS_NAME = dottedIdNoRequire("goog.getCssName");

  public static final Expression GOOG_GET_MSG = dottedIdNoRequire("goog.getMsg");

  public static final Expression ARRAY_IS_ARRAY = dottedIdNoRequire("Array.isArray");

  public static final Expression GOOG_IS_FUNCTION = SOY.dotAccess("$$isFunction");

  public static final Expression SOY_EQUALS = SOY.dotAccess("$$equals");

  public static final Expression SOY_MAKE_ARRAY = SOY.dotAccess("$$makeArray");

  public static final Expression SOY_AS_READONLY = SOY.dotAccess("$$asReadonlyArray");

  public static final Expression SOY_FILTER_AND_MAP = SOY.dotAccess("$$filterAndMap");

  public static final Expression GOOG_IS_OBJECT = dottedIdNoRequire("goog.isObject");

  public static final Expression GOOG_REQUIRE = dottedIdNoRequire("goog.require");

  public static final Expression GOOG_MODULE_GET = dottedIdNoRequire("goog.module.get");

  public static final GoogRequire GOOG_SOY_DATA = GoogRequire.create("goog.soy.data");

  public static final Expression GOOG_SOY_DATA_SANITIZED_CONTENT =
      GOOG_SOY_DATA.dotAccess("SanitizedContent");

  public static final Expression SAFEVALUES_SAFEHTML =
      GoogRequire.create("safevalues").dotAccess("SafeHtml");

  public static final Expression GOOG_HTML_SAFE_ATTRIBUTE =
      GOOG_SOY_DATA.dotAccess("SanitizedHtmlAttribute");

  public static final Expression GOOG_STRING_UNESCAPE_ENTITIES =
      GOOG_STRING.dotAccess("unescapeEntities");

  public static final Expression GOOG_I18N_MESSAGE_FORMAT =
      GoogRequire.create("goog.i18n.MessageFormat").reference();

  public static final Expression SOY_ASSERT_PARAM_TYPE = SOY.dotAccess("assertParamType");

  public static final Expression SOY_ASSIGN_DEFAULTS = SOY.dotAccess("$$assignDefaults");

  public static final Expression SOY_CHECK_NOT_NULL = SOY.dotAccess("$$checkNotNull");

  public static final Expression SERIALIZE_KEY = SOY.dotAccess("$$serializeKey");

  public static final Expression SOY_COERCE_TO_BOOLEAN = SOY.dotAccess("$$coerceToBoolean");

  public static final Expression SOY_IS_TRUTHY_NON_EMPTY = SOY.dotAccess("$$isTruthyNonEmpty");

  public static final Expression SOY_HAS_CONTENT = SOY.dotAccess("$$hasContent");

  public static final Expression SOY_IS_ITERABLE = SOY.dotAccess("$$isIterable");

  public static final Expression SOY_EMPTY_TO_UNDEFINED = SOY.dotAccess("$$emptyToUndefined");

  public static final Expression SOY_ESCAPE_HTML = SOY.dotAccess("$$escapeHtml");

  public static final Expression SOY_GET_DELEGATE_FN = SOY.dotAccess("$$getDelegateFn");

  public static final Expression SOY_MAKE_EMPTY_TEMPLATE_FN =
      SOY.dotAccess("$$makeEmptyTemplateFn");

  public static final Expression SOY_REGISTER_DELEGATE_FN = SOY.dotAccess("$$registerDelegateFn");

  public static final Expression SOY_ALIAS_DELEGATE_ID = SOY.dotAccess("$$aliasDelegateId");

  public static final Expression SOY_GET_DELTEMPLATE_ID = SOY.dotAccess("$$getDelTemplateId");

  public static final Expression SOY_IS_LOCALE_RTL = SOY.dotAccess("$$IS_LOCALE_RTL");
  public static final Expression SOY_CREATE_CONST = SOY.dotAccess("$$createConst");
  public static final Expression SOY_GET_CONST = SOY.dotAccess("$$getConst");

  public static final Expression SOY_DEBUG_SOY_TEMPLATE_INFO =
      SOY.dotAccess("$$getDebugSoyTemplateInfo");
  public static final Expression SOY_ARE_YOU_AN_INTERNAL_CALLER =
      SOY.dotAccess("$$areYouAnInternalCaller");
  public static final Expression SOY_INTERNAL_CALL_MARKER =
      SOY.dotAccess("$$internalCallMarkerDoNotUse");

  public static final Expression SOY_NEWMAPS_TRANSFORM_VALUES =
      SOY_NEWMAPS.googModuleGet().dotAccess("$$transformValues");
  public static final Expression SOY_NEWMAPS_NULL_SAFE_TRANSFORM_VALUES =
      SOY_NEWMAPS.googModuleGet().dotAccess("$$nullSafeTransformValues");
  public static final Expression SOY_NEWMAPS_NULL_SAFE_ARRAY_MAP =
      SOY_NEWMAPS.googModuleGet().dotAccess("$$nullSafeArrayMap");

  // Explicitly group() these calls because they return constructors and the new operator has
  // curious precedence semantics if the constructor expression contains parens.
  public static final Expression SOY_VISUAL_ELEMENT =
      Expressions.group(SOY_VELOG.googModuleGet().dotAccess("$$VisualElement"));
  public static final Expression SOY_VISUAL_ELEMENT_DATA =
      Expressions.group(SOY_VELOG.googModuleGet().dotAccess("$$VisualElementData"));

  public static final Expression SOY_VISUAL_ELEMENT_FLUSH_PENDING_LOGGING_ATTRIBUTES =
      SOY_VELOG.googModuleGet().dotAccess("$$flushPendingLoggingAttributes");

  public static final Expression WINDOW_CONSOLE_LOG = dottedIdNoRequire("window.console.log");

  public static final Expression XID = XID_REQUIRE.reference();

  public static final GoogRequire ELEMENT_LIB_IDOM =
      GoogRequire.createWithAlias(
          "google3.javascript.template.soy.element_lib_idom", "element_lib_idom");

  /**
   * A constant for the template parameter {@code opt_data}.
   *
   * <p>TODO(b/177856412): rename to something that doesn't begin with {@code opt_}
   */
  public static final Expression OPT_DATA = id(StandardNames.OPT_DATA);

  public static final Expression OPT_VARIANT = id(StandardNames.OPT_VARIANT);

  /** A constant for the template parameter {@code $ijData}. */
  public static final Expression IJ_DATA = id(StandardNames.DOLLAR_IJDATA);

  public static final Expression EXPORTS = id("exports");

  public static final Expression MARK_TEMPLATE =
      SOY_TEMPLATES.googModuleGet().dotAccess("$$markTemplate");
  public static final Expression BIND_TEMPLATE_PARAMS =
      SOY_TEMPLATES.googModuleGet().dotAccess("$$bindTemplateParams");
  public static final Expression BIND_FUNCTION_PARAMS =
      SOY.googModuleGet().dotAccess("$$bindFunctionParams");
  public static final Expression BIND_TEMPLATE_PARAMS_FOR_IDOM =
      SOY_TEMPLATES.googModuleGet().dotAccess("$$bindTemplateParamsForIdom");

  private static final Expression SOY_CONVERTERS =
      GoogRequire.create("soy.converters").googModuleGet();

  /** The JavaScript method to pack a sanitized object into a safe proto. */
  public static final ImmutableMap<String, Expression> JS_TO_PROTO_PACK_FN_BASE =
      ImmutableMap.<String, Expression>builder()
          .put(
              SafeScriptProto.getDescriptor().getFullName(),
              SOY_CONVERTERS.dotAccess("packSanitizedJsToProtoSoyRuntimeOnly"))
          .put(
              SafeUrlProto.getDescriptor().getFullName(),
              SOY_CONVERTERS.dotAccess("packSanitizedUriToProtoSoyRuntimeOnly"))
          .put(
              SafeStyleProto.getDescriptor().getFullName(),
              SOY_CONVERTERS.dotAccess("packSanitizedCssToSafeStyleProtoSoyRuntimeOnly"))
          .put(
              SafeStyleSheetProto.getDescriptor().getFullName(),
              SOY_CONVERTERS.dotAccess("packSanitizedCssToSafeStyleSheetProtoSoyRuntimeOnly"))
          .put(
              TrustedResourceUrlProto.getDescriptor().getFullName(),
              SOY_CONVERTERS.dotAccess("packSanitizedTrustedResourceUriToProtoSoyRuntimeOnly"))
          .buildOrThrow();

  public static final ImmutableMap<String, Expression> JS_TO_PROTO_PACK_FN =
      ImmutableMap.<String, Expression>builder()
          .put(
              SafeHtmlProto.getDescriptor().getFullName(),
              SOY_CONVERTERS.dotAccess("packSanitizedHtmlToProtoSoyRuntimeOnly"))
          .putAll(JS_TO_PROTO_PACK_FN_BASE)
          .buildOrThrow();

  /** Create and reference toggle for given path, name. */
  public static Expression getToggleRef(String path, String name, boolean googModuleSyntax) {
    // Translate './path/to/my.toggles' to 'google3.path.to.my$2etoggles' for ts_toggle_lib
    int extensionIndex = path.lastIndexOf(".toggles");
    if (extensionIndex != -1) {
      path = path.substring(0, extensionIndex);
    }
    String togglePathSymbol = "google3." + path.replace('/', '.') + "$2etoggles";
    if (googModuleSyntax) {
      // Map toggle path to unique string for toggle references
      String uniqueTogglePathSymbol = togglePathSymbol.replace('.', '_');
      GoogRequire ref = GoogRequire.createWithAlias(togglePathSymbol, uniqueTogglePathSymbol);
      // Prepend 'TOGGLE_' to toggle name for ts_toggle_lib naming requirement
      return ref.reference().dotAccess("TOGGLE_" + name);
    } else {
      GoogRequire ref = GoogRequire.create(togglePathSymbol);
      return ref.googModuleGet().dotAccess("TOGGLE_" + name);
    }
  }

  /** Returns the field containing the extension object for the given field descriptor. */
  public static Expression extensionField(FieldDescriptor desc) {
    String jsExtensionImport = ProtoUtils.getJsExtensionImport(desc);
    String jsExtensionName = ProtoUtils.getJsExtensionName(desc);
    return symbolWithNamespace(jsExtensionImport, jsExtensionName);
  }

  /** Returns a function that can 'unpack' safe proto types into sanitized content types.. */
  public static Expression protoToSanitizedContentConverterFunction(Descriptor messageType) {
    return SOY_CONVERTERS.dotAccess(NodeContentKinds.toJsUnpackFunction(messageType));
  }

  /**
   * Returns a function that ensure that proto bytes fields are consistently converted oot base64.
   */
  public static Expression protoByteStringToBase64ConverterFunction() {
    return SOY_CONVERTERS.dotAccess("unpackByteStringToBase64String");
  }

  /** Returns a function that ensures that the values of bytes-values maps are coerced. */
  public static Expression protoBytesPackToByteStringFunction() {
    return SOY_CONVERTERS.dotAccess("packBase64StringToByteString");
  }

  /**
   * Returns an 'ordainer' function that can be used wrap a {@code string} in a {@code
   * SanitizedContent} object with no escaping.
   */
  public static Expression sanitizedContentOrdainerFunction(SanitizedContentKind kind) {
    return symbolWithNamespace(
        NodeContentKinds.getJsImportForOrdainersFunctions(kind),
        NodeContentKinds.toJsSanitizedContentOrdainer(kind));
  }

  public static Expression nodeBuilderClass() {
    return SOY.dotAccess("NodeBuilder");
  }

  public static Expression createHtmlOutputBufferFunction() {
    return SOY.dotAccess("$$createHtmlOutputBuffer");
  }

  /** Returns the constructor for the proto. */
  public static Expression protoConstructor(SoyProtoType type) {
    return GoogRequire.create(type.getJsName(ProtoUtils.MutabilityMode.MUTABLE)).reference();
  }

  /** Returns an expression that constructs an empty proto. */
  public static Expression emptyProto(SoyProtoType type) {
    return castAsReadonlyProto(
        SOY.dotAccess("$$emptyProto")
            .call(
                GoogRequire.create(type.getJsName(ProtoUtils.MutabilityMode.MUTABLE)).reference()),
        type);
  }

  static Expression castAsReadonlyProto(Expression expr, SoyProtoType type) {
    String readonlyName = type.getJsName(ProtoUtils.MutabilityMode.READONLY);
    return expr.castAs(
        "!" + readonlyName, ImmutableSet.of(GoogRequire.createTypeRequire(readonlyName)));
  }

  /**
   * Returns the js type for the sanitized content object corresponding to the given ContentKind.
   */
  public static Expression sanitizedContentType(SanitizedContentKind kind) {
    String type = NodeContentKinds.toJsSanitizedContentCtorName(kind);
    return GOOG_SOY_DATA.dotAccess(type);
  }

  /**
   * Returns a code chunk that accesses the given symbol.
   *
   * @param requireSymbol The symbol to {@code goog.require}
   * @param fullyQualifiedSymbol The symbol we want to access.
   */
  private static Expression symbolWithNamespace(String requireSymbol, String fullyQualifiedSymbol) {
    GoogRequire require = GoogRequire.create(requireSymbol);
    if (fullyQualifiedSymbol.equals(require.symbol())) {
      return require.reference();
    }
    String suffix = fullyQualifiedSymbol.substring(require.symbol().length() + 1);
    Expression e = require.reference();
    for (String ident : Splitter.on('.').splitToList(suffix)) {
      e = e.dotAccess(ident);
    }
    return e;
  }
}
