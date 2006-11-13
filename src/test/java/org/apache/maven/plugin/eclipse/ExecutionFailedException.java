package org.apache.maven.plugin.eclipse;

import org.apache.maven.shared.invoker.InvocationResult;
import org.apache.maven.shared.invoker.MavenInvocationException;

public class ExecutionFailedException
    extends Exception
{

    private static final long serialVersionUID = 1L;
    
    private InvocationResult result;

    public ExecutionFailedException( String message, MavenInvocationException cause )
    {
        super( message + " (Maven invoker threw an exception.)", cause );
    }

    public ExecutionFailedException( String message, InvocationResult result )
    {
        super( message + " (Resulting exit code: " + result.getExitCode() + ")" );
        
        this.result = result;
    }
    
    public InvocationResult getInvocationResult()
    {
        return result;
    }

}
