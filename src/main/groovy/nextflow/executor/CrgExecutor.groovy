/*
 * Copyright (c) 2013-2014, Centre for Genomic Regulation (CRG).
 * Copyright (c) 2013-2014, Paolo Di Tommaso and the respective authors.
 *
 *   This file is part of 'Nextflow'.
 *
 *   Nextflow is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU General Public License as published by
 *   the Free Software Foundation, either version 3 of the License, or
 *   (at your option) any later version.
 *
 *   Nextflow is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU General Public License for more details.
 *
 *   You should have received a copy of the GNU General Public License
 *   along with Nextflow.  If not, see <http://www.gnu.org/licenses/>.
 */

package nextflow.executor

import java.nio.file.Path

import groovy.transform.PackageScope
import nextflow.processor.TaskRun


/**
 * An executor specialised for CRG cluster
 */
class CrgExecutor extends SgeExecutor {

    @Override
    List<String> getSubmitCommandLine(TaskRun task, Path scriptFile) {

        if( task.container && isDockerEnabled() ) {
            if( extraOptions == null ) extraOptions = []
            extraOptions << '-soft' << '-l' << "docker_images=${task.container}" << '-hard'
        }

        return super.getSubmitCommandLine(task, scriptFile)
    }

    @PackageScope
    boolean isDockerEnabled() {
        Map dockerConf = session.config.docker as Map
        dockerConf?.enabled?.toString() == 'true'
    }


}
