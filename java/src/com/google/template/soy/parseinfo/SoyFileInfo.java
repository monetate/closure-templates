/*
 * Copyright 2009 Google Inc.
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

package com.google.template.soy.parseinfo;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.protobuf.Descriptors.FileDescriptor;

/** Parsed info about a Soy file. */
public class SoyFileInfo {

  /** The source Soy file's name. */
  private final String fileName;

  /** The Soy file's namespace. */
  private final String namespace;

  /** List of public basic templates in this Soy file. */
  private final ImmutableList<SoyTemplateInfo> templates;

  /** The CSS names appearing in this file. */
  private final ImmutableSet<String> cssNames;

  /**
   * Constructor for internal use only. Do not call, do not subclass.
   *
   * @param fileName The source Soy file's name.
   * @param namespace The Soy file's namespace.
   * @param templates List of templates in this Soy file.
   */
  protected SoyFileInfo(
      String fileName,
      String namespace,
      ImmutableList<SoyTemplateInfo> templates,
      ImmutableSet<String> cssNames) {
    this.fileName = fileName;
    this.namespace = namespace;
    this.templates = templates;
    this.cssNames = cssNames;
  }

  /** Returns the source Soy file's name. */
  public final String getFileName() {
    return fileName;
  }

  /** Returns the Soy file's namespace. */
  public final String getNamespace() {
    return namespace;
  }

  /** Returns the list of templates in this Soy file. */
  public final ImmutableList<SoyTemplateInfo> getTemplates() {
    return templates;
  }

  /** Returns the CSS names appearing in this file. */
  public final ImmutableSet<String> getCssNames() {
    return cssNames;
  }

  /**
   * Returns a list of any protocol buffer types used by the templates.
   *
   * <p>The elements are either Descriptors or EnumDescriptor objects.
   */
  public ImmutableList<FileDescriptor> getProtoDescriptors() {
    return ImmutableList.of();
  }
}
