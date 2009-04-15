/*
 * Crée le 5 janv. 2009 par JB Defard
 *
 */
package org.test;

public aspect Profiling2
{
    pointcut publicOperations()
            : execution(public * *.*(..));

    Object around() : publicOperations()
    {
        long start = System.nanoTime();
        Object ret = proceed();
        long end = System.nanoTime();
        System.out.println(thisJoinPointStaticPart.getSignature()
                            + " took " + (end-start) + " nanoseconds");
        return ret;
    }
}

