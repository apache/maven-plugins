package byebye;

import byebye.ByeBye;
import junit.framework.TestCase;

public class ByeByeTest extends TestCase
{
    public void testByeBye()
    {
        final ByeBye byeBye = new ByeBye();
        assertEquals( "Bye bye world!", byeBye.byebye( null ) );
        assertEquals( "Bye bye EMMA!", byeBye.byebye( "EMMA" ) );
    }
}
