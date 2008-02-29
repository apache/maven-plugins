package org.apache.maven.plugins.shade;

/**
 * Hello world!
 *
 */
public class Lib
{
    // simulate the type of call for static loggers that currently fails
    private static final String name = new String( Lib.class.getName() );

    public static final String CONSTANT = "foo.bar/baz";
}
