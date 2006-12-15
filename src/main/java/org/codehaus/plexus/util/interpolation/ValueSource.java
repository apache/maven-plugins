package org.codehaus.plexus.util.interpolation;


public interface ValueSource
{
    
    /**
     * @return the value related to the expression, or null if not found.
     */
    public Object getValue( String expression );

}
