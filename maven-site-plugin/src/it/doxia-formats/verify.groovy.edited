
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

content = new File( basedir, 'target/site/markdown.html' ).text;

assert content.contains( 'Markdown Format works' );

assert !content.contains( ' quotes and double quotes were stripped' );

assert !content.contains( 'MACRO' );
assert content.contains( 'href="#Subsection"' );

ignore = new File( basedir, 'target/site/ignore.txt' );
assert !ignore.exists();

velocity = new File( basedir, 'target/generated-site/processed/velocity-context.apt' );
assert velocity.exists();
content = velocity.text;

assert content.contains( '= <<<val1>>>' ); // MSITE-550

assert !content.replace('<<<$value', '').contains( '<<<$' );

assert new File( basedir, 'target/site/markdown2.html' ).exists(); // DOXIA-535

return true;
