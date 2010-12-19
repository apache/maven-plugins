package org.apache.maven.plugin.ear.util;

import java.util.HashMap;
import java.util.Map;

/**
 * Represents the supported JavaEE version.
 *
 * @author Stephane Nicoll
 */
public class JavaEEVersion
    implements Comparable
{

    private static final String VERSION_1_3 = "1.3";

    private static final String VERSION_1_4 = "1.4";

    private static final String VERSION_5 = "5";

    private static final String VERSION_6 = "6";

    private static final Map versionsMap = new HashMap();


    /**
     * Represents the J2EE 1.3 version.
     */
    public static final JavaEEVersion OneDotThree = new JavaEEVersion( new Integer( 0 ), VERSION_1_3 );

    /**
     * Represents the J2EE 1.4 version.
     */
    public static final JavaEEVersion OneDotFour = new JavaEEVersion( new Integer( 1 ), VERSION_1_4 );

    /**
     * Represents the JavaEE 5 version.
     */
    public static final JavaEEVersion Five = new JavaEEVersion( new Integer( 2 ), VERSION_5 );

    /**
     * Represents the JavaEE 7 version.
     */
    public static final JavaEEVersion Six = new JavaEEVersion( new Integer( 3 ), VERSION_6 );


    private final Integer index;

    private final String version;

    private JavaEEVersion( Integer index, String version )
    {
        this.index = index;
        this.version = version;
        versionsMap.put( version, this );
    }

    public static JavaEEVersion getJavaEEVersion( String version )
        throws InvalidJavaEEVersion
    {
        if ( !isValid( version ) )
        {
            throw new InvalidJavaEEVersion( "Invalid version [" + version + "]", version );
        }
        return (JavaEEVersion) versionsMap.get( version );
    }

    /**
     * Returns the version as a string.
     *
     * @return the version string
     */
    public String getVersion()
    {
        return version;
    }

    /**
     * Specifies if this version is greater or equal to the specified version.
     *
     * @param version the version to check
     * @return true if this version is greater or equal to <tt>version</tt>
     */
    public boolean ge( JavaEEVersion version )
    {
        return this.compareTo( version ) >= 0;
    }

    /**
     * Specifies if this version is greater than the specified version.
     *
     * @param version the version to check
     * @return true if this version is greater to <tt>version</tt>
     */
    public boolean gt( JavaEEVersion version )
    {
        return this.compareTo( version ) > 0;
    }

    /**
     * Specifies if this version is equal to the specified version.
     *
     * @param version the version to check
     * @return true if this version is equal to <tt>version</tt>
     */
    public boolean eq( JavaEEVersion version )
    {
        return this.compareTo( version ) == 0;
    }

    /**
     * Specifies if this version is less or equal to the specified version.
     *
     * @param version the version to check
     * @return true if this version is less or equal to <tt>version</tt>
     */
    public boolean le( JavaEEVersion version )
    {
        return this.compareTo( version ) <= 0;
    }


    /**
     * Specifies if this version is less than the specified version.
     *
     * @param version the version to check
     * @return true if this version is less or equal to <tt>version</tt>
     */
    public boolean lt( JavaEEVersion version )
    {
        return this.compareTo( version ) < 0;
    }

    /**
     * Checks if the specified version string is valid.
     *
     * @param version the version string to check
     * @return <tt>true</tt> if the version is valid
     */
    private static boolean isValid( String version )
    {
        if ( version == null )
        {
            throw new IllegalArgumentException( "version could not be null." );
        }
        return VERSION_1_3.equals( version ) || VERSION_1_4.equals( version ) || VERSION_5.equals( version ) ||
            VERSION_6.equals( version );
    }

    public int compareTo( Object other )
    {
        if ( other == null )
        {
            throw new IllegalArgumentException( "other object to compare to could not be null." );
        }
        if ( !( other instanceof JavaEEVersion ) )
        {
            throw new IllegalArgumentException(
                "other object to compare must be a JavaEEVersion but was [" + other.getClass().getName() + "]" );
        }
        final JavaEEVersion otherVersion = (JavaEEVersion) other;
        return index.compareTo( otherVersion.index );
    }
}
