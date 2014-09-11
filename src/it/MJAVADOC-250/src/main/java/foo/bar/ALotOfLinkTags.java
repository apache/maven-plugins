package foo.bar;

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import java.util.HashSet;
import java.util.*;

/**
 * Test linktag parsing in javaDoc of class
 * <ul>
 *   <li>{@link Double} should be resolved by the system classloader</li>
 *   <li>{@link     Float        } should be resolved, despite all the spaces</li>
 *   <li>{@link HashSet#hashCode()} should be resolved by the explicit import</li>
 *   <li>{@link    Hashtable#clear()    } should be resolved by the implicit import</li>
 *   <li>{@link UNKNOWN} should stay they same as it can't be resolved</li>
 *   <li>{@link ANestedClass} should be resolved as it is a nested class</li>
 * </ul>
 */
public class ALotOfLinkTags
{

    /**
     * Test linktag parsing in javaDoc of field
     * <ul>
     *   <li>{@link Double} should be resolved by the system classloader</li>
     *   <li>{@link     Float        } should be resolved, despite all the spaces</li>
     *   <li>{@link HashSet#hashCode()} should be resolved by the explicit import</li>
     *   <li>{@link    Hashtable#clear()    } should be resolved by the implicit import</li>
     *   <li>{@link UNKNOWN} should stay they same as it can't be resolved</li>
     *   <li>{@link ANestedClass} should be resolved as it is a nested class</li>
     * </ul>
     */
    public Object aField;

    /**
     * Test linktag parsing in javaDoc of constructor
     * <ul>
     *   <li>{@link Double} should be resolved by the system classloader</li>
     *   <li>{@link     Float        } should be resolved, despite all the spaces</li>
     *   <li>{@link HashSet#hashCode()} should be resolved by the explicit import</li>
     *   <li>{@link    Hashtable#clear()    } should be resolved by the implicit import</li>
     *   <li>{@link UNKNOWN} should stay they same as it can't be resolved</li>
     *   <li>{@link ANestedClass} should be resolved as it is a nested class</li>
     * </ul>
     */
    public ALotOfLinkTags()
    {
    }

    /**
     * Test linktag parsing in javaDoc of method
     * <ul>
     *   <li>{@link Double} should be resolved by the system classloader</li>
     *   <li>{@link     Float        } should be resolved, despite all the spaces</li>
     *   <li>{@link HashSet#hashCode()} should be resolved by the explicit import</li>
     *   <li>{@link    Hashtable#clear()    } should be resolved by the implicit import</li>
     *   <li>{@link UNKNOWN} should stay they same as it can't be resolved</li>
     *   <li>{@link ANestedClass} should be resolved as it is a nested class</li>
     * </ul>
     */
    public void aMethod( String[] args )
    {
    }
    
    /**
     * Test linktag parsing in javaDoc of nested class
     * <ul>
     *   <li>{@link Double} should be resolved by the system classloader</li>
     *   <li>{@link     Float        } should be resolved, despite all the spaces</li>
     *   <li>{@link HashSet#hashCode()} should be resolved by the explicit import</li>
     *   <li>{@link    Hashtable#clear()    } should be resolved by the implicit import</li>
     *   <li>{@link UNKNOWN} should stay they same as it can't be resolved</li>
     *   <li>{@link ANestedClass} should be resolved as it is a nested class</li>
     * </ul>
     */
    public class ANestedClass {
        
    }
}
