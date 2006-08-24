package org.apache.maven.plugin.assembly.archive.archiver;

import org.codehaus.plexus.archiver.zip.ZipUnArchiver;

/**
 * @plexus.component role="org.codehaus.plexus.archiver.UnArchiver" role-hint="sar"
 * @author jdcasey
 * @todo delete this class once the plexus maven plugin can merge a generated components.xml with an existing one.
 */
public class SarUnArchiver
    extends ZipUnArchiver
{

}
