package org.apache.maven.plugin.reactor;

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
import java.util.Iterator;
import java.util.LinkedList;

/**
 * Calculates relative paths
 * @author <a href="mailto:dfabulich@apache.org">Dan Fabulich</a>
 *
 */
class RelativePather {
    /**
     * Calculates a relative path
     * @param context the "current" context directory
     * @param dest the directory to be described by a relative path
     * @return a relative path from the context directory to the dest directory
     */
    public static String getRelativePath(File context, File dest) {
        LinkedList contextChunks = getPathChunks(context);
        LinkedList destChunks = getPathChunks(dest);
        if (!contextChunks.getFirst().equals(destChunks.getFirst())) throw new DifferentRootsException("Roots differ");
        int count = 0;
        Iterator contextChunker = contextChunks.iterator();
        Iterator destChunker = destChunks.iterator();
        String contextChunk = (String) contextChunker.next();
        String destChunk = (String) destChunker.next();
        boolean pathsDiffer = false;
        while (true) {
            count++;
            if (!contextChunker.hasNext()) break;
            if (!destChunker.hasNext()) break;
            contextChunk = (String) contextChunker.next();
            destChunk = (String) destChunker.next();
            if (!contextChunk.equals(destChunk)) {
                pathsDiffer = true;
                break;
            }
        }
        
        // the paths agree for the first N chunks
        
        StringBuffer relativePath = new StringBuffer();
        
        if (count < contextChunks.size()) {
            int dotDotCount = contextChunks.size() - count;
            for (int i = 0; i < dotDotCount; i++) {
                relativePath.append("..");
                // omit trailing slash
                if (i < dotDotCount -1) {
                    relativePath.append(File.separatorChar);
                }
            }
        }
        if (pathsDiffer) {
            if (relativePath.length() > 0) {
                relativePath.append(File.separatorChar);
            }
            relativePath.append(destChunk);
        }
        while (destChunker.hasNext()) {
            if (relativePath.length() > 0) {
                relativePath.append(File.separatorChar);
            }
            relativePath.append(destChunker.next());
        }
        
        return relativePath.toString();
    }
    
    private static LinkedList getPathChunks(File f) {
        LinkedList l = new LinkedList();
        while (f.getParentFile() != null) {
            l.addFirst(f.getName());
            f = f.getParentFile();
        }
        l.addFirst(f.getAbsolutePath());
        return l;
    }
    
    static class DifferentRootsException extends RuntimeException {
        private static final long serialVersionUID = 1L;

        public DifferentRootsException() {
            super();
        }

        public DifferentRootsException(String message, Throwable cause) {
            super(message, cause);
        }

        public DifferentRootsException(String message) {
            super(message);
        }

        public DifferentRootsException(Throwable cause) {
            super(cause);
        }
        
    }
}
