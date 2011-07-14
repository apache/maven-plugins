package hello;

public class Hello
{
    public String hello( String name )
    {
        final String myName = name == null ? "world" : name;
        return "Hello " + myName + "!";
    }
}
