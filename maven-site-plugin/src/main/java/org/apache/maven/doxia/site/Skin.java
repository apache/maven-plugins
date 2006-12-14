/*
 * $Id$
 */

package org.apache.maven.doxia.site.decoration;

  //---------------------------------/
 //- Imported classes and packages -/
//---------------------------------/

import java.util.Date;

/**
 * 
 *         An skin artifact declaratio.
 *       
 * 
 * @version $Revision$ $Date$
 */
public class Skin implements java.io.Serializable {


      //--------------------------/
     //- Class/Member Variables -/
    //--------------------------/

    /**
     * Field groupId
     */
    private String groupId;

    /**
     * Field artifactId
     */
    private String artifactId;

    /**
     * Field version
     */
    private String version;


      //-----------/
     //- Methods -/
    //-----------/

    /**
     * Method equals
     * 
     * @param other
     */
    public boolean equals(Object other)
    {
        if ( this == other)
        {
            return true;
        }
        
        if ( !(other instanceof Skin) )
        {
            return false;
        }
        
        Skin that = (Skin) other;
        boolean result = true;
        result = result && ( getGroupId() == null ? that.getGroupId() == null : getGroupId().equals( that.getGroupId() ) );
        result = result && ( getArtifactId() == null ? that.getArtifactId() == null : getArtifactId().equals( that.getArtifactId() ) );
        result = result && ( getVersion() == null ? that.getVersion() == null : getVersion().equals( that.getVersion() ) );
        return result;
    } //-- boolean equals(Object) 

    /**
     * Get 
     *             The skin artifact ID.
     *           
     */
    public String getArtifactId()
    {
        return this.artifactId;
    } //-- String getArtifactId() 

    /**
     * Get 
     *             The skin group ID.
     *           
     */
    public String getGroupId()
    {
        return this.groupId;
    } //-- String getGroupId() 

    /**
     * Get 
     *             The skin version.
     *           
     */
    public String getVersion()
    {
        return this.version;
    } //-- String getVersion() 

    /**
     * Method hashCode
     */
    public int hashCode()
    {
        int result = 17;
        long tmp;
        result = 37 * result + ( groupId != null ? groupId.hashCode() : 0 );
        result = 37 * result + ( artifactId != null ? artifactId.hashCode() : 0 );
        result = 37 * result + ( version != null ? version.hashCode() : 0 );
        return result;
    } //-- int hashCode() 

    /**
     * Set 
     *             The skin artifact ID.
     *           
     * 
     * @param artifactId
     */
    public void setArtifactId(String artifactId)
    {
        this.artifactId = artifactId;
    } //-- void setArtifactId(String) 

    /**
     * Set 
     *             The skin group ID.
     *           
     * 
     * @param groupId
     */
    public void setGroupId(String groupId)
    {
        this.groupId = groupId;
    } //-- void setGroupId(String) 

    /**
     * Set 
     *             The skin version.
     *           
     * 
     * @param version
     */
    public void setVersion(String version)
    {
        this.version = version;
    } //-- void setVersion(String) 

    /**
     * Method toString
     */
    public java.lang.String toString()
    {
        StringBuffer buf = new StringBuffer();
        buf.append( "groupId = '" );
        buf.append( getGroupId() + "'" );
        buf.append( "\n" ); 
        buf.append( "artifactId = '" );
        buf.append( getArtifactId() + "'" );
        buf.append( "\n" ); 
        buf.append( "version = '" );
        buf.append( getVersion() + "'" );
        return buf.toString();
    } //-- java.lang.String toString() 


            
    public static Skin getDefaultSkin()
    {
        Skin skin = new Skin();
        skin.setGroupId( "org.apache.maven.skins" );
        skin.setArtifactId( "maven-default-skin" );
        return skin;
    }
            
          
    private String modelEncoding = "UTF-8";

    public void setModelEncoding( String modelEncoding )
    {
        this.modelEncoding = modelEncoding;
    }

    public String getModelEncoding()
    {
        return modelEncoding;
    }}
