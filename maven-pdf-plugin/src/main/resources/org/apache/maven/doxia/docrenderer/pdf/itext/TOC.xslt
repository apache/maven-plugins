<?xml version="1.0" encoding="UTF-8"?>
<!--
Licensed to the Apache Software Foundation (ASF) under one
or more contributor license agreements.  See the NOTICE file
distributed with this work for additional information
regarding copyright ownership.  The ASF licenses this file
to you under the Apache License, Version 2.0 (the
"License"); you may not use this file except in compliance
with the License.  You may obtain a copy of the License at

  http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing,
software distributed under the License is distributed on an
"AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
KIND, either express or implied.  See the License for the
specific language governing permissions and limitations
under the License.
-->

<xsl:stylesheet version="1.0"
    xmlns:xsl="http://www.w3.org/1999/XSL/Transform">

    <!-- TOC Parameters -->
    <xsl:param name="toc.position" />

    <!-- MetaData Parameters -->
    <xsl:param name="meta.author" />
    <xsl:param name="meta.creator" />
    <xsl:param name="meta.creationdate" />
    <xsl:param name="meta.keywords" />
    <xsl:param name="meta.pagesize" />
    <xsl:param name="meta.producer" />
    <xsl:param name="meta.subject" />
    <xsl:param name="meta.title" />

    <!--  FrontPage Parameters -->
    <xsl:param name="cover.author" />
    <xsl:param name="cover.companyLogo" />
    <xsl:param name="cover.companyName" />
    <xsl:param name="cover.date" />
    <xsl:param name="cover.subtitle" />
    <xsl:param name="cover.title" />
    <xsl:param name="cover.type" />
    <xsl:param name="cover.version" />
    <xsl:param name="cover.projectLogo" />
    <xsl:param name="cover.projectName" />

    <xsl:template match="/itext">
      <itext
        author="{$meta.author}"
        creator="{$meta.creator}"
        creationdate="{$meta.creationdate}"
        keywords="{$meta.keywords}"
        pagesize="{$meta.pagesize}"
        producer="{$meta.producer}"
        subject="{$meta.subject}"
        title="{$meta.title}">
        <!-- Start Front page -->
        <paragraph>
            <chunk font="Helvetica" size="12.0" fontstyle="normal"
                blue="0" green="0" red="0">
                <table columns="2" left="false" right="false"
                    top="false" bottom="false" align="Center" width="100%">
                    <row>
                        <cell left="false" right="false" top="false"
                            bottom="false" horizontalalign="Left" verticalalign="middle">
                            <xsl:if test="$cover.companyLogo != ''">
                                <image url="{$cover.companyLogo}" />
                            </xsl:if>
                        </cell>
                        <cell left="false" right="false" top="false"
                          bottom="false" horizontalalign="Left" verticalalign="middle">
                          <xsl:if test="$cover.projectLogo != ''">
                            <image url="{$cover.projectLogo}" />
                          </xsl:if>
                        </cell>
                    </row>
                    <row>
                        <cell left="false" right="false" top="false"
                            bottom="false" horizontalalign="Center" verticalalign="middle"
                            leading="300" colspan="2">
                            <chunk font="Helvetica" size="24.0"
                              fontstyle="bold" blue="0" green="0" red="0"><xsl:value-of select="$cover.title"/></chunk>
                        </cell>
                    </row>
                    <row>
                        <cell left="false" right="false" top="false"
                          bottom="false" horizontalalign="Center" verticalalign="middle"
                          colspan="2">
                          <chunk font="Helvetica" size="24.0"
                            fontstyle="bold" blue="0" green="0" red="0"><xsl:value-of select="$cover.subtitle"/></chunk>
                        </cell>
                    </row>
                    <row>
                        <cell left="false" right="false" top="false"
                            bottom="false" horizontalalign="Left" verticalalign="middle"
                            leading="300">
                            <chunk font="Helvetica" size="16.0"
                              fontstyle="bold" blue="0" green="0" red="0"><xsl:value-of select="$cover.companyName"/></chunk>
                        </cell>
                        <cell left="false" right="false" top="false"
                            bottom="false" horizontalalign="right" verticalalign="middle"
                            leading="300">
                            <chunk font="Helvetica" size="16.0"
                              fontstyle="bold" blue="0" green="0" red="0"><xsl:value-of select="$cover.date"/></chunk>
                        </cell>
                    </row>
                </table>
            </chunk>
        </paragraph>
        <!-- End Front page -->

        <!-- Start TOC -->
        <xsl:choose>
          <xsl:when test="$toc.position = 'start'">
            <newpage />
            <paragraph align="Center">
              <!-- TODO i18N -->
                <chunk font="Helvetica" size="24" fontstyle="bold" blue="0"
                    green="0" red="0">Table Of Contents</chunk>
            </paragraph>
            <paragraph align="Left" leading="24.0">
                <newline />
                <xsl:apply-templates select="*" mode="toc" />
            </paragraph>
          </xsl:when>
          <xsl:otherwise>
            <newpage />
          </xsl:otherwise>
        </xsl:choose>
        <!-- End TOC -->

        <xsl:apply-templates select="*" mode="body" />

        <!-- Start TOC -->
        <xsl:choose>
          <xsl:when test="$toc.position = 'end'">
            <newpage />
            <paragraph align="Center">
              <!-- TODO i18N -->
              <chunk font="Helvetica" size="24" fontstyle="bold" blue="0"
                green="0" red="0">Table Of Contents</chunk>
            </paragraph>
            <paragraph align="Left" leading="24.0">
              <newline />
              <xsl:apply-templates select="*" mode="toc" />
            </paragraph>
          </xsl:when>
          <xsl:otherwise>
            <newpage />
          </xsl:otherwise>
        </xsl:choose>
        <!-- End TOC -->
        </itext>
    </xsl:template>

    <!-- Add TOC -->
    <xsl:template match="chapter|section" mode="toc">
        <xsl:if test="./title/chunk != ''">
            <chunk font="Helvetica" size="16.0" fontstyle="normal"
                blue="255" green="0" red="0"
                localgoto="{generate-id(./title/chunk)}">
                <xsl:number level="multiple" format="1.1.1.1.1."
                    count="section|chapter" />
                <xsl:text> </xsl:text>
                <xsl:value-of select="title/chunk" />
            </chunk>
        </xsl:if>
        <xsl:if test="./title/anchor != ''">
            <xsl:if test="./title/anchor/@name != ''">
                <chunk font="Helvetica" size="16.0" fontstyle="normal"
                    blue="255" green="0" red="0" localgoto="{./title/anchor/@name}">
                    <xsl:number level="multiple" format="1.1.1.1.1."
                        count="section|chapter" />
                    <xsl:text> </xsl:text>
                    <xsl:value-of select="title/anchor/chunk" />
                </chunk>
            </xsl:if>
            <xsl:if test="./title/anchor/@name = ''">
                <chunk font="Helvetica" size="16.0" fontstyle="normal"
                    blue="0" green="0" red="0">
                    <xsl:number level="multiple" format="1.1.1.1.1."
                        count="section|chapter" />
                    <xsl:text> </xsl:text>
                    <xsl:value-of select="title/anchor/chunk" />
                </chunk>
            </xsl:if>
        </xsl:if>

        <newline />
        <xsl:apply-templates select="child::*[name() = 'section']"
            mode="toc" />
    </xsl:template>

    <xsl:template match="chapter/title/chunk|section/title/chunk"
        mode="body">
        <xsl:copy>
            <xsl:attribute name="localdestination">
                <xsl:value-of select="generate-id(.)" />
            </xsl:attribute>
            <xsl:apply-templates select="text()|*" mode="body" />
        </xsl:copy>
    </xsl:template>

    <xsl:template
        match="chapter/title/anchor/chunk|section/title/anchor/chunk"
        mode="body">
        <xsl:copy>
            <xsl:apply-templates select="@*" mode="body" />
            <xsl:apply-templates select="text()|*" mode="body" />
        </xsl:copy>
    </xsl:template>

    <xsl:template match="*|text()|@*" mode="body">
        <xsl:copy>
            <xsl:apply-templates select="*|text()|@*" mode="body" />
        </xsl:copy>
    </xsl:template>

    <!--  Update depth and numberdepth -->
    <xsl:template match="chapter" mode="body">
        <xsl:copy>
            <xsl:attribute name="depth">1</xsl:attribute>
            <xsl:attribute name="numberdepth">1</xsl:attribute>
            <xsl:apply-templates select="text()|*" mode="body" />
        </xsl:copy>
    </xsl:template>

    <xsl:template match="section" mode="body">
        <xsl:copy>
            <xsl:variable name="depth.from.context"
                select="count(ancestor::*)" />
            <xsl:attribute name="depth">
                <xsl:value-of select="$depth.from.context" />
            </xsl:attribute>
            <xsl:attribute name="numberdepth">
                <xsl:value-of select="$depth.from.context" />
            </xsl:attribute>
            <xsl:apply-templates select="text()|*" mode="body" />
        </xsl:copy>
    </xsl:template>
</xsl:stylesheet>
