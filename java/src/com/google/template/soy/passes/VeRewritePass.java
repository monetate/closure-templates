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

package com.google.template.soy.passes;

import com.google.template.soy.base.internal.IdGenerator;
import com.google.template.soy.base.internal.Identifier;
import com.google.template.soy.exprtree.ExprNode.Kind;
import com.google.template.soy.exprtree.FunctionNode;
import com.google.template.soy.exprtree.GlobalNode;
import com.google.template.soy.shared.internal.BuiltinFunction;
import com.google.template.soy.soytree.SoyFileNode;
import com.google.template.soy.soytree.SoyTreeUtils;
import com.google.template.soy.soytree.VeLogNode;

/**
 * A compiler pass that rewrites syntactic sugar related to VE logging:
 *
 * <ul>
 *   <li>Rewrites {@code {velog MyVe}} to {@code {velog ve_data(MyVe)}}
 * </ul>
 */
final class VeRewritePass implements CompilerFilePass {

  @Override
  public void run(SoyFileNode file, IdGenerator nodeIdGen) {
    for (VeLogNode node : SoyTreeUtils.getAllNodesOfType(file, VeLogNode.class)) {
      maybeRewriteVeLogNode(node);
    }
  }

  private void maybeRewriteVeLogNode(VeLogNode node) {
    if (node.getVeDataExpression().getRoot().getKind() == Kind.GLOBAL_NODE) {
      GlobalNode veName = (GlobalNode) node.getVeDataExpression().getRoot();
      FunctionNode veData =
          FunctionNode.newPositional(
              Identifier.create(BuiltinFunction.VE_DATA.getName(), veName.getSourceLocation()),
              BuiltinFunction.VE_DATA,
              veName.getSourceLocation());
      veData.addChild(veName);
      // Adding veName as a child of veData above removes veName as a child of the VeLogNode's
      // VeDataExpression. So we can just add a child back here, instead of replacing.
      node.getVeDataExpression().addChild(veData);
    }
  }
}
