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
 *         A menu in the navigation.
 *       
 * 
 * @version $Revision$ $Date$
 */
public class Menu implements java.io.Serializable {


      //--------------------------/
     //- Class/Member Variables -/
    //--------------------------/

    /**
     * Field name
     */
    private String name;

    /**
     * Field inherit
     */
    private String inherit;

    /**
     * Field inheritAsRef
     */
    private boolean inheritAsRef = false;

    /**
     * Field ref
     */
    private String ref;

    /**
     * Field img
     */
    private String img;

    /**
     * Field items
     */
    private java.util.List items;


      //-----------/
     //- Methods -/
    //-----------/

    /**
     * Method addItem
     * 
     * @param menuItem
     */
    public void addItem(MenuItem menuItem)
    {
        getItems().add( menuItem );
    } //-- void addItem(MenuItem) 

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
        
        if ( !(other instanceof Menu) )
        {
            return false;
        }
        
        Menu that = (Menu) other;
        boolean result = true;
        result = result && ( getName() == null ? that.getName() == null : getName().equals( that.getName() ) );
        result = result && ( getInherit() == null ? that.getInherit() == null : getInherit().equals( that.getInherit() ) );
        result = result && ( getRef() == null ? that.getRef() == null : getRef().equals( that.getRef() ) );
        result = result && ( getImg() == null ? that.getImg() == null : getImg().equals( that.getImg() ) );
        result = result && ( getItems() == null ? that.getItems() == null : getItems().equals( that.getItems() ) );
        return result;
    } //-- boolean equals(Object) 

    /**
     * Get 
     *             The location of an image.
     *           
     */
    public String getImg()
    {
        return this.img;
    } //-- String getImg() 

    /**
     * Get 
     *             The way in which the menu is inherited.
     *           
     */
    public String getInherit()
    {
        return this.inherit;
    } //-- String getInherit() 

    /**
     * Method getItems
     */
    public java.util.List getItems()
    {
        if ( this.items == null )
        {
            this.items = new java.util.ArrayList();
        }
        
        return this.items;
    } //-- java.util.List getItems() 

    /**
     * Get 
     *             The name to display for the menu.
     *           
     */
    public String getName()
    {
        return this.name;
    } //-- String getName() 

    /**
     * Get 
     *             A reference to a pre-defined menu, such as a
     * <code>reports</code>, <code>modules</code>
     *             or <code>parentProject</code>.
     *           
     */
    public String getRef()
    {
        return this.ref;
    } //-- String getRef() 

    /**
     * Method hashCode
     */
    public int hashCode()
    {
        int result = 17;
        long tmp;
        result = 37 * result + ( name != null ? name.hashCode() : 0 );
        result = 37 * result + ( inherit != null ? inherit.hashCode() : 0 );
        result = 37 * result + ( ref != null ? ref.hashCode() : 0 );
        result = 37 * result + ( img != null ? img.hashCode() : 0 );
        result = 37 * result + ( items != null ? items.hashCode() : 0 );
        return result;
    } //-- int hashCode() 

    /**
     * Get 
     *             If this is a reference, setting
     * <inheritAsRef>true</inheritAsRef> means that it will be
     * populated
     *             in the project, whereas if it is false, it is
     * populated in the parent and then inherited.
     *           
     */
    public boolean isInheritAsRef()
    {
        return this.inheritAsRef;
    } //-- boolean isInheritAsRef() 

    /**
     * Method removeItem
     * 
     * @param menuItem
     */
    public void removeItem(MenuItem menuItem)
    {
        getItems().remove( menuItem );
    } //-- void removeItem(MenuItem) 

    /**
     * Set 
     *             The location of an image.
     *           
     * 
     * @param img
     */
    public void setImg(String img)
    {
        this.img = img;
    } //-- void setImg(String) 

    /**
     * Set 
     *             The way in which the menu is inherited.
     *           
     * 
     * @param inherit
     */
    public void setInherit(String inherit)
    {
        this.inherit = inherit;
    } //-- void setInherit(String) 

    /**
     * Set 
     *             If this is a reference, setting
     * <inheritAsRef>true</inheritAsRef> means that it will be
     * populated
     *             in the project, whereas if it is false, it is
     * populated in the parent and then inherited.
     *           
     * 
     * @param inheritAsRef
     */
    public void setInheritAsRef(boolean inheritAsRef)
    {
        this.inheritAsRef = inheritAsRef;
    } //-- void setInheritAsRef(boolean) 

    /**
     * Set 
     *             A list of menu item.
     *           
     * 
     * @param items
     */
    public void setItems(java.util.List items)
    {
        this.items = items;
    } //-- void setItems(java.util.List) 

    /**
     * Set 
     *             The name to display for the menu.
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
     *             A reference to a pre-defined menu, such as a
     * <code>reports</code>, <code>modules</code>
     *             or <code>parentProject</code>.
     *           
     * 
     * @param ref
     */
    public void setRef(String ref)
    {
        this.ref = ref;
    } //-- void setRef(String) 

    /**
     * Method toString
     */
    public java.lang.String toString()
    {
        StringBuffer buf = new StringBuffer();
        buf.append( "name = '" );
        buf.append( getName() + "'" );
        buf.append( "\n" ); 
        buf.append( "inherit = '" );
        buf.append( getInherit() + "'" );
        buf.append( "\n" ); 
        buf.append( "ref = '" );
        buf.append( getRef() + "'" );
        buf.append( "\n" ); 
        buf.append( "img = '" );
        buf.append( getImg() + "'" );
        buf.append( "\n" ); 
        buf.append( "items = '" );
        buf.append( getItems() + "'" );
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
