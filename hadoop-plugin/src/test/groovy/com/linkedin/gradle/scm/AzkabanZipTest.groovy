/*
 * Copyright 2015 LinkedIn Corp.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package com.linkedin.gradle.scm;

import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.file.CopySpec;
import org.gradle.api.tasks.bundling.Zip;
import org.gradle.testfixtures.ProjectBuilder;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class AzkabanZipTest {
  private Project project;
  private ScmPluginTest plugin;
  private Closure closure;
  private Closure baseClosure;
  private File azkabanLibDirectory;
  private Configuration azkabanRuntime;
  private String zipName;
  String zipPath;

  @Before
  public void setup() {
    zipName = "magic";
    project = ProjectBuilder.builder().build();
    project.apply plugin: 'distribution';
    plugin = new ScmPluginTest();
    def Folder = project.getProjectDir();
    azkabanRuntime = project.getConfigurations().create("hadoopRuntime");
    closure = {}
    baseClosure = {}

    /**
     * Create the project structure:
     * AzRoot
     *   \_src
     *       \_test
     *       \_main
     *   \_resources
     *   \_conf
     *       \_jobs
     *   \_custom-lib
     *   \_sample
     */
    project.mkdir(Folder.absolutePath + "/AzRoot");
    project.mkdir(Folder.absolutePath + "/AzRoot/src");
    project.mkdir(Folder.absolutePath + "/AzRoot/resources");
    project.mkdir(Folder.absolutePath + "/AzRoot/conf");
    project.mkdir(Folder.absolutePath + "/AzRoot/conf/jobs");
    project.mkdir(Folder.absolutePath + "/AzRoot/src/main");
    project.mkdir(Folder.absolutePath + "/AzRoot/src/test");
    project.mkdir(Folder.absolutePath + "/AzRoot/custom-lib");
    project.mkdir(Folder.absolutePath + "/AzRoot/sample");

    // Create files for testing
    createFilesForTesting(Folder.absolutePath + "/AzRoot/src/main","java", 5);
    createFilesForTesting(Folder.absolutePath + "/AzRoot/src/test","testjava", 5);
    createFilesForTesting(Folder.absolutePath + "/AzRoot/resources","avro", 5);
    createFilesForTesting(Folder.absolutePath + "/AzRoot/conf/jobs","pig", 5);
    createFilesForTesting(Folder.absolutePath + "/AzRoot/custom-lib","jar", 5);
    createFilesForTesting(Folder.absolutePath + "/AzRoot/sample","txt", 5);
  }

  private void createFilesForTesting(String dir, String ext, int number) {
    number.times {
      def filename = dir +  "/sample" + it.toString() + "." + ext;
      String toWrite = "blah";
      PrintWriter writer = new PrintWriter(filename);
      writer.print(toWrite);
      writer.close();
    }
  }

  @Test
  public void testHadoopZipExtension() {
    closure = {
      from("AzRoot/src") {
        into "src"
        exclude "test"
        include "main/**/**"
      }
      from("AzRoot/resources/") { into "resources" }
      from("AzRoot/conf/jobs/") { }
    }

    Set<String> actual = new HashSet<String>();
    Set<String> expected = new HashSet<String>();
    expected.add("resources/sample0.avro");
    expected.add("resources/sample1.avro");
    expected.add("resources/sample2.avro");
    expected.add("resources/sample3.avro");
    expected.add("resources/sample4.avro");
    expected.add("src/main/sample0.java");
    expected.add("src/main/sample1.java");
    expected.add("src/main/sample2.java");
    expected.add("src/main/sample3.java");
    expected.add("src/main/sample4.java");
    expected.add("sample0.pig");
    expected.add("sample1.pig");
    expected.add("sample2.pig");
    expected.add("sample3.pig");
    expected.add("sample4.pig");
    expected.add("test-sources.zip");

    plugin.apply(project);
    project.getRootProject().tasks["buildSourceZip"].execute();
    zipPath = project.getRootProject().tasks["buildSourceZip"].archivePath.path;

    def task = project.getTasksByName("${zipName}HadoopZip", false);
    def zipTask = task.iterator().next();
    zipTask.execute();

    project.zipTree(((Zip)zipTask).archivePath).getFiles().each { file->
      String str = file.path;
      int testIndex = str.indexOf("test-magic.zip");
      int rootIndex =  str.substring(testIndex).indexOf("/") + testIndex;
      actual.add(file.path.substring(rootIndex + 1, str.length()));
    }

    Assert.assertEquals(expected, actual);
  }

  @Test
  public void testBasicZip() {
    Set<String> actual = new HashSet<String>();
    Set<String> expected = new HashSet<String>();
    expected.add("test-sources.zip");

    plugin.apply(project);
    project.getRootProject().tasks["buildSourceZip"].execute();
    zipPath = project.getRootProject().tasks["buildSourceZip"].archivePath.path;

    def task = project.getTasksByName("${zipName}HadoopZip", false);
    def zipTask = task.iterator().next();
    zipTask.execute();

    project.zipTree(((Zip)zipTask).archivePath).getFiles().each { file->
      String str = file.path;
      int testIndex = str.indexOf("test-magic.zip");
      int rootIndex =  str.substring(testIndex).indexOf("/") + testIndex;
      actual.add(file.path.substring(rootIndex + 1, str.length()));
    }

    Assert.assertEquals(expected, actual);
  }

  @Test
  public void testHadoopConf() {
    azkabanRuntime.getDependencies().add(project.getDependencies().create(project.fileTree(new File("AzRoot", "custom-lib"))));

    Set<String> actual = new HashSet<String>();
    Set<String> expected = new HashSet<String>();

    expected.add("lib/sample0.jar");
    expected.add("lib/sample1.jar");
    expected.add("lib/sample2.jar");
    expected.add("lib/sample3.jar");
    expected.add("lib/sample4.jar");
    expected.add("test-sources.zip");

    plugin.apply(project);
    project.getRootProject().tasks["buildSourceZip"].execute();
    zipPath = project.getRootProject().tasks["buildSourceZip"].archivePath.path;

    def task = project.getTasksByName("${zipName}HadoopZip", false);
    def zipTask = task.iterator().next();
    zipTask.execute();

    project.zipTree(((Zip)zipTask).archivePath).getFiles().each { file->
      String str = file.path;
      int testIndex = str.indexOf("test-magic.zip");
      int rootIndex =  str.substring(testIndex).indexOf("/") + testIndex;
      actual.add(file.path.substring(rootIndex + 1, str.length()));
    }

    Assert.assertEquals(expected, actual);
  }

  @Test
  public void testBaseCopySpec() {
    // Add sources and resources using baseClosure
    baseClosure = {
      from("AzRoot/src") {
        into "src"
        exclude "test"
        include "main/**/**"
      }
      from("AzRoot/resources/") { into "resources" }
    }

    // Add jobs using zip specific closure.
    closure = {
      from("AzRoot/conf/jobs/") { }
    }

    Set<String> actual = new HashSet<String>();
    Set<String> expected = new HashSet<String>();
    expected.add("resources/sample0.avro");
    expected.add("resources/sample1.avro");
    expected.add("resources/sample2.avro");
    expected.add("resources/sample3.avro");
    expected.add("resources/sample4.avro");
    expected.add("src/main/sample0.java");
    expected.add("src/main/sample1.java");
    expected.add("src/main/sample2.java");
    expected.add("src/main/sample3.java");
    expected.add("src/main/sample4.java");
    expected.add("sample0.pig");
    expected.add("sample1.pig");
    expected.add("sample2.pig");
    expected.add("sample3.pig");
    expected.add("sample4.pig");
    expected.add("test-sources.zip");

    plugin.apply(project);
    project.getRootProject().tasks["buildSourceZip"].execute();
    zipPath = project.getRootProject().tasks["buildSourceZip"].archivePath.path;

    def task = project.getTasksByName("${zipName}HadoopZip", false);
    def zipTask = task.iterator().next();
    zipTask.execute();

    project.zipTree(((Zip)zipTask).archivePath).getFiles().each { file->
      String str = file.path;
      int testIndex = str.indexOf("test-magic.zip");
      int rootIndex = str.substring(testIndex).indexOf("/") + testIndex;
      actual.add(file.path.substring(rootIndex + 1, str.length()));
    }

    Assert.assertEquals(expected, actual);
  }

  @Test
  public void testBaseUnion() {
    // Create a new folder called groovy under src and add 5 groovy files.
    project.mkdir(project.getProjectDir().absolutePath + "/AzRoot/src/groovy");
    createFilesForTesting(project.getProjectDir().absolutePath + "/AzRoot/src/groovy","groovy",5);

    // Add sources and resources using baseClosure
    baseClosure = {
      // Add source in the base but exclude java files and test folder in the specific zip spec.
      from("AzRoot/src") { into "src" }
      // Add resources in base
      from("AzRoot/resources/") { into "resources" }
    }

    // Add jobs using zip specific closure.
    closure = {
      from("AzRoot/conf/jobs/") { }
      // Exclude test directory and all java files and two avro files.
      exclude "test"
      exclude "main/*.java"
      exclude "sample0.avro"
      exclude "sample1.avro"
    }

    Set<String> actual = new HashSet<String>();
    Set<String> expected = new HashSet<String>();
    expected.add("resources/sample2.avro");
    expected.add("resources/sample3.avro");
    expected.add("resources/sample4.avro");
    expected.add("sample0.pig");
    expected.add("sample1.pig");
    expected.add("sample2.pig");
    expected.add("sample3.pig");
    expected.add("sample4.pig");
    expected.add("src/groovy/sample0.groovy");
    expected.add("src/groovy/sample1.groovy");
    expected.add("src/groovy/sample2.groovy");
    expected.add("src/groovy/sample3.groovy");
    expected.add("src/groovy/sample4.groovy");
    expected.add("test-sources.zip");

    plugin.apply(project);
    project.getRootProject().tasks["buildSourceZip"].execute();
    zipPath = project.getRootProject().tasks["buildSourceZip"].archivePath.path;

    def task = project.getTasksByName("${zipName}HadoopZip", false);
    def zipTask = task.iterator().next();
    zipTask.execute();

    project.zipTree(((Zip)zipTask).archivePath).getFiles().each { file->
      String str = file.path;
      int testIndex = str.indexOf("test-magic.zip");
      int rootIndex = str.substring(testIndex).indexOf("/") + testIndex;
      actual.add(file.path.substring(rootIndex + 1, str.length()));
    }

    Assert.assertEquals(expected, actual);
  }

  @Test
  public void testBaseOverwrite() {
    // Create a new folder called groovy under src and add 5 groovy files.
    project.mkdir(project.getProjectDir().absolutePath + "/AzRoot/src/groovy");
    createFilesForTesting(project.getProjectDir().absolutePath + "/AzRoot/src/groovy","groovy",5);

    // dd sources and resources using baseClosure
    baseClosure = {
      // Add source in the base and exclude test folder
      from("AzRoot/src") {
        into "src"
        exclude "test"
      }
      // Add resources in base and exclude sample0.avro, sample1.avro and sample2.avro
      from("AzRoot/resources/") {
        into "resources"
        exclude "sample0.avro"
        exclude "sample1.avro"
        exclude "sample2.avro"
      }
    }

    // Add jobs using zip specific closure.
    closure = {
      from("AzRoot/conf/jobs/") { }

      // Overwrite base spec and include everything from src including test
      from("AzRoot/src") { into "src" }

      // Overwrite base spec and exclude only sample2.avro
      from("AzRoot/resources/") {
        into "resources"
        exclude "sample2.avro"
      }

      // Exclude only java files
      exclude "main/*.java"
    }

    Set<String> actual = new HashSet<String>();
    Set<String> expected = new HashSet<String>();
    expected.add("resources/sample0.avro");
    expected.add("resources/sample1.avro");
    expected.add("resources/sample3.avro");
    expected.add("resources/sample4.avro");
    expected.add("sample0.pig");
    expected.add("sample1.pig");
    expected.add("sample2.pig");
    expected.add("sample3.pig");
    expected.add("sample4.pig");
    expected.add("src/groovy/sample0.groovy");
    expected.add("src/groovy/sample1.groovy");
    expected.add("src/groovy/sample2.groovy");
    expected.add("src/groovy/sample3.groovy");
    expected.add("src/groovy/sample4.groovy");
    expected.add("src/test/sample0.testjava");
    expected.add("src/test/sample1.testjava");
    expected.add("src/test/sample2.testjava");
    expected.add("src/test/sample3.testjava");
    expected.add("src/test/sample4.testjava");
    expected.add("test-sources.zip");

    plugin.apply(project);
    project.getRootProject().tasks["buildSourceZip"].execute();
    zipPath = project.getRootProject().tasks["buildSourceZip"].archivePath.path;

    def task = project.getTasksByName("${zipName}HadoopZip", false);
    def zipTask = task.iterator().next();
    zipTask.execute();

    project.zipTree(((Zip)zipTask).archivePath).getFiles().each { file->
      String str = file.path;
      int testIndex = str.indexOf("test-magic.zip");
      int rootIndex = str.substring(testIndex).indexOf("/") + testIndex;
      actual.add(file.path.substring(rootIndex + 1, str.length()));
    }

    Assert.assertEquals(expected, actual);
  }

  class HadoopZipExtensionTest extends HadoopZipExtension {
    public HadoopZipExtensionTest(Project project) {
      super(project);
    }

    @Override
    public CopySpec getBaseCopySpec() {
      return project.copySpec(baseClosure);
    }

    @Override
    public CopySpec getZipCopySpec(String zipName) {
      return project.copySpec(closure);
    }

    @Override
    public Map<String, CopySpec> getZipMap() {
      Map<String, CopySpec> map = new HashMap<String, CopySpec>();
      map.put(zipName, project.copySpec(closure));
      return map;
    }
  }

  class ScmPluginTest extends ScmPlugin {
    @Override
    Configuration createZipConfiguration(Project project) {
      return azkabanRuntime;
    }

    @Override
    HadoopZipExtension createZipExtension(Project project) {
      HadoopZipExtensionTest extension = new HadoopZipExtensionTest(project);
      extension.libPath = "lib";
      project.extensions.add("hadoopZip", extension);
      return extension;
    }

    @Override
    String getSourceZipFilePath(Project project) {
      return zipPath;
    }
  }
}