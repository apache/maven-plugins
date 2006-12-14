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
 *         Modify display properties for date published.
 *       
 * 
 * @version $Revision$ $Date$
 */
public class PublishDate implements java.io.Serializable {


      //--------------------------/
     //- Class/Member Variables -/
    //--------------------------/

    /**
     * Field position
     */
    private String position;

    /**
     * Field format
     */
    private String format;


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
        
        if ( !(other instanceof PublishDate) )
        {
            return false;
        }
        
        PublishDate that = (PublishDate) other;
        boolean result = true;
        result = result && ( getPosition() == null ? that.getPosition() == null : getPosition().equals( that.getPosition() ) );
        result = result && ( getFormat() == null ? that.getFormat() == null : getFormat().equals( that.getFormat() ) );
        return result;
    } //-- boolean equals(Object) 

    /**
     * Get 
     *             Date format to use. The default is MM/dd/yyyy.
     *           
     */
    public String getFormat()
    {
        return this.format;
    } //-- String getFormat() 

    /**
     * Get 
     *             Where to place the date published (left, right,
     * navigation-top, navigation-bottom, bottom).
     *           
     */
    public String getPosition()
    {
        return this.position;
    } //-- String getPosition() 

    /**
     * Method hashCode
     */
    public int hashCode()
    {
        int result = 17;
        long tmp;
        result = 37 * result + ( position != null ? position.hashCode() : 0 );
        result = 37 * result + ( format != null ? format.hashCode() : 0 );
        return result;
    } //-- int hashCode() 

    /**
     * Set 
     *             Date format to use. The default is MM/dd/yyyy.
     *           
     * 
     * @param format
     */
    public void setFormat(String format)
    {
        this.format = format;
    } //-- void setFormat(String) 

    /**
     * Set 
     *             Where to place the date published (left, right,
     * navigation-top, navigation-bottom, bottom).
     *           
     * 
     * @param position
     */
    public void setPosition(String position)
    {
        this.position = position;
    } //-- void setPosition(String) 

    /**
     * Method toString
     */
    public java.lang.String toString()
    {
        StringBuffer buf = new StringBuffer();
        buf.append( "position = '" );
        buf.append( getPosition() + "'" );
        buf.append( "\n" ); 
        buf.append( "format = '" );
        buf.append( getFormat() + "'" );
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
