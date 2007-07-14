package org.apache.maven.plugin.assembly.filter;

import org.codehaus.plexus.archiver.ArchiveFinalizer;
import org.codehaus.plexus.components.io.fileselectors.FileSelector;

public interface ContainerDescriptorHandler
    extends ArchiveFinalizer, FileSelector
{

}
