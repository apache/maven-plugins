import junit.framework.TestCase;

public class HelloWorldTest extends TestCase
{
    public static final void testSayHello()
    {
        assertEquals("Hello World!", HelloWorld.sayHello("World"));
    }
}