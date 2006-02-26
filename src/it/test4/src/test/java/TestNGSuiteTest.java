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
public class TestNGSuiteTest {

	/**
	 * Sets up testObject
	 */
	@Configuration(beforeTestClass = true)
	public void configureTest()
	{
		testObject = new Object();
	}
	
	Object testObject;
	
	/**
	 * Tests reporting an error
	 */
	@Test
	public void isTestObjectNull()
	{
		assert testObject != null : "testObject is null";
	}
}