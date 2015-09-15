package org.apache.maven.plugin.compiler.stubs;

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import org.codehaus.plexus.compiler.CompilerConfiguration;
import org.codehaus.plexus.compiler.CompilerError;
import org.codehaus.plexus.compiler.CompilerException;
import org.codehaus.plexus.compiler.CompilerMessage;
import org.codehaus.plexus.compiler.CompilerOutputStyle;
import org.codehaus.plexus.compiler.CompilerResult;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.List;

/**
 * @author Edwin Punzalan
 */
public class CompilerStub
    implements org.codehaus.plexus.compiler.Compiler
{
    private boolean shouldFail;
    private boolean shouldWarn;

    public CompilerStub()
    {
        this( false, false );
    }

    public CompilerStub( boolean shouldFail )
    {
        this( shouldFail, false );
    }

    public CompilerStub( boolean shouldFail, boolean shouldWarn )
    {
        this.shouldFail = shouldFail;
        this.shouldWarn = shouldWarn;
    }

    public CompilerOutputStyle getCompilerOutputStyle()
    {
        return CompilerOutputStyle.ONE_OUTPUT_FILE_FOR_ALL_INPUT_FILES;
    }

    public String getInputFileEnding( CompilerConfiguration compilerConfiguration )
        throws CompilerException
    {
        return "java";
    }

    public String getOutputFileEnding( CompilerConfiguration compilerConfiguration )
        throws CompilerException
    {
        return "class";
    }

    public String getOutputFile( CompilerConfiguration compilerConfiguration )
        throws CompilerException
    {
        return "output-file";
    }

    public boolean canUpdateTarget( CompilerConfiguration compilerConfiguration )
        throws CompilerException
    {
        return false;
    }

    public List<CompilerError> compile( CompilerConfiguration compilerConfiguration )
        throws CompilerException
    {
        File outputDir = new File( compilerConfiguration.getOutputLocation() );

        try
        {
            outputDir.mkdirs();

            File outputFile = new File( outputDir, "compiled.class" );
            if ( !outputFile.exists() && !outputFile.createNewFile() )
            {
                throw new CompilerException( "could not create output file: " + outputFile.getAbsolutePath() );
            }
        }
        catch ( IOException e )
        {
            throw new CompilerException( "An exception occurred while creating output file", e );
        }

        return Collections.singletonList( new CompilerError( "message 1", shouldFail ) );
    }

    public CompilerResult performCompile( CompilerConfiguration compilerConfiguration )
        throws CompilerException
    {
        File outputDir = new File( compilerConfiguration.getOutputLocation() );

        try
        {
            outputDir.mkdirs();

            File outputFile = new File( outputDir, "compiled.class" );
            if ( !outputFile.exists() && !outputFile.createNewFile() )
            {
                throw new CompilerException( "could not create output file: " + outputFile.getAbsolutePath() );
            }
        }
        catch ( IOException e )
        {
            throw new CompilerException( "An exception occurred while creating output file", e );
        }

        return new CompilerResult( !shouldFail,
            Collections.singletonList( new CompilerMessage( "message 1", CompilerMessage.Kind.OTHER ) ) );
    }

    public String[] createCommandLine( CompilerConfiguration compilerConfiguration )
        throws CompilerException
    {
        if ( shouldWarn )
        {
            if ( ! compilerConfiguration.getCustomCompilerArguments().containsKey("-Werror") )
            {
                throw new CompilerException( "Compiler should error on warnings but no Werror flag found" );
            }
        }
        return new String[0];
    }
}
