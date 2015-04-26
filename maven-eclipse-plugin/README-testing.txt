Testing maven-eclipse-plugin

  This is a complicated beast, it generates a bunch of different files (all in different formats: text, xml)
  that have hard coded paths and other junk in them.
  
  Most of the work is done in the integration tests.

  You have to set M2_HOME to the appropriate maven version
  you want to test with like this:
 
  export M2_HOME=/usr/share/java/apache-maven-3.1.1
  
  Use 
    mvn -Prun-its verify
  to run the integration tests
  
  One day these tests will be unified into whatever "sanctioned" way of doing integration tests becomes.
  
Running a single test
* Run mvn and tell surefire to only run your TestCase: 

  (See http://maven.apache.org/plugins/maven-surefire-plugin/examples/single-test.html for more details)
  
  mvn -Prun-its integration-test -Dit.test=EclipsePluginIT#testProject10
  
PluginTestTool
  The bulk of the integration tests are using the old (and obsoleted) method of PluginTestTool.
  These IT tests are invoked via maven-failsafe-plugin:integration-test which looks for JUnit test cases 
  from the ${project.build.testSourceDirectory} of the form:
    (see http://maven.apache.org/plugins/maven-failsafe-plugin/integration-test-mojo.html#includes)
    <includes>
     <include>**/IT*.java</include>
     <include>**/*IT.java</include>
     <include>**/*ITCase.java</include>
    </includes> 
  
  The test classes all extends AbstractEclipsePluginIT which initialised the testing area with a test
  version of the plugin under test.  Each actual test then needs to specify which test project should be run
  in a test method. e.g. EclipsePluginIT has methods like:
    public void testProject63()
        throws Exception
    {
        testProject( "project-63-MECLIPSE-388" );
    }
  which delegates to AbstractEclipsePluginIT.testProject() and specifies the test project directory that should 
  be used.  All test projects are located in src/test/resources/projects/, so in this example it would be
  src/test/resources/projects/project-63-MECLIPSE-388
  
  Each test project needs a pom.xml file.  It's easiest to copy and hack an existing file from another working test project.
  These test projects will not pollute your local ~/.m2/repository.  A separate test repository inside target/ is created
  that will house all the downloaded artifacts and installed test projects.
  
  A negative consequence of using PluginTestTool is that anything downloaded from central is not stored in
  your ~/.m2/repository which means wasted bandwidth after doing "mvn clean".
  
  Remember that your build/plugins/plugin for maven-eclipse-plugin needs:
    <version>test</version>
  for PluginTestTool to work.  You may need additional configuration settings,
  like workspace so that you dont accidentally pollute your tests with settings
  from your actual eclipse workspace used to develop this plugin.
  
* Validating a successful test

  Each test will automatically run a comparison of the generated files.
  A generated file will only be verified if the same file (including path hierarchy) exists in the 
  under the "expected" directory. e.g. src/test/resources/project-63-MECLIPSE-388/expected contains:
  * settings/org.eclipse.jdt.core.prefs
  * .classpath
  * .project
  
  Before comparison is done, each file (both expected and actual) is preprocessed via
  AbstractEclipsePluginIT.preprocess( File file, Map variables ) which
  * removes windows drive details
  * replaces any variables with their values, currently only "${basedir}" and "${M2_REPO}" are supported variable.
  * specific hacks for specific files like eclipse *.prefs files and wst files.  
  See the method for more details.
    
  The comparator read the first few bytes of the actual file to see if it contains an XML header, and 
  if so uses XMLAssert to compare the contents of the file.  This allows for variation in ordering
  but the contents must be the same.
  
  If the file name is ".classpath" then an assertXMLIdentical comparison is made, otherwise XMLAssert.assertXMLEqual
  is used (which is lenient about the ordering of the XML but requires the same contents)  
  
  All other cases do a text file comparison.
   
Invoker
  Some tests are done via invoker. 
  
  If you are behind a firewall then you must configure src/it/settings.xml.
  Do this by copying src/it/settings-default to settings-${user.name}.xml.
  The pom's process-resources configuration will copy this to src/it/settings.xml for you. 
  
  (TODO: Someone who understands how invoker works - can you complete this section)    
  
Running surefire-report:report-only

  After running the integration tests you can run surefire-report:report-only to build a report of the test success/failures.
  Then open target\site\surefire-report.html for more details.
  
Creating expected files

  The antrun plugin has been bound to the phase "post-integration-test" and will invoke the bean shell script
  "verify-integration-tests-checks.bsh".  This script will ensure that each generated file (that is knows about) 
  that exists in your test project directories has a corresponding expected file.
  
  When the expected file does not yet exist it will "seed" the src/test/resources project expected directory with the 
  one actually generated by the test run.  
  
  YOU MUST CHECK THIS FILE.
  
  When you go to check in changes these files should show up as requiring adding to version control.
  Please make sure you ensure that these files have been customized with variables so they work in anyones environment.
  
