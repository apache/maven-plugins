import junit.framework.TestCase;

public class SurefireTest extends TestCase {
    
	private boolean setupCalled = false;

	protected void setUp() {
		setupCalled = true;
	}
	
	protected void tearDown() {
		// is there a way to tests to see if tearDown was called?
	}
	
	public void testSetup() {
		assertTrue("Setup was not called", setupCalled);
	}

}
