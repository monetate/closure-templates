/*
 * Copyright 2025 Google Inc.
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

package com.google.template.soy.basicfunctions;

import static com.google.common.truth.Truth.assertThat;

import com.google.template.soy.plugin.java.restricted.testing.SoyJavaSourceFunctionTester;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** Unit tests for CosFunction. */
@RunWith(JUnit4.class)
public class CosFunctionTest {

  @Test
  public void testComputeForJavaSource() {
    CosFunction cosFunction = new CosFunction();

    SoyJavaSourceFunctionTester factory = new SoyJavaSourceFunctionTester(cosFunction);
    assertThat(factory.callFunction(0)).isEqualTo(1.0);
    assertThat(factory.callFunction(0.0)).isEqualTo(1.0);
    assertThat(factory.callFunction(1)).isEqualTo(0.5403023058681398);
    assertThat(factory.callFunction(Math.PI)).isEqualTo(-1.0);
    assertThat(factory.callFunction(Math.PI * 2)).isEqualTo(1.0);
    assertThat(factory.callFunction(Double.POSITIVE_INFINITY)).isEqualTo(Double.NaN);
  }
}
