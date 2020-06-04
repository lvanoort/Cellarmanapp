package com.lukevanoort.sample.ui.android

import android.content.Context
import android.text.format.DateFormat
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.annotation.AnyThread
import androidx.annotation.UiThread
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.lukevanoort.cellarman.sample.logic.SampleListViewModel
import com.lukevanoort.cellarman.sample.logic.SampleListViewState
import com.lukevanoort.sample.model.Sample
import com.lukevanoort.stuntman.SMView
import io.reactivex.disposables.Disposable
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.UUID

class SampleListView : RecyclerView,
    SMView<SampleListViewState, SampleListViewModel> {
    private var currentState : SampleListViewState = SampleListViewState.HasList(emptyList())
    private var nextState : SampleListViewState = SampleListViewState.HasList(emptyList())

    private var vm : SampleListViewModel? = null
    private var disp : Disposable? = null

    constructor(context: Context) : super(context) { initialize(context) }
    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs) { initialize(context) }
    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(
        context,
        attrs,
        defStyleAttr
    ) { initialize(context) }

    private fun initialize(ctx: Context) {
        layoutManager = LinearLayoutManager(ctx, LinearLayoutManager.VERTICAL, false)
        adapter = vlAdapter
    }

    override fun bindViewModel(vm : SampleListViewModel) {
        disp?.dispose()
        this.vm = vm
        disp = vm.getState().subscribe {
            setState(it)
        }
    }

    override fun unbindViewModel() {
        disp?.dispose()
        this.vm = null
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        unbindViewModel()
    }

    @AnyThread
    override fun setState (state : SampleListViewState) {
        this.nextState = state
        post { reset() }
    }

    @UiThread
    private fun reset() {
        val localNext = nextState
        val localCurrent = currentState
        if (localNext != localCurrent) {
            currentState = localNext
            vlAdapter.notifyDataSetChanged()
        }
    }

    private val dateFormatter = SimpleDateFormat(DateFormat.getBestDateTimePattern(Locale.getDefault(),"hh:mm aa dd MMM yyyy"))
    private inner class VLVH(val view : View) : RecyclerView.ViewHolder(view) {
        private var currentUUID: UUID? = null
        init {
            view.setOnClickListener {
                currentUUID?.let {
                    vm?.sampleSelected(it)
                }
            }
        }

        fun bind(v: Sample) {
            currentUUID = v.id
            view.findViewById<TextView>(R.id.tv_sample_notes).text = v.notes

            view.findViewById<TextView>(R.id.tv_sample_date).text = dateFormatter.format(v.time)
        }
    }

    private val vlAdapter = object : RecyclerView.Adapter<VLVH>() {
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VLVH {
            return VLVH(
                LayoutInflater.from(parent.context).inflate(
                    R.layout.sample_list_item_layout,
                    parent,
                    false
                )
            )
        }

        override fun getItemCount(): Int {
            val l = currentState
            return when(l) {
                is SampleListViewState.HasList -> l.samples.size
            }
        }

        override fun onBindViewHolder(holder: VLVH, position: Int) {
            val l = currentState
            return when(l) {
                is SampleListViewState.HasList -> {
                    holder.bind(l.samples[position])
                }
            }
        }

    }
}