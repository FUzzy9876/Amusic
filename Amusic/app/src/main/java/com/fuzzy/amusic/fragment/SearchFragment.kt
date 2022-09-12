package com.fuzzy.amusic.fragment

import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageButton
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.fuzzy.amusic.MainApplication.getCsvFileReader
import com.fuzzy.amusic.R
import com.fuzzy.amusic.adapter.SearchResultsRecycleAdapter
import com.fuzzy.amusic.adapter.SongsRecycleAdapter
import com.fuzzy.amusic.database.Song
import com.fuzzy.amusic.utils.CsvFileReader
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class SearchFragment : Fragment() {
    private val csvFileReader: CsvFileReader = getCsvFileReader()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_search, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // todo: 绑定事件
        requireView().findViewById<ImageButton>(R.id.search_button).setOnClickListener { onSearchButtonClicked() }
    }

    private fun onSearchButtonClicked() {
        val searchStr: String = requireView().findViewById<EditText>(R.id.search_text).text.toString()
        Log.i("SearchFragment / search", "searching $searchStr")
        search(searchStr)
    }

    private fun search(str: String) {
        CoroutineScope(Dispatchers.Default).launch {
            val searchResult: List<Song> = csvFileReader.search(str)
            withContext(Dispatchers.Main) {
                val layoutManager = LinearLayoutManager(context)
                layoutManager.orientation = LinearLayoutManager.VERTICAL

                val recyclerView = requireView().findViewById<RecyclerView>(R.id.search_result)
                recyclerView.layoutManager = layoutManager
                recyclerView.adapter = SearchResultsRecycleAdapter(searchResult)
            }
        }
    }
}