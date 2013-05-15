package org.apache.maven.test;

import org.apache.maven.component.api.*;

public class TestComponent implements Component
{

    private Component component;

    public String getId()
    {
        return "test-" + component.getId();
    }

}
