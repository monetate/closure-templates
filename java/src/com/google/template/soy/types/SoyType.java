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

package com.google.template.soy.types;

import com.google.errorprone.annotations.ForOverride;
import com.google.errorprone.annotations.concurrent.LazyInit;
import com.google.template.soy.error.ErrorArg;
import com.google.template.soy.soytree.SoyTypeP;

/**
 * Interface for all classes that describe a data type in Soy. These types are used to determine
 * what kinds of values can be bound to a template or function parameter.
 *
 * <p>Note that this type hierarchy only describes types that are visible from a template author's
 * perspective. Actual Soy values may have types which fall outside this hierarchy. An example is
 * Futures - a SoyValue may contain a future of a type, but since the future is always resolved
 * before the value is used, the future type is invisible as far as the template is concerned.
 *
 * <p>All type objects are immutable.
 */
public abstract class SoyType implements ErrorArg {

  /**
   * Enum that identifies the kind of type this is.
   *
   * <ul>
   *   <li>Special types:
   *       <ul>
   *         <li>ANY: Superclass of all types.
   *         <li>UNKNOWN: Indicates that we don't know the type. A value with unknown type is
   *             considered to be dynamically typed.
   *         <li>ERROR: Placeholder which represents a type that failed to parse.
   *       </ul>
   *   <li>Primitive types:
   *       <ul>
   *         <li>NULL: The type of the "null" value
   *         <li>UNDEFINED: The type of the "undefined" value
   *         <li>BOOL
   *         <li>INT
   *         <li>FLOAT
   *         <li>GBIGINT
   *         <li>STRING
   *       </ul>
   *   <li>Sanitized types (subtypes of string):
   *       <ul>
   *         <li>HTML: Possibly containing HTML markup
   *         <li>ATTRIBUTES: key="value" pairs
   *         <li>JS
   *         <li>CSS
   *         <li>URI
   *         <li>TRUSTED_RESOURCE_URI
   *       </ul>
   *   <li>Aggregate types:
   *       <ul>
   *         <li>LIST: Sequence of items indexed by integer.
   *         <li>RECORD: Open-ended record type.
   *         <li>LEGACY_OBJECT_MAP: Deprecated map type.
   *         <li>MAP: Map type that supports proto map (and ES6 map in JS backend).
   *         <li>BASE_PROTO: A generic proto, this type is mostly useful for plugins
   *         <li>PROTO: Protobuf object.
   *         <li>PROTO_ENUM: Protobuf enum object.
   *         <li>UNION: Used to indicate a parameter that can accept multiple alternatives, e.g.
   *             a|b.
   *         <li>VE: A VE ID.
   *       </ul>
   * </ul>
   */
  public enum Kind {
    // Special types
    ANY,
    UNKNOWN,
    NEVER,
    // Primitive types
    NULL,
    UNDEFINED,
    BOOL,
    NUMBER,
    INT,
    FLOAT,
    STRING,
    GBIGINT,
    LITERAL,
    // Sanitized types (subtypes of string)
    HTML,
    ELEMENT,
    ATTRIBUTES,
    JS,
    CSS,
    URI,
    TRUSTED_RESOURCE_URI,
    // Aggregate types
    ITERABLE,
    LIST,
    SET,
    RECORD,
    LEGACY_OBJECT_MAP,
    MAP,
    MESSAGE,
    PROTO,
    PROTO_ENUM,
    TEMPLATE,
    FUNCTION,
    VE,
    VE_DATA,
    // Resolvable types
    UNION,
    COMPUTED,
    // Imported symbol types
    NAMESPACE,
    PROTO_TYPE,
    PROTO_ENUM_TYPE,
    PROTO_EXTENSION,
    TEMPLATE_TYPE,
    ;
  }

  enum AssignabilityPolicy {
    LOOSE(true, true),
    STRICT(false, true),
    STRICT_WITHOUT_COERCIONS(false, false);

    private final boolean unknownAssignmentAllowed;
    private final boolean numericCoercionsAllowed;

    AssignabilityPolicy(boolean unknownAssignmentAllowed, boolean numericCoercionsAllowed) {
      this.unknownAssignmentAllowed = unknownAssignmentAllowed;
      this.numericCoercionsAllowed = numericCoercionsAllowed;
    }

    public boolean isUnknownAssignmentAllowed() {
      return unknownAssignmentAllowed;
    }

    public boolean isNumericCoercionsAllowed() {
      return numericCoercionsAllowed;
    }
  }

  // memoize the proto version.  SoyTypes are immutable so this is safe/correct and types are likely
  // to be serialized many times (think, 'string'), so we can save some work by not calculating it
  // repeatedly.
  @LazyInit private SoyTypeP protoDual;

  // restrict subtypes to this package
  SoyType() {}

  /** Returns what kind of type this is. */
  public abstract Kind getKind();

  /**
   * Returns true if a parameter or field of this type can be assigned from a value of {@code
   * srcType}.
   *
   * <p><i>loose</i> assignment means that the type is only possibly assignable. {@code ?} types are
   * considered to be possibly assignable. Use this in cases where the compiler may insert a runtime
   * test (as in {@code call} commands) or we expect some other part of the runtime to enforce types
   * (e.g. dispatching to plugin methods).
   *
   * @param srcType The type of the incoming value.
   * @return True if the assignment is valid.
   */
  public final boolean isAssignableFromLoose(SoyType srcType) {
    return isAssignableFromInternal(srcType, AssignabilityPolicy.LOOSE);
  }

  /**
   * Returns true if a parameter or field of this type can be strictly assigned from a value of
   * {@code srcType}.
   *
   * <p><i>strict</i> assignment means that the type is definitly assignable. {@code ?} types are
   * not considered to be definitely assignable. Use this in cases where we require certainty, such
   * as when selecting methods based on receiver types or when making code generation decisions.
   *
   * @param srcType The type of the incoming value.
   * @return True if the assignment is valid.
   */
  public final boolean isAssignableFromStrict(SoyType srcType) {
    return isAssignableFromInternal(srcType, AssignabilityPolicy.STRICT);
  }

  /** Needed as long as int and number exist together. TODO(b/395679605): Remove. */
  public final boolean isAssignableFromStrictWithoutCoercions(SoyType srcType) {
    return isAssignableFromInternal(srcType, AssignabilityPolicy.STRICT_WITHOUT_COERCIONS);
  }

  /** Internal helper method for assignment analysis. This should only be used by subclasses. */
  final boolean isAssignableFromInternal(SoyType soyType, AssignabilityPolicy policy) {
    if (policy.isUnknownAssignmentAllowed() && soyType.isOfKind(Kind.UNKNOWN)) {
      return true;
    }
    if (!soyType.isOfKind(Kind.UNION)) {
      return doIsAssignableFromNonUnionType(soyType.getEffectiveType(), policy);
    }
    // Handle unions here with template methods rather than forcing all subclasses to handle. A type
    // is assignable from a union if it is assignable from _all_ members.
    return SoyTypes.flattenUnion(soyType)
        .allMatch(member -> doIsAssignableFromNonUnionType(member, policy));
  }

  /**
   * Subclass integration point to implement assignablility.
   *
   * @param type The target type, guaranteed to <b>not be a union type</b>.
   * @param policy How assignments from the unknown type should be treated. This should be passed
   *     along to {@link #isAssignableFromInternal} calls made on member types.
   */
  @ForOverride
  boolean doIsAssignableFromNonUnionType(SoyType type, AssignabilityPolicy policy) {
    return doIsAssignableFromNonUnionType(type);
  }

  @ForOverride
  boolean doIsAssignableFromNonUnionType(SoyType type) {
    throw new AbstractMethodError();
  }

  /** The type represented in a fully parseable format. */
  @Override
  public abstract String toString();

  @Override
  public final String toErrorArgString() {
    SoyType effective = getEffectiveType();
    if (effective != this) {
      return this + " (" + effective.toErrorArgString() + ")";
    }
    return toString();
  }

  /** The type represented in proto format. For template metadata protos. */
  public final SoyTypeP toProto() {
    SoyTypeP local = protoDual;
    if (local == null) {
      SoyTypeP.Builder builder = SoyTypeP.newBuilder();
      doToProto(builder);
      local = builder.build();
      protoDual = local;
    }
    return local;
  }

  protected abstract void doToProto(SoyTypeP.Builder builder);

  /**
   * Resolves computed types (instances of {@link ComputedType}). Certain type introspections are
   * only valid on the value returned by this function:
   *
   * <ul>
   *   <li>Most uses of {@link SoyType#getKind}, including switch statements over the kind.
   *   <li>Casting SoyType to a subclass.
   * </ul>
   *
   * <p>Other operations work without calling getEffectiveType():
   *
   * <ul>
   *   <li>{@link SoyType#isAssignableFromStrict}, etc
   *   <li>{@link SoyType#isOfKind}
   *   <li>{@link SoyType#asType}
   *   <li>Any utility function in {@link SoyTypes}
   * </ul>
   */
  public SoyType getEffectiveType() {
    return this;
  }

  public boolean isOfKind(Kind kind) {
    return this.getKind() == kind;
  }

  public boolean isEffectivelyEqual(SoyType type) {
    return type != null && this.equals(type.getEffectiveType());
  }

  public <T extends SoyType> T asType(Class<T> subType) {
    return subType.cast(this);
  }
}
