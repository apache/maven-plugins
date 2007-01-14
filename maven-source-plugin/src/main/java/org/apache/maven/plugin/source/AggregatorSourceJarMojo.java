package org.apache.maven.plugin.source;

/**
 * @goal aggregate
 * @phase package
 * @aggregator
 * @execute phase="generate-sources"
 */
public class AggregatorSourceJarMojo
    extends SourceJarMojo
{
}
