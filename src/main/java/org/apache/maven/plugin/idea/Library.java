package org.apache.maven.plugin.idea;

/**
 * @author Patrick Lightbody (plightbo at gmail dot com)
 */
public class Library
{
    private String name;

    private String sources;

    private String classes;

    private boolean exclude;

    public String getName()
    {
        return name;
    }

    public void setName( String name )
    {
        this.name = name;
    }

    public String getSources()
    {
        return sources;
    }

    public void setSources( String sources )
    {
        this.sources = sources;
    }

    public String[] getSplitSources()
    {
        if ( sources == null )
        {
            return new String[0];
        }

        return sources.split( "[,\\s]+" );
    }

    public String[] getSplitClasses()
    {
        if ( classes == null )
        {
            return new String[0];
        }

        return classes.split( "[,\\s]+" );
    }

    public boolean isExclude()
    {
        return exclude;
    }

    public void setExclude( boolean exclude )
    {
        this.exclude = exclude;
    }

    public String getClasses()
    {
        return classes;
    }

    public void setClasses( String classes )
    {
        this.classes = classes;
    }

    public String toString()
    {
        return name + " : " + getSplitSources() + "; " + getSplitClasses() + "; " + exclude;
    }
}
