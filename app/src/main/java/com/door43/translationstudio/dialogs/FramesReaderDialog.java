package com.door43.translationstudio.dialogs;

import android.app.DialogFragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.ListView;

import com.door43.translationstudio.R;
import com.door43.translationstudio.projects.Chapter;
import com.door43.translationstudio.projects.Model;
import com.door43.translationstudio.projects.Project;
import com.door43.translationstudio.projects.SourceLanguage;
import com.door43.translationstudio.projects.data.IndexStore;
import com.door43.translationstudio.util.AppContext;
import com.door43.util.Logger;

/**
 * This class creates a dialog to display a list of frames
 */
public class FramesReaderDialog extends DialogFragment {
    public static final String ARG_PROJECT_ID = "project_id";
    public static final String ARG_CHAPTER_ID = "chapter_id";
    public static final String ARG_DISPLAY_OPTION_ORDINAL = "display_option";
    public static final String ARG_SELECTED_FRAME_INDEX = "selected_frame_index";

    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        getDialog().requestWindowFeature(Window.FEATURE_NO_TITLE);
        View v = inflater.inflate(R.layout.dialog_frame_reader, container, false);

        ListView list = (ListView)v.findViewById(R.id.listView);
        Bundle args = getArguments();
        Model[] frames = {};
        if(args != null) {
            String projectId = args.getString(ARG_PROJECT_ID, "-1");
            String chapterId = args.getString(ARG_CHAPTER_ID, "-1");
            int frameIndex = args.getInt(ARG_SELECTED_FRAME_INDEX, 0);
            int displayOrdinal = args.getInt(ARG_DISPLAY_OPTION_ORDINAL, FramesListAdapter.DisplayOption.SOURCE_TRANSLATION.ordinal());
            Project p = AppContext.projectManager().getProject(projectId);
            if(p != null) {
                Chapter c = p.getChapter(chapterId);
                if(c != null) {
                    if(displayOrdinal == FramesListAdapter.DisplayOption.DRAFT_TRANSLATION.ordinal()) {
                        if(IndexStore.hasIndex(p)) {
                            SourceLanguage draft = p.getSourceLanguageDraft(p.getSelectedTargetLanguage().getId());
                            if(draft != null) {
                                // TODO: users should be able to choose what resource they want to view. For now we will likely only have one resource.
                                frames = IndexStore.getFrames(p, draft, draft.getSelectedResource(), c);
                            }
                        } else {
                            // the project has not been indexed yet
                        }
                        // TODO: need to provide support to load the drafts here. We are waiting for the projects to be indexed so we don't have to load everything in memory.
                    } else {
                        frames = c.getFrames();
                    }
                }
            }
            list.setAdapter(new FramesListAdapter(AppContext.context(), frames, FramesListAdapter.DisplayOption.values()[displayOrdinal]));
        } else {
            Logger.w(this.getClass().getName(), "The dialog was not configured properly");
            list.setAdapter(new FramesListAdapter(AppContext.context(), new Model[]{}, FramesListAdapter.DisplayOption.SOURCE_TRANSLATION));
            dismiss();
        }
        return v;
    }
}
