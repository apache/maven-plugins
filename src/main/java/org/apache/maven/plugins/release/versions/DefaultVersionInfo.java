package org.apache.maven.plugins.release.versions;

/*
 * Copyright 2001-2006 The Apache Software Foundation.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.codehaus.plexus.util.StringUtils;

/** This compares and increments versions for a common java versioning scheme.
 * <p>
 * The supported version scheme has the following parts.<br>
 * <code><i>component-digits-annotation-annotationRevision-buildSpecifier</i></code><br>
 * Example:<br>
 * <code>my-component-1.0.1-alpha-2-SNAPSHOT</code>
 * 
 * <ul>Terms:
 *  <li><i>component</i> - name of the versioned component (log4j, commons-lang, etc)
 *  <li><i>digits</i> - Numeric digits with at least one "." period. (1.0, 1.1, 1.01, 1.2.3, etc)
 *  <li><i>annotation</i> - Version annotation - Valid Values are (alpha, beta, RC).
 *   Use {@link DefaultVersionInfo#setAnnotationOrder(List)} to change the valid values.
 *  <li><i>annotationRevision</i> - Integer qualifier for the annotation. (4 as in RC-4)
 *  <li><i>buildSpecifier</i> - Additional specifier for build. (SNAPSHOT, or build number like "20041114.081234-2")
 * </ul>
 * <b>Digits is the only required piece of the version string, and must contain at lease one "." period.</b>
 * <p>
 * Implementation details:<br>
 * The separators "_" and "-" between components are also optional (though they are usually reccommended).<br>
 * Example:<br>
 * <code>log4j-1.2.9-beta-9-SNAPSHOT == log4j1.2.9beta9SNAPSHOT == log4j_1.2.9_beta_9_SNAPSHOT</code>
 * <p>
 * All numbers in the "digits" part of the version are considered Integers. Therefore 1.01.01 is the same as 1.1.1
 * Leading zeros are ignored when performing comparisons.
 *
 */
public class DefaultVersionInfo
    implements VersionInfo, Cloneable
{
    protected String strVersion;

    protected String component;

    protected List digits;

    protected String annotation;

    protected String annotationRevision;

    protected String buildSpecifier;

    protected String digitSeparator;

    protected String annotationSeparator;

    protected String annotationRevSeparator;

    protected String buildSeparator;
    
    protected List annotationOrder;
    
    private final static int COMPONENT_INDEX = 1;

    private final static int DIGIT_SEPARATOR_INDEX = 2;

    private final static int DIGITS_INDEX = 3;

    private final static int ANNOTATION_SEPARATOR_INDEX = 4;

    private final static int ANNOTATION_INDEX = 5;

    private final static int ANNOTATION_REV_SEPARATOR_INDEX = 6;

    private final static int ANNOTATION_REVISION_INDEX = 7;

    private final static int BUILD_SEPARATOR_INDEX = 8;

    private final static int BUILD_SPECIFIER_INDEX = 9;

    public static final String SNAPSHOT_IDENTIFIER = "SNAPSHOT";
    
    protected final static String DIGIT_SEPARATOR_STRING = ".";

    protected final static Pattern STANDARD_PATTERN = Pattern.compile( 
        "^(.*?)" +                  // non greedy .* to grab the component. 
        "([-_])?" +                 // optional - or _  (digits separator)
        "((?:\\d+[.])+\\d+)" +      // digit(s) and '.' repeated - followed by digit (version digits 1.22.0, etc)
        "([-_])?" +                 // optional - or _  (annotation separator)
        "([a-zA-Z]*)" +             // alpha characters (looking for annotation - alpha, beta, RC, etc.)
        "([-_])?" +                 // optional - or _  (annotation revision separator)
        "(\\d*)" +                  // digits  (any digits after rc or beta is an annotation revision)
        "(?:([-_])?(.*?))?$");      // - or _ followed everything else (build specifier)
    
    protected final static Pattern DIGIT_SEPARATOR_PATTERN = Pattern.compile( "(\\d+)\\.?" );
    
    /** Constructs this object and parses the supplied version string.
     *  
     * @param version
     */
    public DefaultVersionInfo( String version )
        throws VersionParseException
    {
        // TODO: How to handle M (Milestone) or 1.1b (Beta)
        annotationOrder = Arrays.asList( new String[] { "ALPHA", "BETA", "RC" } );

        parseVersion( version );
    }
    
    /** Internal routine for parsing the supplied version string into its parts. 
     * 
     * @param version
     */
    protected void parseVersion( String version )
        throws VersionParseException
    {
        this.strVersion = version;

        Matcher m = STANDARD_PATTERN.matcher( strVersion );
        if ( m.matches() )
        {
            setComponent( m.group( COMPONENT_INDEX ) );
            this.digitSeparator = m.group( DIGIT_SEPARATOR_INDEX );
            setDigits( parseDigits( m.group( DIGITS_INDEX ) ) );
            if ( !SNAPSHOT_IDENTIFIER.equals( m.group( ANNOTATION_INDEX ) ) )
            {
                this.annotationSeparator = m.group( ANNOTATION_SEPARATOR_INDEX );
                setAnnotation( m.group( ANNOTATION_INDEX ) );

                if ( StringUtils.isNotEmpty( m.group( ANNOTATION_REV_SEPARATOR_INDEX ) )
                    && StringUtils.isEmpty( m.group( ANNOTATION_REVISION_INDEX ) ) )
                {
                    // The build separator was picked up as the annotation revision separator
                    this.buildSeparator = m.group( ANNOTATION_REV_SEPARATOR_INDEX );
                    setBuildSpecifier( m.group( BUILD_SPECIFIER_INDEX ) );
                }
                else
                {
                    this.annotationRevSeparator = m.group( ANNOTATION_REV_SEPARATOR_INDEX );
                    setAnnotationRevision( m.group( ANNOTATION_REVISION_INDEX ) );

                    this.buildSeparator = m.group( BUILD_SEPARATOR_INDEX );
                    setBuildSpecifier( m.group( BUILD_SPECIFIER_INDEX ) );
                }
            }
            else
            {
                // Annotation was "SNAPSHOT" so populate the build specifier with that data
                this.buildSeparator = m.group( ANNOTATION_SEPARATOR_INDEX );
                setBuildSpecifier( m.group( ANNOTATION_INDEX ) );
            }
        }
        else
        {
            throw new VersionParseException( "Unable to parse the version string: \"" + version + "\"" );
        }
    }
    
    public boolean isSnapshot()
    {
        return SNAPSHOT_IDENTIFIER.equalsIgnoreCase( this.buildSpecifier );
    }

    public VersionInfo getNextVersion()
    {
        DefaultVersionInfo result;

        try
        {
            result = (DefaultVersionInfo) this.clone();
        }
        catch ( CloneNotSupportedException e )
        {
            return null;
        }

        if ( StringUtils.isNumeric( result.annotationRevision ) )
        {
            result.annotationRevision = incrementVersionString( result.annotationRevision );
        }
        else if ( result.digits != null && !result.digits.isEmpty() )
        {
            try
            {
                List digits = result.digits;
                digits.set( digits.size() - 1, incrementVersionString( (String) digits.get( digits.size() - 1 ) ) );
            }
            catch ( NumberFormatException e )
            {
                return null;
            }
        }
        else
        {
            return null;
        }

        return result;
    }
    
    /** Compares this {@link DefaultVersionInfo} to the supplied {@link DefaultVersionInfo}
     * to determine which version is greater.
     * <p>
     * Decision order is: digits, annotation, annotationRev, buildSpecifier.
     * <p>
     * Presence of an annotation is considered to be less than an equivalent version without an annotation.<br>
     * Example: 1.0 is greater than 1.0-alpha.<br> 
     * <p> 
     * The {@link DefaultVersionInfo#getAnnotationOrder()} is used in determining the rank order of annotations.<br>
     * For example: alpha &lt; beta &lt; RC &lt release 
     * 
     * @param that
     * @return
     * @throws IllegalArgumentException if the components differ between the objects or if either of the annotations can not be determined.
     */
    public int compareTo( Object obj )
    {
        if ( !( obj instanceof DefaultVersionInfo ) )
            throw new ClassCastException( "DefaultVersionInfo object expected" );

        DefaultVersionInfo that = (DefaultVersionInfo) obj;

        if ( !StringUtils.equals( this.component, that.component ) )
        {
            throw new IllegalArgumentException( "Cannot perform comparison on different components: \""
                + this.component + "\" compared to \"" + that.component + "\"" );
        }

        if ( !this.digits.equals( that.digits ) )
        {
            for ( int i = 0; i < this.digits.size(); i++ )
            {
                if ( i >= that.digits.size() )
                {
                    // We've gone past the end of the digit list of that. We are greater
                    return 1;
                }

                if ( !StringUtils.equals( (String) this.digits.get( i ), (String) that.digits.get( i ) ) )
                {
                    return compareToAsIntegers( (String) this.digits.get( i ), (String) that.digits.get( i ) );
                }
            }

            if ( this.digits.size() < that.digits.size() )
            {
                // The lists were equal up to the end of this list. The other has more digits so it is greater.
                return -1;
            }
        }

        if ( !StringUtils.equalsIgnoreCase( this.annotation, that.annotation ) )
        {
            // Having an annotation vs. not is considered to be less than.
            // a 1.0-alpha is less than 1.0
            if ( this.annotation != null && that.annotation == null )
            {
                return -1;
            }
            else if ( this.annotation == null && that.annotation != null )
            {
                return 1;
            }
            else
            {
                int nThis = annotationOrder.indexOf( this.annotation.toUpperCase() );
                int nThat = annotationOrder.indexOf( that.annotation.toUpperCase() );

                if ( nThis == -1 || nThat == -1 )
                {
                    throw new IllegalArgumentException( "Cannot perform comparison on unknown annotation: \""
                        + this.annotation + "\" compared to \"" + that.annotation + "\"" );
                }

                return nThis - nThat;
            }
        }

        if ( !StringUtils.equals( this.annotationRevision, that.annotationRevision ) )
        {
            return compareToAsIntegers( this.annotationRevision, that.annotationRevision );
        }

        if ( !StringUtils.equals( this.buildSpecifier, that.buildSpecifier ) )
        {
            if ( this.buildSpecifier == null && that.buildSpecifier != null )
            {
                return 1;
            }
            else if ( this.buildSpecifier != null && that.buildSpecifier == null )
            {
                return -1;
            }
            else
            {
                // Just do a simple string comparison?
                return this.buildSpecifier.compareTo( that.buildSpecifier );
            }
        }

        return 0;
    }

    private int compareToAsIntegers( String s1, String s2 )
    {
        int n1 = StringUtils.isEmpty( s1 ) ? -1 : Integer.parseInt( s1 );
        int n2 = StringUtils.isEmpty( s2 ) ? -1 : Integer.parseInt( s2 );

        return n1 - n2;
    }
    
    /** Takes a string and increments it as an integer.  
     * Preserves any lpad of "0" zeros.
     * 
     * @param s
     * @return
     */
    protected String incrementVersionString( String s )
    {
        if ( StringUtils.isEmpty( s ) )
        {
            return null;
        }

        try
        {
            Integer n = new Integer( Integer.parseInt( s ) + 1 );
            if ( n.toString().length() < s.length() )
            {
                // String was left-padded with zeros
                return StringUtils.leftPad( n.toString(), s.length(), "0" );
            }
            return n.toString();
        }
        catch ( NumberFormatException e )
        {
            return null;
        }
    }
    
    public String getSnapshotVersionString() 
    {
        return getVersionString(this, SNAPSHOT_IDENTIFIER, StringUtils.defaultString( this.buildSeparator, "-" ) );
    }
    
    public String getReleaseVersionString()
    {
        return getVersionString( this, null, null );
    }

    public String getVersionString()
    {
        return getVersionString( this, this.buildSpecifier, this.buildSeparator );
    }

    protected static String getVersionString( DefaultVersionInfo info, String buildSpecifier, String buildSeparator )
    {
        StringBuffer sb = new StringBuffer();

        if ( StringUtils.isNotEmpty( info.component ) )
        {
            sb.append( info.component );
        }

        if ( info.digits != null )
        {
            sb.append( StringUtils.defaultString( info.digitSeparator ) );
            sb.append( joinDigitString( info.digits ) );
        }

        if ( StringUtils.isNotEmpty( info.annotation ) )
        {
            sb.append( StringUtils.defaultString( info.annotationSeparator ) );
            sb.append( info.annotation );
        }

        if ( StringUtils.isNotEmpty( info.annotationRevision ) )
        {
            if ( StringUtils.isEmpty( info.annotation ) )
            {
                sb.append( StringUtils.defaultString( info.annotationSeparator ) );
            }
            else
            {
                sb.append( StringUtils.defaultString( info.annotationRevSeparator ) );
            }
            sb.append( info.annotationRevision );
        }

        if ( StringUtils.isNotEmpty( buildSpecifier ) )
        {
            sb.append( StringUtils.defaultString( buildSeparator ) );
            sb.append( buildSpecifier );
        }

        return sb.toString();
    }
    
    /** Simply joins the items in the list with "." period
     * 
     * @param digits
     * @return
     */
    protected static String joinDigitString( List digits )
    {
        if ( digits == null )
        {
            return null;
        }

        return StringUtils.join( digits.iterator(), DIGIT_SEPARATOR_STRING );
    }

    /** Splits the string on "." and returns a list 
     * containing each digit.
     * 
     * @param strDigits
     * @return
     */
    protected List parseDigits( String strDigits )
    {
        if ( StringUtils.isEmpty( strDigits ) )
        {
            return null;
        }

        String[] strings = StringUtils.split( strDigits, DIGIT_SEPARATOR_STRING );
        return Arrays.asList( strings );
    }

    //--------------------------------------------------
    // Getters & Setters
    //--------------------------------------------------

    private String nullIfEmpty( String s )
    {
        return ( StringUtils.isEmpty( s ) ) ? null : s;
    }

    public String getAnnotation()
    {
        return annotation;
    }

    protected void setAnnotation( String annotation )
    {
        this.annotation = nullIfEmpty( annotation );
    }

    public String getAnnotationRevision()
    {
        return annotationRevision;
    }

    protected void setAnnotationRevision( String annotationRevision )
    {
        this.annotationRevision = nullIfEmpty( annotationRevision );
    }

    public String getComponent()
    {
        return component;
    }

    protected void setComponent( String component )
    {
        this.component = nullIfEmpty( component );
    }

    public List getDigits()
    {
        return digits;
    }

    protected void setDigits( List digits )
    {
        this.digits = digits;
    }

    public String getBuildSpecifier()
    {
        return buildSpecifier;
    }

    protected void setBuildSpecifier( String buildSpecifier )
    {
        this.buildSpecifier = nullIfEmpty( buildSpecifier );
    }

    public List getAnnotationOrder()
    {
        return annotationOrder;
    }

    protected void setAnnotationOrder( List annotationOrder )
    {
        this.annotationOrder = annotationOrder;
    }

}
