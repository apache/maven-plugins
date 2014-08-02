package org.sugis.maven;

import java.time.Clock;
import java.time.Instant;
import java.util.function.Supplier;
import org.slf4j.LoggerFactory;

public interface Java8Slf4jDep
{
    default void wat()
    {
        Supplier<Instant> time = Clock.systemUTC()::instant;
        LoggerFactory.getLogger(Java8Slf4jDep.class).info(time.get().toString());
    }
}