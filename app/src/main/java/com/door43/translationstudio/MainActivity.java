package com.door43.translationstudio;

import com.door43.delegate.DelegateListener;
import com.door43.delegate.DelegateResponse;
import com.door43.translationstudio.panes.left.LeftPaneFragment;
import com.door43.translationstudio.panes.RightPaneFragment;
import com.door43.translationstudio.projects.Project;
import com.door43.translationstudio.translations.TranslationSyncResponse;
import com.door43.translationstudio.util.MainContextLink;
import com.door43.translationstudio.util.TranslatorBaseActivity;
import com.slidinglayer.SlidingLayer;


import android.app.Fragment;
import android.app.FragmentTransaction;
import android.graphics.PorterDuff;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.Timer;
import java.util.TimerTask;

public class MainActivity extends TranslatorBaseActivity implements DelegateListener {
    private final MainActivity me = this;

    private static final String LANG_CODE = "en"; // TODO: this will eventually need to be managed dynamically by the project manager

    // content panes
    private LeftPaneFragment mLeftPane;
    private RightPaneFragment mRightPane;
//    private TopPaneFragment mTopPane;
    private LinearLayout mCenterPane;

    private int mStackLevel = 0;

    // sliding layers
    private SlidingLayer mLeftSlidingLayer;
    private SlidingLayer mRightSlidingLayer;
//    private SlidingLayer mTopSlidingLayer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        app().getSharedTranslationManager().registerDelegateListener(this);

        // TODO: we should display a splash screen until the project manager has finished loading.
        // we should also not load the project manager when initialized but manually in the splash loader.
        // we should also generate the keys in the splash loader
        // Generate the ssh keys
        if(!app().hasKeys()) {
            app().generateKeys();
        }

        mCenterPane = (LinearLayout)findViewById(R.id.centerPane);

        initSlidingLayers();
        initPanes();

        // automatically open the last viewed frame when the app opens
        if(app().getUserPreferences().getBoolean(SettingsActivity.KEY_PREF_REMEMBER_POSITION, Boolean.parseBoolean(getResources().getString(R.string.pref_default_remember_position)))) {
            String frameId = app().getLastActiveFrame();
            String chapterId = app().getLastActiveChapter();
            String projectSlug = app().getLastActiveProject();
            app().getSharedProjectManager().setSelectedProject(projectSlug);
            app().getSharedProjectManager().getSelectedProject().setSelectedChapter(chapterId);
            app().getSharedProjectManager().getSelectedProject().getSelectedChapter().setSelectedFrame(frameId);
        }
        app().pauseAutoSave(true);
        reloadCenterPane();
        app().pauseAutoSave(false);
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    /**
     * Returns the left pane fragment
     * @return
     */
    public LeftPaneFragment getLeftPane() {
        return mLeftPane;
    }

    /**
     * Sets up the sliding effect between panes. Mostly just closing others when one opens
     */
    public void initSlidingLayers() {
//        mTopSlidingLayer = (SlidingLayer)findViewById(R.id.topPane);
        mLeftSlidingLayer = (SlidingLayer)findViewById(R.id.leftPane);
        mRightSlidingLayer = (SlidingLayer)findViewById(R.id.rightPane);

        // set up pane grips
        final ImageButton leftGrip = (ImageButton)findViewById(R.id.buttonGripLeft);
        final ImageButton rightGrip = (ImageButton)findViewById(R.id.buttonGripRight);
//        final ImageButton topGrip = (ImageButton)findViewById(R.id.buttonGripTop);

//        topGrip.setColorFilter(getResources().getColor(R.color.green), PorterDuff.Mode.SRC_ATOP);
        rightGrip.setColorFilter(getResources().getColor(R.color.purple), PorterDuff.Mode.SRC_ATOP);
        leftGrip.setColorFilter(getResources().getColor(R.color.blue), PorterDuff.Mode.SRC_ATOP);

//        topGrip.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View view) {
//                mTopSlidingLayer.openLayer(true);
//            }
//        });
        rightGrip.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mRightSlidingLayer.openLayer(true);
            }
        });
        leftGrip.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mLeftSlidingLayer.openLayer(true);
            }
        });

        // set up opening/closing
//        mTopSlidingLayer.setOnInteractListener(new SlidingLayer.OnInteractListener() {
//            @Override
//            public void onOpen() {
//                // bring self to front first to cover grip
//                mTopSlidingLayer.bringToFront();
//                mTopPane.onOpen();
//
//                mLeftSlidingLayer.closeLayer(true);
//                mLeftSlidingLayer.bringToFront();
//                mRightSlidingLayer.closeLayer(true);
//                mRightSlidingLayer.bringToFront();
//                leftGrip.bringToFront();
//                rightGrip.bringToFront();
//            }
//
//            @Override
//            public void onClose() {
//
//            }
//
//            @Override
//            public void onOpened() {
//
//            }
//
//            @Override
//            public void onClosed() {
//
//            }
//        });
        mLeftSlidingLayer.setOnInteractListener(new SlidingLayer.OnInteractListener() {
            @Override
            public void onOpen() {
                // bring self to front first to cover grip
                mLeftSlidingLayer.bringToFront();
                mLeftPane.onOpen();

//                mTopSlidingLayer.closeLayer(true);
//                mTopSlidingLayer.bringToFront();
                mRightSlidingLayer.closeLayer(true);
                mRightSlidingLayer.bringToFront();
//                topGrip.bringToFront();
                rightGrip.bringToFront();
            }

            @Override
            public void onClose() {

            }

            @Override
            public void onOpened() {

            }

            @Override
            public void onClosed() {

            }
        });
        mRightSlidingLayer.setOnInteractListener(new SlidingLayer.OnInteractListener() {
            @Override
            public void onOpen() {
                // bring self to front first to cover grip
                mRightSlidingLayer.bringToFront();
                mRightPane.onOpen();

                mLeftSlidingLayer.closeLayer(true);
                mLeftSlidingLayer.bringToFront();
//                mTopSlidingLayer.closeLayer(true);
//                mTopSlidingLayer.bringToFront();
                leftGrip.bringToFront();
//                topGrip.bringToFront();
            }

            @Override
            public void onClose() {

            }

            @Override
            public void onOpened() {

            }

            @Override
            public void onClosed() {

            }
        });
    }

    /**
     * set up the content panes
     */
    private void initPanes() {
//        mTopPane = new TopPaneFragment();
        mLeftPane = new LeftPaneFragment();
        mRightPane = new RightPaneFragment();


//        getFragmentManager().beginTransaction().replace(R.id.topPaneContent, mTopPane).commit();
        getFragmentManager().beginTransaction().replace(R.id.leftPaneContent, mLeftPane).commit();
        getFragmentManager().beginTransaction().replace(R.id.rightPaneContent, mRightPane).commit();

        // close the side panes when the center content is clicked
        findViewById(R.id.inputText).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                closePanes();
            }
        });
        TextView sourceText = ((TextView)findViewById(R.id.sourceText));
        sourceText.setMovementMethod(new ScrollingMovementMethod());
        sourceText.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                closePanes();
            }
        });
        final TextView helpText = (TextView)findViewById(R.id.helpTextView);

        // display help text when sourceText is empty.
        // TODO: enable/disable inputText as sourceText becomes available.
        sourceText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i2, int i3) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i2, int i3) {
                if(charSequence.length() > 0) {
                    helpText.setVisibility(View.GONE);
                } else {
                    helpText.setVisibility(View.VISIBLE);
                }
            }

            @Override
            public void afterTextChanged(Editable editable) {

            }
        });

        // automatically save changes to inputText
        final EditText inputText = (EditText)findViewById(R.id.inputText);

        inputText.addTextChangedListener(new TextWatcher() {
            private Timer timer = new Timer();

            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i2, int i3) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i2, int i3) {

            }

            @Override
            public void afterTextChanged(Editable editable) {
                int saveDelay = Integer.parseInt(app().getUserPreferences().getString(SettingsActivity.KEY_PREF_AUTOSAVE, getResources().getString(R.string.pref_default_autosave)));
                timer.cancel();
                if(saveDelay != -1) {
                    timer = new Timer();
                    if (!app().pauseAutoSave()) {
                        timer.schedule(new TimerTask() {
                            @Override
                            public void run() {
                                // save the changes
                                me.save();
                            }
                        }, saveDelay);
                    }
                }
            }
        });
    }

//    public void closeTopPane() {
//        if(mTopSlidingLayer != null) {
//            mTopSlidingLayer.closeLayer(true);
//        }
//    }

    public void closeLeftPane() {
        if (mLeftSlidingLayer != null) {
            mLeftSlidingLayer.closeLayer(true);
        }
        app().pauseAutoSave(true);
        reloadCenterPane();
        app().pauseAutoSave(false);
    }

    /**
     * Updates the center pane with the selected source frame text and any existing translations
     */
    public void reloadCenterPane() {
        // load source text
        TextView sourceText = (TextView)mCenterPane.findViewById(R.id.sourceText);
        sourceText.setText(app().getSharedProjectManager().getSelectedProject().getSelectedChapter().getSelectedFrame().getText());

        // load translation
        Project p = app().getSharedProjectManager().getSelectedProject();
        String translation = app().getSharedTranslationManager().getTranslation(p.getId(), LANG_CODE, p.getSelectedChapter().getSelectedFrame().getChapterFrameId());
        EditText inputText = (EditText)mCenterPane.findViewById(R.id.inputText);
        inputText.setText(translation);

        // updates preferences so the app opens to the last opened frame
        app().setActiveProject(app().getSharedProjectManager().getSelectedProject().getId());
        app().setActiveChapter(app().getSharedProjectManager().getSelectedProject().getSelectedChapter().getId());
        app().setActiveFrame(app().getSharedProjectManager().getSelectedProject().getSelectedChapter().getSelectedFrame().getId());
    }

    public void closeRightPane() {
        if(mRightSlidingLayer != null) {
            mRightSlidingLayer.closeLayer(true);
        }
    }

    /**
     * Closes all of the edge panes
     */
    public void closePanes() {
        if (mLeftSlidingLayer != null) mLeftSlidingLayer.closeLayer(true);
        if (mRightSlidingLayer != null) mRightSlidingLayer.closeLayer(true);
//        if (mTopSlidingLayer != null) mTopSlidingLayer.closeLayer(true);
    }

    /**
     * Saves the translated content found in inputText
     */
    public void save() {
        if (!app().pauseAutoSave()) {
            Log.d("Save", "Performing auto save");
            // do not allow saves to stack up when saves are running slowly.
            app().pauseAutoSave(true);
            String inputTextValue = ((EditText) findViewById(R.id.inputText)).getText().toString();
            Project p = app().getSharedProjectManager().getSelectedProject();

            // TODO: we need a way to manage what language the translation is being made in. This is different than the source languages
            app().getSharedTranslationManager().save(inputTextValue, p.getId(), LANG_CODE, p.getSelectedChapter().getSelectedFrame().getChapterFrameId());
            app().pauseAutoSave(false);
        }
    }

    /**
     * Override the device contextual menu button to open our custom menu
     * @param keyCode
     * @param event
     * @return
     */
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if(keyCode == KeyEvent.KEYCODE_MENU) {
            showContextualMenu();
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    /**
     * Displays the app contextual menu
     */
    public void showContextualMenu() {
        mStackLevel++;
        FragmentTransaction ft = getFragmentManager().beginTransaction();
        Fragment prev = getFragmentManager().findFragmentByTag("dialog");
        if (prev != null) {
            ft.remove(prev);
        }
        ft.addToBackStack(null);

        app().closeToastMessage();
        // Create and show the dialog.
        MenuDialogFragment newFragment = new MenuDialogFragment();
        newFragment.show(ft, "dialog");
    }

    @Override
    public void onDelegateResponse(String id, DelegateResponse response) {
        if(TranslationSyncResponse.class == response.getClass()) {
            if (((TranslationSyncResponse)response).isSuccess()) {
                showContextualMenu();
            } else {
                // error
            }
        }
    }

    @Override
    public void onDestroy() {
        app().getSharedTranslationManager().removeDelegateListener(this);
        super.onDestroy();
    }
}
