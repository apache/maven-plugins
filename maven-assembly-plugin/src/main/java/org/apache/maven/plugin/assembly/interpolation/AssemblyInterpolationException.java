package org.apache.maven.plugin.assembly.interpolation;

import org.apache.maven.project.interpolation.ModelInterpolationException;

public class AssemblyInterpolationException
    extends Exception
{
    private String expression;

    private String originalMessage;
    
    public AssemblyInterpolationException( String message )
    {
        super( message );
    }

    public AssemblyInterpolationException( String message, Throwable cause )
    {
        super( message, cause );
    }
    
    public AssemblyInterpolationException( String expression, String message, Throwable cause )
    {
        super( "The Assembly expression: " + expression + " could not be evaluated. Reason: " + message, cause );

        this.expression = expression;
        this.originalMessage = message;
    }

    public AssemblyInterpolationException( String expression, String message )
    {
        super( "The Assembly expression: " + expression + " could not be evaluated. Reason: " + message );

        this.expression = expression;
        this.originalMessage = message;
    }
    
    public String getExpression()
    {
        return expression;
    }

    public String getOriginalMessage()
    {
        return originalMessage;
    }
}
