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
 *         A link in the navigation.
 *       
 * 
 * @version $Revision$ $Date$
 */
public class LinkItem implements java.io.Serializable {


      //--------------------------/
     //- Class/Member Variables -/
    //--------------------------/

    /**
     * Field name
     */
    private String name;

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
        
        if ( !(other instanceof LinkItem) )
        {
            return false;
        }
        
        LinkItem that = (LinkItem) other;
        boolean result = true;
        result = result && ( getName() == null ? that.getName() == null : getName().equals( that.getName() ) );
        result = result && ( getHref() == null ? that.getHref() == null : getHref().equals( that.getHref() ) );
        return result;
    } //-- boolean equals(Object) 

    /**
     * Get 
     *             The href to use for the link.
     *           
     */
    public String getHref()
    {
        return this.href;
    } //-- String getHref() 

    /**
     * Get 
     *             The name to display for the link.
     *           
     */
    public String getName()
    {
        return this.name;
    } //-- String getName() 

    /**
     * Method hashCode
     */
    public int hashCode()
    {
        int result = 17;
        long tmp;
        result = 37 * result + ( name != null ? name.hashCode() : 0 );
        result = 37 * result + ( href != null ? href.hashCode() : 0 );
        return result;
    } //-- int hashCode() 

    /**
     * Set 
     *             The href to use for the link.
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
     *             The name to display for the link.
     *           
     * 
     * @param name
     */
    public void setName(String name)
    {
        this.name = name;
    } //-- void setName(String) 

    /**
     * Method toString
     */
    public java.lang.String toString()
    {
        StringBuffer buf = new StringBuffer();
        buf.append( "name = '" );
        buf.append( getName() + "'" );
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
