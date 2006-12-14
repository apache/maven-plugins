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
 *          The <code>&lt;project&gt;</code> element is the root of
 * the site decoration descriptor.
 *          The following table lists all of the possible child
 * elements.
 *       
 * 
 * @version $Revision$ $Date$
 */
public class DecorationModel implements java.io.Serializable {


      //--------------------------/
     //- Class/Member Variables -/
    //--------------------------/

    /**
     * Field name
     */
    private String name;

    /**
     * Field bannerLeft
     */
    private Banner bannerLeft;

    /**
     * Field bannerRight
     */
    private Banner bannerRight;

    /**
     * Field publishDate
     */
    private PublishDate publishDate;

    /**
     * Field version
     */
    private Version version;

    /**
     * Field poweredBy
     */
    private java.util.List poweredBy;

    /**
     * Field skin
     */
    private Skin skin;

    /**
     * Field body
     */
    private Body body;

    /**
     * Field custom
     */
    private Object custom;


      //-----------/
     //- Methods -/
    //-----------/

    /**
     * Method addPoweredBy
     * 
     * @param logo
     */
    public void addPoweredBy(Logo logo)
    {
        getPoweredBy().add( logo );
    } //-- void addPoweredBy(Logo) 

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
        
        if ( !(other instanceof DecorationModel) )
        {
            return false;
        }
        
        DecorationModel that = (DecorationModel) other;
        boolean result = true;
        result = result && ( getName() == null ? that.getName() == null : getName().equals( that.getName() ) );
        result = result && ( getBannerLeft() == null ? that.getBannerLeft() == null : getBannerLeft().equals( that.getBannerLeft() ) );
        result = result && ( getBannerRight() == null ? that.getBannerRight() == null : getBannerRight().equals( that.getBannerRight() ) );
        result = result && ( getPublishDate() == null ? that.getPublishDate() == null : getPublishDate().equals( that.getPublishDate() ) );
        result = result && ( getVersion() == null ? that.getVersion() == null : getVersion().equals( that.getVersion() ) );
        result = result && ( getPoweredBy() == null ? that.getPoweredBy() == null : getPoweredBy().equals( that.getPoweredBy() ) );
        result = result && ( getSkin() == null ? that.getSkin() == null : getSkin().equals( that.getSkin() ) );
        result = result && ( getBody() == null ? that.getBody() == null : getBody().equals( that.getBody() ) );
        result = result && ( getCustom() == null ? that.getCustom() == null : getCustom().equals( that.getCustom() ) );
        return result;
    } //-- boolean equals(Object) 

    /**
     * Get 
     *             Banner logo on the masthead of the site to the
     * left.
     *           
     */
    public Banner getBannerLeft()
    {
        return this.bannerLeft;
    } //-- Banner getBannerLeft() 

    /**
     * Get 
     *             Banner logo on the masthead of the site to the
     * right.
     *           
     */
    public Banner getBannerRight()
    {
        return this.bannerRight;
    } //-- Banner getBannerRight() 

    /**
     * Get 
     *             The main site content decoration.
     *           
     */
    public Body getBody()
    {
        return this.body;
    } //-- Body getBody() 

    /**
     * Get 
     *             Custom configuration for use with customised
     * Velocity templates.
     *           
     */
    public Object getCustom()
    {
        return this.custom;
    } //-- Object getCustom() 

    /**
     * Get 
     *             The full name of the project.
     *           
     */
    public String getName()
    {
        return this.name;
    } //-- String getName() 

    /**
     * Method getPoweredBy
     */
    public java.util.List getPoweredBy()
    {
        if ( this.poweredBy == null )
        {
            this.poweredBy = new java.util.ArrayList();
        }
        
        return this.poweredBy;
    } //-- java.util.List getPoweredBy() 

    /**
     * Get 
     *             Modify the date published display properties.
     *           
     */
    public PublishDate getPublishDate()
    {
        return this.publishDate;
    } //-- PublishDate getPublishDate() 

    /**
     * Get 
     *             The artifact containing the skin for the site.
     *           
     */
    public Skin getSkin()
    {
        return this.skin;
    } //-- Skin getSkin() 

    /**
     * Get 
     *             Modify the version published display properties.
     *           
     */
    public Version getVersion()
    {
        return this.version;
    } //-- Version getVersion() 

    /**
     * Method hashCode
     */
    public int hashCode()
    {
        int result = 17;
        long tmp;
        result = 37 * result + ( name != null ? name.hashCode() : 0 );
        result = 37 * result + ( bannerLeft != null ? bannerLeft.hashCode() : 0 );
        result = 37 * result + ( bannerRight != null ? bannerRight.hashCode() : 0 );
        result = 37 * result + ( publishDate != null ? publishDate.hashCode() : 0 );
        result = 37 * result + ( version != null ? version.hashCode() : 0 );
        result = 37 * result + ( poweredBy != null ? poweredBy.hashCode() : 0 );
        result = 37 * result + ( skin != null ? skin.hashCode() : 0 );
        result = 37 * result + ( body != null ? body.hashCode() : 0 );
        result = 37 * result + ( custom != null ? custom.hashCode() : 0 );
        return result;
    } //-- int hashCode() 

    /**
     * Method removePoweredBy
     * 
     * @param logo
     */
    public void removePoweredBy(Logo logo)
    {
        getPoweredBy().remove( logo );
    } //-- void removePoweredBy(Logo) 

    /**
     * Set 
     *             Banner logo on the masthead of the site to the
     * left.
     *           
     * 
     * @param bannerLeft
     */
    public void setBannerLeft(Banner bannerLeft)
    {
        this.bannerLeft = bannerLeft;
    } //-- void setBannerLeft(Banner) 

    /**
     * Set 
     *             Banner logo on the masthead of the site to the
     * right.
     *           
     * 
     * @param bannerRight
     */
    public void setBannerRight(Banner bannerRight)
    {
        this.bannerRight = bannerRight;
    } //-- void setBannerRight(Banner) 

    /**
     * Set 
     *             The main site content decoration.
     *           
     * 
     * @param body
     */
    public void setBody(Body body)
    {
        this.body = body;
    } //-- void setBody(Body) 

    /**
     * Set 
     *             Custom configuration for use with customised
     * Velocity templates.
     *           
     * 
     * @param custom
     */
    public void setCustom(Object custom)
    {
        this.custom = custom;
    } //-- void setCustom(Object) 

    /**
     * Set 
     *             The full name of the project.
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
     *             Powered by logos list.
     *           
     * 
     * @param poweredBy
     */
    public void setPoweredBy(java.util.List poweredBy)
    {
        this.poweredBy = poweredBy;
    } //-- void setPoweredBy(java.util.List) 

    /**
     * Set 
     *             Modify the date published display properties.
     *           
     * 
     * @param publishDate
     */
    public void setPublishDate(PublishDate publishDate)
    {
        this.publishDate = publishDate;
    } //-- void setPublishDate(PublishDate) 

    /**
     * Set 
     *             The artifact containing the skin for the site.
     *           
     * 
     * @param skin
     */
    public void setSkin(Skin skin)
    {
        this.skin = skin;
    } //-- void setSkin(Skin) 

    /**
     * Set 
     *             Modify the version published display properties.
     *           
     * 
     * @param version
     */
    public void setVersion(Version version)
    {
        this.version = version;
    } //-- void setVersion(Version) 

    /**
     * Method toString
     */
    public java.lang.String toString()
    {
        StringBuffer buf = new StringBuffer();
        buf.append( "name = '" );
        buf.append( getName() + "'" );
        buf.append( "\n" ); 
        buf.append( "bannerLeft = '" );
        buf.append( getBannerLeft() + "'" );
        buf.append( "\n" ); 
        buf.append( "bannerRight = '" );
        buf.append( getBannerRight() + "'" );
        buf.append( "\n" ); 
        buf.append( "publishDate = '" );
        buf.append( getPublishDate() + "'" );
        buf.append( "\n" ); 
        buf.append( "version = '" );
        buf.append( getVersion() + "'" );
        buf.append( "\n" ); 
        buf.append( "poweredBy = '" );
        buf.append( getPoweredBy() + "'" );
        buf.append( "\n" ); 
        buf.append( "skin = '" );
        buf.append( getSkin() + "'" );
        buf.append( "\n" ); 
        buf.append( "body = '" );
        buf.append( getBody() + "'" );
        buf.append( "\n" ); 
        buf.append( "custom = '" );
        buf.append( getCustom() + "'" );
        return buf.toString();
    } //-- java.lang.String toString() 


            
    private java.util.Map menusByRef;

    public Menu getMenuRef( String key )
    {
        if ( menusByRef == null )
        {
            menusByRef = new java.util.HashMap();

            if ( body != null )
            {
                for ( java.util.Iterator i = body.getMenus().iterator(); i.hasNext(); )
                {
                    Menu menu = (Menu) i.next();

                    if ( menu.getRef() != null )
                    {
                        menusByRef.put( menu.getRef(), menu );
                    }
                }
            }
        }
        return (Menu) menusByRef.get( key );
    }

    public void removeMenuRef( String key )
    {
        if ( body != null )
        {
            for ( java.util.Iterator i = body.getMenus().iterator(); i.hasNext(); )
            {
                Menu menu = (Menu) i.next();
                if ( key.equals( menu.getRef() ) )
                {
                    i.remove();
                }
            }
        }
    }

    public java.util.List getMenus()
    {
        java.util.List menus;
        if ( body != null && body.getMenus() != null )
        {
            menus = body.getMenus();
        }
        else
        {
            menus = java.util.Collections.EMPTY_LIST;
        }
        return menus;
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
