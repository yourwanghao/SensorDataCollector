
package sysnetlab.android.sdc.ui.fragments;

import sysnetlab.android.sdc.R;
import sysnetlab.android.sdc.datacollector.ExperimentManagerSingleton;
import sysnetlab.android.sdc.datacollector.Tag;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

public class ExperimentViewTagsFragment extends Fragment {
    private View mView;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);

        mView = inflater.inflate(R.layout.fragment_experiment_tag_viewing, container,
                false);

        LinearLayout layout = (LinearLayout) mView
                .findViewById(R.id.layout_fragment_tag_viewing_tags);

        for (Tag tag : ExperimentManagerSingleton.getInstance().getActiveExperiment().getTags()) {
            TextView tv = (TextView) inflater.inflate(R.layout.textview_experiment_tag, null);
            tv.setText(tag.getName());
            LinearLayout.LayoutParams layoutParams =
                    new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            layoutParams.setMargins(8, 0, 8, 0);
            tv.setTextAppearance(tv.getContext(), android.R.style.TextAppearance_Small);
            layout.addView(tv, layoutParams);
        }

        FragmentTransaction transaction = getFragmentManager().beginTransaction();
        transaction.commit();
        return mView;
    }
}