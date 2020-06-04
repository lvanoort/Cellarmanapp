package com.lukevanoort.cellarman.vessel.ui.android

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.annotation.AnyThread
import androidx.annotation.UiThread
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.lukevanoort.cellarman.vessel.logic.VesselListViewModel
import com.lukevanoort.cellarman.vessel.logic.VesselListViewState
import com.lukevanoort.cellarman.vessel.model.Vessel
import com.lukevanoort.stuntman.SMView
import io.reactivex.disposables.Disposable
import java.util.UUID

class VesselListView : RecyclerView,
    SMView<VesselListViewState, VesselListViewModel> {
    private var currentState : VesselListViewState = VesselListViewState.HasList(emptyList())
    private var nextState : VesselListViewState = VesselListViewState.HasList(emptyList())

    private var vm : VesselListViewModel? = null
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

    override fun bindViewModel(vm : VesselListViewModel) {
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
    override fun setState (state : VesselListViewState) {
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

    private inner class VLVH(val view : View) : RecyclerView.ViewHolder(view) {
        private var currentUUID: UUID? = null
        init {
            view.setOnClickListener {
                currentUUID?.let {
                    vm?.vesselSelected(it)
                }
            }
        }

        fun bind(v: Vessel) {
            currentUUID = v.id
            view.findViewById<TextView>(R.id.tv_vessel_name).text = v.name

            view.findViewById<TextView>(R.id.tv_vessel_description).text = v.getConstructionShortSummary(view.context)
        }
    }

    private val vlAdapter = object : RecyclerView.Adapter<VLVH>() {
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VLVH {
            return VLVH(
                LayoutInflater.from(parent.context).inflate(
                    R.layout.vessel_list_item_layout,
                    parent,
                    false
                )
            )
        }

        override fun getItemCount(): Int {
            val l = currentState
            return when(l) {
                is VesselListViewState.HasList -> l.vessels.size
            }
        }

        override fun onBindViewHolder(holder: VLVH, position: Int) {
            val l = currentState
            return when(l) {
                is VesselListViewState.HasList -> {
                    holder.bind(l.vessels[position])
                }
            }
        }

    }
}