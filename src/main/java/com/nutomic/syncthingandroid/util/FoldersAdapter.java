package com.nutomic.syncthingandroid.util;

import android.content.Context;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import com.nutomic.syncthingandroid.BuildConfig;
import com.nutomic.syncthingandroid.R;
import com.nutomic.syncthingandroid.syncthing.RestApi;

import java.util.HashMap;

import static android.view.View.GONE;
import static android.view.View.VISIBLE;
import static com.nutomic.syncthingandroid.syncthing.RestApi.readableFileSize;

/**
 * Generates item views for folder items.
 */
public class FoldersAdapter extends ArrayAdapter<RestApi.Folder>
        implements RestApi.OnReceiveModelListener {

    private HashMap<String, RestApi.Model> mModels = new HashMap<>();
    private LayoutInflater mInflater;

    public FoldersAdapter(Context context) {
        super(context, R.layout.item_folder_list);
        mInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if (convertView == null)
            convertView = mInflater.inflate(R.layout.item_folder_list, parent, false);

        TextView id = (TextView) convertView.findViewById(R.id.id);
        TextView state = (TextView) convertView.findViewById(R.id.state);
        TextView directory = (TextView) convertView.findViewById(R.id.directory);
        TextView items = (TextView) convertView.findViewById(R.id.items);
        TextView size = (TextView) convertView.findViewById(R.id.size);
        TextView invalid = (TextView) convertView.findViewById(R.id.invalid);

        RestApi.Folder folder = getItem(position);
        RestApi.Model model = mModels.get(folder.id);
        id.setText(folder.id);
        state.setTextColor(getContext().getResources().getColor(R.color.text_green));
        directory.setText(folder.path);
        if (model != null) {
            int percentage = (model.globalBytes != 0)
                    ? Math.round(100 * model.inSyncBytes / model.globalBytes)
                    : 100;
            state.setText(getLocalizedState(getContext(), model.state, percentage));
            items.setVisibility(VISIBLE);
            items.setText(getContext()
                    .getString(R.string.files, model.inSyncFiles, model.globalFiles));
            size.setVisibility(VISIBLE);
            size.setText(getContext().getString(R.string.folder_size_format,
                    readableFileSize(getContext(), model.inSyncBytes),
                    readableFileSize(getContext(), model.globalBytes)));
            setTextOrHide(invalid, model.invalid);
        } else {
            items.setVisibility(GONE);
            size.setVisibility(GONE);
            setTextOrHide(invalid, folder.invalid);
        }

        return convertView;
    }

    /**
     * Returns the folder's state as a localized string.
     */
    public static String getLocalizedState(Context c, String state, int percentage) {
        switch (state) {
            case "idle":     return c.getString(R.string.state_idle);
            case "scanning": return c.getString(R.string.state_scanning);
            case "cleaning": return c.getString(R.string.state_cleaning);
            case "syncing":  return c.getString(R.string.state_syncing, percentage);
            case "error":    return c.getString(R.string.state_error);
            case "unknown":  // Fallthrough
            case "":         return c.getString(R.string.state_unknown);
        }
        if (BuildConfig.DEBUG) {
            throw new AssertionError("Unexpected folder state " + state);
        }
        return "";
    }

    /**
     * Requests updated model info from the api for all visible items.
     */
    public void updateModel(RestApi api) {
        for (int i = 0; i < getCount(); i++) {
            api.getModel(getItem(i).id, this);
        }
    }

    @Override
    public void onReceiveModel(String folderId, RestApi.Model model) {
        mModels.put(folderId, model);
        notifyDataSetChanged();
    }

    private void setTextOrHide(TextView view, String text) {
        if (TextUtils.isEmpty(text)) {
            view.setVisibility(GONE);
        } else {
            view.setText(text);
            view.setVisibility(VISIBLE);
        }
    }

}
