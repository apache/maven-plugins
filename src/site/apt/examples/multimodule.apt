 ------
 Multi-module Sites
 ------
 Vincent Siveton
 <vincent.siveton@gmail.com>
 Maria Odea Ching
 Dennis Lundberg
 Lukas Theussl
 ------
 2011-01-18
 ------

 ~~ Licensed to the Apache Software Foundation (ASF) under one
 ~~ or more contributor license agreements.  See the NOTICE file
 ~~ distributed with this work for additional information
 ~~ regarding copyright ownership.  The ASF licenses this file
 ~~ to you under the Apache License, Version 2.0 (the
 ~~ "License"); you may not use this file except in compliance
 ~~ with the License.  You may obtain a copy of the License at
 ~~
 ~~   http://www.apache.org/licenses/LICENSE-2.0
 ~~
 ~~ Unless required by applicable law or agreed to in writing,
 ~~ software distributed under the License is distributed on an
 ~~ "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 ~~ KIND, either express or implied.  See the License for the
 ~~ specific language governing permissions and limitations
 ~~ under the License.

 ~~ NOTE: For help with the syntax of this file, see:
 ~~ http://maven.apache.org/doxia/references/apt-format.html


Building multi-module sites

  When running <<<mvn site>>> on a top level project, Maven will build all sites for every module
  defined in the <<<modules>>> section of the pom individually. Note that each site is
  generated in each individual project's target location. As a result, relative links between
  different modules will NOT work. They will however work when you deploy or stage the site.

* Staging and deploying

  To preview the whole tree of a multi-module site, you can use the
  <<<{{{../stage-mojo.html}site:stage}}>>> goal. This will
  copy individual sites to their proper place at the staging location.

  <<<{{{../deploy-mojo.html}site:deploy}}>>> does the same thing as <<<site:stage>>>,
  but uses the url defined in the <<<\<distributionManagement\>>>> element of the pom
  as deployment location. This is usually a remote url, but both remote protocols like
  <<<scp>>> and local protocols like <<<file>>> are supported.

  Finally, <<<{{{../stage-deploy-mojo.html}site:stage-deploy}}>>> does the same thing as
  <<<site:deploy>>> but uses the <<<stagingDirectory>>> parameter as deployment location.
  This can be used to deploy the site to a remote staging area (<<<site:stage>>> is always local).

  <<Notes:>>

  * If subprojects inherit the (distribution) site URL from a parent POM, they will automatically
  append their <artifactId> to form their effective deployment location.
  This goes for both the project url and the url defined in the
  <<<\<distributionManagement\>>>> element of the pom.

  * If your multi-module tree does not
  follow the Maven conventions, or if module directories are named differently than
  module artifacts, you have to specify the url's for <<each>> child project.
  See also <<<{{{../faq.html#Use_of_url}How does the Site Plugin use the \<url\> element in the POM?}}>>>.

  * The pom.xml of the topmost project in a multi-module build must define the distributionManagement URL
  element (called "rootURL" hereafter). The rootURL must be the topmost distributionManagement URL
  in the multi-module project, implying that any distributionManagement URL defined within another
  project in a multimodule build must start with the rootURL and append unique paths
  (to be situated "below" the rootURL).

  * All projects in multi-module builds must define unique distributionManagement url elements,
  below/under the root distributionManagement URL in terms of URL path.

  * If the artifactId and module name (i.e. directory name) are not identical for any project within a
  multi-module build, the distributionManagement URL element must be defined in the pom.xml file in the project.

* Inheritance

  Site descriptors are inherited along the same lines as project descriptors
  are. If you want your project's site descriptor to be inherited, you need to
  attach it to the project's main artifact. You use the
  <<<{{{../attach-descriptor-mojo.html}site:attach-descriptor}}>>>
  goal to attach the site descriptor to your project's main artifact.

  In Maven 3 you must attach the site descriptor yourself, even for projects
  with packaging set to "pom".

  By default, all the parent descriptor settings are inherited with the only exception of menus, see below.
  From the body, the links and breadcrumbs accumulate to contain all the parent's
  site descriptor links and breadcrumbs.
  If the parent contains some breadcrumb and the inheriting site descriptor doesn't,
  a breadcrumb with the current site descriptor's name will be added automatically.

  However, it is possible to inherit menus as well. To do so, use the <<<inherit>>> attribute in the site descriptor. This can
  be either <<<top>>> or <<<bottom>>>, indicating where the inherited menu will be placed. For example:

+-----+
<project>
  ...
  <body>
    ...
    <menu name="My Inherited Menu" inherit="top">
      ...
    </menu>
    ...
  </body>
  ...
</project>
+-----+

* Aggregate Reports

  Some reports can be run against the sum of every modules: these are aggregate reports. This is the case of
  maven-javadoc-plugin, maven-jxr-plugin or maven-checkstyle-plugin.

  To benefit from aggregate reports, you must configure a reportSet in the parent pom, and turn inheritance off to avoid
  the aggregate report to be run in modules. For example, with maven-jxr-report:

+--------------+
<project>
  ...
  <reporting>
    <plugins>
      <plugin>
        <groupId>org.apache.maven.plugins</groupId>
        <artifactId>maven-jxr-plugin</artifactId>
        <version>2.3</version>
        <reportSets>
          <reportSet>
            <id>aggregate</id>
            <inherited>false</inherited>
            <reports>
              <report>aggregate</report>
            </reports>
          </reportSet>
        </reportSets>
      </plugin>
    </plugins>
  </reporting>
  ...
</project>
+--------------+
