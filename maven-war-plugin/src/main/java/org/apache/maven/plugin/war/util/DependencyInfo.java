package org.apache.maven.plugin.war.util;

import org.apache.maven.model.Dependency;

/**
 * Holds a dependency and packaging information.
 *
 * @author Stephane Nicoll
 */
public class DependencyInfo
{


    private final Dependency dependency;

    private String targetFileName;

    /**
     * Creates a new instance.
     *
     * @param dependency the dependency
     */
    public DependencyInfo( Dependency dependency )
    {
        this.dependency = dependency;
    }

    /**
     * Returns the dependency.
     *
     * @return the dependency
     */
    public Dependency getDependency()
    {
        return dependency;
    }

    /**
     * Returns the target filen ame of the dependency. If no target file name
     * is associated, returns <tt>null</tt>.
     *
     * @return the target file name or <tt>null</tt>
     */
    public String getTargetFileName()
    {
        return targetFileName;
    }

    /**
     * Sets the target file name.
     *
     * @param targetFileName the target file name
     */
    public void setTargetFileName( String targetFileName )
    {
        this.targetFileName = targetFileName;
    }

    public boolean equals( Object o )
    {
        if ( this == o )
        {
            return true;
        }
        if ( o == null || getClass() != o.getClass() )
        {
            return false;
        }

        DependencyInfo that = (DependencyInfo) o;

        if ( dependency != null ? !dependency.equals( that.dependency ) : that.dependency != null )
        {
            return false;
        }

        return true;
    }

    public int hashCode()
    {
        int result;
        result = ( dependency != null ? dependency.hashCode() : 0 );
        result = 31 * result + ( targetFileName != null ? targetFileName.hashCode() : 0 );
        return result;
    }
}
