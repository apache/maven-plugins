package it;

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

/**
 * This class violates http://pmd.sourceforge.net/pmd-5.0.1/rules/java/junit.html#TestClassWithoutTestCases but for PMD
 * to actually detect this, it needs to do type resolution which in turn requires use of the proper aux classpath for
 * PMD.
 */
// does not directly extend *TestCase to enforce need for type resolution, otherwise PMD uses name-based heuristics
public class NoTestsHere
    extends TestSupport
{

}
