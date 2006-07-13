/*
 * Copyright  2000-2004 The Apache Software Foundation
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */

package org.apache.maven.plugin.assembly.mappers;

import java.util.Enumeration;
import java.util.Properties;
import org.apache.maven.plugins.assembly.model.Mapper;
import org.apache.maven.plugin.assembly.mappers.FileNameMapper;

/**
 * Element to define a FileNameMapper.
 *
 */
public class MapperUtil {

    private Properties implementations;
    protected String from = null;
    protected String to = null;
    protected String type = "identity";
    protected String classname = null;


    /**
     * Construct a new <CODE>MapperUtil</CODE> element.
     * @param p   the owning Ant <CODE>Project</CODE>.
     */
    public MapperUtil(Mapper mapper) {
        initializeProperties();
        this.type = mapper.getType();
        this.from = mapper.getFrom();
        this.to = mapper.getTo();
        this.classname = mapper.getClassname();
    }


    /**
     * Initializes a properties object to store the built-in classnames.
     */
    public void initializeProperties () {
        implementations = new Properties();
        implementations.setProperty("identity",
                                    "org.apache.maven.plugin.assembly.mappers.IdentityMapper");
        implementations.setProperty("flatten",
                                    "org.apache.maven.plugin.assembly.mappers.FlatFileNameMapper");
        implementations.setProperty("glob",
                                    "org.apache.maven.plugin.assembly.mappers.GlobPatternMapper");
        implementations.setProperty("merge",
                                    "org.apache.maven.plugin.assembly.mappers.MergingMapper");
        implementations.setProperty("regexp",
                                    "org.apache.maven.plugin.assembly.mappers.RegexpPatternMapper");
        implementations.setProperty("package",
                                    "org.apache.maven.plugin.assembly.mappers.PackageNameMapper");
        implementations.setProperty("unpackage",
                                    "org.apache.maven.plugin.assembly.mappers.UnPackageNameMapper");
    }





    /**
     * Returns a fully configured FileNameMapper implementation.
     */
    public FileNameMapper getImplementation() throws Exception {
        if (type == null && classname == null) {
            throw new Exception(
                               "nested mapper or "
                               + "one of the attributes type or classname is required");
        }

        if (type != null && classname != null) {
            throw new Exception(
                               "must not specify both type and classname attribute");
        }
        if (type != null) {
            classname = implementations.getProperty(type);
        }

        try {
            FileNameMapper m
            = (FileNameMapper) Class.forName(classname).newInstance();

            m.setFrom(from);
            m.setTo(to);

            return m;
        }
        catch (ClassNotFoundException cnfe) {
            throw cnfe;
        }
        catch (Throwable t) {
            throw new Exception(t);
        }
    }



    public Enumeration getTypes() {
        return implementations.propertyNames();
    }

    public Properties getImplementations(){
        return this.implementations;
    }


}
