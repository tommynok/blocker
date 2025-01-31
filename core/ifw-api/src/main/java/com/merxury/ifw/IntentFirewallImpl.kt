/*
 * Copyright 2023 Blocker
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.merxury.ifw

import com.merxury.blocker.core.exception.RootUnavailableException
import com.merxury.blocker.core.utils.FileUtils
import com.merxury.blocker.core.utils.PermissionUtils
import com.merxury.ifw.entity.Activity
import com.merxury.ifw.entity.Broadcast
import com.merxury.ifw.entity.Component
import com.merxury.ifw.entity.ComponentFilter
import com.merxury.ifw.entity.IfwComponentType
import com.merxury.ifw.entity.Rules
import com.merxury.ifw.entity.Service
import com.merxury.ifw.util.IfwStorageUtils
import com.topjohnwu.superuser.io.SuFile
import com.topjohnwu.superuser.io.SuFileInputStream
import com.topjohnwu.superuser.io.SuFileOutputStream
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.simpleframework.xml.Serializer
import org.simpleframework.xml.core.Persister
import timber.log.Timber

class IntentFirewallImpl @AssistedInject constructor(
    @Assisted override val packageName: String,
) : IntentFirewall {

    private val filename: String = "$packageName$EXTENSION"
    private val destFile = SuFile(IfwStorageUtils.getIfwFolder() + filename)
    private var rule: Rules = Rules()

    override suspend fun load() = withContext(Dispatchers.IO) {
        if (PermissionUtils.isRootAvailable() && destFile.exists()) {
            val serializer: Serializer = Persister()
            try {
                val input = SuFileInputStream.open(destFile)
                rule = serializer.read(Rules::class.java, input)
            } catch (e: Exception) {
                Timber.e(e, "Error reading rules file $destFile:")
            }
        }
        return@withContext this@IntentFirewallImpl
    }

    override suspend fun save() {
        withContext(Dispatchers.IO) {
            ensureNoEmptyTag()
            if (rule.activity == null && rule.broadcast == null && rule.service == null) {
                // If there is no rules presented, delete rule file (if exists)
                clear()
                return@withContext
            }
            SuFileOutputStream.open(destFile).use {
                val serializer: Serializer = Persister()
                serializer.write(rule, it)
            }
            FileUtils.chmod(destFile.absolutePath, 644, false)
            Timber.i("Saved $destFile")
        }
    }

    override suspend fun clear() {
        withContext(Dispatchers.IO) {
            if (!PermissionUtils.isRootAvailable()) {
                throw RootUnavailableException()
            }
            Timber.d("Clear IFW rule $filename")
            if (destFile.exists()) {
                destFile.delete()
            }
            rule = Rules()
        }
    }

    override suspend fun add(
        packageName: String,
        componentName: String,
        type: IfwComponentType?,
    ): Boolean {
        if (!PermissionUtils.isRootAvailable()) {
            Timber.e("Root unavailable, cannot add rule")
            throw RootUnavailableException()
        }
        var result = false
        when (type) {
            IfwComponentType.ACTIVITY -> {
                if (rule.activity == null) {
                    rule.activity = Activity()
                }
                result = addComponentFilter(packageName, componentName, rule.activity)
            }

            IfwComponentType.BROADCAST -> {
                if (rule.broadcast == null) {
                    rule.broadcast = Broadcast()
                }
                result = addComponentFilter(packageName, componentName, rule.broadcast)
            }

            IfwComponentType.SERVICE -> {
                if (rule.service == null) {
                    rule.service = Service()
                }
                result = addComponentFilter(packageName, componentName, rule.service)
            }

            else -> {}
        }
        return result
    }

    override suspend fun remove(
        packageName: String,
        componentName: String,
        type: IfwComponentType?,
    ): Boolean {
        if (!PermissionUtils.isRootAvailable()) {
            Timber.e("Root unavailable, cannot remove rule")
            throw RootUnavailableException()
        }
        return when (type) {
            IfwComponentType.ACTIVITY -> removeComponentFilter(
                packageName,
                componentName,
                rule.activity,
            )

            IfwComponentType.BROADCAST -> removeComponentFilter(
                packageName,
                componentName,
                rule.broadcast,
            )

            IfwComponentType.SERVICE -> removeComponentFilter(
                packageName,
                componentName,
                rule.service,
            )

            else -> false
        }
    }

    override suspend fun getComponentEnableState(
        packageName: String,
        componentName: String,
    ): Boolean {
        val filters: MutableList<ComponentFilter> = ArrayList()
        rule.activity?.let {
            filters.addAll(it.componentFilters)
        }
        rule.broadcast?.let {
            filters.addAll(it.componentFilters)
        }
        rule.service?.let {
            filters.addAll(it.componentFilters)
        }
        return getFilterEnableState(packageName, componentName, filters)
    }

    private fun ensureNoEmptyTag() {
        if (rule.activity != null && rule.activity.componentFilters.isNullOrEmpty()) {
            rule.activity = null
        }
        if (rule.broadcast != null && rule.broadcast.componentFilters.isNullOrEmpty()) {
            rule.broadcast = null
        }
        if (rule.service != null && rule.service.componentFilters.isNullOrEmpty()) {
            rule.service = null
        }
    }

    private fun addComponentFilter(
        packageName: String,
        componentName: String,
        component: Component?,
    ): Boolean {
        if (component == null) {
            return false
        }
        var filters = component.componentFilters
        if (filters == null) {
            filters = ArrayList()
            component.componentFilters = filters
        }
        val filterRule = formatName(packageName, componentName)
        // Duplicate filter detection
        for (filter in filters) {
            if (filter.name == filterRule) {
                return false
            }
        }
        filters.add(ComponentFilter(filterRule))
        Timber.i("Added component:$packageName/$componentName")
        return true
    }

    private fun removeComponentFilter(
        packageName: String,
        componentName: String,
        component: Component?,
    ): Boolean {
        if (component == null) {
            return false
        }
        var filters = component.componentFilters
        if (filters == null) {
            filters = ArrayList()
        }
        val filterRule = formatName(packageName, componentName)
        for (filter in ArrayList(filters)) {
            if (filterRule == filter.name) {
                filters.remove(filter)
            }
        }
        return true
    }

    private fun getFilterEnableState(
        packageName: String,
        componentName: String,
        componentFilters: List<ComponentFilter>?,
    ): Boolean {
        if (componentFilters == null) {
            return true
        }
        for (filter in componentFilters) {
            val filterName = formatName(packageName, componentName)
            if (filterName == filter.name) {
                return false
            }
        }
        return true
    }

    private fun formatName(packageName: String, name: String): String {
        return "$packageName/$name"
    }

    companion object {
        private const val EXTENSION = ".xml"
    }
}
