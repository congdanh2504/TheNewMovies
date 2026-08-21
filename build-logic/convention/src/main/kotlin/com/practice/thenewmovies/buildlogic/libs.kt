package com.practice.thenewmovies.buildlogic

import org.gradle.api.Project
import org.gradle.api.artifacts.MinimalExternalModuleDependency
import org.gradle.api.artifacts.VersionCatalog
import org.gradle.api.artifacts.VersionCatalogsExtension
import org.gradle.api.artifacts.VersionConstraint
import org.gradle.api.provider.Provider
import org.gradle.kotlin.dsl.getByType
import org.gradle.plugin.use.PluginDependency

/** The `libs` version catalog, readable from plugin code. */
internal val Project.libs: VersionCatalog
    get() = extensions.getByType<VersionCatalogsExtension>().named("libs")

internal operator fun VersionCatalog.get(alias: String): Provider<MinimalExternalModuleDependency> =
    findLibrary(alias).get()

internal fun VersionCatalog.version(alias: String): VersionConstraint =
    findVersion(alias).get()

internal fun VersionCatalog.plugin(alias: String): PluginDependency =
    findPlugin(alias).get().get()
