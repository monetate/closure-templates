/*
 * Copyright 2011 Google Inc.
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

import static com.google.common.base.Preconditions.checkState;
import static com.google.common.collect.ImmutableList.toImmutableList;
import static java.util.stream.Collectors.joining;

import com.google.common.collect.ImmutableList;
import com.google.template.soy.base.SourceLocation;
import com.google.template.soy.base.SourceLogicalPath;
import com.google.template.soy.basetree.CopyState;
import com.google.template.soy.exprtree.StringNode;
import com.google.template.soy.soytree.defn.SymbolVar;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

/** Node representing a 'import' statement with a value expression. */
public final class ImportNode extends AbstractSoyNode {

  /** The value expression that the variable is set to. */
  private final ImmutableList<SymbolVar> identifiers;

  private final StringNode path;
  private final SourceLogicalPath sourceLogicalPath;
  private ImportType importType;

  private Optional<SoyFileNode.CssPath> requiredCssPath;

  /** The category of import, based on the path suffix. */
  public enum ImportType {
    CSS,
    TOGGLE,
    PROTO,
    TEMPLATE,
    UNKNOWN
  }

  public ImportNode(int id, SourceLocation location, StringNode path, List<SymbolVar> defns) {
    super(id, location);
    this.identifiers = ImmutableList.copyOf(defns);
    this.path = path;
    this.sourceLogicalPath = SourceLogicalPath.create(path.getValue());
    this.importType = ImportType.UNKNOWN;
    this.requiredCssPath = Optional.empty();

    for (SymbolVar defn : identifiers) {
      defn.initFromSoyNode(true, getSourceFilePath());
    }
  }

  /**
   * Copy constructor.
   *
   * @param orig The node to copy.
   */
  private ImportNode(ImportNode orig, CopyState copyState) {
    super(orig, copyState);
    this.identifiers =
        orig.identifiers.stream()
            .map(
                prev -> {
                  SymbolVar next = prev.copy(copyState);
                  copyState.updateRefs(prev, next);
                  return next;
                })
            .collect(toImmutableList());
    this.path = orig.path.copy(copyState);
    this.sourceLogicalPath = orig.sourceLogicalPath;
    this.importType = orig.importType;
    this.requiredCssPath = orig.requiredCssPath;
  }

  @Override
  public Kind getKind() {
    return Kind.IMPORT_NODE;
  }

  @Override
  public ImportNode copy(CopyState copyState) {
    return new ImportNode(this, copyState);
  }

  public void setImportType(ImportType importType) {
    this.importType = importType;
    if (importType == ImportType.CSS) {
      String sourcePath = "google3/" + getPath().substring(0, getPath().length() - ".css".length());
      this.requiredCssPath = Optional.of(new SoyFileNode.CssPath(sourcePath));
    }
  }

  public ImportType getImportType() {
    return importType;
  }

  public String getPath() {
    return path.getValue();
  }

  public SourceLogicalPath getSourceFilePath() {
    return sourceLogicalPath;
  }

  /**
   * Whether this is a module import (e.g. "import * as foo from ..."), as opposed to a symbol
   * import node (e.g. "import {foo,bar,baz} from ...").
   */
  public boolean isModuleImport() {
    return identifiers.size() == 1 && identifiers.get(0).isModuleImport();
  }

  /**
   * Returns the module alias (e.g. "foo" if the import is "import * as foo from 'my_foo.soy';").
   * This should only be called on module import nodes (i.e. if {@link #isModuleImport} node is
   * true).
   */
  public String getModuleAlias() {
    checkState(
        isModuleImport(),
        "Module alias can only be retrieved for module imports (e.g. \"import * as fooTemplates"
            + " from 'my_foo.soy';\")");
    return identifiers.get(0).name();
  }

  public SourceLocation getPathSourceLocation() {
    return path.getSourceLocation();
  }

  public ImmutableList<SymbolVar> getIdentifiers() {
    return identifiers;
  }

  public Optional<SoyFileNode.CssPath> getRequiredCssPath() {
    return requiredCssPath;
  }

  @Override
  public String toSourceString() {
    String exprs = "";
    if (!identifiers.isEmpty()) {
      exprs =
          String.format(
              "{%s} from ",
              identifiers.stream()
                  .map(i -> i.isAliased() ? i.getSymbol() + " as " + i.name() : i.name())
                  .collect(joining(",")));
    }
    return String.format("import %s'%s'", exprs, path.getValue());
  }

  /**
   * Visits all {@link SymbolVar} descending from this import node. {@code visitor} is called once
   * for each var.
   */
  public void visitVars(Consumer<SymbolVar> visitor) {
    getIdentifiers().forEach(id -> visitVars(id, visitor));
  }

  private static void visitVars(SymbolVar id, Consumer<SymbolVar> visitor) {
    visitor.accept(id);
    id.getNestedVars().forEach(nestedVar -> visitVars(nestedVar, visitor));
  }
}
