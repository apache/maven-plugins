package org.apache.maven.plugins.site.test.ftp;

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

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.ftpserver.FtpServer;
import org.apache.ftpserver.FtpServerFactory;
import org.apache.ftpserver.ftplet.Authority;
import org.apache.ftpserver.ftplet.FtpException;
import org.apache.ftpserver.ftplet.UserManager;
import org.apache.ftpserver.impl.FtpIoSession;
import org.apache.ftpserver.listener.Listener;
import org.apache.ftpserver.listener.ListenerFactory;
import org.apache.ftpserver.usermanager.PropertiesUserManagerFactory;
import org.apache.ftpserver.usermanager.SaltedPasswordEncryptor;
import org.apache.ftpserver.usermanager.impl.BaseUser;
import org.apache.ftpserver.usermanager.impl.WritePermission;

public class SimpleFtpServer
{

    private SimpleFtpServer()
    {
        super();
    }

    public static void main( String[] args )
        throws Exception
    {
        // FTP server : See http://mina.apache.org/ftpserver-project/embedding_ftpserver.html
        FtpServerFactory serverFactory = new FtpServerFactory();
        ListenerFactory factory = new ListenerFactory();
        factory.setPort( 2121 );

        Listener listener = factory.createListener();
        serverFactory.addListener( "default", listener );

        // Associate user/password/home
        File tmpConfigFile = File.createTempFile( "ftp-config-", ".properties" );
        File home = new File( "target/ftp-home/" );
        home.mkdirs();
        serverFactory.setUserManager( addFtpUser( "guest", "guest", home.getPath(), tmpConfigFile ) );

        // start server
        FtpServer ftpServer = serverFactory.createServer();
        ftpServer.start();

        // The FTP server is waiting the number of sessions close and auto stop (or if max time reached)
        // On a clean build, the download of Maven dependencies between goals could be long ... so the max time should
        // be important
        final long sessionsToWait = 2;
        final long maxWait = System.currentTimeMillis() + ( 10 * 60 * 1000 );

        Set<Long> sessionsConnected = new HashSet<Long>();

        while ( !ftpServer.isStopped() )
        {
            Thread.sleep( 1000 ); // NOSONAR

            for ( FtpIoSession session : listener.getActiveSessions() )
            {
                sessionsConnected.add( session.getCreationTime() );
            }

            if ( ( listener.getActiveSessions().size() == 0 && sessionsConnected.size() >= sessionsToWait )
                || System.currentTimeMillis() > maxWait )
            {
                ftpServer.stop();
                tmpConfigFile.delete();
            }
        }

    }

    private static UserManager addFtpUser( final String username, final String password, final String ftproot,
                                           final File userTmpConfigFile )
                                               throws FtpException
    {
        PropertiesUserManagerFactory userManagerFactory = new PropertiesUserManagerFactory();
        userManagerFactory.setFile( userTmpConfigFile );
        userManagerFactory.setPasswordEncryptor( new SaltedPasswordEncryptor() );
        UserManager um = userManagerFactory.createUserManager();
        BaseUser user = new BaseUser();
        user.setName( username );
        user.setPassword( password );
        user.setHomeDirectory( ftproot );
        List<Authority> authorities = new ArrayList<Authority>();
        authorities.add( new WritePermission() );
        user.setAuthorities( authorities );
        um.save( user );
        return um;
    }

}
