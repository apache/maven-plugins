package org.apache.maven.plugins.release.versions;

public interface VersionInfo
    extends Comparable
{
    /** Returns a string representing the version without modification.
     * 
     * @return
     */
    public String getVersionString();

    /** Returns a string representing the version with a snapshot specification
     * 
     * @return
     */
    public String getSnapshotVersionString();

    /** Returns a string representing the version without a snapshot specification.
     * 
     * @return
     */
    public String getReleaseVersionString();

    /** Returns a {@link VersionInfo} object which represents the next version of this object.
     * 
     * @return
     */
    public VersionInfo getNextVersion();

    /** Returns whether this represents a snapshot version. ("xxx-SNAPSHOT");
     * 
     * @return
     */
    public boolean isSnapshot();
}
