/*
 * $Id$
 */

package org.apache.maven.doxia.site.decoration;

  //---------------------------------/
 //- Imported classes and packages -/
//---------------------------------/

import java.util.Collection;
import java.util.Date;

/**
 * 
 *         A menu item.
 *       
 * 
 * @version $Revision$ $Date$
 */
public class MenuItem extends LinkItem 
implements java.io.Serializable
{


      //--------------------------/
     //- Class/Member Variables -/
    //--------------------------/

    /**
     * Field description
     */
    private String description;

    /**
     * Field collapse
     */
    private boolean collapse = false;

    /**
     * Field ref
     */
    private String ref;

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
        menuItem.createMenuItemAssociation( this );
    } //-- void addItem(MenuItem) 

    /**
     * Method breakMenuItemAssociation
     * 
     * @param menuItem
     */
    public void breakMenuItemAssociation(MenuItem menuItem)
    {
        if ( ! getItems().contains( menuItem ) )
        {
            throw new IllegalStateException( "menuItem isn't associated." );
        }
        
        getItems().remove( menuItem );
    } //-- void breakMenuItemAssociation(MenuItem) 

    /**
     * Method createMenuItemAssociation
     * 
     * @param menuItem
     */
    public void createMenuItemAssociation(MenuItem menuItem)
    {
        Collection items = getItems();
        
        if ( getItems().contains(menuItem) )
        {
            throw new IllegalStateException( "menuItem is already assigned." );
        }
        
        items.add( menuItem );
    } //-- void createMenuItemAssociation(MenuItem) 

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
        
        if ( !(other instanceof MenuItem) )
        {
            return false;
        }
        
        MenuItem that = (MenuItem) other;
        boolean result = true;
        result = result && ( getDescription() == null ? that.getDescription() == null : getDescription().equals( that.getDescription() ) );
        result = result && collapse== that.collapse;
        result = result && ( getRef() == null ? that.getRef() == null : getRef().equals( that.getRef() ) );
        result = result && ( getItems() == null ? that.getItems() == null : getItems().equals( that.getItems() ) );
        return result;
    } //-- boolean equals(Object) 

    /**
     * Get 
     *            A description of the menu item. This is used on
     * any summary pages for a menu.
     *           
     */
    public String getDescription()
    {
        return this.description;
    } //-- String getDescription() 

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
     *             A reference to a pre-defined menu item, such as
     * a report (specified by the report goal
     *             name). Any elements explicitly given override
     * those from the pre-defined reference.
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
        result = 37 * result + ( description != null ? description.hashCode() : 0 );
        result = 37 * result + ( collapse ? 0 : 1 );
        result = 37 * result + ( ref != null ? ref.hashCode() : 0 );
        result = 37 * result + ( items != null ? items.hashCode() : 0 );
        return result;
    } //-- int hashCode() 

    /**
     * Get 
     *            Whether to collapse children elements of an item
     * menu (by default).
     *           
     */
    public boolean isCollapse()
    {
        return this.collapse;
    } //-- boolean isCollapse() 

    /**
     * Method removeItem
     * 
     * @param menuItem
     */
    public void removeItem(MenuItem menuItem)
    {
        menuItem.breakMenuItemAssociation( this );
        getItems().remove( menuItem );
    } //-- void removeItem(MenuItem) 

    /**
     * Set 
     *            Whether to collapse children elements of an item
     * menu (by default).
     *           
     * 
     * @param collapse
     */
    public void setCollapse(boolean collapse)
    {
        this.collapse = collapse;
    } //-- void setCollapse(boolean) 

    /**
     * Set 
     *            A description of the menu item. This is used on
     * any summary pages for a menu.
     *           
     * 
     * @param description
     */
    public void setDescription(String description)
    {
        this.description = description;
    } //-- void setDescription(String) 

    /**
     * Set Menu item.
     * 
     * @param items
     */
    public void setItems(java.util.List items)
    {
        this.items = items;
    } //-- void setItems(java.util.List) 

    /**
     * Set 
     *             A reference to a pre-defined menu item, such as
     * a report (specified by the report goal
     *             name). Any elements explicitly given override
     * those from the pre-defined reference.
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
        buf.append( "description = '" );
        buf.append( getDescription() + "'" );
        buf.append( "\n" ); 
        buf.append( "collapse = '" );
        buf.append( isCollapse() + "'" );
        buf.append( "\n" ); 
        buf.append( "ref = '" );
        buf.append( getRef() + "'" );
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
