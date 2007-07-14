package org.apache.maven.plugin.assembly.archive.archiver;

import org.codehaus.plexus.archiver.FileSet;
import org.codehaus.plexus.components.io.fileselectors.FileSelector;

import java.io.File;

public class PrefixedFileSet
    implements FileSet
{

    private final String rootPrefix;
    private final FileSet fileSet;
    private final FileSelector[] selectors;

    public PrefixedFileSet( FileSet fileSet, String rootPrefix, FileSelector[] selectors )
    {
        this.fileSet = fileSet;
        this.selectors = selectors;

        if ( ! rootPrefix.endsWith( "/" ) )
        {
            this.rootPrefix = rootPrefix + "/";
        }
        else
        {
            this.rootPrefix = rootPrefix;
        }
    }

    public String[] getExcludes()
    {
        return fileSet.getExcludes();
    }

    public FileSelector[] getFileSelectors()
    {
        FileSelector[] sel = fileSet.getFileSelectors();
        if ( ( sel != null ) && ( selectors != null ) )
        {
            FileSelector[] temp = new FileSelector[ sel.length + selectors.length ];

            System.arraycopy( sel, 0, temp, 0, sel.length );
            System.arraycopy( selectors, 0, temp, sel.length, selectors.length );

            sel = temp;
        }
        else if ( ( sel == null ) && ( selectors != null ) )
        {
            sel = selectors;
        }

        return sel;
    }

    public String[] getIncludes()
    {
        return fileSet.getIncludes();
    }

    public String getPrefix()
    {
        String prefix = fileSet.getPrefix();
        if ( prefix.startsWith( "/" ) )
        {
            if ( prefix.length() > 1 )
            {
                prefix = prefix.substring( 1 );
            }
            else
            {
                prefix = "";
            }
        }

        return rootPrefix + prefix;
    }

    public boolean isCaseSensitive()
    {
        return fileSet.isCaseSensitive();
    }

    public boolean isIncludingEmptyDirectories()
    {
        return fileSet.isIncludingEmptyDirectories();
    }

    public boolean isUsingDefaultExcludes()
    {
        return fileSet.isUsingDefaultExcludes();
    }

    public File getDirectory()
    {
        return fileSet.getDirectory();
    }

}
