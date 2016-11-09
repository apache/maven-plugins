/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import java.nio.file.Files

import static org.apache.commons.io.FileUtils.write;

File root = new File( basedir, "symlinks" );
root.mkdirs();

File srcDir = new File(root, "/src");
srcDir.mkdirs();
File target = new File(srcDir, "targetDir");
target.mkdirs();
write(new File(target, "targetFile.txt"), "a regular File payload");
File aRegularDir = new File(srcDir, "aRegularDir");
aRegularDir.mkdirs();
write(new File(aRegularDir, "aRegularFile.txt"), "a regular File payload");

File dirOnTheOutside = new File(root, "dirOnTheOutside");
dirOnTheOutside.mkdirs();
write(new File(dirOnTheOutside, "FileInDirOnTheOutside.txt"), "a file in dir on the outside");
write(new File(root, "onTheOutside.txt"), "A file on the outside");
write(new File(srcDir, "fileR.txt"), "FileR payload");
write(new File(srcDir, "fileW.txt"), "FileW payload");
write(new File(srcDir, "fileX.txt"), "FileX payload");
// todo: set file attributes (not used here)

Files.createSymbolicLink(new File(srcDir, "symDir").toPath(), new File("targetDir").toPath());

Files.createSymbolicLink(new File(srcDir, "symLinkToDirOnTheOutside").toPath(), new File("../dirOnTheOutside").toPath());

Files.createSymbolicLink(new File(srcDir, "symLinkToFileOnTheOutside").toPath(), new File("../onTheOutside.txt").toPath());

Files.createSymbolicLink(new File(srcDir, "symR").toPath(), new File("fileR.txt").toPath());

Files.createSymbolicLink(new File(srcDir, "symW").toPath(), new File("fileW.txt").toPath());

Files.createSymbolicLink(new File(srcDir, "symX").toPath(), new File("fileX.txt").toPath());
return true;
