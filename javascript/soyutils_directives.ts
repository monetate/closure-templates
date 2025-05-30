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

import {VERY_UNSAFE} from 'google3/javascript/template/soy/soydata_aliases';
import * as soy from 'google3/javascript/template/soy/soyutils_usegoog';
import {
  SanitizedContentKind,
  SanitizedHtml,
} from 'google3/third_party/javascript/closure/soy/data';

import {IncrementalDomRenderer} from './api_idom';
import {IdomFunction} from './element_lib_idom';

function isIdomFunctionType(
  value: unknown,
  type: SanitizedContentKind,
): value is IdomFunction {
  return (
    typeof value === 'function' &&
    (value as IdomFunction).isInvokableFn &&
    (value as IdomFunction).contentKind === type
  );
}

/**
 * Specialization of filterHtmlAttributes for Incremental DOM that can handle
 * attribute functions gracefully. In any other situation, this delegates to
 * the regular escaping directive.
 */
function filterHtmlAttributes(value: unknown): unknown {
  if (
    isIdomFunctionType(value, SanitizedContentKind.ATTRIBUTES) ||
    soy.$$isAttribute(value)
  ) {
    return value;
  }
  return soy.$$filterHtmlAttributes(value);
}

/**
 * Specialization of escapeHtml for Incremental DOM that can handle
 * html functions gracefully. In any other situation, this delegates to
 * the regular escaping directive.
 */
function escapeHtml(
  value: unknown,
  renderer: IncrementalDomRenderer,
): SanitizedHtml {
  if (isIdomFunctionType(value, SanitizedContentKind.HTML)) {
    return VERY_UNSAFE.ordainSanitizedHtml(value.toString(renderer));
  }
  return soy.$$escapeHtml(value);
}

/**
 * Specialization of bidiUnicodeWrap for Incremental DOM that can handle
 * html functions gracefully. In any other situation, this delegates to
 * the regular escaping directive.
 */
function bidiUnicodeWrap(
  bidiGlobalDir: number,
  value: unknown,
  renderer: IncrementalDomRenderer,
): string | SanitizedHtml {
  if (isIdomFunctionType(value, SanitizedContentKind.HTML)) {
    return soy.$$bidiUnicodeWrap(
      bidiGlobalDir,
      VERY_UNSAFE.ordainSanitizedHtml(value.toString(renderer)),
    );
  }
  return soy.$$bidiUnicodeWrap(bidiGlobalDir, value);
}

export {
  bidiUnicodeWrap as $$bidiUnicodeWrap,
  escapeHtml as $$escapeHtml,
  filterHtmlAttributes as $$filterHtmlAttributes,
  isIdomFunctionType as $$isIdomFunctionType,
};
