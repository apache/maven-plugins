package com.example.bar;

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

import org.apache.commons.io.ByteOrderMark;

/**
 * This is a Bar.
 */
public class Bar
{
    /**
     * Glibbifies the bar.
     * <p>
     * NOTE: The parameter using an external dependency is essential to
     * make the test fail with maven-javadoc-plugin 2.10.3.
     * </p>
     * <p>
     * If one dependency fails to be resolved (the one on module1), all
     * dependencies of the current module will be skipped.
     * </p>
     * <p>
     * As a result, in the generated Javadoc, ByteOrderMark will be missing
     * the package prefix.
     * </p>
     *
     * @param bom byte order mark
     */
    public void glibbify(ByteOrderMark bom)
    {
        // empty
    }
}
