package foo.bar;

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
