package com.lukevanoort.cellarman.app.canvases

import android.app.Activity
import android.view.View
import android.view.ViewGroup
import androidx.annotation.IdRes
import androidx.annotation.UiThread
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.lukevanoort.cellarman.app.R
import com.lukevanoort.cellarman.fill.ui.android.FillListView
import com.lukevanoort.cellarman.fill.logic.FillListViewModel
import com.lukevanoort.cellarman.fill.logic.FillListViewState
import com.lukevanoort.cellarman.sample.logic.SampleListViewModel
import com.lukevanoort.cellarman.sample.logic.SampleListViewState
import com.lukevanoort.sample.ui.android.SampleListView
import com.lukevanoort.cellarman.tasks.ManageVesselCanvas
import com.lukevanoort.cellarman.ui.common.android.SMButton
import com.lukevanoort.cellarman.vessel.logic.EditVesselState
import com.lukevanoort.cellarman.vessel.logic.EditVesselViewModel
import com.lukevanoort.cellarman.vessel.ui.android.EditVesselDataView
import com.lukevanoort.stuntman.*
import java.lang.IllegalArgumentException
import javax.inject.Inject

private const val LAYOUT_ID = R.layout.manage_vessel_canvas

class ManageVesselCanvasFactory @Inject constructor() {
    fun createCanvasInActivity(act:Activity) : ManageVesselCanvas {

        act.setContentView(LAYOUT_ID)
        return ManageVesselCanvasImpl(
            act.findViewById<ViewGroup>(
                R.id.manage_vessel_canvas
            )
        )
    }
}

class ManageVesselCanvasImpl constructor(val canvasRoot: ViewGroup) : ManageVesselCanvas,
    SMBackPressedListener {
    private val vmSyncLock: Any = Any()

    private val vesselViewBinder: SMVMBinder<EditVesselViewModel, SMView<EditVesselState, EditVesselViewModel>> =
        SMVMBinder()
    private val fillViewBinder: SMVMBinder<FillListViewModel, SMView<FillListViewState, FillListViewModel>> =
        SMVMBinder()
    private val sampleViewBinder: SMVMBinder<SampleListViewModel, SMView<SampleListViewState, SampleListViewModel>> =
        SMVMBinder()

    private var actionButton: SMButton = canvasRoot.findViewById<SMButton>(R.id.smb_action)
    private val addFillViewBinder: SMVMBinder<SimpleButtonVM, SMView<SimpleButtonState, SimpleButtonVM>> =
        SMVMBinder()
    private val addSampleViewBinder: SMVMBinder<SimpleButtonVM, SMView<SimpleButtonState, SimpleButtonVM>> =
        SMVMBinder()

    private var backVm: SimpleButtonVM? = null
    private val backBt: SMView<SimpleButtonState, SimpleButtonVM> = canvasRoot.findViewById<SMButton>(R.id.smb_goback)


    private val bnv = canvasRoot.findViewById<BottomNavigationView>(R.id.bnv_summary_tabs)
    private val vpChanger = canvasRoot.findViewById<ViewPager2>(R.id.vp2_summary_content)
    private val vpAdapter = SummaryAdapter()


    private sealed class SelectedTab {
        abstract fun getPagerIdx() : Int
        @IdRes
        abstract fun getBottomNavId() : Int

        object Vessel : SelectedTab() {
            override fun getPagerIdx(): Int =
                IDX_EDIT
            override fun getBottomNavId(): Int = R.id.menu_item_vessel
        }

        object Fills  : SelectedTab(){
            override fun getPagerIdx(): Int =
                IDX_FILLS
            override fun getBottomNavId(): Int =R.id.menu_item_fills
        }
        object Samples : SelectedTab(){
            override fun getPagerIdx(): Int =
                IDX_SAMPLES
            override fun getBottomNavId(): Int = R.id.menu_item_samples
        }

        companion object {
            fun fromPagerIdx(idx: Int) : SelectedTab? = when (idx) {
                IDX_EDIT -> Vessel
                IDX_FILLS -> Fills
                IDX_SAMPLES -> Samples
                else -> null
            }

            fun fromBottomNavID(id: Int) : SelectedTab? = when(id) {
                R.id.menu_item_vessel -> Vessel
                R.id.menu_item_fills -> Fills
                R.id.menu_item_samples -> Samples
                else -> null
            }

            const val IDX_EDIT = 0
            const val IDX_FILLS = 1
            const val IDX_SAMPLES = 2
        }
    }

    private var currentTab: SelectedTab? = null

    init {
        vpChanger.adapter = vpAdapter
        setSelectedTab(SelectedTab.Vessel)
        vpChanger.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                SelectedTab.fromPagerIdx(
                    position
                )?.let { setSelectedTab(it) }
            }
        })

        bnv.setOnNavigationItemSelectedListener { mi ->
            SelectedTab.fromBottomNavID(
                mi.itemId
            )?.let {
                setSelectedTab(it)
                true
            } ?: false
        }
    }

    @UiThread
    private fun setSelectedTab(selectedTab: SelectedTab) {
        val localCurrent = currentTab
        if (selectedTab != localCurrent) {
            currentTab = selectedTab // setting it here to avoid infinite loops from the changed listeners

            vpChanger.currentItem = selectedTab.getPagerIdx()
            bnv.selectedItemId = selectedTab.getBottomNavId()

//            selectedTab.getPagerIdx().takeIf { it != vpChanger.currentItem }?.let { vpChanger.currentItem = it }
//            selectedTab.getBottomNavId().takeIf { it != bnv.selectedItemId }?.let { bnv.selectedItemId = it }

            when (selectedTab) {
                SelectedTab.Vessel -> {
                    actionButton.text = ""
                    actionButton.visibility = View.INVISIBLE
                    addFillViewBinder.detachView()
                    addSampleViewBinder.detachView()
                }
                SelectedTab.Fills -> {
                    actionButton.text = "Add"
                    actionButton.visibility = View.VISIBLE
                    addSampleViewBinder.detachView()
                    addFillViewBinder.attachView(actionButton)
                }
                SelectedTab.Samples -> {
                    actionButton.text = "Add"
                    actionButton.visibility = View.VISIBLE
                    addFillViewBinder.detachView()
                    addSampleViewBinder.attachView(actionButton)
                }
            }?.let {  }

        }
    }

    override fun attachViewVesselViewModel(vm : EditVesselViewModel) {
        synchronized(vmSyncLock) {
            vesselViewBinder.bindVM(vm)
        }
    }

    override fun attachFillListViewModel(vm : FillListViewModel) {
        synchronized(vmSyncLock) {
            fillViewBinder.bindVM(vm)
        }
    }

    override fun attachSampleListViewModel(vm : SampleListViewModel) {
        synchronized(vmSyncLock) {
            sampleViewBinder.bindVM(vm)
        }
    }

    override fun attachBackButtonViewModel(vm: SimpleButtonVM) {
        synchronized(vmSyncLock) {
            backVm = vm
            backBt.bindViewModel(vm)
        }
    }

    override fun attachAddFillViewModel(vm: SimpleButtonVM) {
        synchronized(vmSyncLock) {
            addFillViewBinder.bindVM(vm)
        }
    }

    override fun attachAddSampleViewModel(vm: SimpleButtonVM) {
        synchronized(vmSyncLock) {
            addSampleViewBinder.bindVM(vm)
        }
    }

    override fun detachAllViewModels() {
        synchronized(vmSyncLock) {
            vesselViewBinder.unbindVM()
            fillViewBinder.unbindVM()
            sampleViewBinder.unbindVM()
            addSampleViewBinder.unbindVM()
            addFillViewBinder.unbindVM()
            backBt.unbindViewModel()
            backVm = null
        }
    }


    override fun backPressed(): Boolean {
        backVm?.buttonPressed()
        return true
    }

    private class SummaryVH(val v: com.lukevanoort.cellarman.vessel.ui.android.EditVesselDataView) : RecyclerView.ViewHolder(v) {

        companion object {
            fun inflateInto(parent: ViewGroup) : SummaryVH {
                return SummaryVH(
                    EditVesselDataView(
                        parent.context
                    ).also {
                        it.layoutParams = ViewGroup.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT
                        )
                    })
            }
        }
    }

    private class FillsVH(val v: FillListView) : RecyclerView.ViewHolder(v) {
        companion object {
            fun inflateInto(parent: ViewGroup) : FillsVH {
                return FillsVH(
                    FillListView(
                        parent.context
                    ).also {
                        it.layoutParams = ViewGroup.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT
                        )
                    })
            }
        }
    }

    private class SamplesVH(val v: com.lukevanoort.sample.ui.android.SampleListView) : RecyclerView.ViewHolder(v) {
        companion object {
            fun inflateInto(parent: ViewGroup) : SamplesVH {
                return SamplesVH(
                    SampleListView(
                        parent.context
                    ).also {
                        it.layoutParams = ViewGroup.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT
                        )
                    })
            }
        }
    }

    inner class SummaryAdapter : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder = when(SelectedTab.fromPagerIdx(
            viewType
        )) {
            is SelectedTab.Vessel -> {
                SummaryVH.inflateInto(
                    parent
                )
            }
            is SelectedTab.Fills -> {
                FillsVH.inflateInto(
                    parent
                )
            }
            is SelectedTab.Samples -> {
                SamplesVH.inflateInto(
                    parent
                )
            }
            else -> {
                throw IllegalArgumentException("invalid tab")
            }
        }

        override fun getItemCount(): Int = 3

        override fun getItemViewType(position: Int): Int = position

        override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
            when(SelectedTab.fromPagerIdx(
                position
            )) {
                is SelectedTab.Vessel -> {
                    (holder as? SummaryVH)?.v?.let { vesselViewBinder.attachView(it) }
                }
                is SelectedTab.Fills -> {
                    (holder as? FillsVH)?.v?.let { fillViewBinder.attachView(it) }
                }
                is SelectedTab.Samples -> {
                    (holder as? SamplesVH)?.v?.let { sampleViewBinder.attachView(it) }
                }
            }
        }
    }


}