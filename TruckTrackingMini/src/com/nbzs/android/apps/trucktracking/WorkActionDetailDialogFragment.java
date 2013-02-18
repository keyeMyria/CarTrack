package com.nbzs.android.apps.trucktracking;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.text.SpannableString;
import android.text.style.ForegroundColorSpan;
import android.text.style.UnderlineSpan;
import android.text.util.Linkify;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;
import com.nbzs.android.apps.SystemUtils;
import org.json.JSONObject;

/**
 * Created with IntelliJ IDEA.
 * User: Administrator
 * Date: 12-11-5
 * Time: 下午6:27
 * To change this template use File | Settings | File Templates.
 */
public class WorkActionDetailDialogFragment extends DialogFragment {
    public WorkActionDetailDialogFragment(JSONObject jData) {
        m_jData = jData;
    }

    JSONObject m_jData;

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        // Get the layout inflater
        LayoutInflater inflater = getActivity().getLayoutInflater();

        // Pass null as the parent view because its going in the dialog layout
        View v = inflater.inflate(R.layout.work_action_detail, null);
        try {
            TextView tv = (TextView) v.findViewById(R.id.workActionDetailAddress);
            SpannableString towedToAddress = new SpannableString(m_jData.getString("Address"));
            towedToAddress.setSpan(new UnderlineSpan(), 0,towedToAddress.length(), 0);
            towedToAddress.setSpan(new ForegroundColorSpan(tv.getLinkTextColors().getDefaultColor()), 0, towedToAddress.length(), 0);
            //tv.setText(m_jData.getString("Address"));
            //Linkify.addLinks(tv, Linkify.MAP_ADDRESSES);
            tv.setText(towedToAddress);
            tv.setClickable(true);
            tv.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    String address;
                    CharSequence text = ((TextView)v).getText();
                    int idx = text.toString().lastIndexOf(",");
                    if (idx != -1)
                    {
                        address = text.toString().substring(idx+1).trim();
                    }
                    else
                    {
                        address = text.toString();
                    }
                    Intent searchAddress = new Intent(Intent.ACTION_VIEW, Uri.parse("geo:0,0?q=" + address));
                    startActivity(searchAddress);
                }
            });

            tv = (TextView) v.findViewById(R.id.workActionDetailTel);
            tv.setText(m_jData.getString("Tel"));
            Linkify.addLinks(tv, Linkify.PHONE_NUMBERS);
            tv = (TextView) v.findViewById(R.id.workActionDetailDetail);
            tv.setText(m_jData.getString("Detail"));
            Linkify.addLinks(tv, Linkify.ALL);
        } catch (Exception e) {
            SystemUtils.processException(e);
        }
        builder.setView(v)
            // Add action buttons
            .setPositiveButton(R.string.Ok, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int id) {
                    // sign in the user ...
                }
            });
        return builder.create();
    }
}