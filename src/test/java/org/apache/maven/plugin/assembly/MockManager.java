package org.apache.maven.plugin.assembly;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.easymock.MockControl;

public class MockManager
{
    
    private List mockControls = new ArrayList();
    
    public void add( MockControl control )
    {
        mockControls.add( control );
    }
    
    public void replayAll()
    {
        for ( Iterator it = mockControls.iterator(); it.hasNext(); )
        {
            MockControl control = (MockControl) it.next();
            
            control.replay();
        }
    }
    
    public void verifyAll()
    {
        for ( Iterator it = mockControls.iterator(); it.hasNext(); )
        {
            MockControl control = (MockControl) it.next();
            
            control.verify();
        }
    }

}
