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

def assertCheckstyleErrorExists(final File theFile,
                                final String[] fileContent,
                                final int lineNumber,
                                final int colNumber,
                                final String severity) {

    String expected = "<error line=\"" + lineNumber + "\" column=\"" + colNumber + "\" severity=\"" + severity + "\"";
    if(colNumber < 0) {
        expected = "<error line=\"" + lineNumber + "\" severity=\"" + severity + "\"";
    }

    boolean allOK = false;
    for(String current : fileContent) {
        if(current.contains(expected)) {
            allOK = true;
        }
    }

    if (allOK) println("Asserted OK: " + theFile.getName() + " contained expected text [" + expected + "]");
    assert allOK, "Expected line [" + expected + "] missing from [" + theFile.getName() + "]: " + fileContent;
}

def assertCheckstyleErrorDoesNotExist(final File theFile,
                                      final String[] fileContent,
                                      final int lineNumber,
                                      final int colNumber,
                                      final String severity) {

    String unexpected = "<error line=\"" + lineNumber + "\" column=\"" + colNumber + "\" severity=\"" + severity + "\"";
    if(colNumber < 0) {
        unexpected = "<error line=\"" + lineNumber + "\" severity=\"" + severity + "\"";
    }

    boolean allOK = true;
    for(String current : fileContent) {
        if(current.contains(unexpected)) {
            allOK = false;
        }
    }

    if (allOK) println("Asserted OK: " + theFile.getName() + " did not contain unexpected text [" + unexpected + "]");
    assert allOK, "Unexpected line [" + unexpected + "] present in [" + theFile.getName() + "]: " + fileContent;
}

// #1) Read the checkstyle-result.xml files from the two modules.
File resultModule1 = new File(basedir, 'module1/target/checkstyle-result.xml')
File resultModule2 = new File(basedir, 'module2/target/checkstyle-result.xml')
assert resultModule1.exists(), "Could not find expected file [" + resultModule1.getAbsolutePath() + "]";
assert resultModule2.exists(), "Could not find expected file [" + resultModule2.getAbsolutePath() + "]";

println("Checkstyle result for module1 exists. [" + resultModule1.getAbsolutePath() + "]")
println("Checkstyle result for module2 exists. [" + resultModule2.getAbsolutePath() + "]")

String[] result1Lines = resultModule1;
String[] result2Lines = resultModule2;

// #2) Assert content of module1/target/checkstyle-result.xml
//
// Module 1 should use *only* the configuration defined within the condestyle1 project.
//
assertCheckstyleErrorExists(resultModule1, result1Lines, 28, 1, "warning");
assertCheckstyleErrorDoesNotExist(resultModule1, result1Lines, 29, 5, "error"); // This incorrectly occurs in 6.11.2
assertCheckstyleErrorExists(resultModule1, result1Lines, 32, 5, "warning");
assertCheckstyleErrorExists(resultModule1, result1Lines, 39, 12, "error");
assertCheckstyleErrorExists(resultModule1, result1Lines, 40, 5, "warning");

/*
Expected checkstyle-result.xml for module 1:

<?xml version="1.0" encoding="UTF-8"?>
<checkstyle version="6.16.1">
<file name="/Users/lj/Development/Research/Maven/maven-plugins/maven-checkstyle-plugin/target/it/multimodule-configlocation/module1/src/main/java/se/west/foobar/module1/UsesIncorrectImports.java">
<error line="28" column="1" severity="warning" message="&apos;{&apos; at column 1 should be on the previous line." source="com.puppycrawl.tools.checkstyle.checks.blocks.LeftCurlyCheck"/>
<error line="32" column="5" severity="warning" message="&apos;{&apos; at column 5 should be on the previous line." source="com.puppycrawl.tools.checkstyle.checks.blocks.LeftCurlyCheck"/>
<error line="39" column="12" severity="error" message="Declaring variables, return values or parameters of type &apos;Date&apos; is not allowed." source="com.puppycrawl.tools.checkstyle.checks.coding.IllegalTypeCheck"/>
<error line="40" column="5" severity="warning" message="&apos;{&apos; at column 5 should be on the previous line." source="com.puppycrawl.tools.checkstyle.checks.blocks.LeftCurlyCheck"/>
</file>
</checkstyle>
*/


// #3) Assert content of module2/target/checkstyle-result.xml
//
// Module 2 should use *only* the configuration defined within the condestyle2 project.
//
assertCheckstyleErrorExists(resultModule2, result2Lines, 24, -1, "warning");
assertCheckstyleErrorExists(resultModule2, result2Lines, 29, 5, "warning");
assertCheckstyleErrorExists(resultModule2, result2Lines, 30, -1, "warning");

/*
Expected checkstyle-result.xml for module 2:

<?xml version="1.0" encoding="UTF-8"?>
<checkstyle version="6.16.1">
<file name="/Users/lj/Development/Research/Maven/maven-plugins/maven-checkstyle-plugin/target/it/multimodule-configlocation/module2/src/main/java/se/west/foobar/module2/UsesTooLongLine.java">
<error line="24" severity="warning" message="Line is longer than 110 characters (found 117)." source="com.puppycrawl.tools.checkstyle.checks.sizes.LineLengthCheck"/>
<error line="27" column="1" severity="warning" message="&apos;{&apos; at column 1 should be on the previous line." source="com.puppycrawl.tools.checkstyle.checks.blocks.LeftCurlyCheck"/>
<error line="29" column="5" severity="warning" message="&apos;{&apos; at column 5 should be on the previous line." source="com.puppycrawl.tools.checkstyle.checks.blocks.LeftCurlyCheck"/>
<error line="30" severity="warning" message="Line is longer than 110 characters (found 114)." source="com.puppycrawl.tools.checkstyle.checks.sizes.LineLengthCheck"/>
</file>
</checkstyle>
*/
