package org.apache.maven.plugin.acr.it.it02;


public class DependencyTest
{

    private final SampleApp sampleApp;

    public DependencyTest( SampleApp sampleApp )
    {
        this.sampleApp = sampleApp;
    }


    public String doIt( String t )
    {
        return sampleApp.sayHello( t );
    }

}