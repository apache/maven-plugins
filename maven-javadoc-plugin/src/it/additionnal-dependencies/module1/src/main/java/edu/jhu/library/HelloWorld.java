package edu.jhu.library;

import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.Element;
import net.sf.ehcache.Status;
import net.sf.ehcache.store.MemoryStoreEvictionPolicy;

/**
 * Created by IntelliJ IDEA.
 * User: esm
 * Date: May 17, 2008
 * Time: 11:26:25 AM
 * To change this template use File | Settings | File Templates.
 */
public class HelloWorld
{
    private CacheManager cacheManager = CacheManager.getInstance();

    public String hello( String s )
    {
        return s;
    }
}
