package org.apache.maven.doxia.module.fo;

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

import javax.swing.text.html.HTML.Tag;

import org.apache.maven.doxia.markup.XmlMarkup;

/**
 * List of <code>FO</code> markups.
 *
 * @author ltheussl
 * @version $Id: FoMarkup.java 1633964 2014-10-23 22:01:01Z hboutemy $
 * @since 1.1
 */
@SuppressWarnings( "checkstyle:interfaceistype" )
public interface FoMarkup
    extends XmlMarkup
{
    /** FO namespace: "http://www.w3.org/1999/XSL/Format" */
    String FO_NAMESPACE = "http://www.w3.org/1999/XSL/Format";

    // ----------------------------------------------------------------------
    // Specific FO tags
    // ----------------------------------------------------------------------

    /** FO tag for <code>root</code>. */
    Tag ROOT_TAG = new Tag()
    {
        /** {@inheritDoc} */
        public String toString()
        {
            return "root";
        }
    };

    /** FO tag for <code>layout-master-set</code>. */
    Tag LAYOUT_MASTER_SET_TAG = new Tag()
    {
        /** {@inheritDoc} */
        public String toString()
        {
            return "layout-master-set";
        }
    };

    /** FO tag for <code>simple-page-master</code>. */
    Tag SIMPLE_PAGE_MASTER_TAG = new Tag()
    {
        /** {@inheritDoc} */
        public String toString()
        {
            return "simple-page-master";
        }
    };

    /** FO tag for <code>region-body</code>. */
    Tag REGION_BODY_TAG = new Tag()
    {
        /** {@inheritDoc} */
        public String toString()
        {
            return "region-body";
        }
    };

    /** FO tag for <code>region-before</code>. */
    Tag REGION_BEFORE_TAG = new Tag()
    {
        /** {@inheritDoc} */
        public String toString()
        {
            return "region-before";
        }
    };

    /** FO tag for <code>region-after</code>. */
    Tag REGION_AFTER_TAG = new Tag()
    {
        /** {@inheritDoc} */
        public String toString()
        {
            return "region-after";
        }
    };

    /** FO tag for <code>static-content</code>. */
    Tag STATIC_CONTENT_TAG = new Tag()
    {
        /** {@inheritDoc} */
        public String toString()
        {
            return "static-content";
        }
    };


    /** FO tag for <code>page-sequence</code>. */
    Tag PAGE_SEQUENCE_TAG = new Tag()
    {
        /** {@inheritDoc} */
        public String toString()
        {
            return "page-sequence";
        }
    };

    /** FO tag for <code>flow</code>. */
    Tag FLOW_TAG = new Tag()
    {
        /** {@inheritDoc} */
        public String toString()
        {
            return "flow";
        }
    };

    /** FO tag for <code>block</code>. */
    Tag BLOCK_TAG = new Tag()
    {
        /** {@inheritDoc} */
        public String toString()
        {
            return "block";
        }
    };

    /** FO tag for <code>list-block</code>. */
    Tag LIST_BLOCK_TAG = new Tag()
    {
        /** {@inheritDoc} */
        public String toString()
        {
            return "list-block";
        }
    };

    /** FO tag for <code>list-item</code>. */
    Tag LIST_ITEM_TAG = new Tag()
    {
        /** {@inheritDoc} */
        public String toString()
        {
            return "list-item";
        }
    };

    /** FO tag for <code>list-item-body</code>. */
    Tag LIST_ITEM_BODY_TAG = new Tag()
    {
        /** {@inheritDoc} */
        public String toString()
        {
            return "list-item-body";
        }
    };

    /** FO tag for <code>list-item-label</code>. */
    Tag LIST_ITEM_LABEL_TAG = new Tag()
    {
        /** {@inheritDoc} */
        public String toString()
        {
            return "list-item-label";
        }
    };

    /** FO tag for <code>table-and-caption</code>. */
    Tag TABLE_AND_CAPTION_TAG = new Tag()
    {
        /** {@inheritDoc} */
        public String toString()
        {
            return "table-and-caption";
        }
    };

    /** FO tag for <code>table</code>. */
    Tag TABLE_TAG = new Tag()
    {
        /** {@inheritDoc} */
        public String toString()
        {
            return "table";
        }
    };

    /** FO tag for <code>table-body</code>. */
    Tag TABLE_BODY_TAG = new Tag()
    {
        /** {@inheritDoc} */
        public String toString()
        {
            return "table-body";
        }
    };

    /** FO tag for <code>table-column</code>. */
    Tag TABLE_COLUMN_TAG = new Tag()
    {
        /** {@inheritDoc} */
        public String toString()
        {
            return "table-column";
        }
    };

    /** FO tag for <code>table-row</code>. */
    Tag TABLE_ROW_TAG = new Tag()
    {
        /** {@inheritDoc} */
        public String toString()
        {
            return "table-row";
        }
    };

    /** FO tag for <code>table-cell</code>. */
    Tag TABLE_CELL_TAG = new Tag()
    {
        /** {@inheritDoc} */
        public String toString()
        {
            return "table-cell";
        }
    };

    /** FO tag for <code>table-caption</code>. */
    Tag TABLE_CAPTION_TAG = new Tag()
    {
        /** {@inheritDoc} */
        public String toString()
        {
            return "table-caption";
        }
    };

    /** FO tag for <code>inline</code>. */
    Tag INLINE_TAG = new Tag()
    {
        /** {@inheritDoc} */
        public String toString()
        {
            return "inline";
        }
    };

    /** FO tag for <code>basic-link</code>. */
    Tag BASIC_LINK_TAG = new Tag()
    {
        /** {@inheritDoc} */
        public String toString()
        {
            return "basic-link";
        }
    };

    /** FO tag for <code>leader</code>. */
    Tag LEADER_TAG = new Tag()
    {
        /** {@inheritDoc} */
        public String toString()
        {
            return "leader";
        }
    };

    /** FO tag for <code>page-number</code>. */
    Tag PAGE_NUMBER_TAG = new Tag()
    {
        /** {@inheritDoc} */
        public String toString()
        {
            return "page-number";
        }
    };

    /** FO tag for <code>page-number-citation</code>. */
    Tag PAGE_NUMBER_CITATION_TAG = new Tag()
    {
        /** {@inheritDoc} */
        public String toString()
        {
            return "page-number-citation";
        }
    };

    /** FO tag for <code>bookmark-tree</code>. */
    Tag BOOKMARK_TREE_TAG = new Tag()
    {
        /** {@inheritDoc} */
        public String toString()
        {
            return "bookmark-tree";
        }
    };

    /** FO tag for <code>bookmark</code>. */
    Tag BOOKMARK_TAG = new Tag()
    {
        /** {@inheritDoc} */
        public String toString()
        {
            return "bookmark";
        }
    };

    /** FO tag for <code>bookmark-title</code>. */
    Tag BOOKMARK_TITLE_TAG = new Tag()
    {
        /** {@inheritDoc} */
        public String toString()
        {
            return "bookmark-title";
        }
    };
}
