package org.apache.maven.it0001;

import junit.framework.TestCase;

public class PersonTwoTest
    extends TestCase
{
    public void testPerson()
    {
        Person person = new Person();
        
        person.setName( "foo" );
        
        assertEquals( "foo", person.getName() );
    }
}
