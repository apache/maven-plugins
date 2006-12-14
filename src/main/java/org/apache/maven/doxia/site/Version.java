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
 *         Modify display properties for version published.
 *       
 * 
 * @version $Revision$ $Date$
 */
public class Version implements java.io.Serializable {


      //--------------------------/
     //- Class/Member Variables -/
    //--------------------------/

    /**
     * Field position
     */
    private String position;


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
        
        if ( !(other instanceof Version) )
        {
            return false;
        }
        
        Version that = (Version) other;
        boolean result = true;
        result = result && ( getPosition() == null ? that.getPosition() == null : getPosition().equals( that.getPosition() ) );
        return result;
    } //-- boolean equals(Object) 

    /**
     * Get 
     *             Where to place the version published (left,
     * right, navigation-top, navigation-bottom, bottom).
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
        return result;
    } //-- int hashCode() 

    /**
     * Set 
     *             Where to place the version published (left,
     * right, navigation-top, navigation-bottom, bottom).
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
