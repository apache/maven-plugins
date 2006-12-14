/*
 * $Id$
 */

package org.apache.maven.doxia.site.decoration.io.xpp3;

  //---------------------------------/
 //- Imported classes and packages -/
//---------------------------------/

import java.io.Writer;
import java.text.DateFormat;
import java.util.Iterator;
import org.apache.maven.doxia.site.decoration.Banner;
import org.apache.maven.doxia.site.decoration.Body;
import org.apache.maven.doxia.site.decoration.DecorationModel;
import org.apache.maven.doxia.site.decoration.LinkItem;
import org.apache.maven.doxia.site.decoration.Logo;
import org.apache.maven.doxia.site.decoration.Menu;
import org.apache.maven.doxia.site.decoration.MenuItem;
import org.apache.maven.doxia.site.decoration.PublishDate;
import org.apache.maven.doxia.site.decoration.Skin;
import org.apache.maven.doxia.site.decoration.Version;
import org.codehaus.plexus.util.xml.Xpp3Dom;
import org.codehaus.plexus.util.xml.pull.*;

/**
 * Class DecorationXpp3Writer.
 * 
 * @version $Revision$ $Date$
 */
public class DecorationXpp3Writer {


      //--------------------------/
     //- Class/Member Variables -/
    //--------------------------/

    /**
     * Field serializer
     */
    private org.codehaus.plexus.util.xml.pull.XmlSerializer serializer;

    /**
     * Field NAMESPACE
     */
    private String NAMESPACE;


      //-----------/
     //- Methods -/
    //-----------/

    /**
     * Method write
     * 
     * @param writer
     * @param project
     */
    public void write(Writer writer, DecorationModel project)
        throws java.io.IOException
    {
        serializer = new MXSerializer();
        serializer.setProperty( "http://xmlpull.org/v1/doc/properties.html#serializer-indentation", "  " );
        serializer.setProperty( "http://xmlpull.org/v1/doc/properties.html#serializer-line-separator", "\n" );
        serializer.setOutput( writer );
        serializer.startDocument( project.getModelEncoding(), null );
        writeDecorationModel( project, "project", serializer );
        serializer.endDocument();
    } //-- void write(Writer, DecorationModel) 

    /**
     * Method writeBanner
     * 
     * @param banner
     * @param serializer
     * @param tagName
     */
    private void writeBanner(Banner banner, String tagName, XmlSerializer serializer)
        throws java.io.IOException
    {
        if ( banner != null )
        {
            serializer.startTag( NAMESPACE, tagName );
            if ( banner.getName() != null )
            {
                serializer.startTag( NAMESPACE, "name" ).text( banner.getName() ).endTag( NAMESPACE, "name" );
            }
            if ( banner.getSrc() != null )
            {
                serializer.startTag( NAMESPACE, "src" ).text( banner.getSrc() ).endTag( NAMESPACE, "src" );
            }
            if ( banner.getAlt() != null )
            {
                serializer.startTag( NAMESPACE, "alt" ).text( banner.getAlt() ).endTag( NAMESPACE, "alt" );
            }
            if ( banner.getHref() != null )
            {
                serializer.startTag( NAMESPACE, "href" ).text( banner.getHref() ).endTag( NAMESPACE, "href" );
            }
            serializer.endTag( NAMESPACE, tagName );
        }
    } //-- void writeBanner(Banner, String, XmlSerializer) 

    /**
     * Method writeBody
     * 
     * @param body
     * @param serializer
     * @param tagName
     */
    private void writeBody(Body body, String tagName, XmlSerializer serializer)
        throws java.io.IOException
    {
        if ( body != null )
        {
            serializer.startTag( NAMESPACE, tagName );
            if ( body.getHead() != null )
            {
                ((Xpp3Dom) body.getHead()).writeToSerializer( NAMESPACE, serializer );
            }
            if ( body.getLinks() != null && body.getLinks().size() > 0 )
            {
                serializer.startTag( NAMESPACE, "links" );
                for ( Iterator iter = body.getLinks().iterator(); iter.hasNext(); )
                {
                    LinkItem o = (LinkItem) iter.next();
                    writeLinkItem( o, "item", serializer );
                }
                serializer.endTag( NAMESPACE, "links" );
            }
            if ( body.getBreadcrumbs() != null && body.getBreadcrumbs().size() > 0 )
            {
                serializer.startTag( NAMESPACE, "breadcrumbs" );
                for ( Iterator iter = body.getBreadcrumbs().iterator(); iter.hasNext(); )
                {
                    LinkItem o = (LinkItem) iter.next();
                    writeLinkItem( o, "item", serializer );
                }
                serializer.endTag( NAMESPACE, "breadcrumbs" );
            }
            if ( body.getMenus() != null && body.getMenus().size() > 0 )
            {
                for ( Iterator iter = body.getMenus().iterator(); iter.hasNext(); )
                {
                    Menu o = (Menu) iter.next();
                    writeMenu( o, "menu", serializer );
                }
            }
            serializer.endTag( NAMESPACE, tagName );
        }
    } //-- void writeBody(Body, String, XmlSerializer) 

    /**
     * Method writeDecorationModel
     * 
     * @param decorationModel
     * @param serializer
     * @param tagName
     */
    private void writeDecorationModel(DecorationModel decorationModel, String tagName, XmlSerializer serializer)
        throws java.io.IOException
    {
        if ( decorationModel != null )
        {
            serializer.startTag( NAMESPACE, tagName );
            if ( decorationModel.getName() != null )
            {
                serializer.attribute( NAMESPACE, "name", decorationModel.getName() );
            }
            if ( decorationModel.getBannerLeft() != null )
            {
                writeBanner( decorationModel.getBannerLeft(), "bannerLeft", serializer );
            }
            if ( decorationModel.getBannerRight() != null )
            {
                writeBanner( decorationModel.getBannerRight(), "bannerRight", serializer );
            }
            if ( decorationModel.getPublishDate() != null )
            {
                writePublishDate( decorationModel.getPublishDate(), "publishDate", serializer );
            }
            if ( decorationModel.getVersion() != null )
            {
                writeVersion( decorationModel.getVersion(), "version", serializer );
            }
            if ( decorationModel.getPoweredBy() != null && decorationModel.getPoweredBy().size() > 0 )
            {
                serializer.startTag( NAMESPACE, "poweredBy" );
                for ( Iterator iter = decorationModel.getPoweredBy().iterator(); iter.hasNext(); )
                {
                    Logo o = (Logo) iter.next();
                    writeLogo( o, "logo", serializer );
                }
                serializer.endTag( NAMESPACE, "poweredBy" );
            }
            if ( decorationModel.getSkin() != null )
            {
                writeSkin( decorationModel.getSkin(), "skin", serializer );
            }
            if ( decorationModel.getBody() != null )
            {
                writeBody( decorationModel.getBody(), "body", serializer );
            }
            if ( decorationModel.getCustom() != null )
            {
                ((Xpp3Dom) decorationModel.getCustom()).writeToSerializer( NAMESPACE, serializer );
            }
            serializer.endTag( NAMESPACE, tagName );
        }
    } //-- void writeDecorationModel(DecorationModel, String, XmlSerializer) 

    /**
     * Method writeLinkItem
     * 
     * @param linkItem
     * @param serializer
     * @param tagName
     */
    private void writeLinkItem(LinkItem linkItem, String tagName, XmlSerializer serializer)
        throws java.io.IOException
    {
        if ( linkItem != null )
        {
            serializer.startTag( NAMESPACE, tagName );
            if ( linkItem.getName() != null )
            {
                serializer.attribute( NAMESPACE, "name", linkItem.getName() );
            }
            if ( linkItem.getHref() != null )
            {
                serializer.attribute( NAMESPACE, "href", linkItem.getHref() );
            }
            serializer.endTag( NAMESPACE, tagName );
        }
    } //-- void writeLinkItem(LinkItem, String, XmlSerializer) 

    /**
     * Method writeLogo
     * 
     * @param logo
     * @param serializer
     * @param tagName
     */
    private void writeLogo(Logo logo, String tagName, XmlSerializer serializer)
        throws java.io.IOException
    {
        if ( logo != null )
        {
            serializer.startTag( NAMESPACE, tagName );
            if ( logo.getImg() != null )
            {
                serializer.attribute( NAMESPACE, "img", logo.getImg() );
            }
            if ( logo.getName() != null )
            {
                serializer.attribute( NAMESPACE, "name", logo.getName() );
            }
            if ( logo.getHref() != null )
            {
                serializer.attribute( NAMESPACE, "href", logo.getHref() );
            }
            serializer.endTag( NAMESPACE, tagName );
        }
    } //-- void writeLogo(Logo, String, XmlSerializer) 

    /**
     * Method writeMenu
     * 
     * @param menu
     * @param serializer
     * @param tagName
     */
    private void writeMenu(Menu menu, String tagName, XmlSerializer serializer)
        throws java.io.IOException
    {
        if ( menu != null )
        {
            serializer.startTag( NAMESPACE, tagName );
            if ( menu.getName() != null )
            {
                serializer.attribute( NAMESPACE, "name", menu.getName() );
            }
            if ( menu.getInherit() != null )
            {
                serializer.attribute( NAMESPACE, "inherit", menu.getInherit() );
            }
            if ( menu.isInheritAsRef() != false )
            {
                serializer.attribute( NAMESPACE, "inheritAsRef", String.valueOf( menu.isInheritAsRef() ) );
            }
            if ( menu.getRef() != null )
            {
                serializer.attribute( NAMESPACE, "ref", menu.getRef() );
            }
            if ( menu.getImg() != null )
            {
                serializer.attribute( NAMESPACE, "img", menu.getImg() );
            }
            if ( menu.getItems() != null && menu.getItems().size() > 0 )
            {
                for ( Iterator iter = menu.getItems().iterator(); iter.hasNext(); )
                {
                    MenuItem o = (MenuItem) iter.next();
                    writeMenuItem( o, "item", serializer );
                }
            }
            serializer.endTag( NAMESPACE, tagName );
        }
    } //-- void writeMenu(Menu, String, XmlSerializer) 

    /**
     * Method writeMenuItem
     * 
     * @param menuItem
     * @param serializer
     * @param tagName
     */
    private void writeMenuItem(MenuItem menuItem, String tagName, XmlSerializer serializer)
        throws java.io.IOException
    {
        if ( menuItem != null )
        {
            serializer.startTag( NAMESPACE, tagName );
            if ( menuItem.isCollapse() != false )
            {
                serializer.attribute( NAMESPACE, "collapse", String.valueOf( menuItem.isCollapse() ) );
            }
            if ( menuItem.getRef() != null )
            {
                serializer.attribute( NAMESPACE, "ref", menuItem.getRef() );
            }
            if ( menuItem.getName() != null )
            {
                serializer.attribute( NAMESPACE, "name", menuItem.getName() );
            }
            if ( menuItem.getHref() != null )
            {
                serializer.attribute( NAMESPACE, "href", menuItem.getHref() );
            }
            if ( menuItem.getDescription() != null )
            {
                serializer.startTag( NAMESPACE, "description" ).text( menuItem.getDescription() ).endTag( NAMESPACE, "description" );
            }
            if ( menuItem.getItems() != null && menuItem.getItems().size() > 0 )
            {
                for ( Iterator iter = menuItem.getItems().iterator(); iter.hasNext(); )
                {
                    MenuItem o = (MenuItem) iter.next();
                    writeMenuItem( o, "item", serializer );
                }
            }
            serializer.endTag( NAMESPACE, tagName );
        }
    } //-- void writeMenuItem(MenuItem, String, XmlSerializer) 

    /**
     * Method writePublishDate
     * 
     * @param publishDate
     * @param serializer
     * @param tagName
     */
    private void writePublishDate(PublishDate publishDate, String tagName, XmlSerializer serializer)
        throws java.io.IOException
    {
        if ( publishDate != null )
        {
            serializer.startTag( NAMESPACE, tagName );
            if ( publishDate.getPosition() != null )
            {
                serializer.attribute( NAMESPACE, "position", publishDate.getPosition() );
            }
            if ( publishDate.getFormat() != null )
            {
                serializer.attribute( NAMESPACE, "format", publishDate.getFormat() );
            }
            serializer.endTag( NAMESPACE, tagName );
        }
    } //-- void writePublishDate(PublishDate, String, XmlSerializer) 

    /**
     * Method writeSkin
     * 
     * @param skin
     * @param serializer
     * @param tagName
     */
    private void writeSkin(Skin skin, String tagName, XmlSerializer serializer)
        throws java.io.IOException
    {
        if ( skin != null )
        {
            serializer.startTag( NAMESPACE, tagName );
            if ( skin.getGroupId() != null )
            {
                serializer.startTag( NAMESPACE, "groupId" ).text( skin.getGroupId() ).endTag( NAMESPACE, "groupId" );
            }
            if ( skin.getArtifactId() != null )
            {
                serializer.startTag( NAMESPACE, "artifactId" ).text( skin.getArtifactId() ).endTag( NAMESPACE, "artifactId" );
            }
            if ( skin.getVersion() != null )
            {
                serializer.startTag( NAMESPACE, "version" ).text( skin.getVersion() ).endTag( NAMESPACE, "version" );
            }
            serializer.endTag( NAMESPACE, tagName );
        }
    } //-- void writeSkin(Skin, String, XmlSerializer) 

    /**
     * Method writeVersion
     * 
     * @param version
     * @param serializer
     * @param tagName
     */
    private void writeVersion(Version version, String tagName, XmlSerializer serializer)
        throws java.io.IOException
    {
        if ( version != null )
        {
            serializer.startTag( NAMESPACE, tagName );
            if ( version.getPosition() != null )
            {
                serializer.attribute( NAMESPACE, "position", version.getPosition() );
            }
            serializer.endTag( NAMESPACE, tagName );
        }
    } //-- void writeVersion(Version, String, XmlSerializer) 

}
