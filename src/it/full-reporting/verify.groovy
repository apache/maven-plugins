
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
assert new File( basedir, 'target/surefire-reports' ).exists();
assert new File( basedir, 'target/surefire-reports/org.apache.maven.plugins.site.its.AppTest.txt' ).exists();
content = new File( basedir, 'target/surefire-reports/org.apache.maven.plugins.site.its.AppTest.txt' ).text;

assert content.contains( 'Test set: org.apache.maven.plugins.site.its.AppTest' );

assert content.contains( 'Tests run: 1, Failures: 0, Errors: 0, Skipped: 0' );

sitedir = new File( basedir, 'target/site' );

assert new File( sitedir, 'surefire-report.html' ).exists();
assert new File( sitedir, 'index.html' ).exists();
assert new File( sitedir, 'checkstyle.html' ).exists();
assert new File( sitedir, 'cpd.html' ).exists();
assert new File( sitedir, 'apidocs/index.html' ).exists();
assert new File( sitedir, 'apidocs/org/apache/maven/plugins/site/its/App.html' ).exists();
assert new File( sitedir, 'cobertura/index.html' ).exists();
assert new File( sitedir, 'xref/index.html' ).exists();
assert new File( sitedir, 'xref-test/index.html' ).exists();

assert new File( sitedir, 'taglist.html' ).exists();
assert new File( sitedir, 'team-list.html' ).exists();

assert new File( sitedir, 'dependencies.html' ).exists();
content = new File( sitedir, 'dependencies.html' ).text;
assert content.contains( 'junit:junit:jar:3.8.2' );

// check reports order
String[] reports = [ 'index',                  // <report>index</report>
                     'project-summary',        // <report>summary</report>
                     'license',                // <report>license</report>
                     'team-list',              // <report>project-team</report>
                     'source-repository',      // <report>scm</report>
                     'issue-tracking',         // <report>issue-tracking</report>
                     'mail-lists',             // <report>mailing-list</report>
                     'dependencies',           // <report>dependencies</report>
                     'integration',            // <report>cim</report>
                     'plugin-management',      // <report>plugin-management</report>
                     'plugins'                 // <report>plugins</report>
                   ];
String info = new File( sitedir, 'project-info.html' ).text;
int index1 = 10;
int index2 = 10;
String previousReportLink;
for ( String report : reports )
{
    File reportFile = new File( sitedir, report + ".html" );
    if ( !reportFile.isFile() )
    {
        println "Report file not existent: $reportFile";
        return false;
    }

    String link = "<a href=\"" + reportFile.getName() + "\"";
    int i1 = info.indexOf( link );
    int i2 = info.indexOf( link, i1 + 1 );
    if ( i1 < index1 )
    {
        println "Wrong order for first report link: expected $previousReportLink -> $link, but found $i1 < $index1";
        println "   previous report link: " + info.substring( index1 - 10, index1 + 70 );
        println "     actual report link: " + info.substring( i1 - 10, i1 + 70 );
        //return false; // does not work with Maven 2.2 for the moment
    }
    if ( i2 < index2 )
    {
        println "Wrong order for second report link: expected $previousReportLink -> $link, but found $i2 < $index2";
        println "   previous report link: " + info.substring( index2 - 10, index2 + 70 );
        println "     actual report link: " + info.substring( i2 - 10, i2 + 70 );
        //return false; // does not work with Maven 2.2 for the moment
    }
    index1 = i1;
    index2 = i2;
    previousReportLink = link;
}

return true;