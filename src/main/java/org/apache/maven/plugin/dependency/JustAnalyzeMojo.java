package org.apache.maven.plugin.dependency;

/**
 * This goal is the same as analyze except it won't call test-compile first. You must have
 * already executed test-compile (or a later phase) to get accurate results.
 * 
 * @author <a href="mailto:brianf@apache.org">Brian Fox</a>
 * @version $Id: AnalyzeMojo.java 523899 2007-03-30 01:07:08Z brianf $
 * @goal just-analyze
 * @requiresDependencyResolution test
 * @execute phase="validate"
 * @since 2.0-alpha-5
 */
public class JustAnalyzeMojo
    extends AnalyzeMojo
{

}
