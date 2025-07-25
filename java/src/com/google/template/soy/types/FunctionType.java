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

package com.google.template.soy.types;

import static com.google.common.collect.ImmutableMap.toImmutableMap;

import com.google.auto.value.AutoValue;
import com.google.auto.value.extension.memoized.Memoized;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.template.soy.soytree.FunctionTypeP;
import com.google.template.soy.soytree.SoyTypeP;
import java.util.Collection;

/** Function type, containing a list of named, typed parameters and a return type. */
@AutoValue
public abstract class FunctionType extends SoyType {

  public static FunctionType of(Collection<Parameter> parameters, SoyType returnType) {
    return new AutoValue_FunctionType(returnType, ImmutableList.copyOf(parameters));
  }

  public abstract SoyType getReturnType();

  public abstract ImmutableList<Parameter> getParameters();

  public boolean isVarArgs() {
    return getParameters().stream().anyMatch(Parameter::isVarArgs);
  }

  public int getArity() {
    return getParameters().size();
  }

  @Memoized
  public ImmutableMap<String, SoyType> getParameterMap() {
    return getParameters().stream().collect(toImmutableMap(Parameter::getName, Parameter::getType));
  }

  /**
   * Represents minimal information about a template parameter.
   *
   * <p>This only represents normal parameters. Information about injected params or state variables
   * is not recorded.
   */
  @AutoValue
  public abstract static class Parameter {

    public static Parameter of(String name, SoyType type) {
      return of(name, type, false);
    }

    public static Parameter of(String name, SoyType type, boolean isVarArgs) {
      return new AutoValue_FunctionType_Parameter(name, type, isVarArgs);
    }

    public abstract String getName();

    public abstract SoyType getType();

    public abstract boolean isVarArgs();
  }

  @Override
  public final Kind getKind() {
    return Kind.FUNCTION;
  }

  @Override
  final boolean doIsAssignableFromNonUnionType(SoyType srcType, AssignabilityPolicy policy) {
    if (srcType.getKind() != Kind.FUNCTION) {
      return false;
    }

    FunctionType srcFunction = (FunctionType) srcType;
    int paramsInCommon = Math.min(getParameters().size(), srcFunction.getParameters().size());
    if (srcFunction.getParameters().size() > paramsInCommon) {
      return false;
    }

    for (int i = 0; i < paramsInCommon; i++) {
      Parameter thisParam = getParameters().get(i);
      Parameter srcParam = srcFunction.getParameters().get(i);
      if (!srcParam.getType().isAssignableFromInternal(thisParam.getType(), policy)) {
        return false;
      }
    }

    return this.getReturnType().isAssignableFromStrict(srcFunction.getReturnType());
  }

  @Override
  public final String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("(");
    boolean first = true;
    for (Parameter parameter : getParameters()) {
      if (first) {
        first = false;
      } else {
        sb.append(", ");
      }
      if (parameter.isVarArgs()) {
        sb.append("...");
      }
      String name = parameter.getName();
      sb.append(name);
      sb.append(": ");
      sb.append(parameter.getType());
    }
    sb.append(") => ");
    sb.append(getReturnType());
    return sb.toString();
  }

  @Override
  protected void doToProto(SoyTypeP.Builder builder) {
    FunctionTypeP.Builder templateBuilder =
        builder.getFunctionBuilder().setReturnType(getReturnType().toProto());
    for (Parameter parameter : getParameters()) {
      templateBuilder.addParameters(
          FunctionTypeP.Parameter.newBuilder()
              .setName(parameter.getName())
              .setType(parameter.getType().toProto())
              .setIsVarArgs(parameter.isVarArgs()));
    }
  }
}
