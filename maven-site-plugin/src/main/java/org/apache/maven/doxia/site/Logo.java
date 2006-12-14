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
 *         Power by logo on the navigation.
 *       
 * 
 * @version $Revision$ $Date$
 */
public class Logo extends LinkItem 
implements java.io.Serializable
{


      //--------------------------/
     //- Class/Member Variables -/
    //--------------------------/

    /**
     * Field img
     */
    private String img;


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
        
        if ( !(other instanceof Logo) )
        {
            return false;
        }
        
        Logo that = (Logo) other;
        boolean result = true;
        result = result && ( getImg() == null ? that.getImg() == null : getImg().equals( that.getImg() ) );
        return result;
    } //-- boolean equals(Object) 

    /**
     * Get 
     *             The href of a link to be used for the power by
     * image.
     *           
     */
    public String getImg()
    {
        return this.img;
    } //-- String getImg() 

    /**
     * Method hashCode
     */
    public int hashCode()
    {
        int result = 17;
        long tmp;
        result = 37 * result + ( img != null ? img.hashCode() : 0 );
        return result;
    } //-- int hashCode() 

    /**
     * Set 
     *             The href of a link to be used for the power by
     * image.
     *           
     * 
     * @param img
     */
    public void setImg(String img)
    {
        this.img = img;
    } //-- void setImg(String) 

    /**
     * Method toString
     */
    public java.lang.String toString()
    {
        StringBuffer buf = new StringBuffer();
        buf.append( "img = '" );
        buf.append( getImg() + "'" );
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
