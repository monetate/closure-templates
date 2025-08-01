/*
 * Copyright 2018 Google Inc.
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

import static com.google.common.collect.ImmutableList.toImmutableList;
import static com.google.template.soy.types.SoyTypes.SAFE_PROTO_TO_SANITIZED_TYPE;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.errorprone.annotations.CanIgnoreReturnValue;
import com.google.errorprone.annotations.DoNotCall;
import com.google.template.soy.data.restricted.StringData;
import com.google.template.soy.error.ErrorReporter;
import com.google.template.soy.error.SoyErrorKind;
import com.google.template.soy.error.SoyErrorKind.StyleAllowance;
import com.google.template.soy.error.SoyErrors;
import com.google.template.soy.exprtree.ExprNode;
import com.google.template.soy.exprtree.NullNode;
import com.google.template.soy.exprtree.StringNode;
import com.google.template.soy.exprtree.UndefinedNode;
import com.google.template.soy.types.ExcludeType;
import com.google.template.soy.types.ExtractType;
import com.google.template.soy.types.FunctionType;
import com.google.template.soy.types.IndexedType;
import com.google.template.soy.types.ListType;
import com.google.template.soy.types.LiteralType;
import com.google.template.soy.types.MutableListType;
import com.google.template.soy.types.NamedType;
import com.google.template.soy.types.NeverType;
import com.google.template.soy.types.NonNullableType;
import com.google.template.soy.types.NullType;
import com.google.template.soy.types.OmitType;
import com.google.template.soy.types.PickType;
import com.google.template.soy.types.ProtoTypeRegistry;
import com.google.template.soy.types.RecordType;
import com.google.template.soy.types.SanitizedType;
import com.google.template.soy.types.SoyType;
import com.google.template.soy.types.SoyType.Kind;
import com.google.template.soy.types.SoyTypeRegistry;
import com.google.template.soy.types.TemplateType;
import com.google.template.soy.types.TypeInterner;
import com.google.template.soy.types.TypeRegistries;
import com.google.template.soy.types.TypeRegistry;
import com.google.template.soy.types.UndefinedType;
import com.google.template.soy.types.UnknownType;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/** Resolves {@link TypeNode}s into {@link SoyType}s. */
public final class TypeNodeConverter
    implements TypeNodeVisitor<SoyType>, Function<TypeNode, SoyType> {

  private static final SoyErrorKind UNKNOWN_TYPE =
      SoyErrorKind.of("Unknown type ''{0}''.{1}", StyleAllowance.NO_PUNCTUATION);

  private static final SoyErrorKind DUPLICATE_RECORD_FIELD =
      SoyErrorKind.of("Duplicate field ''{0}'' in record declaration.");

  private static final SoyErrorKind DUPLICATE_TEMPLATE_ARGUMENT =
      SoyErrorKind.of("Duplicate argument ''{0}'' in template type declaration.");

  private static final SoyErrorKind DUPLICATE_FUNCTION_PARAM =
      SoyErrorKind.of("Duplicate parameter ''{0}'' in function type declaration.");

  private static final SoyErrorKind INVALID_LITERAL_TYPE =
      SoyErrorKind.of("Not a valid literal type.");

  private static final SoyErrorKind INVALID_TEMPLATE_RETURN_TYPE =
      SoyErrorKind.of(
          "Template types can only return html, attributes, string, js, css, uri, or"
              + " trusted_resource_uri.");

  private static final SoyErrorKind UNEXPECTED_TYPE_PARAM =
      SoyErrorKind.of(
          "Unexpected type parameter: ''{0}'' only has {1}", StyleAllowance.NO_PUNCTUATION);

  private static final SoyErrorKind EXPECTED_TYPE_PARAM =
      SoyErrorKind.of("Expected a type parameter: ''{0}'' has {1}", StyleAllowance.NO_PUNCTUATION);

  private static final SoyErrorKind NOT_A_GENERIC_TYPE =
      SoyErrorKind.of("''{0}'' is not a generic type.");

  private static final SoyErrorKind MISSING_GENERIC_TYPE_PARAMETERS =
      SoyErrorKind.of("''{0}'' is a generic type, expected {1}.");

  private static final SoyErrorKind VAR_ARGS_PARAM_NOT_LAST =
      SoyErrorKind.of("Var args parameters must be the last parameter in a function.");

  private static final SoyErrorKind VAR_ARGS_PARAM_NOT_LIST =
      SoyErrorKind.of("Var args parameters must be a list.");

  public static final SoyErrorKind SAFE_PROTO_TYPE =
      SoyErrorKind.of("Please use Soy''s native ''{0}'' type instead of the ''{1}'' type.");

  public static final SoyErrorKind INDEXED_BASE_NOT_NAMED =
      SoyErrorKind.of("The base of an indexed type must be a named type.");

  private static final SoyErrorKind BAD_INDEXED =
      SoyErrorKind.of("Type ''{1}'' does not have field {0}.");

  public static final SoyErrorKind DASH_NOT_ALLOWED =
      SoyErrorKind.of(
          "parse error at ''-'': expected identifier",
          StyleAllowance.NO_CAPS,
          StyleAllowance.NO_PUNCTUATION);

  private static final ImmutableSet<Kind> ALLOWED_TEMPLATE_RETURN_TYPES =
      Sets.immutableEnumSet(
          Kind.ELEMENT,
          Kind.HTML,
          Kind.ATTRIBUTES,
          Kind.STRING,
          Kind.JS,
          Kind.CSS,
          Kind.URI,
          Kind.TRUSTED_RESOURCE_URI);

  private static final ImmutableMap<String, BaseGenericTypeInfo> GENERIC_TYPES =
      ImmutableMap.<String, BaseGenericTypeInfo>builder()
          .put(
              "iterable",
              new GenericTypeInfo(1) {
                @Override
                SoyType create(List<SoyType> types, TypeInterner interner) {
                  return interner.getOrCreateIterableType(types.get(0));
                }
              })
          .put(
              "list",
              new GenericTypeInfo(1) {
                @Override
                SoyType create(List<SoyType> types, TypeInterner interner) {
                  return interner.getOrCreateListType(types.get(0));
                }
              })
          .put(
              "mutable_list",
              new GenericTypeInfo(1) {
                @Override
                SoyType create(List<SoyType> types, TypeInterner interner) {
                  return interner.intern(MutableListType.of(types.get(0)));
                }
              })
          .put(
              "set",
              new GenericTypeInfo(1) {
                @Override
                SoyType create(List<SoyType> types, TypeInterner interner) {
                  return interner.getOrCreateSetType(types.get(0));
                }
              })
          .put(
              "legacy_object_map",
              new GenericTypeInfo(2) {
                @Override
                SoyType create(List<SoyType> types, TypeInterner interner) {
                  return interner.getOrCreateLegacyObjectMapType(types.get(0), types.get(1));
                }
              })
          .put(
              "map",
              new GenericTypeInfo(2) {
                @Override
                SoyType create(List<SoyType> types, TypeInterner interner) {
                  return interner.getOrCreateMapType(types.get(0), types.get(1));
                }
              })
          .put(
              "ve",
              new GenericTypeInfo(1) {
                @Override
                SoyType create(List<SoyType> types, TypeInterner interner) {
                  return interner.getOrCreateVeType(types.get(0).toString());
                }
              })
          .put(
              "Pick",
              new GenericTypeInfo(2) {
                @Override
                SoyType create(List<SoyType> types, TypeInterner interner) {
                  return interner.intern(PickType.create(types.get(0), types.get(1)));
                }
              })
          .put(
              "Omit",
              new GenericTypeInfo(2) {
                @Override
                SoyType create(List<SoyType> types, TypeInterner interner) {
                  return interner.intern(OmitType.create(types.get(0), types.get(1)));
                }
              })
          .put(
              "Exclude",
              new GenericTypeInfo(2) {
                @Override
                SoyType create(List<SoyType> types, TypeInterner interner) {
                  return interner.intern(ExcludeType.create(types.get(0), types.get(1)));
                }
              })
          .put(
              "Extract",
              new GenericTypeInfo(2) {
                @Override
                SoyType create(List<SoyType> types, TypeInterner interner) {
                  return interner.intern(ExtractType.create(types.get(0), types.get(1)));
                }
              })
          .put(
              "NonNullable",
              new GenericTypeInfo(1) {
                @Override
                SoyType create(List<SoyType> types, TypeInterner interner) {
                  return interner.intern(NonNullableType.create(types.get(0)));
                }
              })
          .buildOrThrow();

  private static final ImmutableMap<String, BaseGenericTypeInfo> GENERIC_TYPES_WITH_ELEMENT =
      new ImmutableMap.Builder<String, BaseGenericTypeInfo>()
          .putAll(GENERIC_TYPES)
          .put(
              "html",
              new StringArgGenericTypeInfo(1) {
                @Override
                SoyType create(List<String> types, TypeInterner interner) {
                  String tag = "";
                  if (types.size() == 1) {
                    String type = types.get(0);
                    if (!"?".equals(type)) {
                      tag = type;
                    }
                  }
                  return interner.getOrCreateElementType(tag);
                }
              })
          .build();

  private abstract static class BaseGenericTypeInfo {
    final int numParams;

    BaseGenericTypeInfo(int numParams) {
      this.numParams = numParams;
    }

    final String formatNumTypeParams() {
      return numParams + " type parameter" + (numParams > 1 ? "s" : "");
    }
  }

  /** Simple representation of a generic type specification. */
  private abstract static class GenericTypeInfo extends BaseGenericTypeInfo {
    public GenericTypeInfo(int numParams) {
      super(numParams);
    }

    /**
     * Creates the given type. There are guaranteed to be exactly {@link #numParams} in the list.
     */
    abstract SoyType create(List<SoyType> types, TypeInterner interner);
  }

  private abstract static class StringArgGenericTypeInfo extends BaseGenericTypeInfo {
    public StringArgGenericTypeInfo(int numParams) {
      super(numParams);
    }

    abstract SoyType create(List<String> types, TypeInterner interner);
  }

  public static Builder builder(ErrorReporter errorReporter) {
    return new Builder().setErrorReporter(errorReporter);
  }

  /** Builder pattern for {@link TypeNodeConverter}. */
  public static class Builder {
    private ErrorReporter errorReporter;
    private TypeInterner interner;
    private TypeRegistry typeRegistry;
    private ProtoTypeRegistry protoRegistry;
    private boolean reportMissingTypes = true;
    private boolean systemExternal = false;

    private Builder() {}

    @CanIgnoreReturnValue
    public Builder setErrorReporter(ErrorReporter errorReporter) {
      this.errorReporter = Preconditions.checkNotNull(errorReporter);
      return this;
    }

    @CanIgnoreReturnValue
    public Builder setReportMissingTypes(boolean reportMissingTypes) {
      this.reportMissingTypes = reportMissingTypes;
      return this;
    }

    /**
     * Set to true if {@link TypeNode} inputs will be parsed from non-template sources. If true then
     * FQ proto names will be supported.
     */
    @CanIgnoreReturnValue
    public Builder setSystemExternal(boolean systemExternal) {
      this.systemExternal = systemExternal;
      return this;
    }

    @CanIgnoreReturnValue
    public Builder setTypeRegistry(SoyTypeRegistry typeRegistry) {
      this.interner = typeRegistry;
      this.typeRegistry = typeRegistry;
      this.protoRegistry = typeRegistry.getProtoRegistry();
      return this;
    }

    public TypeNodeConverter build() {
      Preconditions.checkState(interner != null);
      return new TypeNodeConverter(
          errorReporter,
          interner,
          systemExternal ? TypeRegistries.builtinTypeRegistry() : typeRegistry,
          systemExternal ? protoRegistry : null,
          reportMissingTypes);
    }
  }

  private final ErrorReporter errorReporter;
  private final TypeInterner interner;
  private final TypeRegistry typeRegistry;
  private final ProtoTypeRegistry protoRegistry;
  private final boolean reportMissingTypes;

  private TypeNodeConverter(
      ErrorReporter errorReporter,
      TypeInterner interner,
      TypeRegistry typeRegistry,
      ProtoTypeRegistry protoRegistry,
      boolean reportMissingTypes) {
    this.errorReporter = errorReporter;
    this.interner = interner;
    this.typeRegistry = typeRegistry;
    this.protoRegistry = protoRegistry;
    this.reportMissingTypes = reportMissingTypes;
  }

  /**
   * Converts a TypeNode into a SoyType.
   *
   * <p>If any errors are encountered they are reported to the error reporter.
   */
  public SoyType getOrCreateType(TypeNode node) {
    return exec(node);
  }

  @Override
  public SoyType visit(NamedTypeNode node) {
    String name = node.name().identifier();

    // This is OK to check unconditionally because where '-' is allowed (in the TemplateType return
    // type generics) NamedTypeNode is not parsed here. It is processed with TypeNode::toString.
    if (name.contains("-")) {
      errorReporter.report(node.sourceLocation(), DASH_NOT_ALLOWED);
      node.setResolvedType(UnknownType.getInstance());
      return UnknownType.getInstance();
    }

    SoyType type =
        typeRegistry instanceof SoyTypeRegistry
            ? TypeRegistries.getTypeOrProtoFqn(
                (SoyTypeRegistry) typeRegistry, errorReporter, node.name())
            : typeRegistry.getType(name);
    if (type == null && protoRegistry != null) {
      type = protoRegistry.getProtoType(name);
    }

    if (type == null) {
      BaseGenericTypeInfo genericType = GENERIC_TYPES.get(name);
      if (genericType != null) {
        errorReporter.report(
            node.sourceLocation(),
            MISSING_GENERIC_TYPE_PARAMETERS,
            name,
            genericType.formatNumTypeParams());
      } else {
        if (reportMissingTypes) {
          errorReporter.report(
              node.sourceLocation(),
              UNKNOWN_TYPE,
              name,
              SoyErrors.getDidYouMeanMessage(typeRegistry.getAllSortedTypeNames(), name));
        }
      }
      type = UnknownType.getInstance();
    } else if (type.getKind() == Kind.PROTO) {
      SanitizedType safeProtoType = SAFE_PROTO_TO_SANITIZED_TYPE.get(type.toString());

      if (safeProtoType != null) {
        String safeProtoNativeType = safeProtoType.getContentKind().asAttributeValue();
        errorReporter.report(node.sourceLocation(), SAFE_PROTO_TYPE, safeProtoNativeType, name);
        type = UnknownType.getInstance();
      }
    }
    node.setResolvedType(type);
    return type;
  }

  @Override
  public SoyType visit(IndexedTypeNode node) {
    SoyType base = exec(node.type());
    if (!(base instanceof NamedType)) {
      errorReporter.report(node.sourceLocation(), INDEXED_BASE_NOT_NAMED);
    }
    IndexedType rv = interner.intern(IndexedType.create(base, exec(node.property())));
    if (rv.getEffectiveType().getKind() == Kind.NEVER) {
      errorReporter.report(node.sourceLocation(), BAD_INDEXED, rv.getProperty(), rv.getType());
    }
    return rv;
  }

  @Override
  public SoyType visit(GenericTypeNode node) {
    return visit(node, GENERIC_TYPES);
  }

  private SoyType visit(
      GenericTypeNode node, ImmutableMap<String, BaseGenericTypeInfo> genericTypes) {
    ImmutableList<TypeNode> args = node.arguments();
    String name = node.name();
    BaseGenericTypeInfo genericType = genericTypes.get(name);
    if (genericType == null) {
      errorReporter.report(node.sourceLocation(), NOT_A_GENERIC_TYPE, name);
      return UnknownType.getInstance();
    }
    if (args.size() < genericType.numParams) {
      errorReporter.report(
          // blame the '>'
          node.sourceLocation().getEndLocation(),
          EXPECTED_TYPE_PARAM,
          name,
          genericType.formatNumTypeParams());
      return UnknownType.getInstance();
    } else if (args.size() > genericType.numParams) {
      errorReporter.report(
          // blame the first unexpected argument
          args.get(genericType.numParams).sourceLocation(),
          UNEXPECTED_TYPE_PARAM,
          name,
          genericType.formatNumTypeParams());
      return UnknownType.getInstance();
    }

    SoyType type;
    if (genericType instanceof GenericTypeInfo) {
      type =
          ((GenericTypeInfo) genericType)
              .create(args.stream().map(this).collect(toImmutableList()), interner);
    } else if (genericType instanceof StringArgGenericTypeInfo) {
      type =
          ((StringArgGenericTypeInfo) genericType)
              .create(args.stream().map(TypeNode::toString).collect(Collectors.toList()), interner);
    } else {
      throw new AssertionError();
    }
    node.setResolvedType(type);
    return type;
  }

  @Override
  public SoyType visit(UnionTypeNode node) {
    // Copy the result of the transform because transform is lazy. The union evaluation code short
    // circuits if it sees a ? type so for types like ?|list<?> the union evaluation would get
    // short circuited and the lazy transform would never visit list<?>. By copying the transform
    // result (which the transform documentation recommends to avoid lazy evaluation), we ensure
    // that all type nodes are visited.
    SoyType type =
        interner.getOrCreateUnionType(
            node.candidates().stream().map(this).collect(toImmutableList()));
    node.setResolvedType(type);
    return type;
  }

  @Override
  public SoyType visit(IntersectionTypeNode node) {
    SoyType type =
        interner.getOrCreateIntersectionType(
            node.candidates().stream().map(this).collect(toImmutableList()));
    node.setResolvedType(type);
    return type;
  }

  @Override
  public SoyType visit(RecordTypeNode node) {
    // LinkedHashMap insertion order iteration on values() is important here.
    Map<String, RecordType.Member> map = Maps.newLinkedHashMap();
    for (RecordTypeNode.Property property : node.properties()) {
      SoyType propertyType = exec(property.type());
      RecordType.Member duplicatePropertyNameMember =
          map.put(
              property.name(),
              RecordType.memberOf(property.name(), property.optional(), propertyType));
      if (duplicatePropertyNameMember != null) {
        errorReporter.report(property.nameLocation(), DUPLICATE_RECORD_FIELD, property.name());
        // restore old mapping and keep going
        map.put(property.name(), duplicatePropertyNameMember);
      }
    }
    SoyType type = interner.getOrCreateRecordType(map.values());
    node.setResolvedType(type);
    return type;
  }

  @Override
  public SoyType visit(TemplateTypeNode node) {
    Map<String, TemplateType.Parameter> map = new LinkedHashMap<>();
    for (TemplateTypeNode.Parameter parameter : node.parameters()) {
      TemplateType.Parameter newParameter =
          TemplateType.Parameter.builder()
              .setName(parameter.name())
              .setKind(parameter.kind())
              .setType(exec(parameter.type()))
              .setRequired(parameter.required())
              .setImplicit(false)
              .build();
      TemplateType.Parameter oldParameter = map.put(parameter.name(), newParameter);
      if (oldParameter != null) {
        errorReporter.report(
            parameter.nameLocation(), DUPLICATE_TEMPLATE_ARGUMENT, parameter.name());
        map.put(parameter.name(), oldParameter);
      }
    }
    SoyType returnType = handleReturnTypeOfTemplateType(node.returnType());
    // Validate return type.
    if (!ALLOWED_TEMPLATE_RETURN_TYPES.contains(returnType.getKind())) {
      errorReporter.report(node.returnType().sourceLocation(), INVALID_TEMPLATE_RETURN_TYPE);
    }
    // There is no syntax for specifying the usevarianttype in a template type literal. This means
    // "variant" won't work on calls of template-typed template parameters.
    // There is also no syntax for specifying legacydeltemplatenamespace. This is ok since that is
    // used to calculate whether positional-style calls are possible, and they aren't possible on
    // template-typed template parameters to start with. legacydeltemplatenamespace is also a
    // temporary feature for the go/symbolize-deltemplates migration.
    SoyType type =
        interner.internTemplateType(
            TemplateType.declaredTypeOf(
                map.values(), returnType, UndefinedType.getInstance(), false, false, ""));
    node.setResolvedType(type);
    return type;
  }

  @Override
  public SoyType visit(FunctionTypeNode node) {
    Map<String, FunctionType.Parameter> map = new LinkedHashMap<>();
    for (int i = 0; i < node.parameters().size(); i++) {
      FunctionTypeNode.Parameter parameter = node.parameters().get(i);
      if (parameter.isVarArgs() && i != node.parameters().size() - 1) {
        errorReporter.report(parameter.nameLocation(), VAR_ARGS_PARAM_NOT_LAST);
      }
      FunctionType.Parameter oldParameter =
          map.put(
              parameter.name(),
              FunctionType.Parameter.of(
                  parameter.name(), exec(parameter.type()), parameter.isVarArgs()));
      if (parameter.isVarArgs()
          && !ListType.ANY_LIST.isAssignableFromStrict(parameter.type().getResolvedType())) {
        errorReporter.report(parameter.nameLocation(), VAR_ARGS_PARAM_NOT_LIST);
      }
      if (oldParameter != null) {
        errorReporter.report(parameter.nameLocation(), DUPLICATE_FUNCTION_PARAM, parameter.name());
        map.put(parameter.name(), oldParameter);
      }
    }
    SoyType type = interner.intern(FunctionType.of(map.values(), exec(node.returnType())));
    node.setResolvedType(type);
    return type;
  }

  @Override
  public SoyType visit(LiteralTypeNode node) {
    SoyType type;
    ExprNode literal = node.literal();
    if (literal instanceof NullNode) {
      type = NullType.getInstance();
    } else if (literal instanceof UndefinedNode) {
      type = UndefinedType.getInstance();
    } else if (literal instanceof StringNode) {
      type = LiteralType.create(StringData.forValue(((StringNode) literal).getValue()));
    } else {
      errorReporter.report(literal.getSourceLocation(), INVALID_LITERAL_TYPE);
      type = NeverType.getInstance();
    }
    node.setResolvedType(type);
    return type;
  }

  private SoyType handleReturnTypeOfTemplateType(TypeNode node) {
    if (node instanceof GenericTypeNode) {
      return visit((GenericTypeNode) node, GENERIC_TYPES_WITH_ELEMENT);
    }
    return exec(node);
  }

  @DoNotCall
  @Override
  public SoyType apply(TypeNode node) {
    return exec(node);
  }
}
