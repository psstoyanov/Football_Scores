package barqsoft.footballscores.Fragments;

import android.app.Activity;
import android.content.Intent;
import android.content.res.TypedArray;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.AbsListView;
import android.widget.TextView;

import com.yqritc.recyclerviewflexibledivider.HorizontalDividerItemDecoration;

import barqsoft.footballscores.Activities.MainActivity;
import barqsoft.footballscores.Database.DatabaseContract;
import barqsoft.footballscores.R;
import barqsoft.footballscores.Adapters.RecyclerScoresAdapter;
import barqsoft.footballscores.Services.myFetchService;
import barqsoft.footballscores.Sync.FootballScoresSyncAdapter;

/**
 * A placeholder fragment containing a simple view.
 */
public class MainScreenFragment extends Fragment implements LoaderManager.LoaderCallbacks<Cursor> {
    public static final String LOG_TAG = MainScreenFragment.class.getSimpleName();

    public RecyclerScoresAdapter mRecyclerScoresAdapter;
    public static final int SCORES_LOADER = 0;
    public static final int COL_ID = 8;
    private String[] fragmentdate = new String[1];
    private int last_selected_item = -1;

    private RecyclerView mRecyclerView;
    private RecyclerView.LayoutManager mLayoutManager;
    private int mPosition = RecyclerView.NO_POSITION;
    private boolean mUseTodayLayout, mAutoSelectView;
    private int mChoiceMode;
    private static final String SELECTED_KEY = "selected_position";


    public MainScreenFragment() {
    }

    /**
     * A callback interface that all activities containing this fragment must
     * implement. This mechanism allows activities to be notified of item
     * selections.
     */
    public interface Callback {
        /**
         * DetailFragmentCallback for when an item has been selected.
         */
        public void onItemSelected(Long matchId);
    }

    private void update_scores() {
        //Intent service_start = new Intent(getActivity(), myFetchService.class);
        //getActivity().startService(service_start);

        //FootballScoresSyncAdapter.syncImmediately(getActivity());
    }

    public void setFragmentDate(String date) {
        fragmentdate[0] = date;
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             final Bundle savedInstanceState) {
        update_scores();
        //final ListView score_list = (ListView) rootView.findViewById(R.id.scores_list);

        // The ForecastAdapter will take data from a source and
        // use it to populate the RecyclerView it's attached to.

        View rootView = inflater.inflate(R.layout.fragment_main, container, false);


        mRecyclerView = (RecyclerView) rootView.findViewById(R.id.scores_recyclerview);
        // Set the layout manager
        mRecyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));
        View emptyView = rootView.findViewById(R.id.recyclerview_scores_empty);

        mRecyclerScoresAdapter = new RecyclerScoresAdapter(getActivity(), new RecyclerScoresAdapter.ScoresAdapterOnClickHandler() {
            @Override
            public void onClick(Long match_Id, RecyclerScoresAdapter.ScoresAdapterViewHolder vh) {
                //String locationSetting = Utility.getPreferredLocation(getActivity());

                mPosition = vh.getAdapterPosition();
                Log.v(LOG_TAG, String.valueOf(mPosition));

                ((Callback) getActivity()).onItemSelected(match_Id);

            }
        }, emptyView, mChoiceMode);

        if (savedInstanceState != null) {
            if (savedInstanceState.containsKey(SELECTED_KEY)) {
                // The Recycler View probably hasn't even been populated yet.  Actually perform the
                // swapout in onLoadFinished.
                mPosition = savedInstanceState.getInt(SELECTED_KEY);
            }
            mRecyclerScoresAdapter.onRestoreInstanceState(savedInstanceState);
        }


        getLoaderManager().initLoader(SCORES_LOADER, null, this);
        mRecyclerScoresAdapter.detail_match_id = MainActivity.selected_match_id;
        mRecyclerView.setAdapter(mRecyclerScoresAdapter);
        mRecyclerView.addItemDecoration(new HorizontalDividerItemDecoration.Builder(getContext()).build());
        return rootView;
    }


    @Override
    public void onSaveInstanceState(Bundle outState) {
        // When tablets rotate, the currently selected list item needs to be saved.
        // When no item is selected, mPosition will be set to RecyclerView.NO_POSITION,
        // so check for that before storing.
        if (mPosition != RecyclerView.NO_POSITION) {
            outState.putInt(SELECTED_KEY, mPosition);
        }
        mRecyclerScoresAdapter.onSaveInstanceState(outState);
        super.onSaveInstanceState(outState);
    }


    @Override
    public void onInflate(Activity activity, AttributeSet attrs, Bundle savedInstanceState) {
        super.onInflate(activity, attrs, savedInstanceState);
        TypedArray a = activity.obtainStyledAttributes(attrs, R.styleable.MainScreenFragment, 0, 0);
        mChoiceMode = a.getInt(R.styleable.MainScreenFragment_android_choiceMode, AbsListView.CHOICE_MODE_NONE);
        mAutoSelectView = a.getBoolean(R.styleable.MainScreenFragment_autoSelectView, false);
        a.recycle();
    }

    @Override
    public Loader<Cursor> onCreateLoader(int i, Bundle bundle) {
        return new CursorLoader(getActivity(),
                DatabaseContract.scores_table.buildScoreWithDate(),
                null,
                null,
                fragmentdate,
                null);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> cursorLoader, Cursor data) {
        mRecyclerScoresAdapter.swapCursor(data);

        if (mPosition != RecyclerView.NO_POSITION) {
            // If we don't need to restart the loader, and there's a desired position to restore
            // to, do so now.
            mRecyclerView.smoothScrollToPosition(mPosition);
        }

        updateEmptyView();

        if (data.getCount() > 0) {
            mRecyclerView.getViewTreeObserver().addOnPreDrawListener(new ViewTreeObserver.OnPreDrawListener() {
                @Override
                public boolean onPreDraw() {
                    // Since we know we're going to get items, we keep the listener around until
                    // we see Children.
                    if (mRecyclerView.getChildCount() > 0) {
                        mRecyclerView.getViewTreeObserver().removeOnPreDrawListener(this);
                        int itemPosition = mRecyclerScoresAdapter.getSelectedItemPosition();
                        if (RecyclerView.NO_POSITION == itemPosition) itemPosition = 0;
                        RecyclerView.ViewHolder vh = mRecyclerView.findViewHolderForAdapterPosition(itemPosition);
                        if (null != vh && mAutoSelectView) {
                            mRecyclerScoresAdapter.selectView(vh);
                        }
                        return true;
                    }
                    return false;
                }
            });
        }
    }

    private void updateEmptyView() {
        if (mRecyclerScoresAdapter.getItemCount() == 0) {
            TextView tv = (TextView) getView().findViewById(R.id.recyclerview_scores_empty);
            if (null != tv) {
                // if cursor is empty, why? do we have an invalid location
                int message = R.string.empty_scores_list;
                tv.setText(message);
            }
        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> cursorLoader) {
        mRecyclerScoresAdapter.swapCursor(null);
    }


}
