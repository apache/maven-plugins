package org.apache.maven.plugin.install;

import junit.framework.Assert;
import junit.framework.TestCase;

import java.io.ByteArrayInputStream;

public class DualDigesterTest  extends TestCase
{
    public void testGetMd5()
        throws Exception
    {
        DualDigester dualDigester = new DualDigester();
        dualDigester.calculate( new ByteArrayInputStream( "A Dog And A Cat".getBytes() ) );
        Assert.assertEquals( "39bc6b34be719cab3a3dc922445aae7c", dualDigester.getMd5() );
        Assert.assertEquals( "d07b1e7ecc7986b3f1126ddf1b67e3601ec362a9", dualDigester.getSha1() );
        dualDigester.calculate( new ByteArrayInputStream( "Yep, we do it again".getBytes() ) );
        Assert.assertEquals( "8cd83a9cbbd7076f668c2bcc0379ed49", dualDigester.getMd5() );
        Assert.assertEquals( "194ebcb8d168cffdc25c3b854d6187b568cf6273", dualDigester.getSha1() );
    }

}
