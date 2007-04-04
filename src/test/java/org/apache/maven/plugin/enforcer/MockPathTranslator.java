package org.apache.maven.plugin.enforcer;

import java.io.File;

import org.apache.maven.model.Model;
import org.apache.maven.project.path.PathTranslator;

/**
 * @author <a href="mailto:brianf@apache.org">Brian Fox</a>
 * 
 */
public class MockPathTranslator
    implements PathTranslator
{

    /*
     * (non-Javadoc)
     * 
     * @see org.apache.maven.project.path.PathTranslator#alignToBaseDirectory(org.apache.maven.model.Model,
     *      java.io.File)
     */
    public void alignToBaseDirectory( Model theModel, File theBasedir )
    {
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.apache.maven.project.path.PathTranslator#alignToBaseDirectory(java.lang.String,
     *      java.io.File)
     */
    public String alignToBaseDirectory( String thePath, File theBasedir )
    {
        return theBasedir.getAbsolutePath();
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.apache.maven.project.path.PathTranslator#unalignFromBaseDirectory(org.apache.maven.model.Model,
     *      java.io.File)
     */
    public void unalignFromBaseDirectory( Model theModel, File theBasedir )
    {
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.apache.maven.project.path.PathTranslator#unalignFromBaseDirectory(java.lang.String,
     *      java.io.File)
     */
    public String unalignFromBaseDirectory( String theDirectory, File theBasedir )
    {
        return theBasedir.getAbsolutePath();
    }

}
