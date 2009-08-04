package org.apache.maven.plugins.repository.testutil;

import org.codehaus.plexus.components.interactivity.InputHandler;

import java.io.IOException;
import java.util.List;
import java.util.Stack;

public class TestInputHandler
    implements InputHandler
{
    
    private Stack lineResponses;
    
    private Stack lineListResponses;
    
    private Stack passwordResponses;

    public String readLine()
        throws IOException
    {
        return (String) ( lineResponses == null || lineResponses.isEmpty() ? null : lineResponses.pop() );
    }

    public List readMultipleLines()
        throws IOException
    {
        return (List) ( lineListResponses == null || lineListResponses.isEmpty() ? null : lineListResponses.pop() );
    }

    public String readPassword()
        throws IOException
    {
        return (String) ( passwordResponses == null || passwordResponses.isEmpty() ? null : passwordResponses.pop() );
    }

    public void setLineResponses( Stack responses )
    {
        this.lineResponses = responses;
    }

    public void setLineListResponses( Stack lineLists )
    {
        this.lineListResponses = lineLists;
    }

    public void setPasswordResponses( Stack responses )
    {
        this.passwordResponses = responses;
    }

}
