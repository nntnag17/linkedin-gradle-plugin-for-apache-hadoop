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
package com.linkedin.gradle.liscm;

import com.linkedin.gradle.scm.HadoopZipExtension;
import com.linkedin.gradle.scm.ScmPlugin;

import org.apache.tools.ant.filters.ReplaceTokens;

import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.api.tasks.bundling.Zip

/**
 * LinkedIn-specific customizations to the SCM Plugin.
 */
class LiScmPlugin extends ScmPlugin {
  /**
   * Applies the ScmPlugin.
   * <p>
   * In the LiScmPlugin, any azkabanZip tasks (from the li-azkaban2 plugin in RUM) are made to be
   * dependent on the buildScmMetadata task, and to include the metadata JSON file.
   *
   * @param project The Gradle project
   */
  @Override
  void apply(Project project) {
    // Enable users to skip the plugin
    if (project.hasProperty("disableScmPlugin")) {
      println("ScmPlugin disabled");
      return;
    }

    super.apply(project);

    if (!project.hasProperty("disableLiAzkabanPlugin2")) {
      // After we apply the SCM plugin, grab the buildScmMetadata task.
      Task buildScmTask = project.tasks["buildScmMetadata"];

      // After we apply the SCM plugin, grab the root project's buildSourceZip task.
      Task buildSrcTask = project.getRootProject().tasks["buildSourceZip"];

      // Look up any li-azkaban2 zip tasks (e.g. "azkabanZip" or "azkabanMagicZip") and make them
      // depend on the buildScmTask (and add the buildMetadata.json file to the zip).
      String zipRegex = "azkaban(.*)Zip";

      project.tasks.each { Task task ->
        if (task.getName().matches(zipRegex)) {
          Zip zipTask = (Zip)task;
          zipTask.dependsOn(buildScmTask);
          zipTask.dependsOn(buildSrcTask);
          zipTask.from(getMetadataFilePath(project));
          zipTask.from(getSourceZipFilePath(project));
        }
      }
    }

    project.afterEvaluate {
      LiHadoopZipExtension zipExtension = (LiHadoopZipExtension)hadoopZipExtension;
      if (zipExtension.declaresCRT) {
        createDeploymentZipTask(project);
      }
    }
  }

  /**
   * Builds a list of relative paths to exclude from the sources zip for the project.
   *
   * @param project The Gradle project
   * @return The list of relative paths to exclude from the sources zip
   */
  @Override
  List<String> buildExcludeList(Project project) {
    List<String> excludeList = super.buildExcludeList(project);
    excludeList.add("ligradle");
    return excludeList;
  }

  /**
   * Helper method to create the Hadoop zip extension. Having this method allows for the unit tests
   * to override it.
   *
   * @param project The Gradle project
   * @return The Hadoop zip extension
   */
  @Override
  HadoopZipExtension createZipExtension(Project project) {
    LiHadoopZipExtension extension = new LiHadoopZipExtension(project);
    project.extensions.add("hadoopZip", extension);
    return extension;
  }

  /**
   * Creates a zip archive that contains the .crt file and is setup correctly to work with CRT. CRT
   * has a couple of weird bugs. To be able to deploy from CRT, there must be:
   *
   * 1. An "artifact": line in the artifact-spec.json for this project. This is required by mint
   *    deploy and crt deploy.
   *
   * 2. An artifact in Artifactory with no classifier in the file name, i.e. the entry must be of
   *    the form: ${project.name}-${project.version}.zip.
   *
   * The Tools Python code explicitly tries to form this name as a part of the URL it downloads,
   * since it assumes you want to deploy the project artifact, rather than something with a
   * classifier in the name. Thus if you "artifact" line in the artifact-spec.json for your project
   * is ${project.name}-${project.version}-azkaban.zip, it won't work!
   *
   * To work around these problems, we'll give the CRT zip the special "defaultArtifact"
   * classifier. When the Tools Python code constructs the artifact-spec, it will explicitly use an
   * artifact with this classifier in the "artifact" line. This accomplishes #1 above.
   *
   * Then we'll explicitly set the archiveName property on the zip so that it has the right form.
   * This accomplishes #2 above. These two together produce the artifacts and an artifact-spec.json
   * that is compatible with CRT.
   *
   * @param project The Gradle project
   * @return The zip task
   */
  Task createDeploymentZipTask(Project project) {
    Task zipTask = project.tasks.create(name: "CRTHadoopZip", type: Zip) { task ->
      archiveName = "${project.name}-${project.version}.zip";
      classifier = "defaultArtifact";;
      description = "Creates a Hadoop CRT deployment zip archive";
      group = "Hadoop Plugin";

      // This task is a dependency of buildHadoopZips and depends on the startHadoopZips
      project.tasks["buildHadoopZips"].dependsOn task;
      dependsOn "startHadoopZips";

      // Check that the .crt file exists
      if (!new File("${project.projectDir}/.crt").exists()) {
        throw new Exception("Could not find a .crt file at ${project.projectDir}. See go/HadoopCRT for more information.");
      }

      // The deployment zip consists of just the .crt file
      from("${project.projectDir}/.crt") {
        filter(ReplaceTokens, tokens: [version: project.version.toString()]);
      }

      // Add the task to project artifacts
      project.artifacts.add("archives", task);

      // When everything is done, print out a message
      doLast {
        project.logger.lifecycle("Prepared Hadoop zip archive at: ${archivePath}");
      }
    }
    return zipTask;
  }
}