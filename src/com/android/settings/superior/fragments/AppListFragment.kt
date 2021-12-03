/*
 * Copyright (C) 2021 AOSP-Krypton Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.settings.superior.fragments

import android.content.Context
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.ImageView
import android.widget.SearchView
import android.widget.TextView

import androidx.core.view.ViewCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView

import com.android.settings.R
import com.google.android.material.appbar.AppBarLayout

/**
 * [Fragment] that hosts a [RecyclerView] with a vertical
 * list of application info. Items display an icon, name
 * and package name of the application, along with a [CheckBox]
 * indicating whether the item is selected or not.
 */
abstract class AppListFragment: Fragment(R.layout.apps_list_layout), MenuItem.OnActionExpandListener {

    private lateinit var appBarLayout: AppBarLayout
    private lateinit var packageManager: PackageManager
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: AppListAdapter
    private lateinit var packageList: List<PackageInfo>

    private var searchText = ""
    private var category: Int = CATEGORY_USER_ONLY
    private var customFilter: ((PackageInfo) -> Boolean)? = null
    private var comparator: ((PackageInfo, PackageInfo) -> Int)? = null
    private var listener: ((List<String>) -> Unit)? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
        activity!!.let {
            it.setTitle(getTitle())
            appBarLayout = it.findViewById(R.id.app_bar)
        }
        packageManager = context!!.packageManager
        packageList = packageManager.getInstalledPackages(0)
    }

    /**
     * Override this function to set the title res id of this fragment.
     */
    abstract protected fun getTitle(): Int

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        adapter = AppListAdapter()
        recyclerView = view.findViewById<RecyclerView>(R.id.apps_list).also {
            it.layoutManager = LinearLayoutManager(context)
            it.adapter = adapter
        }
        refreshList()
    }

    /**
     * Abstract function for subclasses to override for providing
     * an inital list of packages that should appear as selected.
     */
    abstract protected fun getInitialCheckedList(): List<String>

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.apps_list_menu, menu)
        val searchItem = menu.findItem(R.id.search).also {
            it.setOnActionExpandListener(this)
        }
        val searchView = searchItem.actionView as SearchView
        searchView.setQueryHint(getString(R.string.search_apps));
        searchView.setOnQueryTextListener(object: SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String) = false

            override fun onQueryTextChange(newText: String): Boolean {
                searchText = newText
                refreshList()
                return true
            }
        })
    }

    override fun onMenuItemActionExpand(item: MenuItem): Boolean {
        // To prevent a large space on tool bar.
        appBarLayout.setExpanded(false /*expanded*/, false /*animate*/)
        // To prevent user expanding the collapsing tool bar view.
        ViewCompat.setNestedScrollingEnabled(recyclerView, false)
        return true
    }

    override fun onMenuItemActionCollapse(item: MenuItem): Boolean {
        // We keep the collapsed status after user cancel the search function.
        appBarLayout.setExpanded(false /*expanded*/, false /*animate*/)
        // Allow user to expande the tool bar view.
        ViewCompat.setNestedScrollingEnabled(recyclerView, true)
        return true
    }

    /**
     * Set the type of apps that should be displayed in the list.
     * Defaults to [CATEGORY_USER_ONLY].
     *
     * @param category one of [CATEGORY_SYSTEM_ONLY],
     * [CATEGORY_USER_ONLY], [CATEGORY_BOTH]
     */
    fun setDisplayCategory(category: Int) {
        this.category = category
    }

    /**
     * Set a custom filter to filter out items from the list.
     *
     * @param customFilter a function that takes a [PackageInfo] and
     * returns a [Boolean] indicating whether to show the item or not. 
     */
    fun setCustomFilter(customFilter: ((packageInfo: PackageInfo) -> Boolean)?) {
        this.customFilter = customFilter
        refreshList()
    }

    /**
     * Set a [Comparator] for sorting the elements in the list. Can be
     * set to null to sort based on their labels.
     *
     * @param comparator a function that takes two [PackageInfo]'s and returns
     * an [Int] representing their relative priority.
     */
    fun setComparator(comparator: ((a: PackageInfo, b: PackageInfo) -> Int)?) {
        this.comparator = comparator
        refreshList()
    }

    /**
     * Called when user selects an item.
     * 
     * @param list a [List<String>] of selected items.
     */
    open protected fun onListUpdate(list: List<String>) {}

    private fun refreshList() {
        var list = packageList.filter {
            when(category) {
                CATEGORY_SYSTEM_ONLY -> it.applicationInfo.isSystemApp()
                CATEGORY_USER_ONLY -> !it.applicationInfo.isSystemApp()
                else -> true
            }
        }.filter {
            getLabel(it).contains(searchText, true)
        }
        list = customFilter?.let { customFilter ->
            list.filter {
                customFilter(it)
            }
        } ?: list
        list = comparator?.let {
            list.sortedWith(it)
        } ?: list.sortedWith { a, b ->
            getLabel(a).compareTo(getLabel(b))
        }
        if (::adapter.isInitialized) adapter.submitList(list.map { appInfoFromPackageInfo(it) })
    }

    private fun appInfoFromPackageInfo(packageInfo: PackageInfo) =
        AppInfo(
            packageInfo.packageName,
            getLabel(packageInfo),
            packageInfo.applicationInfo.loadIcon(packageManager),
        )

    private fun getLabel(packageInfo: PackageInfo) =
        packageInfo.applicationInfo.loadLabel(packageManager).toString()

    private inner class AppListAdapter: ListAdapter<AppInfo, AppListViewHolder>(itemCallback) {
        private val selectedIndices = mutableSetOf<Int>()
        private val initialList: MutableList<String>

        init {
            initialList = getInitialCheckedList().toMutableList()
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
            AppListViewHolder(layoutInflater.inflate(
                R.layout.apps_list_item, parent, false))

        override fun onBindViewHolder(holder: AppListViewHolder, position: Int) {
            getItem(position).let {
                holder.label.setText(it.label)
                holder.packageName.setText(it.packageName)
                holder.icon.setImageDrawable(it.icon)
                holder.itemView.setOnClickListener {
                    if (selectedIndices.contains(position)){
                        selectedIndices.remove(position)
                    } else {
                        selectedIndices.add(position)
                    }
                    notifyItemChanged(position)
                    onListUpdate(getSelectedPackages())
                }
                if (initialList.contains(it.packageName)) {
                    initialList.remove(it.packageName)
                    selectedIndices.add(position)
                }
                holder.checkBox.setChecked(selectedIndices.contains(position))
            }
        }

        override fun submitList(list: List<AppInfo>?) {
            selectedIndices.clear()
            super.submitList(list)
        }

        private fun getSelectedPackages(): List<String> =
            selectedIndices.map {
                getItem(it)
            }.map {
                it.packageName
            }.toList()
    }

    private class AppListViewHolder(itemView: View): RecyclerView.ViewHolder(itemView) {
        val icon: ImageView
        val label: TextView
        val packageName: TextView
        val checkBox: CheckBox

        init {
            icon = itemView.findViewById(R.id.icon)
            label = itemView.findViewById(R.id.label)
            packageName = itemView.findViewById(R.id.packageName)
            checkBox = itemView.findViewById(R.id.checkBox)
        }
    }

    private data class AppInfo(
        val packageName: String,
        val label: String,
        val icon: Drawable,
    )

    companion object {
        private const val TAG = "AppListFragment"

        const val CATEGORY_SYSTEM_ONLY = 0
        const val CATEGORY_USER_ONLY = 1
        const val CATEGORY_BOTH = 2

        private val itemCallback = object: DiffUtil.ItemCallback<AppInfo>() {
            override fun areItemsTheSame(oldInfo: AppInfo, newInfo: AppInfo) =
                oldInfo.packageName == newInfo.packageName

            override fun areContentsTheSame(oldInfo: AppInfo, newInfo: AppInfo) =
                oldInfo == newInfo
        }
    }
}
