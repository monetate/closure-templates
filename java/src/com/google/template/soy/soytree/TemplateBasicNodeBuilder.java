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

package com.google.template.soy.soytree;

import com.google.common.base.Joiner;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.template.soy.base.internal.Identifier;
import com.google.template.soy.error.ErrorReporter;
import com.google.template.soy.error.SoyErrorKind;
import com.google.template.soy.exprtree.ExprRootNode;
import com.google.template.soy.soytree.TemplateNode.SoyFileHeaderInfo;
import java.util.List;

/**
 * Builder for TemplateBasicNode.
 *
 * <p>Important: Do not use outside of Soy code (treat as superpackage-private).
 */
public class TemplateBasicNodeBuilder extends TemplateNodeBuilder<TemplateBasicNodeBuilder> {

  public static final SoyErrorKind MODIFIABLE_AND_MODIFIES_BOTH_SET =
      SoyErrorKind.of("\"modifies\" and \"modifiable\" cannot both be set.");

  public static final SoyErrorKind LEGACYDELTEMPLATENAMESPACE_REQUIRES_MODIFIABLE =
      SoyErrorKind.of("\"legacydeltemplatenamespace\" requires \"modifiable\" to be set.");

  public static final SoyErrorKind USEVARIANTTYPE_REQUIRES_MODIFIABLE =
      SoyErrorKind.of("\"usevarianttype\" requires \"modifiable\" to be set.");

  public static final SoyErrorKind MODIFIABLE_REQUIRES_PUBLIC_VISIBILITY =
      SoyErrorKind.of("\"modifiable\" requires public visibility.");

  public static final SoyErrorKind MODIFIES_REQUIRES_PRIVATE_VISIBILITY =
      SoyErrorKind.of("\"modifies\" requires private visibility.");

  public static final SoyErrorKind VARIANT_REQUIRES_MODIFIES =
      SoyErrorKind.of("\"variant\" requires \"modifiable\" to be set.");

  /** The "modifiable" attribute. */
  private boolean modifiable = false;

  /** Expression that will evaluate to "modifies" attribute. */
  private ExprRootNode modifiesExpr = null;

  /** The "legacydeltemplatenamespace" attribute. */
  private String legacyDeltemplateNamespace = "";

  /** Expression that will evaluate to the value of the "variant" attribute. */
  private ExprRootNode variantExpr = null;

  /** The "usevarianttype" attribute. */
  private String useVariantType = "";

  /** @param soyFileHeaderInfo Info from the containing Soy file's header declarations. */
  public TemplateBasicNodeBuilder(
      SoyFileHeaderInfo soyFileHeaderInfo, ErrorReporter errorReporter) {
    super(soyFileHeaderInfo, errorReporter);
  }

  @Override
  public TemplateBasicNodeBuilder setCommandValues(
      Identifier templateName, List<CommandTagAttribute> attrs) {
    this.cmdText = templateName.identifier() + " " + Joiner.on(' ').join(attrs);
    setCommonCommandValues(attrs);

    visibility = Visibility.PUBLIC;
    for (CommandTagAttribute attribute : attrs) {
      Identifier name = attribute.getName();
      if (COMMON_ATTRIBUTE_NAMES.contains(name.identifier())) {
        continue;
      }
      switch (name.identifier()) {
        case "visibility":
          visibility = attribute.valueAsVisibility(errorReporter);
          break;
        case "modifiable":
          modifiable = attribute.valueAsEnabled(errorReporter);
          break;
        case "modifies":
          modifiesExpr = attribute.valueAsExpr(errorReporter);
          break;
        case "legacydeltemplatenamespace":
          legacyDeltemplateNamespace = attribute.getValue();
          break;
        case "variant":
          variantExpr = attribute.valueAsExpr(errorReporter);
          break;
        case "usevarianttype":
          useVariantType = attribute.getValue();
          break;
        default:
          errorReporter.report(
              name.location(),
              CommandTagAttribute.UNSUPPORTED_ATTRIBUTE_KEY,
              name.identifier(),
              "template",
              ImmutableList.builder()
                  .add("visibility")
                  .add("modifiable")
                  .add("modifies")
                  .add("legacydeltemplatenamespace")
                  .add("variant")
                  .add("usevarianttype")
                  .addAll(COMMON_ATTRIBUTE_NAMES)
                  .build());
      }
    }

    setTemplateNames(templateName, soyFileHeaderInfo.getNamespace());
    return this;
  }

  @Override
  public TemplateBasicNode build() {
    Preconditions.checkState(id != null && cmdText != null);
    if (modifiable && modifiesExpr != null) {
      errorReporter.report(openTagLocation, MODIFIABLE_AND_MODIFIES_BOTH_SET);
    }
    if (!modifiable && !legacyDeltemplateNamespace.isEmpty()) {
      errorReporter.report(openTagLocation, LEGACYDELTEMPLATENAMESPACE_REQUIRES_MODIFIABLE);
    }
    if (!modifiable && !useVariantType.isEmpty()) {
      errorReporter.report(openTagLocation, USEVARIANTTYPE_REQUIRES_MODIFIABLE);
    }
    if (modifiable && visibility != Visibility.PUBLIC) {
      errorReporter.report(openTagLocation, MODIFIABLE_REQUIRES_PUBLIC_VISIBILITY);
    }
    if (modifiesExpr != null && visibility != Visibility.PRIVATE) {
      errorReporter.report(openTagLocation, MODIFIES_REQUIRES_PRIVATE_VISIBILITY);
    }
    if (modifiesExpr == null && variantExpr != null) {
      errorReporter.report(openTagLocation, VARIANT_REQUIRES_MODIFIES);
    }
    return new TemplateBasicNode(
        this,
        soyFileHeaderInfo,
        visibility,
        modifiable,
        modifiesExpr,
        legacyDeltemplateNamespace,
        variantExpr,
        useVariantType,
        params);
  }

  @Override
  protected TemplateBasicNodeBuilder self() {
    return this;
  }
}
