/*
 * Copyright 2010-2017 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.kotlin.caches.resolve

import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.libraries.Library
import com.intellij.openapi.roots.libraries.PersistentLibraryKind
import org.jetbrains.kotlin.analyzer.ResolverForModuleFactory
import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.context.ProjectContext
import org.jetbrains.kotlin.extensions.ApplicationExtensionDescriptor
import org.jetbrains.kotlin.idea.caches.project.LibraryInfo
import org.jetbrains.kotlin.idea.caches.resolve.PlatformAnalysisSettings
import org.jetbrains.kotlin.platform.IdePlatformKind

interface IdePlatformKindResolution {
    val kind: IdePlatformKind<*>

    val resolverForModuleFactory: ResolverForModuleFactory

    fun createBuiltIns(settings: PlatformAnalysisSettings, projectContext: ProjectContext): KotlinBuiltIns

    fun isLibraryPathForPlatform(libraryPath: String): Boolean

    val libraryKind: PersistentLibraryKind<*>?

    fun createLibraryInfo(project: Project, library: Library): List<LibraryInfo> {
        return listOf(LibraryInfo(project, library))
    }

    companion object : ApplicationExtensionDescriptor<IdePlatformKindResolution>(
        "org.jetbrains.kotlin.idePlatformKindResolution", IdePlatformKindResolution::class.java
    ) {
        private val CACHED_RESOLUTION_SUPPORT by lazy {
            val allPlatformKinds = IdePlatformKind.ALL_KINDS
            val groupedResolution = IdePlatformKindResolution.getInstances().groupBy { it.kind }.mapValues { it.value.single() }

            for (kind in allPlatformKinds) {
                if (kind !in groupedResolution) {
                    throw IllegalStateException(
                        "Resolution support for the platform '$kind' is missing. " +
                                "Implement 'IdePlatformKindResolution' for it."
                    )
                }
            }

            groupedResolution
        }

        fun getResolution(kind: IdePlatformKind<*>): IdePlatformKindResolution {
            return CACHED_RESOLUTION_SUPPORT[kind] ?: error("Unknown platform $this")
        }
    }
}

val IdePlatformKind<*>.resolution: IdePlatformKindResolution
    get() = IdePlatformKindResolution.getResolution(this)