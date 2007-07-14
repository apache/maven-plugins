package org.apache.maven.plugin.assembly.archive.archiver;

import org.codehaus.plexus.archiver.ArchivedFileSet;
import org.codehaus.plexus.components.io.fileselectors.FileSelector;

import java.io.File;

public class PrefixedArchivedFileSet
    implements ArchivedFileSet
{

    private final String rootPrefix;
    private final ArchivedFileSet fileSet;
    private final FileSelector[] selectors;

    public PrefixedArchivedFileSet( ArchivedFileSet fileSet, String rootPrefix, FileSelector[] selectors )
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

    public File getArchive()
    {
        return fileSet.getArchive();
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

}
