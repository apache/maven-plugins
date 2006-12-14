/*
 * $Id$
 */

package org.apache.maven.doxia.site.decoration.io.xpp3;

  //---------------------------------/
 //- Imported classes and packages -/
//---------------------------------/

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.text.DateFormat;
import java.text.ParsePosition;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
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
import org.codehaus.plexus.util.IOUtil;
import org.codehaus.plexus.util.xml.Xpp3DomBuilder;
import org.codehaus.plexus.util.xml.pull.MXParser;
import org.codehaus.plexus.util.xml.pull.XmlPullParser;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;

/**
 * Class DecorationXpp3Reader.
 * 
 * @version $Revision$ $Date$
 */
public class DecorationXpp3Reader {


      //--------------------------/
     //- Class/Member Variables -/
    //--------------------------/

    /**
     * If set the parser till be loaded with all single characters
     * from the XHTML specification.
     * The entities used:
     * <ul>
     * <li>http://www.w3.org/TR/xhtml1/DTD/xhtml-lat1.ent</li>
     * <li>http://www.w3.org/TR/xhtml1/DTD/xhtml-special.ent</li>
     * <li>http://www.w3.org/TR/xhtml1/DTD/xhtml-symbol.ent</li>
     * </ul>
     */
    private boolean addDefaultEntities = true;


      //-----------/
     //- Methods -/
    //-----------/

    /**
     * Method getAddDefaultEntities
     */
    public boolean getAddDefaultEntities()
    {
        return addDefaultEntities;
    } //-- boolean getAddDefaultEntities() 

    /**
     * Method getBooleanValue
     * 
     * @param s
     * @param parser
     * @param attribute
     */
    public boolean getBooleanValue(String s, String attribute, XmlPullParser parser)
        throws XmlPullParserException
    {
        if ( s != null )
        {
            return Boolean.valueOf( s ).booleanValue();
        }
        return false;
    } //-- boolean getBooleanValue(String, String, XmlPullParser) 

    /**
     * Method getCharacterValue
     * 
     * @param s
     * @param parser
     * @param attribute
     */
    public char getCharacterValue(String s, String attribute, XmlPullParser parser)
        throws XmlPullParserException
    {
        if ( s != null )
        {
            return s.charAt( 0 );
        }
        return 0;
    } //-- char getCharacterValue(String, String, XmlPullParser) 

    /**
     * Method getDateValue
     * 
     * @param s
     * @param parser
     * @param attribute
     */
    public java.util.Date getDateValue(String s, String attribute, XmlPullParser parser)
        throws XmlPullParserException
    {
        if ( s != null )
        {
            DateFormat dateParser = DateFormat.getDateTimeInstance( DateFormat.FULL, DateFormat.FULL );
            dateParser.setLenient( true );
            try
            {
                return dateParser.parse( s );
            }
            catch ( java.text.ParseException e )
            {
                throw new XmlPullParserException( e.getMessage() );
            }
        }
        return null;
    } //-- java.util.Date getDateValue(String, String, XmlPullParser) 

    /**
     * Method getDoubleValue
     * 
     * @param s
     * @param strict
     * @param parser
     * @param attribute
     */
    public double getDoubleValue(String s, String attribute, XmlPullParser parser, boolean strict)
        throws XmlPullParserException
    {
        if ( s != null )
        {
            try
            {
                return Double.valueOf( s ).doubleValue();
            }
            catch ( NumberFormatException e )
            {
                if ( strict )
                {
                    throw new XmlPullParserException( "Unable to parse element '" + attribute + "', must be a floating point number", parser, null );
                }
            }
        }
        return 0;
    } //-- double getDoubleValue(String, String, XmlPullParser, boolean) 

    /**
     * Method getFloatValue
     * 
     * @param s
     * @param strict
     * @param parser
     * @param attribute
     */
    public float getFloatValue(String s, String attribute, XmlPullParser parser, boolean strict)
        throws XmlPullParserException
    {
        if ( s != null )
        {
            try
            {
                return Float.valueOf( s ).floatValue();
            }
            catch ( NumberFormatException e )
            {
                if ( strict )
                {
                    throw new XmlPullParserException( "Unable to parse element '" + attribute + "', must be a floating point number", parser, null );
                }
            }
        }
        return 0;
    } //-- float getFloatValue(String, String, XmlPullParser, boolean) 

    /**
     * Method getIntegerValue
     * 
     * @param s
     * @param strict
     * @param parser
     * @param attribute
     */
    public int getIntegerValue(String s, String attribute, XmlPullParser parser, boolean strict)
        throws XmlPullParserException
    {
        if ( s != null )
        {
            try
            {
                return Integer.valueOf( s ).intValue();
            }
            catch ( NumberFormatException e )
            {
                if ( strict )
                {
                    throw new XmlPullParserException( "Unable to parse element '" + attribute + "', must be an integer", parser, null );
                }
            }
        }
        return 0;
    } //-- int getIntegerValue(String, String, XmlPullParser, boolean) 

    /**
     * Method getLongValue
     * 
     * @param s
     * @param strict
     * @param parser
     * @param attribute
     */
    public long getLongValue(String s, String attribute, XmlPullParser parser, boolean strict)
        throws XmlPullParserException
    {
        if ( s != null )
        {
            try
            {
                return Long.valueOf( s ).longValue();
            }
            catch ( NumberFormatException e )
            {
                if ( strict )
                {
                    throw new XmlPullParserException( "Unable to parse element '" + attribute + "', must be a long integer", parser, null );
                }
            }
        }
        return 0;
    } //-- long getLongValue(String, String, XmlPullParser, boolean) 

    /**
     * Method getRequiredAttributeValue
     * 
     * @param s
     * @param strict
     * @param parser
     * @param attribute
     */
    public String getRequiredAttributeValue(String s, String attribute, XmlPullParser parser, boolean strict)
        throws XmlPullParserException
    {
        if ( s == null )
        {
            if ( strict )
            {
                throw new XmlPullParserException( "Missing required value for attribute '" + attribute + "'", parser, null );
            }
        }
        return s;
    } //-- String getRequiredAttributeValue(String, String, XmlPullParser, boolean) 

    /**
     * Method getShortValue
     * 
     * @param s
     * @param strict
     * @param parser
     * @param attribute
     */
    public short getShortValue(String s, String attribute, XmlPullParser parser, boolean strict)
        throws XmlPullParserException
    {
        if ( s != null )
        {
            try
            {
                return Short.valueOf( s ).shortValue();
            }
            catch ( NumberFormatException e )
            {
                if ( strict )
                {
                    throw new XmlPullParserException( "Unable to parse element '" + attribute + "', must be a short integer", parser, null );
                }
            }
        }
        return 0;
    } //-- short getShortValue(String, String, XmlPullParser, boolean) 

    /**
     * Method getTrimmedValue
     * 
     * @param s
     */
    public String getTrimmedValue(String s)
    {
        if ( s != null )
        {
            s = s.trim();
        }
        return s;
    } //-- String getTrimmedValue(String) 

    /**
     * Method parseBanner
     * 
     * @param tagName
     * @param encoding
     * @param strict
     * @param parser
     */
    private Banner parseBanner(String tagName, XmlPullParser parser, boolean strict, String encoding)
        throws IOException, XmlPullParserException
    {
        Banner banner = new Banner();
        banner.setModelEncoding( encoding );
        java.util.Set parsed = new java.util.HashSet();
        while ( parser.nextTag() == XmlPullParser.START_TAG )
        {
            if ( parser.getName().equals( "name" )  )
            {
                if ( parsed.contains( "name" ) )
                {
                    throw new XmlPullParserException( "Duplicated tag: '" + parser.getName() + "'", parser, null);
                }
                parsed.add( "name" );
                banner.setName( getTrimmedValue( parser.nextText()) );
            }
            else if ( parser.getName().equals( "src" )  )
            {
                if ( parsed.contains( "src" ) )
                {
                    throw new XmlPullParserException( "Duplicated tag: '" + parser.getName() + "'", parser, null);
                }
                parsed.add( "src" );
                banner.setSrc( getTrimmedValue( parser.nextText()) );
            }
            else if ( parser.getName().equals( "alt" )  )
            {
                if ( parsed.contains( "alt" ) )
                {
                    throw new XmlPullParserException( "Duplicated tag: '" + parser.getName() + "'", parser, null);
                }
                parsed.add( "alt" );
                banner.setAlt( getTrimmedValue( parser.nextText()) );
            }
            else if ( parser.getName().equals( "href" )  )
            {
                if ( parsed.contains( "href" ) )
                {
                    throw new XmlPullParserException( "Duplicated tag: '" + parser.getName() + "'", parser, null);
                }
                parsed.add( "href" );
                banner.setHref( getTrimmedValue( parser.nextText()) );
            }
            else
            {
                if ( strict )
                {
                    throw new XmlPullParserException( "Unrecognised tag: '" + parser.getName() + "'", parser, null);
                }
            }
        }
        return banner;
    } //-- Banner parseBanner(String, XmlPullParser, boolean, String) 

    /**
     * Method parseBody
     * 
     * @param tagName
     * @param encoding
     * @param strict
     * @param parser
     */
    private Body parseBody(String tagName, XmlPullParser parser, boolean strict, String encoding)
        throws IOException, XmlPullParserException
    {
        Body body = new Body();
        body.setModelEncoding( encoding );
        java.util.Set parsed = new java.util.HashSet();
        while ( parser.nextTag() == XmlPullParser.START_TAG )
        {
            if ( parser.getName().equals( "head" )  )
            {
                if ( parsed.contains( "head" ) )
                {
                    throw new XmlPullParserException( "Duplicated tag: '" + parser.getName() + "'", parser, null);
                }
                parsed.add( "head" );
                body.setHead( Xpp3DomBuilder.build( parser ) );
            }
            else if ( parser.getName().equals( "links" )  )
            {
                if ( parsed.contains( "links" ) )
                {
                    throw new XmlPullParserException( "Duplicated tag: '" + parser.getName() + "'", parser, null);
                }
                parsed.add( "links" );
                java.util.List links = new java.util.ArrayList();
                body.setLinks( links );
                while ( parser.nextTag() == XmlPullParser.START_TAG )
                {
                    if ( parser.getName().equals( "item" ) )
                    {
                        links.add( parseLinkItem( "item", parser, strict, encoding ) );
                    }
                    else
                    {
                        parser.nextText();
                    }
                }
            }
            else if ( parser.getName().equals( "breadcrumbs" )  )
            {
                if ( parsed.contains( "breadcrumbs" ) )
                {
                    throw new XmlPullParserException( "Duplicated tag: '" + parser.getName() + "'", parser, null);
                }
                parsed.add( "breadcrumbs" );
                java.util.List breadcrumbs = new java.util.ArrayList();
                body.setBreadcrumbs( breadcrumbs );
                while ( parser.nextTag() == XmlPullParser.START_TAG )
                {
                    if ( parser.getName().equals( "item" ) )
                    {
                        breadcrumbs.add( parseLinkItem( "item", parser, strict, encoding ) );
                    }
                    else
                    {
                        parser.nextText();
                    }
                }
            }
            else if ( parser.getName().equals( "menu" ) )
            {
                java.util.List menus = body.getMenus();
                if ( menus == null )
                {
                    menus = new java.util.ArrayList();
                    body.setMenus( menus );
                }
                menus.add( parseMenu( "menu", parser, strict, encoding ) );
            }
            else
            {
                if ( strict )
                {
                    throw new XmlPullParserException( "Unrecognised tag: '" + parser.getName() + "'", parser, null);
                }
            }
        }
        return body;
    } //-- Body parseBody(String, XmlPullParser, boolean, String) 

    /**
     * Method parseDecorationModel
     * 
     * @param tagName
     * @param encoding
     * @param strict
     * @param parser
     */
    private DecorationModel parseDecorationModel(String tagName, XmlPullParser parser, boolean strict, String encoding)
        throws IOException, XmlPullParserException
    {
        DecorationModel decorationModel = new DecorationModel();
        decorationModel.setModelEncoding( encoding );
        decorationModel.setName( getTrimmedValue( parser.getAttributeValue( "", "name" )) );
        java.util.Set parsed = new java.util.HashSet();
        int eventType = parser.getEventType();
        boolean foundRoot = false;
        while ( eventType != XmlPullParser.END_DOCUMENT )
        {
            if ( eventType == XmlPullParser.START_TAG )
            {
                if ( parser.getName().equals( tagName ) )
                {
                    foundRoot = true;
                }
                else if ( parser.getName().equals( "bannerLeft" )  )
                {
                    if ( parsed.contains( "bannerLeft" ) )
                    {
                        throw new XmlPullParserException( "Duplicated tag: '" + parser.getName() + "'", parser, null);
                    }
                    parsed.add( "bannerLeft" );
                    decorationModel.setBannerLeft( parseBanner( "bannerLeft", parser, strict, encoding ) );
                }
                else if ( parser.getName().equals( "bannerRight" )  )
                {
                    if ( parsed.contains( "bannerRight" ) )
                    {
                        throw new XmlPullParserException( "Duplicated tag: '" + parser.getName() + "'", parser, null);
                    }
                    parsed.add( "bannerRight" );
                    decorationModel.setBannerRight( parseBanner( "bannerRight", parser, strict, encoding ) );
                }
                else if ( parser.getName().equals( "publishDate" )  )
                {
                    if ( parsed.contains( "publishDate" ) )
                    {
                        throw new XmlPullParserException( "Duplicated tag: '" + parser.getName() + "'", parser, null);
                    }
                    parsed.add( "publishDate" );
                    decorationModel.setPublishDate( parsePublishDate( "publishDate", parser, strict, encoding ) );
                }
                else if ( parser.getName().equals( "version" )  )
                {
                    if ( parsed.contains( "version" ) )
                    {
                        throw new XmlPullParserException( "Duplicated tag: '" + parser.getName() + "'", parser, null);
                    }
                    parsed.add( "version" );
                    decorationModel.setVersion( parseVersion( "version", parser, strict, encoding ) );
                }
                else if ( parser.getName().equals( "poweredBy" )  )
                {
                    if ( parsed.contains( "poweredBy" ) )
                    {
                        throw new XmlPullParserException( "Duplicated tag: '" + parser.getName() + "'", parser, null);
                    }
                    parsed.add( "poweredBy" );
                    java.util.List poweredBy = new java.util.ArrayList();
                    decorationModel.setPoweredBy( poweredBy );
                    while ( parser.nextTag() == XmlPullParser.START_TAG )
                    {
                        if ( parser.getName().equals( "logo" ) )
                        {
                            poweredBy.add( parseLogo( "logo", parser, strict, encoding ) );
                        }
                        else
                        {
                            parser.nextText();
                        }
                    }
                }
                else if ( parser.getName().equals( "skin" )  )
                {
                    if ( parsed.contains( "skin" ) )
                    {
                        throw new XmlPullParserException( "Duplicated tag: '" + parser.getName() + "'", parser, null);
                    }
                    parsed.add( "skin" );
                    decorationModel.setSkin( parseSkin( "skin", parser, strict, encoding ) );
                }
                else if ( parser.getName().equals( "body" )  )
                {
                    if ( parsed.contains( "body" ) )
                    {
                        throw new XmlPullParserException( "Duplicated tag: '" + parser.getName() + "'", parser, null);
                    }
                    parsed.add( "body" );
                    decorationModel.setBody( parseBody( "body", parser, strict, encoding ) );
                }
                else if ( parser.getName().equals( "custom" )  )
                {
                    if ( parsed.contains( "custom" ) )
                    {
                        throw new XmlPullParserException( "Duplicated tag: '" + parser.getName() + "'", parser, null);
                    }
                    parsed.add( "custom" );
                    decorationModel.setCustom( Xpp3DomBuilder.build( parser ) );
                }
                else
                {
                    if ( foundRoot )
                    {
                        if ( strict )
                        {
                            throw new XmlPullParserException( "Unrecognised tag: '" + parser.getName() + "'", parser, null);
                        }
                    }
                }
            }
            eventType = parser.next();
        }
        return decorationModel;
    } //-- DecorationModel parseDecorationModel(String, XmlPullParser, boolean, String) 

    /**
     * Method parseLinkItem
     * 
     * @param tagName
     * @param encoding
     * @param strict
     * @param parser
     */
    private LinkItem parseLinkItem(String tagName, XmlPullParser parser, boolean strict, String encoding)
        throws IOException, XmlPullParserException
    {
        LinkItem linkItem = new LinkItem();
        linkItem.setModelEncoding( encoding );
        linkItem.setName( getTrimmedValue( parser.getAttributeValue( "", "name" )) );
        linkItem.setHref( getTrimmedValue( parser.getAttributeValue( "", "href" )) );
        java.util.Set parsed = new java.util.HashSet();
        while ( parser.nextTag() == XmlPullParser.START_TAG )
        {
            if ( strict )
            {
                throw new XmlPullParserException( "Unrecognised tag: '" + parser.getName() + "'", parser, null);
            }
        }
        return linkItem;
    } //-- LinkItem parseLinkItem(String, XmlPullParser, boolean, String) 

    /**
     * Method parseLogo
     * 
     * @param tagName
     * @param encoding
     * @param strict
     * @param parser
     */
    private Logo parseLogo(String tagName, XmlPullParser parser, boolean strict, String encoding)
        throws IOException, XmlPullParserException
    {
        Logo logo = new Logo();
        logo.setModelEncoding( encoding );
        logo.setImg( getTrimmedValue( parser.getAttributeValue( "", "img" )) );
        logo.setName( getTrimmedValue( parser.getAttributeValue( "", "name" )) );
        logo.setHref( getTrimmedValue( parser.getAttributeValue( "", "href" )) );
        java.util.Set parsed = new java.util.HashSet();
        while ( parser.nextTag() == XmlPullParser.START_TAG )
        {
            if ( strict )
            {
                throw new XmlPullParserException( "Unrecognised tag: '" + parser.getName() + "'", parser, null);
            }
        }
        return logo;
    } //-- Logo parseLogo(String, XmlPullParser, boolean, String) 

    /**
     * Method parseMenu
     * 
     * @param tagName
     * @param encoding
     * @param strict
     * @param parser
     */
    private Menu parseMenu(String tagName, XmlPullParser parser, boolean strict, String encoding)
        throws IOException, XmlPullParserException
    {
        Menu menu = new Menu();
        menu.setModelEncoding( encoding );
        menu.setName( getTrimmedValue( parser.getAttributeValue( "", "name" )) );
        menu.setInherit( getTrimmedValue( parser.getAttributeValue( "", "inherit" )) );
        menu.setInheritAsRef( getBooleanValue( getTrimmedValue( parser.getAttributeValue( "", "inheritAsRef" )), "inheritAsRef", parser ) );
        menu.setRef( getTrimmedValue( parser.getAttributeValue( "", "ref" )) );
        menu.setImg( getTrimmedValue( parser.getAttributeValue( "", "img" )) );
        java.util.Set parsed = new java.util.HashSet();
        while ( parser.nextTag() == XmlPullParser.START_TAG )
        {
            if ( parser.getName().equals( "item" ) )
            {
                java.util.List items = menu.getItems();
                if ( items == null )
                {
                    items = new java.util.ArrayList();
                    menu.setItems( items );
                }
                items.add( parseMenuItem( "item", parser, strict, encoding ) );
            }
            else
            {
                if ( strict )
                {
                    throw new XmlPullParserException( "Unrecognised tag: '" + parser.getName() + "'", parser, null);
                }
            }
        }
        return menu;
    } //-- Menu parseMenu(String, XmlPullParser, boolean, String) 

    /**
     * Method parseMenuItem
     * 
     * @param tagName
     * @param encoding
     * @param strict
     * @param parser
     */
    private MenuItem parseMenuItem(String tagName, XmlPullParser parser, boolean strict, String encoding)
        throws IOException, XmlPullParserException
    {
        MenuItem menuItem = new MenuItem();
        menuItem.setModelEncoding( encoding );
        menuItem.setCollapse( getBooleanValue( getTrimmedValue( parser.getAttributeValue( "", "collapse" )), "collapse", parser ) );
        menuItem.setRef( getTrimmedValue( parser.getAttributeValue( "", "ref" )) );
        menuItem.setName( getTrimmedValue( parser.getAttributeValue( "", "name" )) );
        menuItem.setHref( getTrimmedValue( parser.getAttributeValue( "", "href" )) );
        java.util.Set parsed = new java.util.HashSet();
        while ( parser.nextTag() == XmlPullParser.START_TAG )
        {
            if ( parser.getName().equals( "description" )  )
            {
                if ( parsed.contains( "description" ) )
                {
                    throw new XmlPullParserException( "Duplicated tag: '" + parser.getName() + "'", parser, null);
                }
                parsed.add( "description" );
                menuItem.setDescription( getTrimmedValue( parser.nextText()) );
            }
            else if ( parser.getName().equals( "item" ) )
            {
                java.util.List items = menuItem.getItems();
                if ( items == null )
                {
                    items = new java.util.ArrayList();
                    menuItem.setItems( items );
                }
                items.add( parseMenuItem( "item", parser, strict, encoding ) );
            }
            else
            {
                if ( strict )
                {
                    throw new XmlPullParserException( "Unrecognised tag: '" + parser.getName() + "'", parser, null);
                }
            }
        }
        return menuItem;
    } //-- MenuItem parseMenuItem(String, XmlPullParser, boolean, String) 

    /**
     * Method parsePublishDate
     * 
     * @param tagName
     * @param encoding
     * @param strict
     * @param parser
     */
    private PublishDate parsePublishDate(String tagName, XmlPullParser parser, boolean strict, String encoding)
        throws IOException, XmlPullParserException
    {
        PublishDate publishDate = new PublishDate();
        publishDate.setModelEncoding( encoding );
        publishDate.setPosition( getTrimmedValue( parser.getAttributeValue( "", "position" )) );
        publishDate.setFormat( getTrimmedValue( parser.getAttributeValue( "", "format" )) );
        java.util.Set parsed = new java.util.HashSet();
        while ( parser.nextTag() == XmlPullParser.START_TAG )
        {
            if ( strict )
            {
                throw new XmlPullParserException( "Unrecognised tag: '" + parser.getName() + "'", parser, null);
            }
        }
        return publishDate;
    } //-- PublishDate parsePublishDate(String, XmlPullParser, boolean, String) 

    /**
     * Method parseSkin
     * 
     * @param tagName
     * @param encoding
     * @param strict
     * @param parser
     */
    private Skin parseSkin(String tagName, XmlPullParser parser, boolean strict, String encoding)
        throws IOException, XmlPullParserException
    {
        Skin skin = new Skin();
        skin.setModelEncoding( encoding );
        java.util.Set parsed = new java.util.HashSet();
        while ( parser.nextTag() == XmlPullParser.START_TAG )
        {
            if ( parser.getName().equals( "groupId" )  )
            {
                if ( parsed.contains( "groupId" ) )
                {
                    throw new XmlPullParserException( "Duplicated tag: '" + parser.getName() + "'", parser, null);
                }
                parsed.add( "groupId" );
                skin.setGroupId( getTrimmedValue( parser.nextText()) );
            }
            else if ( parser.getName().equals( "artifactId" )  )
            {
                if ( parsed.contains( "artifactId" ) )
                {
                    throw new XmlPullParserException( "Duplicated tag: '" + parser.getName() + "'", parser, null);
                }
                parsed.add( "artifactId" );
                skin.setArtifactId( getTrimmedValue( parser.nextText()) );
            }
            else if ( parser.getName().equals( "version" )  )
            {
                if ( parsed.contains( "version" ) )
                {
                    throw new XmlPullParserException( "Duplicated tag: '" + parser.getName() + "'", parser, null);
                }
                parsed.add( "version" );
                skin.setVersion( getTrimmedValue( parser.nextText()) );
            }
            else
            {
                if ( strict )
                {
                    throw new XmlPullParserException( "Unrecognised tag: '" + parser.getName() + "'", parser, null);
                }
            }
        }
        return skin;
    } //-- Skin parseSkin(String, XmlPullParser, boolean, String) 

    /**
     * Method parseVersion
     * 
     * @param tagName
     * @param encoding
     * @param strict
     * @param parser
     */
    private Version parseVersion(String tagName, XmlPullParser parser, boolean strict, String encoding)
        throws IOException, XmlPullParserException
    {
        Version version = new Version();
        version.setModelEncoding( encoding );
        version.setPosition( getTrimmedValue( parser.getAttributeValue( "", "position" )) );
        java.util.Set parsed = new java.util.HashSet();
        while ( parser.nextTag() == XmlPullParser.START_TAG )
        {
            if ( strict )
            {
                throw new XmlPullParserException( "Unrecognised tag: '" + parser.getName() + "'", parser, null);
            }
        }
        return version;
    } //-- Version parseVersion(String, XmlPullParser, boolean, String) 

    /**
     * Method read
     * 
     * @param reader
     * @param strict
     */
    public DecorationModel read(Reader reader, boolean strict)
        throws IOException, XmlPullParserException
    {
        XmlPullParser parser = new MXParser();
        
        parser.setInput( reader );
        
        if ( addDefaultEntities ) 
        {
            // ----------------------------------------------------------------------
            // Latin 1 entities
            // ----------------------------------------------------------------------
            
            parser.defineEntityReplacementText( "nbsp", "\u00a0" ); 
            parser.defineEntityReplacementText( "iexcl", "\u00a1" ); 
            parser.defineEntityReplacementText( "cent", "\u00a2" ); 
            parser.defineEntityReplacementText( "pound", "\u00a3" ); 
            parser.defineEntityReplacementText( "curren", "\u00a4" ); 
            parser.defineEntityReplacementText( "yen", "\u00a5" ); 
            parser.defineEntityReplacementText( "brvbar", "\u00a6" ); 
            parser.defineEntityReplacementText( "sect", "\u00a7" ); 
            parser.defineEntityReplacementText( "uml", "\u00a8" ); 
            parser.defineEntityReplacementText( "copy", "\u00a9" ); 
            parser.defineEntityReplacementText( "ordf", "\u00aa" ); 
            parser.defineEntityReplacementText( "laquo", "\u00ab" ); 
            parser.defineEntityReplacementText( "not", "\u00ac" ); 
            parser.defineEntityReplacementText( "shy", "\u00ad" ); 
            parser.defineEntityReplacementText( "reg", "\u00ae" ); 
            parser.defineEntityReplacementText( "macr", "\u00af" ); 
            parser.defineEntityReplacementText( "deg", "\u00b0" ); 
            parser.defineEntityReplacementText( "plusmn", "\u00b1" ); 
            parser.defineEntityReplacementText( "sup2", "\u00b2" ); 
            parser.defineEntityReplacementText( "sup3", "\u00b3" ); 
            parser.defineEntityReplacementText( "acute", "\u00b4" ); 
            parser.defineEntityReplacementText( "micro", "\u00b5" ); 
            parser.defineEntityReplacementText( "para", "\u00b6" ); 
            parser.defineEntityReplacementText( "middot", "\u00b7" ); 
            parser.defineEntityReplacementText( "cedil", "\u00b8" ); 
            parser.defineEntityReplacementText( "sup1", "\u00b9" ); 
            parser.defineEntityReplacementText( "ordm", "\u00ba" ); 
            parser.defineEntityReplacementText( "raquo", "\u00bb" ); 
            parser.defineEntityReplacementText( "frac14", "\u00bc" ); 
            parser.defineEntityReplacementText( "frac12", "\u00bd" ); 
            parser.defineEntityReplacementText( "frac34", "\u00be" ); 
            parser.defineEntityReplacementText( "iquest", "\u00bf" ); 
            parser.defineEntityReplacementText( "Agrave", "\u00c0" ); 
            parser.defineEntityReplacementText( "Aacute", "\u00c1" ); 
            parser.defineEntityReplacementText( "Acirc", "\u00c2" ); 
            parser.defineEntityReplacementText( "Atilde", "\u00c3" ); 
            parser.defineEntityReplacementText( "Auml", "\u00c4" ); 
            parser.defineEntityReplacementText( "Aring", "\u00c5" ); 
            parser.defineEntityReplacementText( "AElig", "\u00c6" ); 
            parser.defineEntityReplacementText( "Ccedil", "\u00c7" ); 
            parser.defineEntityReplacementText( "Egrave", "\u00c8" ); 
            parser.defineEntityReplacementText( "Eacute", "\u00c9" ); 
            parser.defineEntityReplacementText( "Ecirc", "\u00ca" ); 
            parser.defineEntityReplacementText( "Euml", "\u00cb" ); 
            parser.defineEntityReplacementText( "Igrave", "\u00cc" ); 
            parser.defineEntityReplacementText( "Iacute", "\u00cd" ); 
            parser.defineEntityReplacementText( "Icirc", "\u00ce" ); 
            parser.defineEntityReplacementText( "Iuml", "\u00cf" ); 
            parser.defineEntityReplacementText( "ETH", "\u00d0" ); 
            parser.defineEntityReplacementText( "Ntilde", "\u00d1" ); 
            parser.defineEntityReplacementText( "Ograve", "\u00d2" ); 
            parser.defineEntityReplacementText( "Oacute", "\u00d3" ); 
            parser.defineEntityReplacementText( "Ocirc", "\u00d4" ); 
            parser.defineEntityReplacementText( "Otilde", "\u00d5" ); 
            parser.defineEntityReplacementText( "Ouml", "\u00d6" ); 
            parser.defineEntityReplacementText( "times", "\u00d7" ); 
            parser.defineEntityReplacementText( "Oslash", "\u00d8" ); 
            parser.defineEntityReplacementText( "Ugrave", "\u00d9" ); 
            parser.defineEntityReplacementText( "Uacute", "\u00da" ); 
            parser.defineEntityReplacementText( "Ucirc", "\u00db" ); 
            parser.defineEntityReplacementText( "Uuml", "\u00dc" ); 
            parser.defineEntityReplacementText( "Yacute", "\u00dd" ); 
            parser.defineEntityReplacementText( "THORN", "\u00de" ); 
            parser.defineEntityReplacementText( "szlig", "\u00df" ); 
            parser.defineEntityReplacementText( "agrave", "\u00e0" ); 
            parser.defineEntityReplacementText( "aacute", "\u00e1" ); 
            parser.defineEntityReplacementText( "acirc", "\u00e2" ); 
            parser.defineEntityReplacementText( "atilde", "\u00e3" ); 
            parser.defineEntityReplacementText( "auml", "\u00e4" ); 
            parser.defineEntityReplacementText( "aring", "\u00e5" ); 
            parser.defineEntityReplacementText( "aelig", "\u00e6" ); 
            parser.defineEntityReplacementText( "ccedil", "\u00e7" ); 
            parser.defineEntityReplacementText( "egrave", "\u00e8" ); 
            parser.defineEntityReplacementText( "eacute", "\u00e9" ); 
            parser.defineEntityReplacementText( "ecirc", "\u00ea" ); 
            parser.defineEntityReplacementText( "euml", "\u00eb" ); 
            parser.defineEntityReplacementText( "igrave", "\u00ec" ); 
            parser.defineEntityReplacementText( "iacute", "\u00ed" ); 
            parser.defineEntityReplacementText( "icirc", "\u00ee" ); 
            parser.defineEntityReplacementText( "iuml", "\u00ef" ); 
            parser.defineEntityReplacementText( "eth", "\u00f0" ); 
            parser.defineEntityReplacementText( "ntilde", "\u00f1" ); 
            parser.defineEntityReplacementText( "ograve", "\u00f2" ); 
            parser.defineEntityReplacementText( "oacute", "\u00f3" ); 
            parser.defineEntityReplacementText( "ocirc", "\u00f4" ); 
            parser.defineEntityReplacementText( "otilde", "\u00f5" ); 
            parser.defineEntityReplacementText( "ouml", "\u00f6" ); 
            parser.defineEntityReplacementText( "divide", "\u00f7" ); 
            parser.defineEntityReplacementText( "oslash", "\u00f8" ); 
            parser.defineEntityReplacementText( "ugrave", "\u00f9" ); 
            parser.defineEntityReplacementText( "uacute", "\u00fa" ); 
            parser.defineEntityReplacementText( "ucirc", "\u00fb" ); 
            parser.defineEntityReplacementText( "uuml", "\u00fc" ); 
            parser.defineEntityReplacementText( "yacute", "\u00fd" ); 
            parser.defineEntityReplacementText( "thorn", "\u00fe" ); 
            parser.defineEntityReplacementText( "yuml", "\u00ff" ); 
            
            // ----------------------------------------------------------------------
            // Special entities
            // ----------------------------------------------------------------------
            
            parser.defineEntityReplacementText( "OElig", "\u0152" ); 
            parser.defineEntityReplacementText( "oelig", "\u0153" ); 
            parser.defineEntityReplacementText( "Scaron", "\u0160" ); 
            parser.defineEntityReplacementText( "scaron", "\u0161" ); 
            parser.defineEntityReplacementText( "Yuml", "\u0178" ); 
            parser.defineEntityReplacementText( "circ", "\u02c6" ); 
            parser.defineEntityReplacementText( "tilde", "\u02dc" ); 
            parser.defineEntityReplacementText( "ensp", "\u2002" ); 
            parser.defineEntityReplacementText( "emsp", "\u2003" ); 
            parser.defineEntityReplacementText( "thinsp", "\u2009" ); 
            parser.defineEntityReplacementText( "zwnj", "\u200c" ); 
            parser.defineEntityReplacementText( "zwj", "\u200d" ); 
            parser.defineEntityReplacementText( "lrm", "\u200e" ); 
            parser.defineEntityReplacementText( "rlm", "\u200f" ); 
            parser.defineEntityReplacementText( "ndash", "\u2013" ); 
            parser.defineEntityReplacementText( "mdash", "\u2014" ); 
            parser.defineEntityReplacementText( "lsquo", "\u2018" ); 
            parser.defineEntityReplacementText( "rsquo", "\u2019" ); 
            parser.defineEntityReplacementText( "sbquo", "\u201a" ); 
            parser.defineEntityReplacementText( "ldquo", "\u201c" ); 
            parser.defineEntityReplacementText( "rdquo", "\u201d" ); 
            parser.defineEntityReplacementText( "bdquo", "\u201e" ); 
            parser.defineEntityReplacementText( "dagger", "\u2020" ); 
            parser.defineEntityReplacementText( "Dagger", "\u2021" ); 
            parser.defineEntityReplacementText( "permil", "\u2030" ); 
            parser.defineEntityReplacementText( "lsaquo", "\u2039" ); 
            parser.defineEntityReplacementText( "rsaquo", "\u203a" ); 
            parser.defineEntityReplacementText( "euro", "\u20ac" ); 
            
            // ----------------------------------------------------------------------
            // Symbol entities
            // ----------------------------------------------------------------------
            
            parser.defineEntityReplacementText( "fnof", "\u0192" ); 
            parser.defineEntityReplacementText( "Alpha", "\u0391" ); 
            parser.defineEntityReplacementText( "Beta", "\u0392" ); 
            parser.defineEntityReplacementText( "Gamma", "\u0393" ); 
            parser.defineEntityReplacementText( "Delta", "\u0394" ); 
            parser.defineEntityReplacementText( "Epsilon", "\u0395" ); 
            parser.defineEntityReplacementText( "Zeta", "\u0396" ); 
            parser.defineEntityReplacementText( "Eta", "\u0397" ); 
            parser.defineEntityReplacementText( "Theta", "\u0398" ); 
            parser.defineEntityReplacementText( "Iota", "\u0399" ); 
            parser.defineEntityReplacementText( "Kappa", "\u039a" ); 
            parser.defineEntityReplacementText( "Lambda", "\u039b" ); 
            parser.defineEntityReplacementText( "Mu", "\u039c" ); 
            parser.defineEntityReplacementText( "Nu", "\u039d" ); 
            parser.defineEntityReplacementText( "Xi", "\u039e" ); 
            parser.defineEntityReplacementText( "Omicron", "\u039f" ); 
            parser.defineEntityReplacementText( "Pi", "\u03a0" ); 
            parser.defineEntityReplacementText( "Rho", "\u03a1" ); 
            parser.defineEntityReplacementText( "Sigma", "\u03a3" ); 
            parser.defineEntityReplacementText( "Tau", "\u03a4" ); 
            parser.defineEntityReplacementText( "Upsilon", "\u03a5" ); 
            parser.defineEntityReplacementText( "Phi", "\u03a6" ); 
            parser.defineEntityReplacementText( "Chi", "\u03a7" ); 
            parser.defineEntityReplacementText( "Psi", "\u03a8" ); 
            parser.defineEntityReplacementText( "Omega", "\u03a9" ); 
            parser.defineEntityReplacementText( "alpha", "\u03b1" ); 
            parser.defineEntityReplacementText( "beta", "\u03b2" ); 
            parser.defineEntityReplacementText( "gamma", "\u03b3" ); 
            parser.defineEntityReplacementText( "delta", "\u03b4" ); 
            parser.defineEntityReplacementText( "epsilon", "\u03b5" ); 
            parser.defineEntityReplacementText( "zeta", "\u03b6" ); 
            parser.defineEntityReplacementText( "eta", "\u03b7" ); 
            parser.defineEntityReplacementText( "theta", "\u03b8" ); 
            parser.defineEntityReplacementText( "iota", "\u03b9" ); 
            parser.defineEntityReplacementText( "kappa", "\u03ba" ); 
            parser.defineEntityReplacementText( "lambda", "\u03bb" ); 
            parser.defineEntityReplacementText( "mu", "\u03bc" ); 
            parser.defineEntityReplacementText( "nu", "\u03bd" ); 
            parser.defineEntityReplacementText( "xi", "\u03be" ); 
            parser.defineEntityReplacementText( "omicron", "\u03bf" ); 
            parser.defineEntityReplacementText( "pi", "\u03c0" ); 
            parser.defineEntityReplacementText( "rho", "\u03c1" ); 
            parser.defineEntityReplacementText( "sigmaf", "\u03c2" ); 
            parser.defineEntityReplacementText( "sigma", "\u03c3" ); 
            parser.defineEntityReplacementText( "tau", "\u03c4" ); 
            parser.defineEntityReplacementText( "upsilon", "\u03c5" ); 
            parser.defineEntityReplacementText( "phi", "\u03c6" ); 
            parser.defineEntityReplacementText( "chi", "\u03c7" ); 
            parser.defineEntityReplacementText( "psi", "\u03c8" ); 
            parser.defineEntityReplacementText( "omega", "\u03c9" ); 
            parser.defineEntityReplacementText( "thetasym", "\u03d1" ); 
            parser.defineEntityReplacementText( "upsih", "\u03d2" ); 
            parser.defineEntityReplacementText( "piv", "\u03d6" ); 
            parser.defineEntityReplacementText( "bull", "\u2022" ); 
            parser.defineEntityReplacementText( "hellip", "\u2026" ); 
            parser.defineEntityReplacementText( "prime", "\u2032" ); 
            parser.defineEntityReplacementText( "Prime", "\u2033" ); 
            parser.defineEntityReplacementText( "oline", "\u203e" ); 
            parser.defineEntityReplacementText( "frasl", "\u2044" ); 
            parser.defineEntityReplacementText( "weierp", "\u2118" ); 
            parser.defineEntityReplacementText( "image", "\u2111" ); 
            parser.defineEntityReplacementText( "real", "\u211c" ); 
            parser.defineEntityReplacementText( "trade", "\u2122" ); 
            parser.defineEntityReplacementText( "alefsym", "\u2135" ); 
            parser.defineEntityReplacementText( "larr", "\u2190" ); 
            parser.defineEntityReplacementText( "uarr", "\u2191" ); 
            parser.defineEntityReplacementText( "rarr", "\u2192" ); 
            parser.defineEntityReplacementText( "darr", "\u2193" ); 
            parser.defineEntityReplacementText( "harr", "\u2194" ); 
            parser.defineEntityReplacementText( "crarr", "\u21b5" ); 
            parser.defineEntityReplacementText( "lArr", "\u21d0" ); 
            parser.defineEntityReplacementText( "uArr", "\u21d1" ); 
            parser.defineEntityReplacementText( "rArr", "\u21d2" ); 
            parser.defineEntityReplacementText( "dArr", "\u21d3" ); 
            parser.defineEntityReplacementText( "hArr", "\u21d4" ); 
            parser.defineEntityReplacementText( "forall", "\u2200" ); 
            parser.defineEntityReplacementText( "part", "\u2202" ); 
            parser.defineEntityReplacementText( "exist", "\u2203" ); 
            parser.defineEntityReplacementText( "empty", "\u2205" ); 
            parser.defineEntityReplacementText( "nabla", "\u2207" ); 
            parser.defineEntityReplacementText( "isin", "\u2208" ); 
            parser.defineEntityReplacementText( "notin", "\u2209" ); 
            parser.defineEntityReplacementText( "ni", "\u220b" ); 
            parser.defineEntityReplacementText( "prod", "\u220f" ); 
            parser.defineEntityReplacementText( "sum", "\u2211" ); 
            parser.defineEntityReplacementText( "minus", "\u2212" ); 
            parser.defineEntityReplacementText( "lowast", "\u2217" ); 
            parser.defineEntityReplacementText( "radic", "\u221a" ); 
            parser.defineEntityReplacementText( "prop", "\u221d" ); 
            parser.defineEntityReplacementText( "infin", "\u221e" ); 
            parser.defineEntityReplacementText( "ang", "\u2220" ); 
            parser.defineEntityReplacementText( "and", "\u2227" ); 
            parser.defineEntityReplacementText( "or", "\u2228" ); 
            parser.defineEntityReplacementText( "cap", "\u2229" ); 
            parser.defineEntityReplacementText( "cup", "\u222a" ); 
            parser.defineEntityReplacementText( "int", "\u222b" ); 
            parser.defineEntityReplacementText( "there4", "\u2234" ); 
            parser.defineEntityReplacementText( "sim", "\u223c" ); 
            parser.defineEntityReplacementText( "cong", "\u2245" ); 
            parser.defineEntityReplacementText( "asymp", "\u2248" ); 
            parser.defineEntityReplacementText( "ne", "\u2260" ); 
            parser.defineEntityReplacementText( "equiv", "\u2261" ); 
            parser.defineEntityReplacementText( "le", "\u2264" ); 
            parser.defineEntityReplacementText( "ge", "\u2265" ); 
            parser.defineEntityReplacementText( "sub", "\u2282" ); 
            parser.defineEntityReplacementText( "sup", "\u2283" ); 
            parser.defineEntityReplacementText( "nsub", "\u2284" ); 
            parser.defineEntityReplacementText( "sube", "\u2286" ); 
            parser.defineEntityReplacementText( "supe", "\u2287" ); 
            parser.defineEntityReplacementText( "oplus", "\u2295" ); 
            parser.defineEntityReplacementText( "otimes", "\u2297" ); 
            parser.defineEntityReplacementText( "perp", "\u22a5" ); 
            parser.defineEntityReplacementText( "sdot", "\u22c5" ); 
            parser.defineEntityReplacementText( "lceil", "\u2308" ); 
            parser.defineEntityReplacementText( "rceil", "\u2309" ); 
            parser.defineEntityReplacementText( "lfloor", "\u230a" ); 
            parser.defineEntityReplacementText( "rfloor", "\u230b" ); 
            parser.defineEntityReplacementText( "lang", "\u2329" ); 
            parser.defineEntityReplacementText( "rang", "\u232a" ); 
            parser.defineEntityReplacementText( "loz", "\u25ca" ); 
            parser.defineEntityReplacementText( "spades", "\u2660" ); 
            parser.defineEntityReplacementText( "clubs", "\u2663" ); 
            parser.defineEntityReplacementText( "hearts", "\u2665" ); 
            parser.defineEntityReplacementText( "diams", "\u2666" ); 
            
        }
        
        parser.next();
        String encoding = parser.getInputEncoding();
        
        return parseDecorationModel( "project", parser, strict, encoding );
    } //-- DecorationModel read(Reader, boolean) 

    /**
     * Method read
     * 
     * @param reader
     */
    public DecorationModel read(Reader reader)
        throws IOException, XmlPullParserException
    {
        return read( reader, true );
    } //-- DecorationModel read(Reader) 

    /**
     * Returns the state of the "add default entities" flag.
     * 
     * @param addDefaultEntities
     */
    public void setAddDefaultEntities(boolean addDefaultEntities)
    {
        this.addDefaultEntities = addDefaultEntities;
    } //-- void setAddDefaultEntities(boolean) 

}
