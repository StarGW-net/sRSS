package net.frju.flym.ui.discover

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.inputmethod.EditorInfo
import android.webkit.URLUtil
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.FragmentContainerView
import androidx.fragment.app.FragmentTransaction
import net.fred.feedex.R
import net.frju.flym.App
import net.frju.flym.data.entities.Feed
import net.frju.flym.data.entities.SearchFeedResult
import org.jetbrains.anko.design.snackbar
import org.jetbrains.anko.doAsync
import org.jetbrains.anko.sdk21.listeners.onClick
import org.jetbrains.anko.sdk21.listeners.onEditorAction
import org.jetbrains.anko.startActivity
import org.jetbrains.anko.uiThread
import java.util.*


class DiscoverActivity : AppCompatActivity(), FeedManagementInterface {

    companion object {

        @JvmStatic
        fun newInstance(context: Context) = context.startActivity<DiscoverActivity>()

        @JvmStatic
        fun newInstance(context: Context, query: String) =
                context.startActivity<DiscoverActivity>(FeedSearchFragment.ARG_QUERY to query)
    }

    private var searchInput: AutoCompleteTextView? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_feed_search)
        this.initSearchInputs()
        this.goDiscover()

        intent.getStringExtra(FeedSearchFragment.ARG_QUERY)?.let { searchForFeed(it) }
    }

    public fun triggerSearch(term: String) {
        goSearch(term)
   }

    public fun noResults()  {
        App.myLog("No search results for feed")

        var lv: FragmentContainerView = this.findViewById(R.id.fcv_discover_fragments)
        lv.setVisibility(View.GONE)

        var tt:  TextView = this.findViewById(R.id.feed_no_results_text)
        tt.setVisibility(View.VISIBLE)

        var results:  LinearLayout = this.findViewById(R.id.feed_no_results)
        results.setVisibility(View.VISIBLE)

        var button:  Button = this.findViewById(R.id.feed_add_manually)
        button.setVisibility(View.GONE)

        val text = searchInput?.text.toString()

        if (URLUtil.isNetworkUrl(text)) {
            App.myLog("Got a URL " + text)
            button.setVisibility(View.VISIBLE)

            button.onClick {
                val text = searchInput?.text.toString()
                App.myLog( "Add feed URL for " + text)
                searchInput?.let { it1 -> addFeed(it1, text, text) }
                finish()
            }

        }
    }

    public fun urlResults()  {
        // Manually add for now - but later load the URL and populate the ListView...
        App.myLog("search for results for feed")

        val text = searchInput?.text.toString()

        if (URLUtil.isNetworkUrl(text)) {
            var lv: FragmentContainerView = this.findViewById(R.id.fcv_discover_fragments)
            lv.setVisibility(View.GONE)

            var results:  LinearLayout = this.findViewById(R.id.feed_no_results)
            results.setVisibility(View.VISIBLE)

            var tt:  TextView = this.findViewById(R.id.feed_no_results_text)
            tt.setVisibility(View.GONE)

            var button:  Button = this.findViewById(R.id.feed_add_manually)
            button.setVisibility(View.GONE)

            App.myLog("Got a URL " + text)
            button.setVisibility(View.VISIBLE)

            button.onClick {
                val text = searchInput?.text.toString()
                App.myLog( "Add feed URL for " + text)
                searchInput?.let { it1 -> addFeed(it1, text, text) }
                finish()
            }

        }
    }
    private fun initSearchInputs() {
        var timer = Timer()
        searchInput = this.findViewById(R.id.et_search_input)
        searchInput?.let { searchInput ->

            // Handle IME Options Search Action
            searchInput.onEditorAction { _, actionId, event ->
                if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                    val term = searchInput.text.toString().trim()
                    if (term.isNotEmpty()) {
                        goSearch(term)
                    }
                    true
                } else
                    false
            }
/*
            // Handle search after N ms pause after input
            searchInput.textChangedListener {
                afterTextChanged {
                    val term = searchInput.text.toString().trim()
                    if (term.isNotEmpty()) {
                        timer.cancel()
                        timer = Timer()
                        timer.schedule(object : TimerTask() {
                            override fun run() {
                                App.myLog("Search for " + term)
                                goSearch(term)
                            }
                        }, 1000)
                    }
                }
            }
*/
            this.findViewById<Button>(R.id.btn_add_feed).onClick {
                val text = searchInput.text.toString()
                App.myLog("Search for " + text)

                if (URLUtil.isNetworkUrl(text)) {
                    App.myLog("Got a URL " + text)
                    urlResults()
                } else {
                    goSearch(text)
                }

            }




/*



            // Handle manually adding URL
            this.findViewById<Button>(R.id.btn_add_feed).onClick {
                val text = searchInput.text.toString()
                if (URLUtil.isNetworkUrl(text)) {
                    App.myLog("Got a URL " + text)
                    addFeed(searchInput, text, text)
                    searchInput.setText("")
                }
            }

 */
        }
    }

    private fun goSearch(query: String) {

        var results:  LinearLayout = this.findViewById(R.id.feed_no_results)
        results.setVisibility(View.GONE)

        var lv: FragmentContainerView = this.findViewById(R.id.fcv_discover_fragments)
        lv.setVisibility(View.VISIBLE)

        val currentFragment = supportFragmentManager.findFragmentById(R.id.fcv_discover_fragments)
        if (currentFragment is FeedSearchFragment) {
            currentFragment.search(query)
        } else {
            val fragment = FeedSearchFragment.newInstance(query)
            supportFragmentManager
                    .beginTransaction()
                    .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE)
                    .replace(R.id.fcv_discover_fragments, fragment, FeedSearchFragment.TAG)
                    .addToBackStack(DiscoverFragment.TAG)
                    .commitAllowingStateLoss()
        }
    }

    private fun goDiscover() {
        val currentFragment = supportFragmentManager.findFragmentById(R.id.fcv_discover_fragments)
        if (currentFragment !is DiscoverFragment) {
            val fragment = DiscoverFragment.newInstance()
            supportFragmentManager
                    .beginTransaction()
                    .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE)
                    .replace(R.id.fcv_discover_fragments, fragment, DiscoverFragment.TAG)
                    .commitAllowingStateLoss()
        }
    }

    override fun searchForFeed(query: String) {
        searchInput?.setText(query)
    }

    override fun addFeed(view: View, title: String, link: String) {
        doAsync {
            val feedToAdd = Feed(link = link, title = title)
            App.db.feedDao().insert(feedToAdd)
            uiThread {
                view.snackbar(R.string.feed_added)
            }
        }
    }

    override fun deleteFeed(view: View, feed: SearchFeedResult) {
        doAsync {
            App.db.feedDao().deleteByLink(feed.link)
        }
    }

    override fun previewFeed(view: View, feed: SearchFeedResult) {
        TODO("Not yet implemented")
    }
}