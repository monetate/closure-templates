/*
 * Copyright 2015 Google Inc.
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

import com.google.template.soy.exprtree.AbstractReturningExprNodeVisitor;
import com.google.template.soy.exprtree.ExprNode;
import com.google.template.soy.exprtree.FunctionNode;
import com.google.template.soy.exprtree.ListComprehensionNode.ComprehensionVarDefn;
import com.google.template.soy.exprtree.VarDefn;
import com.google.template.soy.exprtree.VarRefNode;
import com.google.template.soy.shared.internal.BuiltinFunction;
import com.google.template.soy.soytree.SoyNode.LocalVarNode;
import com.google.template.soy.soytree.defn.LocalVar;
import com.google.template.soy.soytree.defn.SymbolVar;
import com.google.template.soy.soytree.defn.TemplateParam;

/**
 * An abstract base class that adds extra visitor methods for unpacking varrefs and functions based
 * on their subtypes.
 */
abstract class EnhancedAbstractExprNodeVisitor<T> extends AbstractReturningExprNodeVisitor<T> {

  @Override
  protected T visit(ExprNode node) {
    try {
      return super.visit(node);
    } catch (UnexpectedCompilerFailureException e) {
      e.addLocation(node);
      throw e;
    } catch (Throwable t) {
      throw new UnexpectedCompilerFailureException(node, t);
    }
  }

  @Override
  protected final T visitVarRefNode(VarRefNode node) {
    VarDefn defn = node.getDefnDecl();
    switch (defn.kind()) {
      case LOCAL_VAR:
        LocalVar local = (LocalVar) defn;
        LocalVarNode declaringNode = local.declaringNode();
        switch (declaringNode.getKind()) {
          case FOR_NONEMPTY_NODE:
            return visitForLoopVar(node, local);
          case LET_CONTENT_NODE:
          case LET_VALUE_NODE:
            return visitLetNodeVar(node, local);
          default:
            throw new AssertionError("Unexpected local variable: " + local);
        }
      case PARAM:
        return visitParam(node, (TemplateParam) defn);
      case STATE:
        throw new AssertionError("state should have been desugared");
      case COMPREHENSION_VAR:
        return visitListComprehensionVar(node, (ComprehensionVarDefn) defn);
      case SYMBOL:
        return visitSymbolVar(node, (SymbolVar) defn);
    }
    throw new AssertionError(defn.kind());
  }

  @Override
  protected final T visitFunctionNode(FunctionNode node) {
    Object function = node.getSoyFunction();

    if (function instanceof BuiltinFunction) {
      BuiltinFunction builtinFn = (BuiltinFunction) function;
      switch (builtinFn) {
        case CHECK_NOT_NULL:
          return visitCheckNotNullFunction(node);
        case CSS:
          return visitCssFunction(node);
        case EVAL_TOGGLE:
          return visitToggleFunction(node);
        case XID:
          return visitXidFunction(node);
        case RECORD_JS_ID:
          return visitRecordJsIdFunction(node);
        case IS_PRIMARY_MSG_IN_USE:
          return visitIsPrimaryMsgInUse(node);
        case TO_NUMBER:
          return visitToNumberFunction(node);
        case INT_TO_NUMBER:
          return visitIntToNumberFunction(node);
        case DEBUG_SOY_TEMPLATE_INFO:
          return visitDebugSoyTemplateInfoFunction(node);
        case VE_DATA:
          return visitVeDataFunction(node);
        case SOY_SERVER_KEY:
          return visitSoyServerKeyFunction(node);
        case PROTO_INIT:
          return visitProtoInitFunction(node);
        case VE_DEF:
          return visitVeDefNode(node);
        case EMPTY_TO_UNDEFINED:
          return visitEmptyToUndefinedFunction(node);
        case UNDEFINED_TO_NULL:
        case UNDEFINED_TO_NULL_SSR:
          return visitUndefinedToNullFunction(node);
        case BOOLEAN:
          return visitBooleanFunction(node);
        case HAS_CONTENT:
          return visitHasContentFunction(node);
        case IS_TRUTHY_NON_EMPTY:
          return visitIsTruthyNonEmptyFunction(node);
        case NEW_SET:
          return visitNewSetFunction(node);
        case FLUSH_PENDING_LOGGING_ATTRIBUTES:
          return visitFlushPendingLoggingAttributesFunction(node);
        case MSG_WITH_ID:
        case REMAINDER:
          // should have been removed earlier in the compiler
        case UNKNOWN_JS_GLOBAL:
        case LEGACY_DYNAMIC_TAG:
          // unknownJsGlobals should not exist in jbcsrc
          throw new AssertionError();
      }
    }

    return visitPluginFunction(node);
  }

  T visitForLoopVar(VarRefNode varRef, LocalVar local) {
    return visitExprNode(varRef);
  }

  T visitSymbolVar(VarRefNode node, SymbolVar c) {
    return visitExprNode(node);
  }

  T visitLetNodeVar(VarRefNode node, LocalVar local) {
    return visitExprNode(node);
  }

  T visitParam(VarRefNode varRef, TemplateParam param) {
    return visitExprNode(varRef);
  }

  T visitListComprehensionVar(VarRefNode varRef, ComprehensionVarDefn var) {
    return visitExprNode(varRef);
  }


  T visitCheckNotNullFunction(FunctionNode node) {
    return visitExprNode(node);
  }

  T visitCssFunction(FunctionNode node) {
    return visitExprNode(node);
  }

  T visitRecordJsIdFunction(FunctionNode node) {
    return visitExprNode(node);
  }

  T visitToggleFunction(FunctionNode node) {
    return visitExprNode(node);
  }

  T visitXidFunction(FunctionNode node) {
    return visitExprNode(node);
  }

  T visitSoyServerKeyFunction(FunctionNode node) {
    return visitExprNode(node);
  }

  T visitIsPrimaryMsgInUse(FunctionNode node) {
    return visitExprNode(node);
  }

  T visitToNumberFunction(FunctionNode node) {
    return visitExprNode(node);
  }

  T visitIntToNumberFunction(FunctionNode node) {
    return visitExprNode(node);
  }

  T visitDebugSoyTemplateInfoFunction(FunctionNode node) {
    return visitExprNode(node);
  }

  T visitVeDataFunction(FunctionNode node) {
    return visitExprNode(node);
  }

  T visitVeDefNode(FunctionNode node) {
    return visitExprNode(node);
  }

  T visitEmptyToUndefinedFunction(FunctionNode node) {
    return visitExprNode(node);
  }

  T visitUndefinedToNullFunction(FunctionNode node) {
    return visitExprNode(node);
  }

  T visitProtoInitFunction(FunctionNode node) {
    return visitExprNode(node);
  }

  T visitPluginFunction(FunctionNode node) {
    return visitExprNode(node);
  }

  T visitBooleanFunction(FunctionNode node) {
    return visitExprNode(node);
  }

  T visitHasContentFunction(FunctionNode node) {
    return visitExprNode(node);
  }

  T visitIsTruthyNonEmptyFunction(FunctionNode node) {
    return visitExprNode(node);
  }

  T visitNewSetFunction(FunctionNode node) {
    return visitExprNode(node);
  }

  T visitFlushPendingLoggingAttributesFunction(FunctionNode node) {
    return visitExprNode(node);
  }
}
