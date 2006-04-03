package org.apache.maven.plugin.idea.stubs;

/**
 * @author Edwin Punzalan
 */
public class TestCounter
{
    public static int ctr = 0;

    public static int nextCount()
    {
        return ++ctr;
    }

    public static int currentCount()
    {
        return ctr;
    }
}
