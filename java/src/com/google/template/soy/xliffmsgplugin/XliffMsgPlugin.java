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

package com.google.template.soy.xliffmsgplugin;

import com.google.common.io.CharSource;
import com.google.errorprone.annotations.Immutable;
import com.google.template.soy.error.ErrorReporter;
import com.google.template.soy.msgs.SoyMsgBundle;
import com.google.template.soy.msgs.SoyMsgBundleHandler.OutputFileOptions;
import com.google.template.soy.msgs.SoyMsgException;
import com.google.template.soy.msgs.SoyMsgPlugin;
import java.io.IOException;
import org.xml.sax.SAXException;

/**
 * Message plugin for XLIFF format.
 */
@Immutable
public final class XliffMsgPlugin implements SoyMsgPlugin {

  @Override
  public CharSequence generateExtractedMsgsFile(
      SoyMsgBundle msgBundle, OutputFileOptions options, ErrorReporter errorReporter) {

    return XliffGenerator.generateXliff(
        msgBundle, options.getSourceLocaleString(), options.getTargetLocaleString());
  }

  @Override
  public SoyMsgBundle parseTranslatedMsgsFile(CharSource translatedMsgsFileContent)
      throws IOException {

    try {
      return XliffParser.parseXliffTargetMsgs(translatedMsgsFileContent);
    } catch (SAXException e) {
      throw new SoyMsgException(e);
    }
  }
}
