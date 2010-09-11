package org.apache.maven;

import org.apache.log4j.Logger;

public class App 
{
    public static void main( String[] args )
    {
        Logger mylogger = Logger.getLogger("MyLogger");
        mylogger.info( "Hello World!" );
    }
}
