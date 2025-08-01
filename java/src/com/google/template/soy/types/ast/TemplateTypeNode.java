/*
 * Copyright 2020 Google Inc.
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

package com.google.template.soy.types.ast;

import com.google.auto.value.AutoValue;
import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableList;
import com.google.template.soy.base.SourceLocation;
import com.google.template.soy.basetree.CopyState;
import com.google.template.soy.types.TemplateType.ParameterKind;

/** Node representing a template type, e.g. () => html. */
@AutoValue
public abstract class TemplateTypeNode extends TypeNode {

  public static TemplateTypeNode create(
      SourceLocation sourceLocation, Iterable<Parameter> parameters, TypeNode returnType) {
    return new AutoValue_TemplateTypeNode(
        sourceLocation, ImmutableList.copyOf(parameters), returnType);
  }

  /** A single named, typed parameter to a template. */
  @AutoValue
  public abstract static class Parameter {
    public static Parameter create(
        SourceLocation nameLocation,
        String name,
        String sourceName,
        ParameterKind kind,
        TypeNode type,
        boolean required) {
      return new AutoValue_TemplateTypeNode_Parameter(
          nameLocation, name, sourceName, kind, type, required);
    }

    public abstract SourceLocation nameLocation();

    public abstract String name();

    public abstract String sourceName();

    public abstract ParameterKind kind();

    public abstract TypeNode type();

    public abstract boolean required();

    @Override
    public final String toString() {
      return sourceName() + (required() ? "" : "?") + ": " + type();
    }

    Parameter copy(CopyState copyState) {
      return create(
          nameLocation(), name(), sourceName(), kind(), type().copy(copyState), required());
    }
  }

  public abstract ImmutableList<Parameter> parameters();

  public abstract TypeNode returnType();

  @Override
  public final String toString() {
    return "template (" + Joiner.on(", ").join(parameters()) + ") => " + returnType();
  }

  @Override
  public TemplateTypeNode copy(CopyState copyState) {
    ImmutableList.Builder<Parameter> newParameters = ImmutableList.builder();
    for (Parameter parameter : parameters()) {
      newParameters.add(parameter.copy(copyState));
    }
    TemplateTypeNode copy =
        create(sourceLocation(), newParameters.build(), returnType().copy(copyState));
    copy.copyInternal(this);
    return copy;
  }
}
