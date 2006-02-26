import org.testng.annotations.Configuration;
import org.testng.annotations.Test;
import org.testng.internal.Utils;


/**
 * Tests that forcing testng to run tests via the 
 * <code>"${maven.test.forcetestng}"</code> configuration option
 * works.
 * 
 * @author jkuhnert
 */
public class TestNGTest {

	/**
	 * Sets up testObject
	 */
	@Configuration(beforeTestClass = true, groups = "functional")
	public void configureTest()
	{
		testObject = new Object();
	}
	
	Object testObject;
	
	/**
	 * Tests reporting an error
	 */
	@Test(groups = {"functional", "notincluded"})
	public void isTestObjectNull()
	{
		assert testObject != null : "testObject is null";
	}
	
	/**
	 * Sample method that shouldn't be run by test suite.
	 */
	@Test(groups = "notincluded")
	public void shouldNotRun()
	{
		assert false == true : "Group specified by test shouldnt be run.";
	}
}