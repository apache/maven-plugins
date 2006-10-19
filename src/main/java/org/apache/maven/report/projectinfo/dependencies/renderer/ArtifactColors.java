package org.apache.maven.report.projectinfo.dependencies.renderer;

/*
 * Copyright 2001-2006 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/**
 * ArtifactColors 
 *
 * @author <a href="mailto:joakim@erdfelt.com">Joakim Erdfelt</a>
 * @version $Id$
 */
public class ArtifactColors
{
    private String backgroundColor;

    private String borderColor;

    private String labelColor;
    
    public ArtifactColors(String border, String background, String label)
    {
        this.borderColor = border;
        this.backgroundColor = background;
        this.labelColor = label;
    }

    public String getBackgroundColor()
    {
        return backgroundColor;
    }

    public void setBackgroundColor( String backgroundColor )
    {
        this.backgroundColor = backgroundColor;
    }

    public String getBorderColor()
    {
        return borderColor;
    }

    public void setBorderColor( String borderColor )
    {
        this.borderColor = borderColor;
    }

    public String getLabelColor()
    {
        return labelColor;
    }

    public void setLabelColor( String labelColor )
    {
        this.labelColor = labelColor;
    }

}
