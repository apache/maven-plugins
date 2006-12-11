package org.apache.maven.plugin.ear;

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import org.codehaus.plexus.util.xml.XMLWriter;

/**
 * The representation of a security-role entry within an
 * application.xml file.
 *
 * @author <a href="snicoll@apache.org">Stephane Nicoll</a>
 * @version $Id: SecurityRole.java 332974 2005-11-13 12:42:44Z snicoll $
 */
class SecurityRole
{

    protected static final String SECURITY_ROLE = "security-role";

    protected static final String ID_ATTRIBUTE = "id";

    protected static final String DESCRIPTION = "description";

    protected static final String ROLE_NAME = "role-name";

    private final String roleName;

    private final String roleNameId;

    private final String roleId;

    private final String description;

    private final String descriptionId;

    public SecurityRole( String roleName, String roleNameId, String roleId, String description, String descriptionId )
    {
        if ( roleName == null )
        {
            throw new NullPointerException( "role-name in security-role element could not be null." );
        }
        this.roleName = roleName;
        this.roleNameId = roleNameId;
        this.roleId = roleId;
        this.description = description;
        this.descriptionId = descriptionId;
    }

    public String getRoleName()
    {
        return roleName;
    }

    public String getRoleNameId()
    {
        return roleNameId;
    }

    public String getRoleId()
    {
        return roleId;
    }

    public String getDescription()
    {
        return description;
    }

    public String getDescriptionId()
    {
        return descriptionId;
    }

    /**
     * Appends the <tt>XML</tt> representation of this security role.
     *
     * @param writer the writer to use
     */
    public void appendSecurityRole( XMLWriter writer )
    {
        writer.startElement( SECURITY_ROLE );

        // role id
        if ( getRoleId() != null )
        {
            writer.addAttribute( ID_ATTRIBUTE, getRoleId() );
        }

        // description
        if ( getDescription() != null )
        {
            writer.startElement( DESCRIPTION );
            if ( getDescriptionId() != null )
            {
                writer.addAttribute( ID_ATTRIBUTE, getDescriptionId() );
            }
            writer.writeText( getDescription() );
            writer.endElement();

        }

        // role name
        writer.startElement( ROLE_NAME );
        if ( getRoleNameId() != null )
        {
            writer.addAttribute( ID_ATTRIBUTE, getRoleNameId() );
        }
        writer.writeText( getRoleName() );
        writer.endElement();

        // end of security-role
        writer.endElement();
    }

    public String toString()
    {
        return "Security role " + getRoleName();
    }


}
