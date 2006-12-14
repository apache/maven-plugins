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
 *         Banner logo on the masthead of the site.
 *       
 * 
 * @version $Revision$ $Date$
 */
public class Banner implements java.io.Serializable {


      //--------------------------/
     //- Class/Member Variables -/
    //--------------------------/

    /**
     * Field name
     */
    private String name;

    /**
     * Field src
     */
    private String src;

    /**
     * Field alt
     */
    private String alt;

    /**
     * Field href
     */
    private String href;


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
        
        if ( !(other instanceof Banner) )
        {
            return false;
        }
        
        Banner that = (Banner) other;
        boolean result = true;
        result = result && ( getName() == null ? that.getName() == null : getName().equals( that.getName() ) );
        result = result && ( getSrc() == null ? that.getSrc() == null : getSrc().equals( that.getSrc() ) );
        result = result && ( getAlt() == null ? that.getAlt() == null : getAlt().equals( that.getAlt() ) );
        result = result && ( getHref() == null ? that.getHref() == null : getHref().equals( that.getHref() ) );
        return result;
    } //-- boolean equals(Object) 

    /**
     * Get 
     *             The alt description for the banner image.
     *           
     */
    public String getAlt()
    {
        return this.alt;
    } //-- String getAlt() 

    /**
     * Get 
     *             The href of a link to be used for the banner
     * image.
     *           
     */
    public String getHref()
    {
        return this.href;
    } //-- String getHref() 

    /**
     * Get 
     *             The name of the banner.
     *           
     */
    public String getName()
    {
        return this.name;
    } //-- String getName() 

    /**
     * Get 
     *             The location of an image for the banner.
     *           
     */
    public String getSrc()
    {
        return this.src;
    } //-- String getSrc() 

    /**
     * Method hashCode
     */
    public int hashCode()
    {
        int result = 17;
        long tmp;
        result = 37 * result + ( name != null ? name.hashCode() : 0 );
        result = 37 * result + ( src != null ? src.hashCode() : 0 );
        result = 37 * result + ( alt != null ? alt.hashCode() : 0 );
        result = 37 * result + ( href != null ? href.hashCode() : 0 );
        return result;
    } //-- int hashCode() 

    /**
     * Set 
     *             The alt description for the banner image.
     *           
     * 
     * @param alt
     */
    public void setAlt(String alt)
    {
        this.alt = alt;
    } //-- void setAlt(String) 

    /**
     * Set 
     *             The href of a link to be used for the banner
     * image.
     *           
     * 
     * @param href
     */
    public void setHref(String href)
    {
        this.href = href;
    } //-- void setHref(String) 

    /**
     * Set 
     *             The name of the banner.
     *           
     * 
     * @param name
     */
    public void setName(String name)
    {
        this.name = name;
    } //-- void setName(String) 

    /**
     * Set 
     *             The location of an image for the banner.
     *           
     * 
     * @param src
     */
    public void setSrc(String src)
    {
        this.src = src;
    } //-- void setSrc(String) 

    /**
     * Method toString
     */
    public java.lang.String toString()
    {
        StringBuffer buf = new StringBuffer();
        buf.append( "name = '" );
        buf.append( getName() + "'" );
        buf.append( "\n" ); 
        buf.append( "src = '" );
        buf.append( getSrc() + "'" );
        buf.append( "\n" ); 
        buf.append( "alt = '" );
        buf.append( getAlt() + "'" );
        buf.append( "\n" ); 
        buf.append( "href = '" );
        buf.append( getHref() + "'" );
        return buf.toString();
    } //-- java.lang.String toString() 


    private String modelEncoding = "UTF-8";

    public void setModelEncoding( String modelEncoding )
    {
        this.modelEncoding = modelEncoding;
    }

    public String getModelEncoding()
    {
        return modelEncoding;
    }}
