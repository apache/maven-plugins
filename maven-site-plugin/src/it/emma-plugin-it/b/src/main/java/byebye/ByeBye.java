package byebye;

public class ByeBye
{
    public String byebye( String name )
    {
        final String myName = name == null ? "world" : name;
        return "Bye bye " + myName + "!";
    }
}
