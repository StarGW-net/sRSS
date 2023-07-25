/*
 * Copyright (c) 2012-2018 Frederic Julian
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http:></http:>//www.gnu.org/licenses/>.
 */

package net.frju.flym.ui.entries

import android.content.Intent
import android.content.SharedPreferences.OnSharedPreferenceChangeListener
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.view.*
import android.widget.Toast
import androidx.appcompat.widget.SearchView
import androidx.core.os.bundleOf
import androidx.core.view.isGone
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import androidx.paging.LivePagedListBuilder
import androidx.paging.PagedList
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.synthetic.main.fragment_entries.*
import kotlinx.android.synthetic.main.view_entry.view.*
import kotlinx.android.synthetic.main.view_main_containers.*
import net.fred.feedex.R
import net.frju.flym.App
import net.frju.flym.data.entities.EntryWithFeed
import net.frju.flym.data.entities.Feed
import net.frju.flym.data.utils.PrefConstants
import net.frju.flym.service.FetcherService
import net.frju.flym.ui.about.AboutActivity
import net.frju.flym.ui.main.MainNavigator
import net.frju.flym.usage.UsageActivity
import net.frju.flym.utils.*
import org.jetbrains.anko.*
import org.jetbrains.anko.appcompat.v7.titleResource
import org.jetbrains.anko.design.longSnackbar
import org.jetbrains.anko.sdk21.listeners.onClick
import org.jetbrains.anko.support.v4.dip
import org.jetbrains.anko.support.v4.share
import org.jetbrains.anko.support.v4.startActivity
import java.util.*

const val VIEW_CURRENT = "current_view"
const val VIEW_ALL = 0;
const val VIEW_UNREAD = 1;
const val VIEW_FAV = 2;

class EntriesFragment : Fragment() {

    // val newAtricles: Int = 0

    companion object {

        private const val ARG_FEED = "ARG_FEED"
        private const val STATE_FEED = "STATE_FEED"
        private const val STATE_SEARCH_TEXT = "STATE_SEARCH_TEXT"
        private const val STATE_SELECTED_ENTRY_ID = "STATE_SELECTED_ENTRY_ID"
        private const val STATE_LIST_DISPLAY_DATE = "STATE_LIST_DISPLAY_DATE"

        fun newInstance(feed: Feed?): EntriesFragment {
            return EntriesFragment().apply {
                feed?.let {
                    arguments = bundleOf(ARG_FEED to feed)
                }
            }
        }
    }

    var feed: Feed? = null
        set(value) {
            field = value

            setupTitle()
            initDataObservers()
            // bottom_navigation.post { initDataObservers() } // Needed to retrieve the correct selected tab position
        }

    private val navigator: MainNavigator by lazy { activity as MainNavigator }

    private val adapter = EntryAdapter({ entryWithFeed ->
        navigator.goToEntryDetails(entryWithFeed.entry.id, entryIds!!)
    }, { entryWithFeed ->
        share(entryWithFeed.entry.link.orEmpty(), entryWithFeed.entry.title.orEmpty())
    }, { entryWithFeed, view ->
        entryWithFeed.entry.favorite = !entryWithFeed.entry.favorite

        view.favorite_icon?.let {
            if (entryWithFeed.entry.favorite) {
                it.setImageResource(R.drawable.ic_star_24dp)
            } else {
                it.setImageResource(R.drawable.ic_star_border_24dp)
            }
        }

        doAsync {
            if (entryWithFeed.entry.favorite) {
                App.db.entryDao().markAsFavorite(entryWithFeed.entry.id)
            } else {
                App.db.entryDao().markAsNotFavorite(entryWithFeed.entry.id)
            }
        }
    })
    private var listDisplayDate = Date().time
    private var entriesLiveData: LiveData<PagedList<EntryWithFeed>>? = null
    private var entryIdsLiveData: LiveData<List<String>>? = null
    private var entryIds: List<String>? = null
    private var newCountLiveData: LiveData<Long>? = null
    // private var unreadBadge: Badge? = null
    private var searchText: String? = null
    private val searchHandler = Handler()
    private var isDesc: Boolean = true

    private val prefListener = OnSharedPreferenceChangeListener { sharedPreferences, key ->
        if (PrefConstants.IS_REFRESHING == key) {
            refreshSwipeProgress()
        }
    }

    init {
        setHasOptionsMenu(true)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? =
            inflater.inflate(R.layout.fragment_entries, container, false)

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        if (savedInstanceState != null) {
            feed = savedInstanceState.getParcelable(STATE_FEED)
            adapter.selectedEntryId = savedInstanceState.getString(STATE_SELECTED_ENTRY_ID)
            listDisplayDate = savedInstanceState.getLong(STATE_LIST_DISPLAY_DATE)
            searchText = savedInstanceState.getString(STATE_SEARCH_TEXT)
        } else {
            feed = arguments?.getParcelable(ARG_FEED)
        }

        setupRecyclerView()

        // STEVE
        recycler_view.addItemDecoration(DividerItemDecoration(context,
                DividerItemDecoration.VERTICAL))

        // STEVE
        /*
        bottom_navigation.setOnNavigationItemSelectedListener {
            recycler_view.post {
                listDisplayDate = Date().time
                initDataObservers()
                recycler_view.scrollToPosition(0)
            }

            activity?.toolbar?.menu?.findItem(R.id.menu_entries__share)?.isVisible = it.itemId == R.id.favorites
            true
        }
        */

        /*
        unreadBadge = QBadgeView(context).bindTarget((bottom_navigation.getChildAt(0) as ViewGroup).getChildAt(0)).apply {
            setGravityOffset(35F, 0F, true)
            isShowShadow = false
            badgeBackgroundColor = requireContext().colorAttr(R.attr.colorAccent)
        }
        */


        // STEVE
        /*
        read_all_fab.onClick { _ ->
            val i:Int = bottom_navigation.visibility;

            if (i == GONE) {
                bottom_navigation.visibility = VISIBLE;
            } else {
                bottom_navigation.visibility = GONE;
            }
        }

         */

        read_all_fab.onClick { _ ->
            read_all_fab_do()
        }
    }

    private fun read_all_fab_do()
    {
        entryIds?.let { entryIds ->
            if (entryIds.isNotEmpty()) {
                doAsync {
                    // TODO check if limit still needed
                    entryIds.withIndex().groupBy { it.index / 300 }.map { pair -> pair.value.map { it.value } }.forEach {
                        App.db.entryDao().markAsRead(it)
                    }
                }

                coordinator.longSnackbar(R.string.marked_as_read, R.string.undo) { _ ->
                    doAsync {
                        // TODO check if limit still needed
                        entryIds.withIndex().groupBy { it.index / 300 }.map { pair -> pair.value.map { it.value } }.forEach {
                            App.db.entryDao().markAsUnread(it)
                        }

                        uiThread {
                            // we need to wait for the list to be empty before displaying the new items (to avoid scrolling issues)
                            listDisplayDate = Date().time
                            initDataObservers()
                        }
                    }
                }
            }

            if (feed == null || feed?.id == Feed.ALL_ENTRIES_ID) {
                activity?.notificationManager?.cancel(0)
            }
        }
    }

    // bottom_navigation.selectedItemId   - replace with a prefs

    private fun initDataObservers() {

        val view_current: Int? = context?.getPrefInt(VIEW_CURRENT, VIEW_ALL);

        isDesc = context?.getPrefBoolean(PrefConstants.SORT_ORDER, true)!!
        entryIdsLiveData?.removeObservers(viewLifecycleOwner)
        entryIdsLiveData = when {

            searchText != null -> App.db.entryDao().observeIdsBySearch(searchText!!, isDesc)
            feed?.isGroup == true && view_current == VIEW_UNREAD -> App.db.entryDao().observeUnreadIdsByGroup(feed!!.id, listDisplayDate, isDesc)
            feed?.isGroup == true && view_current == VIEW_FAV -> App.db.entryDao().observeFavoriteIdsByGroup(feed!!.id, listDisplayDate, isDesc)
            feed?.isGroup == true -> App.db.entryDao().observeIdsByGroup(feed!!.id, listDisplayDate, isDesc)

            feed != null && feed?.id != Feed.ALL_ENTRIES_ID && view_current == VIEW_UNREAD -> App.db.entryDao().observeUnreadIdsByFeed(feed!!.id, listDisplayDate, isDesc)
            feed != null && feed?.id != Feed.ALL_ENTRIES_ID && view_current == VIEW_FAV -> App.db.entryDao().observeFavoriteIdsByFeed(feed!!.id, listDisplayDate, isDesc)
            feed != null && feed?.id != Feed.ALL_ENTRIES_ID -> App.db.entryDao().observeIdsByFeed(feed!!.id, listDisplayDate, isDesc)

            view_current == VIEW_UNREAD  -> App.db.entryDao().observeAllUnreadIds(listDisplayDate, isDesc)
            view_current == VIEW_FAV -> App.db.entryDao().observeAllFavoriteIds(listDisplayDate, isDesc)
            else -> App.db.entryDao().observeAllIds(listDisplayDate, isDesc)
        }

        entryIdsLiveData?.observe(viewLifecycleOwner, Observer { list ->
            entryIds = list
        })

        entriesLiveData?.removeObservers(viewLifecycleOwner)
        entriesLiveData = LivePagedListBuilder(when {
            searchText != null -> App.db.entryDao().observeSearch(searchText!!, isDesc)
            feed?.isGroup == true && view_current == VIEW_UNREAD -> App.db.entryDao().observeUnreadsByGroup(feed!!.id, listDisplayDate, isDesc)
            feed?.isGroup == true && view_current == VIEW_FAV -> App.db.entryDao().observeFavoritesByGroup(feed!!.id, listDisplayDate, isDesc)
            feed?.isGroup == true -> App.db.entryDao().observeByGroup(feed!!.id, listDisplayDate, isDesc)

            feed != null && feed?.id != Feed.ALL_ENTRIES_ID && view_current == VIEW_UNREAD  -> App.db.entryDao().observeUnreadsByFeed(feed!!.id, listDisplayDate, isDesc)
            feed != null && feed?.id != Feed.ALL_ENTRIES_ID && view_current == VIEW_FAV -> App.db.entryDao().observeFavoritesByFeed(feed!!.id, listDisplayDate, isDesc)
            feed != null && feed?.id != Feed.ALL_ENTRIES_ID -> App.db.entryDao().observeByFeed(feed!!.id, listDisplayDate, isDesc)

            view_current == VIEW_UNREAD  -> App.db.entryDao().observeAllUnreads(listDisplayDate, isDesc)
            view_current == VIEW_FAV -> App.db.entryDao().observeAllFavorites(listDisplayDate, isDesc)
            else -> App.db.entryDao().observeAll(listDisplayDate, isDesc)
        }, 30).build()

        entriesLiveData?.observe(viewLifecycleOwner, Observer { pagedList ->
            adapter.submitList(pagedList)
        })

        newCountLiveData?.removeObservers(viewLifecycleOwner)
        newCountLiveData = when {
            feed?.isGroup == true -> App.db.entryDao().observeNewEntriesCountByGroup(feed!!.id, listDisplayDate)
            feed != null && feed?.id != Feed.ALL_ENTRIES_ID -> App.db.entryDao().observeNewEntriesCountByFeed(feed!!.id, listDisplayDate)
            else -> App.db.entryDao().observeNewEntriesCount(listDisplayDate)
        }

        newCountLiveData?.observe(viewLifecycleOwner, Observer { count ->
            if (count != null && count > 0L) {
                // If we have an empty list, let's immediately display the new items
                // App.myLog("New feed entries = " + count)
                if (entryIds?.isEmpty() == true && view_current != VIEW_FAV) {
                    listDisplayDate = Date().time
                    initDataObservers()
                } else {
                    // unreadBadge?.badgeNumber = count.toInt() // STEVE - where do I put the unread badge?
                    App.myLog( "New feed entries = " + count)
                    // Toast.makeText(context, count.toString() + " new articles", Toast.LENGTH_SHORT).show()
                    if (feed?.title == null) {
                        coordinator.longSnackbar("New articles", count.toString()) {}
                    } else {
                        coordinator.longSnackbar("New: " + feed?.title.toString(), count.toString()) {}
                    }
                    // recycler_view.smoothScrollToPosition(0);
                    listDisplayDate = Date().time
                    initDataObservers()
                    // val llm = recycler_view.getLayoutManager()
                    // recycler_view.smoothScrollToPosition(0);
                    // llm?.scrollToPosition(0);
                    // recycler_view?.post { llm?.scrollToPosition(0) }
                    // refresh_layout.postDelayed({ llm?.scrollToPosition(0) }, 500)
                }
            } else {
                // unreadBadge?.hide(false)
                App.myLog("No new entries!")
            }
        })

        val llm = recycler_view.getLayoutManager()
        llm?.scrollToPosition(0);

    }

    override fun onStart() {
        super.onStart()
        context?.registerOnPrefChangeListener(prefListener)
        refreshSwipeProgress()

        if (context?.getPrefBoolean(PrefConstants.HIDE_BUTTON_MARK_ALL_AS_READ, false) == true) {
            read_all_fab.visibility = View.INVISIBLE;
        } else {
            read_all_fab.visibility = View.VISIBLE;
        }
    }

    override fun onStop() {
        super.onStop()
        context?.unregisterOnPrefChangeListener(prefListener)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putParcelable(STATE_FEED, feed)
        outState.putString(STATE_SELECTED_ENTRY_ID, adapter.selectedEntryId)
        outState.putLong(STATE_LIST_DISPLAY_DATE, listDisplayDate)
        outState.putString(STATE_SEARCH_TEXT, searchText)

        super.onSaveInstanceState(outState)
    }

    private fun setupRecyclerView() {
        recycler_view.setHasFixedSize(true)

        val layoutManager = LinearLayoutManager(activity)
        recycler_view.layoutManager = layoutManager
        recycler_view.adapter = adapter

        refresh_layout.setColorScheme(R.color.colorAccent,
                requireContext().attr(R.attr.colorPrimaryDark).resourceId,
                R.color.colorAccent,
                requireContext().attr(R.attr.colorPrimaryDark).resourceId)

        // STEVE - put this on a button
        /*
        refresh_layout.setOnRefreshListener {
            startRefresh()
        }
         */

        val itemTouchHelperCallback = object : ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT) {
            private val VELOCITY = dip(800).toFloat()

            override fun onMove(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder, target: RecyclerView.ViewHolder): Boolean {
                return false
            }

            override fun getSwipeEscapeVelocity(defaultValue: Float): Float {
                return VELOCITY
            }

            override fun getSwipeVelocityThreshold(defaultValue: Float): Float {
                return VELOCITY
            }

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                adapter.currentList?.get(viewHolder.adapterPosition)?.let { entryWithFeed ->
                    entryWithFeed.entry.read = !entryWithFeed.entry.read
                    doAsync {
                        if (entryWithFeed.entry.read) {
                            App.db.entryDao().markAsRead(listOf(entryWithFeed.entry.id))
                        } else {
                            App.db.entryDao().markAsUnread(listOf(entryWithFeed.entry.id))
                        }
/*
                        coordinator.longSnackbar(R.string.marked_as_read, R.string.undo) { _ ->
                            doAsync {
                                if (entryWithFeed.entry.read) {
                                    App.db.entryDao().markAsUnread(listOf(entryWithFeed.entry.id))
                                } else {
                                    App.db.entryDao().markAsRead(listOf(entryWithFeed.entry.id))
                                }
                            }
                        }
*/

                        if (entryWithFeed.entry.read)
                        {
                            coordinator.longSnackbar(R.string.marked_as_read, R.string.undo) { _ ->
                                doAsync {
                                     App.db.entryDao().markAsUnread(listOf(entryWithFeed.entry.id))
                                }
                            }
                        } else {
                            coordinator.longSnackbar(R.string.marked_as_unread, R.string.undo) { _ ->
                                doAsync {
                                     App.db.entryDao().markAsRead(listOf(entryWithFeed.entry.id))
                                }
                            }
                        }

                        val view_current: Int? = context?.getPrefInt(VIEW_CURRENT, VIEW_ALL);

                        if (view_current != VIEW_UNREAD) {
                            uiThread {
                                adapter.notifyItemChanged(viewHolder.adapterPosition)
                            }
                        }
                    }
                }
            }
        }

        // attaching the touch helper to recycler view
        ItemTouchHelper(itemTouchHelperCallback).attachToRecyclerView(recycler_view)

        recycler_view.emptyView = empty_view

        recycler_view.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                super.onScrollStateChanged(recyclerView, newState)
                activity?.closeKeyboard()
            }
        })
    }

    private fun startRefresh() {
        if (context?.getPrefBoolean(PrefConstants.IS_REFRESHING, false) == false) {
            if (feed?.id != Feed.ALL_ENTRIES_ID) {
                context?.startService(Intent(context, FetcherService::class.java).setAction(FetcherService.ACTION_REFRESH_FEEDS).putExtra(FetcherService.EXTRA_FEED_ID,
                        feed?.id))
            } else {
                context?.startService(Intent(context, FetcherService::class.java).setAction(FetcherService.ACTION_REFRESH_FEEDS))
            }
        }

        // In case there is no internet, the service won't even start, let's quickly stop the refresh animation
        refresh_layout.postDelayed({ refreshSwipeProgress() }, 500)
    }

    // STEVE - title is set here
    private fun setupTitle() {
        activity?.toolbar?.apply {
            if (feed == null || feed?.id == Feed.ALL_ENTRIES_ID) {
                titleResource = R.string.all_entries
            } else {
                title = feed?.title
            }
        }
    }


    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        App.myLog( "Inflating Menu")

        inflater.inflate(R.menu.menu_fragment_entries, menu)

        val view_current: Int? = context?.getPrefInt(VIEW_CURRENT, VIEW_ALL);

        menu.findItem(R.id.menu_entries__share).isVisible = view_current == VIEW_FAV;

        if (view_current == VIEW_FAV)
        {
            menu.findItem(R.id.menu_entries__fav).isChecked = true
            menu.findItem(R.id.menu_entries__unread).isChecked = false
            menu.findItem(R.id.menu_entries__all).isChecked = false
        }

        if (view_current == VIEW_ALL)
        {
            menu.findItem(R.id.menu_entries__fav).isChecked = false
            menu.findItem(R.id.menu_entries__unread).isChecked = false
            menu.findItem(R.id.menu_entries__all).isChecked = true
        }

        if (view_current == VIEW_UNREAD)
        {
            menu.findItem(R.id.menu_entries__fav).isChecked = false
            menu.findItem(R.id.menu_entries__unread).isChecked = true
            menu.findItem(R.id.menu_entries__all).isChecked = false
        }

        // val refreshItems = menu.findItem(R.id.menu_entries__refresh)



        // refreshItems.setOnClickListener(View.OnClickListener { displayAppLoadDialog() })


        val searchItem = menu.findItem(R.id.menu_entries__search)
        val searchView = searchItem.actionView as SearchView
        if (searchText != null) {
            searchItem.expandActionView()
            searchView.post {
                searchView.setQuery(searchText, false)
                searchView.clearFocus()
            }
        }

        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String): Boolean {
                return false
            }

            override fun onQueryTextChange(newText: String): Boolean {
                if (searchText != null) { // needed because it can actually be called after the onMenuItemActionCollapse event
                    searchText = newText

                    // In order to avoid plenty of request, we add a small throttle time
                    searchHandler.removeCallbacksAndMessages(null)
                    searchHandler.postDelayed({
                        initDataObservers()
                    }, 700)
                }
                return false
            }
        })
        searchItem.setOnActionExpandListener(object : MenuItem.OnActionExpandListener {
            override fun onMenuItemActionExpand(item: MenuItem?): Boolean {
                searchText = ""
                initDataObservers()
                bottom_navigation.isGone = true
                return true
            }

            override fun onMenuItemActionCollapse(item: MenuItem?): Boolean {
                searchText = null
                initDataObservers()
                bottom_navigation.isVisible = true
                return true
            }
        })
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {

        when (item.itemId) {
            R.id.menu_entries__share -> {
                // TODO: will only work for the visible 30 items, need to find something better
                adapter.currentList?.joinToString("\n\n") { it.entry.title + ": " + it.entry.link }?.let { content ->
                    val title = getString(R.string.app_name) + " " + getString(R.string.favorites)
                    share(content.take(300000), title) // take() to avoid crashing with a too big intent
                }
            }
            R.id.menu_entries__about -> {
                var verName :String? = "latest"
                try {
                    val pInfo: PackageInfo? = context?.packageManager?.getPackageInfo(context?.packageName, 0)
                    verName = pInfo?.versionName
                } catch (e: PackageManager.NameNotFoundException) {
                    // oh dear
                    e.printStackTrace();
                }

                val url = "https://www.frju.net/apps/flym/help.html?ver=$verName"
                val i = Intent(Intent.ACTION_VIEW)
                i.data = Uri.parse(url)
                startActivity(i)
            }
            R.id.menu_entries__refresh -> {
                startRefresh()
            }
            R.id.menu_entries__settings -> {
                navigator.goToSettings()
            }
            // STEVE
            R.id.menu_entries__all_read -> {
                read_all_fab_do()
            }
            R.id.menu_entries__usage -> {
                startActivity<UsageActivity>()
            }
            R.id.menu_entries__all -> {
                context?.putPrefInt(VIEW_CURRENT, VIEW_ALL);
                requireActivity().invalidateOptionsMenu()
                listDisplayDate = Date().time
                initDataObservers();
            }

            R.id.menu_entries__unread -> {
                context?.putPrefInt(VIEW_CURRENT, VIEW_UNREAD);
                requireActivity().invalidateOptionsMenu()
                listDisplayDate = Date().time
                initDataObservers();
            }

            R.id.menu_entries__fav -> {
                context?.putPrefInt(VIEW_CURRENT, VIEW_FAV);
                requireActivity().invalidateOptionsMenu()
                listDisplayDate = Date().time
                initDataObservers();

            }
        }

        return true
    }

    fun setSelectedEntryId(selectedEntryId: String) {
        adapter.selectedEntryId = selectedEntryId
    }

    private fun refreshSwipeProgress() {
        refresh_layout.isRefreshing = context?.getPrefBoolean(PrefConstants.IS_REFRESHING, false)
                ?: false
    }
}
