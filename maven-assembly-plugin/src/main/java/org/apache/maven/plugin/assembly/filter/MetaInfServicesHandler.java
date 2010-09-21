/*
 *  Copyright (C) 2010 John Casey.
 *  
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU Affero General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *  
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU Affero General Public License for more details.
 *  
 *  You should have received a copy of the GNU Affero General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.apache.maven.plugin.assembly.filter;

import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.components.io.fileselectors.FileInfo;

@Component( role = ContainerDescriptorHandler.class, hint = "metaInf-services" )
public class MetaInfServicesHandler
    extends AbstractLineAggregatingHandler
{

    private static final String SERVICES_PATH_PREFIX = "/META-INF/services/";

    @Override
    protected String getOutputPathPrefix( final FileInfo fileInfo )
    {
        return SERVICES_PATH_PREFIX;
    }

    @Override
    protected boolean fileMatches( final FileInfo fileInfo )
    {
        return fileInfo.getName().startsWith( SERVICES_PATH_PREFIX )
                        || fileInfo.getName().startsWith( "META-INF/services" );
    }

}
