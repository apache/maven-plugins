package org.apache.maven.plugin.assembly.aspect.log;

import org.apache.maven.plugin.assembly.archive.phase.AssemblyArchiverPhase;

public aspect PhaseTracingAspect
{

    private boolean timingsEnabled()
    {
        return "true".equals( System.getProperty( "assembly.tracePhases", "false" ) ) ||
                "true".equals( System.getProperty( "assembly.traceAll", "false" ) );
    }

    private pointcut phaseExecution(): execution( * AssemblyArchiverPhase+.execute( .. ) );

    void around(): phaseExecution()
    {
        if ( timingsEnabled() )
        {
            Class phaseClass = thisJoinPointStaticPart.getSignature().getDeclaringType();
            String phaseName = phaseClass.getName().substring( phaseClass.getPackage().getName().length() + 1 );


            System.out.println( "Entering assembly phase: " + phaseName );

            proceed();

            System.out.println( "Exiting phase: " + phaseName );
        }
        else
        {
            proceed();
        }
    }

}
