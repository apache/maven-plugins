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
 *         The main content decoration.
 *       
 * 
 * @version $Revision$ $Date$
 */
public class Body implements java.io.Serializable {


      //--------------------------/
     //- Class/Member Variables -/
    //--------------------------/

    /**
     * Field head
     */
    private Object head;

    /**
     * Field links
     */
    private java.util.List links;

    /**
     * Field breadcrumbs
     */
    private java.util.List breadcrumbs;

    /**
     * Field menus
     */
    private java.util.List menus;


      //-----------/
     //- Methods -/
    //-----------/

    /**
     * Method addBreadcrumb
     * 
     * @param linkItem
     */
    public void addBreadcrumb(LinkItem linkItem)
    {
        getBreadcrumbs().add( linkItem );
    } //-- void addBreadcrumb(LinkItem) 

    /**
     * Method addLink
     * 
     * @param linkItem
     */
    public void addLink(LinkItem linkItem)
    {
        getLinks().add( linkItem );
    } //-- void addLink(LinkItem) 

    /**
     * Method addMenu
     * 
     * @param menu
     */
    public void addMenu(Menu menu)
    {
        getMenus().add( menu );
    } //-- void addMenu(Menu) 

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
        
        if ( !(other instanceof Body) )
        {
            return false;
        }
        
        Body that = (Body) other;
        boolean result = true;
        result = result && ( getHead() == null ? that.getHead() == null : getHead().equals( that.getHead() ) );
        result = result && ( getLinks() == null ? that.getLinks() == null : getLinks().equals( that.getLinks() ) );
        result = result && ( getBreadcrumbs() == null ? that.getBreadcrumbs() == null : getBreadcrumbs().equals( that.getBreadcrumbs() ) );
        result = result && ( getMenus() == null ? that.getMenus() == null : getMenus().equals( that.getMenus() ) );
        return result;
    } //-- boolean equals(Object) 

    /**
     * Method getBreadcrumbs
     */
    public java.util.List getBreadcrumbs()
    {
        if ( this.breadcrumbs == null )
        {
            this.breadcrumbs = new java.util.ArrayList();
        }
        
        return this.breadcrumbs;
    } //-- java.util.List getBreadcrumbs() 

    /**
     * Get 
     *             Additional content (like Javascript) to include
     * in the HEAD block of the generated pages.
     *           
     */
    public Object getHead()
    {
        return this.head;
    } //-- Object getHead() 

    /**
     * Method getLinks
     */
    public java.util.List getLinks()
    {
        if ( this.links == null )
        {
            this.links = new java.util.ArrayList();
        }
        
        return this.links;
    } //-- java.util.List getLinks() 

    /**
     * Method getMenus
     */
    public java.util.List getMenus()
    {
        if ( this.menus == null )
        {
            this.menus = new java.util.ArrayList();
        }
        
        return this.menus;
    } //-- java.util.List getMenus() 

    /**
     * Method hashCode
     */
    public int hashCode()
    {
        int result = 17;
        long tmp;
        result = 37 * result + ( head != null ? head.hashCode() : 0 );
        result = 37 * result + ( links != null ? links.hashCode() : 0 );
        result = 37 * result + ( breadcrumbs != null ? breadcrumbs.hashCode() : 0 );
        result = 37 * result + ( menus != null ? menus.hashCode() : 0 );
        return result;
    } //-- int hashCode() 

    /**
     * Method removeBreadcrumb
     * 
     * @param linkItem
     */
    public void removeBreadcrumb(LinkItem linkItem)
    {
        getBreadcrumbs().remove( linkItem );
    } //-- void removeBreadcrumb(LinkItem) 

    /**
     * Method removeLink
     * 
     * @param linkItem
     */
    public void removeLink(LinkItem linkItem)
    {
        getLinks().remove( linkItem );
    } //-- void removeLink(LinkItem) 

    /**
     * Method removeMenu
     * 
     * @param menu
     */
    public void removeMenu(Menu menu)
    {
        getMenus().remove( menu );
    } //-- void removeMenu(Menu) 

    /**
     * Set 
     *             A list of breadcrumbs to display in the
     * navigation.
     *           
     * 
     * @param breadcrumbs
     */
    public void setBreadcrumbs(java.util.List breadcrumbs)
    {
        this.breadcrumbs = breadcrumbs;
    } //-- void setBreadcrumbs(java.util.List) 

    /**
     * Set 
     *             Additional content (like Javascript) to include
     * in the HEAD block of the generated pages.
     *           
     * 
     * @param head
     */
    public void setHead(Object head)
    {
        this.head = head;
    } //-- void setHead(Object) 

    /**
     * Set 
     *             A list of links to display in the navigation.
     *           
     * 
     * @param links
     */
    public void setLinks(java.util.List links)
    {
        this.links = links;
    } //-- void setLinks(java.util.List) 

    /**
     * Set 
     *             A list of menus to include in the navigation.
     *           
     * 
     * @param menus
     */
    public void setMenus(java.util.List menus)
    {
        this.menus = menus;
    } //-- void setMenus(java.util.List) 

    /**
     * Method toString
     */
    public java.lang.String toString()
    {
        StringBuffer buf = new StringBuffer();
        buf.append( "head = '" );
        buf.append( getHead() + "'" );
        buf.append( "\n" ); 
        buf.append( "links = '" );
        buf.append( getLinks() + "'" );
        buf.append( "\n" ); 
        buf.append( "breadcrumbs = '" );
        buf.append( getBreadcrumbs() + "'" );
        buf.append( "\n" ); 
        buf.append( "menus = '" );
        buf.append( getMenus() + "'" );
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
