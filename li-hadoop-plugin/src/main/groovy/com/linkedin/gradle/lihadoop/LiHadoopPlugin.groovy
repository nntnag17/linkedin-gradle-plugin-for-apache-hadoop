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
package com.linkedin.gradle.lihadoop;

import com.linkedin.gradle.hadoop.HadoopPlugin;
import com.linkedin.gradle.hadoopdsl.HadoopDslPlugin;
import com.linkedin.gradle.oozie.OoziePlugin;
import com.linkedin.gradle.pig.PigPlugin;
import com.linkedin.gradle.scm.ScmPlugin;

import com.linkedin.gradle.lihadoopdsl.LiHadoopDslPlugin;
import com.linkedin.gradle.lioozie.LiOoziePlugin;
import com.linkedin.gradle.lipig.LiPigPlugin;
import com.linkedin.gradle.liscm.LiScmPlugin;

/**
 * LinkedIn-specific customizations to the Hadoop Plugin.
 */
class LiHadoopPlugin extends HadoopPlugin {
  /**
   * Returns the LinkedIn-specific LiHadoopDslPlugin class. Subclasses can override this method to
   * return their own HadoopDslPlugin class.
   *
   * @return Class that implements the HadoopDslPlugin
   */
  Class<? extends HadoopDslPlugin> getHadoopDslPluginClass() {
    return LiHadoopDslPlugin.class;
  }

  /**
   * Returns the LinkedIn-specific PigPlugin class. Subclasses can override this method to return
   * their own PigPlugin class.
   *
   * @return Class that implements the PigPlugin
   */
  @Override
  Class<? extends PigPlugin> getPigPluginClass() {
    return LiPigPlugin.class;
  }

  /**
   * Returns the LinkedIn-specific LiScmPlugin class. Subclasses can override this method to return
   * their own ScmPlugin class.
   *
   * @return Class that implements the ScmPlugin
   */
  @Override
  Class<? extends ScmPlugin> getScmPluginClass() {
    return LiScmPlugin.class;
  }

  /**
   * Factory method to return the LiOoziePlugin class. Subclasses can override this method to return
   * their own OoziePlugin class.
   *
   * @return Class that implements the LiOoziePlugin
   */
  @Override
  Class<? extends OoziePlugin> getOoziePluginClass() {
    return LiOoziePlugin.class;
  }
}