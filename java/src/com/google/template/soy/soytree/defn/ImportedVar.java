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

package com.google.template.soy.soytree.defn;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.template.soy.base.SourceLocation;
import com.google.template.soy.base.SourceLogicalPath;
import com.google.template.soy.base.internal.SetOnce;
import com.google.template.soy.basetree.CopyState;
import com.google.template.soy.basetree.Copyable;
import com.google.template.soy.exprtree.AbstractVarDefn;
import com.google.template.soy.exprtree.VarDefn;
import com.google.template.soy.types.SoyType;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import javax.annotation.Nullable;

/**
 * A reference to an imported symbol. For proto deps, which have nested symbols under the imported
 * module or type, we may create nested instances of this class via {@link #nested}. Likewise for
 * templates and constants under a template module import.
 */
public final class ImportedVar extends AbstractVarDefn implements Copyable<ImportedVar> {

  /** The kind of symbol this var references. */
  public enum SymbolKind {
    SOY_FILE,
    TEMPLATE,
    CONST,
    EXTERN,
    TYPEDEF,

    PROTO_MESSAGE,
    PROTO_ENUM,
    PROTO_EXT,

    CSS_MODULE,
    CSS_CLASS,

    TOGGLE,

    UNKNOWN
  }

  public static final String MODULE_IMPORT = "*";

  private final String symbol;
  // A registry of all lazily created nested types. Store these in a BIDI tree here for reachability
  // during AST copying as well as during constant type resolution.
  private final Map<String, ImportedVar> nestedVarDefns;
  // A back reference to the parent if this is a nested type.
  private final ImportedVar parent;
  // The file path of the ImportNode that owns this var. Only set if parent == null.
  private SourceLogicalPath filePath;
  private SetOnce<SymbolKind> kind = new SetOnce<>();

  public ImportedVar(String name, @Nullable String alias, SourceLocation nameLocation) {
    super(alias != null ? alias : name, nameLocation, null);
    Preconditions.checkArgument(alias == null || !alias.isEmpty());
    this.nestedVarDefns = new LinkedHashMap<>();
    this.symbol = name;
    this.parent = null;
  }

  private ImportedVar(ImportedVar parent, String symbol) {
    super(parent.name() + "." + symbol, parent.nameLocation(), null);
    this.nestedVarDefns = new LinkedHashMap<>();
    this.symbol = symbol;
    this.parent = parent;
  }

  private ImportedVar(ImportedVar var, ImportedVar parent, CopyState copyState) {
    super(var);
    this.nestedVarDefns = new LinkedHashMap<>();
    for (Map.Entry<String, ImportedVar> entry : var.nestedVarDefns.entrySet()) {
      ImportedVar newNested = new ImportedVar(entry.getValue(), this, copyState);
      this.nestedVarDefns.put(entry.getKey(), newNested);
      copyState.updateRefs(entry.getValue(), newNested);
    }
    this.symbol = var.symbol;
    this.parent = parent;
    this.filePath = var.filePath;
    this.kind = var.kind.copy();
  }

  public void onParentInit(SourceLogicalPath path) {
    Preconditions.checkState(parent == null);
    this.filePath = path;
  }

  @Nullable
  public SymbolKind getSymbolKind() {
    return kind.isPresent() ? kind.get() : null;
  }

  public void setSymbolKind(SymbolKind kind) {
    this.kind.set(kind);
  }

  @Override
  public ImportedVar copy(CopyState copyState) {
    return new ImportedVar(this, parent, copyState);
  }

  /**
   * Traverses up through any nested types and returns the top-level var corresponding to what's in
   * the source code.
   */
  public ImportedVar getRoot() {
    return parent != null ? parent.getRoot() : this;
  }

  /** Returns all the lazily created nested vars. */
  public Collection<ImportedVar> getNestedVars() {
    return Collections.unmodifiableCollection(nestedVarDefns.values());
  }

  /** Creates if necessary and returns a var representing a nested symbol. */
  public ImportedVar nested(String symbolName) {
    return nestedVarDefns.computeIfAbsent(symbolName, n -> new ImportedVar(this, n));
  }

  /**
   * Returns the non-prefixed name of the symbol as it appears in the dependency, or "*" for module
   * imports.
   */
  public String getSymbol() {
    return symbol;
  }

  @Override
  public Kind kind() {
    return Kind.IMPORT_VAR;
  }

  public boolean isAliased() {
    return !name().equals(symbol);
  }

  @Override
  public boolean isInjected() {
    return false;
  }

  public boolean isModuleImport() {
    return MODULE_IMPORT.equals(symbol);
  }

  public void setType(SoyType type) {
    this.type = type;
  }

  public SourceLogicalPath getSourceFilePath() {
    return parent != null ? parent.getSourceFilePath() : filePath;
  }

  /** Returns a list of imported vars, from the root imported symbol to the leaf symbol. */
  public ImmutableList<ImportedVar> getChain() {
    ImmutableList.Builder<ImportedVar> builder = ImmutableList.builder();
    ImportedVar var = this;
    do {
      builder.add(var);
      var = var.parent;
    } while (var != null);
    return builder.build().reverse();
  }

  private static boolean isProtoImport(ImportedVar var) {
    return var.getSymbolKind() == SymbolKind.PROTO_MESSAGE
        || var.getSymbolKind() == SymbolKind.PROTO_ENUM
        || var.getSymbolKind() == SymbolKind.PROTO_EXT;
  }

  @Override
  public boolean isEquivalent(VarDefn other) {
    // TODO: This logic is incomplete, there are other cases where imported vars should be
    // considered equivalent. For now, only imported proto types are handled.
    if (this == other) {
      return true;
    }
    if (!(other instanceof ImportedVar)) {
      return false;
    }
    return hasType()
        && other.hasType()
        && isProtoImport(this)
        && isProtoImport((ImportedVar) other)
        && type().equals(other.type());
  }
}
