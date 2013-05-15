package org.apache.maven.plugins.shade;

/**
 * Hello world!
 *
 */
public class Lib
{
    // simulate the type of call for static loggers that currently fails
    private static final String name = new String( Lib.class.getName() );

    public static final String CONSTANT = "foo.bar/baz";

    // constant shouldn't be changed if "org.codehaus.plexus.util.xml.pull.*" is excluded from relocation.
    public static final String CLASS_REALM_PACKAGE_IMPORT = "org.codehaus.plexus.util.xml.pull";

    public static String getClassRealmPackageImport()
    {
        // argument shouldn't be changed if "org.codehaus.plexus.util.xml.pull.*" is excluded from relocation.
        return importFrom( "org.codehaus.plexus.util.xml.pull" );
    }

    private static String importFrom( String packageName )
    {
        return packageName;
    }
}
