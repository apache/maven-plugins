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

File targetDir = new File( basedir, "target" );
File siteDir = new File( targetDir, "site" );

File projectReports = new File( siteDir, "project-reports.html" )
assert projectReports.getText( "UTF-8" ).contains( "pmd.html" )
assert projectReports.getText( "UTF-8" ).contains( "cpd.html" )

File pmdReportInSite = new File( siteDir, "pmd.html" )
assert pmdReportInSite.exists()

File pmdXmlInTarget = new File( targetDir, "pmd.xml" )
assert pmdXmlInTarget.exists()

File cpdReportInSite = new File( siteDir, "cpd.html" )
assert cpdReportInSite.exists()

File cpdXmlInTarget = new File( targetDir, "cpd.xml" )
assert cpdXmlInTarget.exists()
