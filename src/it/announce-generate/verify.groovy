
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
assert new File(basedir, 'target/announcement/announcement.vm').exists();
content = new File(basedir, 'target/announcement/announcement.vm').text;

assert content.contains( 'Test report.' );

assert content.contains( 'Changes in this version include:' );

assert content.contains( 'New features:' );

assert content.contains( 'o Added additional documentation on how to configure the plugin.' );

assert content.contains( 'Fixed Bugs:' );

assert content.contains( 'o Enable retrieving component-specific issues.  Issue: MCHANGES-88.' );

assert content.contains( 'Changes:' );

assert content.contains( 'o Handle different issue systems.  Issue: MCHANGES-999.' );

assert content.contains( 'o Updated dependencies.' );

assert content.contains( 'Removed:' );

assert content.contains( 'o The element type \" link \" must be terminated by the matching end-tag.' );

assert content.contains( 'Deleted the erroneous code.  Issue: MCHANGES-899.' );

return true;
