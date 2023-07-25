package net.frju.flym.ui.discover

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.content.res.AppCompatResources
import androidx.fragment.app.Fragment
import net.fred.feedex.R
import net.frju.flym.App
import net.frju.flym.GlideApp
import net.frju.flym.data.entities.Feed
import org.jetbrains.anko.layoutInflater


class DiscoverFragment : Fragment(), AdapterView.OnItemClickListener {

    lateinit var myContext: Context

    companion object {
        const val TAG = "DiscoverFragment"

        @JvmStatic
        fun newInstance() = DiscoverFragment()
    }

    private lateinit var manageFeeds: FeedManagementInterface

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_discover, container, false)
        initGridView(view)
        return view
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)

        myContext = context

        manageFeeds = context as FeedManagementInterface
    }



    private fun initGridView(view: View) {
        val gvTopics: ListView = view.findViewById(R.id.gv_topics)
        val topics = view.context.resources.getStringArray(R.array.discover_topics)
        gvTopics.adapter = TopicAdapter(view.context, topics)
        gvTopics.onItemClickListener = this
    }

    override fun onItemClick(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
        val topic = parent?.getItemAtPosition(position) as String
        App.myLog("Calling searchForFeed " + topic)
        manageFeeds.searchForFeed("#$topic")
        (activity as DiscoverActivity?)?.triggerSearch("#$topic")
    }

    class TopicAdapter(context: Context, topics: Array<String>) :
            ArrayAdapter<String>(context, R.layout.item_discover_topic, topics) {

        private class ItemViewHolder {
            internal var image: ImageView? = null
            internal var title: TextView? = null
        }

        private fun setTopicTitle(viewHolder: ItemViewHolder, topic: String) {
            viewHolder.title?.text = topic
        }

        private fun setTopicImage(viewHolder: ItemViewHolder, topic: String) {
            val letterDrawable = Feed.getLetterDrawable(topic.hashCode().toLong(), topic)
            viewHolder.image?.let { iv ->
                GlideApp.with(context).clear(iv)

                val resID = context.resources.getIdentifier(topic.toLowerCase() , "drawable", context.getPackageName())

                if ((resID == null) || (resID == 0) )
                {
                    App.myLog("Missing icon for topic: " + topic)
                    iv.setImageResource(R.drawable.news)
                } else {
                    // val image: Drawable = AppCompatResources.getDrawable(resID)
                    iv.setImageDrawable(AppCompatResources.getDrawable(context, resID))
                }
                // iv.setImageResource(resID)

            }
        }

        fun XgetView(i: Int, view: View?, viewGroup: ViewGroup): View {
            val viewHolder: ItemViewHolder
            var inflatedView: View? = view
            if (inflatedView == null) {
                inflatedView = context.layoutInflater.inflate(R.layout.item_discover_topic, null)
                viewHolder = ItemViewHolder()
                inflatedView?.let { vw ->
                    viewHolder.image = vw.findViewById(R.id.iv_topic_image) as ImageView
                    viewHolder.title = vw.findViewById(R.id.tv_topic_title) as TextView
                }
            } else {
                viewHolder = inflatedView.tag as ItemViewHolder
            }
            val item = getItem(i)
            item?.let { it ->
                setTopicImage(viewHolder, it)
                setTopicTitle(viewHolder, it)
            }
            inflatedView?.tag = viewHolder
            return inflatedView!!
        }

        override fun getView(i: Int, view: View?, viewGroup: ViewGroup): View {
            val viewHolder: ItemViewHolder
            var inflatedView: View? = view
            if (inflatedView == null) {
                inflatedView = context.layoutInflater.inflate(R.layout.item_discover_topic, null)
                viewHolder = ItemViewHolder()
                inflatedView?.let { vw ->
                    viewHolder.image = vw.findViewById(R.id.iv_topic_image) as ImageView
                    viewHolder.title = vw.findViewById(R.id.tv_topic_title) as TextView
                }
            } else {
                viewHolder = inflatedView.tag as ItemViewHolder
            }
            val item = getItem(i)
            item?.let { it ->
                setTopicImage(viewHolder, it)
                setTopicTitle(viewHolder, it)
            }
            inflatedView?.tag = viewHolder
            return inflatedView!!
        }
    }
}