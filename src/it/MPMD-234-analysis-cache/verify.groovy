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

File cacheFile = new File( basedir, 'no-analysis-cache/target/pmd/pmd.cache' )
assert !cacheFile.exists()

File defaultAnalysisCacheLocation = new File( basedir, 'default-analysis-cache/target/pmd/pmd.cache' )
assert defaultAnalysisCacheLocation.exists()

File customCacheLocation = new File( basedir, 'custom-analysis-cache/target/custom-analysis-pmd.cache' )
assert customCacheLocation.exists()
