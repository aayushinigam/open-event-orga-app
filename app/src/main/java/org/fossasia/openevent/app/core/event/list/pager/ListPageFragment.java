package org.fossasia.openevent.app.core.event.list.pager;

import android.arch.lifecycle.ViewModelProvider;
import android.arch.lifecycle.ViewModelProviders;
import android.content.Context;
import android.databinding.DataBindingUtil;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.view.ViewGroup;

import com.timehop.stickyheadersrecyclerview.StickyRecyclerHeadersDecoration;

import org.fossasia.openevent.app.R;
import org.fossasia.openevent.app.common.di.Injectable;
import org.fossasia.openevent.app.core.event.list.EventListFragment;
import org.fossasia.openevent.app.core.event.list.EventsViewModel;
import org.fossasia.openevent.app.core.event.list.sales.SalesSummaryFragment;
import org.fossasia.openevent.app.data.Bus;
import org.fossasia.openevent.app.data.event.Event;
import org.fossasia.openevent.app.databinding.FragmentListPageBinding;
import org.fossasia.openevent.app.ui.ViewUtils;

import java.util.List;

import javax.inject.Inject;

import static org.fossasia.openevent.app.core.event.list.EventListFragment.POSITION;

/**
 * A simple {@link android.app.Fragment} subclass.
 * Activities that contain this fragment must implement the
 * Use the {@link EventListFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
@SuppressWarnings("PMD.TooManyMethods")
public class ListPageFragment extends Fragment implements ListPageView, Injectable {

    @Inject
    Bus bus;

    @Inject
    ViewModelProvider.Factory viewModelFactory;

    EventsViewModel eventsViewModel;

    private FragmentListPageBinding binding;
    private RecyclerView recyclerView;

    private ListPageAdapter eventListAdapter;
    private RecyclerView.AdapterDataObserver adapterDataObserver;

    private Context context;
    private boolean initialized;

    private int position;

    private static final String SUMMARY_FRAGMENT_TAG = "summary";

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     * <p>
     * parameters can be added in future if required so
     * which can be passed in bundle.
     *
     * @return A new instance of fragment EventListFragment.
     */
    public static ListPageFragment newInstance() {
        return new ListPageFragment();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        context = getActivity();
        setHasOptionsMenu(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_list_page, container, false);

        position = getArguments().getInt(POSITION);
        eventsViewModel = ViewModelProviders.of(getActivity(), viewModelFactory).get(EventsViewModel.class);

        return binding.getRoot();
    }

    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.menu_events, menu);
    }

    @Override
    public void onStart() {
        super.onStart();
        setupRecyclerView();

        initialized = true;

        eventsViewModel.getEvents(position).observe(this, (events) -> {
            eventListAdapter.updateList(events);

            if (events == null || events.isEmpty())
                this.showEmptyView(true);
            else
                this.showEmptyView(false);
        });
    }

    @Override
    public void onStop() {
        super.onStop();
        eventListAdapter.unregisterAdapterDataObserver(adapterDataObserver);
    }

    private void setupRecyclerView() {
        if (!initialized) {
            eventListAdapter = new ListPageAdapter(eventsViewModel.getEvents(position).getValue(), bus, this);

            recyclerView = binding.eventRecyclerView;
            recyclerView.setLayoutManager(new LinearLayoutManager(context));
            recyclerView.setAdapter(eventListAdapter);
            recyclerView.addItemDecoration(new DividerItemDecoration(context, DividerItemDecoration.VERTICAL));
            recyclerView.setItemAnimator(new DefaultItemAnimator());
            StickyRecyclerHeadersDecoration decoration = new StickyRecyclerHeadersDecoration(eventListAdapter);
            recyclerView.addItemDecoration(decoration);

            adapterDataObserver = new RecyclerView.AdapterDataObserver() {
                @Override
                public void onChanged() {
                    decoration.invalidateHeaders();
                }
            };
        }
        eventListAdapter.registerAdapterDataObserver(adapterDataObserver);
    }

    @Override
    public void showResults(List<Event> events) {
        eventListAdapter.updateList(events);
    }

    @Override
    public void showEmptyView(boolean show) {
        ViewUtils.showView(binding.emptyView, show);
    }

    @Override
    public void openSalesSummary(Long eventId) {
        DialogFragment fragment = SalesSummaryFragment.newInstance(eventId);
        fragment.show(getFragmentManager(), SUMMARY_FRAGMENT_TAG);
    }

    @Override
    public void closeSalesSummary() {
        DialogFragment fragment = ((DialogFragment) getFragmentManager().findFragmentByTag(SUMMARY_FRAGMENT_TAG));
        if (fragment != null)
            fragment.dismiss();
    }
}
