package org.apache.maven.plugins.stage;

/** @author Jason van Zyl */
public interface RepositoryCopier
{
    String ROLE = RepositoryCopier.class.getName();

    String IN_PROCESS_MARKER = ".rip";

    String MD5 = "md5";

    String SHA1 = "sha1";

    String MAVEN_METADATA = "maven-metadata.xml";

    public void copy( String source, String target, String version )
        throws Exception;
}
