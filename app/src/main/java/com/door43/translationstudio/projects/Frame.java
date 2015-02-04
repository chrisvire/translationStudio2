package com.door43.translationstudio.projects;

import android.text.TextUtils;

import com.door43.translationstudio.R;
import com.door43.translationstudio.events.FrameTranslationStatusChangedEvent;
import com.door43.translationstudio.rendering.DefaultRenderer;
import com.door43.translationstudio.rendering.RenderingEngine;
import com.door43.translationstudio.rendering.USXRenderer;
import com.door43.translationstudio.spannables.VerseSpan;
import com.door43.translationstudio.util.FileUtilities;
import com.door43.translationstudio.util.Logger;
import com.door43.translationstudio.util.MainContext;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Frames encapsulates a specific piece of translated work
 */
public class Frame implements Model {
    private String mChapterFrameId;
    private String mText;
    private String mId;
    private String mChapterId;
    private Chapter mChapter;
    private Translation mTranslation;
    private TranslationNote mNotes;
    private ArrayList<String> mImportantTerms;
    public final Format format;
    private String mCachedDescription;
    private String mCachedTitle;

    /**
     * The format of the resource
     */
    public static enum Format {
        DEFAULT,
        USX
    }

    /**
     * Creates a new frame.
     * @param chapterFrameId This is a combination of the chapter id and frame id. e.g. 01-02 for chapter 1 frame 2.
     * @param text a short description of the frame
     */
    public Frame(String chapterFrameId, String image, String text, String format) {
        // parse id
        String[] pieces = chapterFrameId.split("-");
        if(pieces.length == 2) {
            mChapterId = pieces[0];
            mId = pieces[1];
        } else {
            Logger.w(this.getClass().getName(), "Invalid frame id " + chapterFrameId);
        }
        mChapterFrameId = chapterFrameId;
        mText = text;
        // identify text format
        if(format.toLowerCase().equals("usx")) {
            this.format = Format.USX;
        } else {
            this.format = Format.DEFAULT;
        }
    }

    /**
     * Sets the translation notes available for this frame
     * @param notes
     */
    public void setTranslationNotes(TranslationNote notes) {
        mNotes = notes;
    }

    /**
     * Returns the translation notes for this frame
     * @return
     */
    public TranslationNote getTranslationNotes() {
        return mNotes;
    }

    /**
     * Returns a list of key terms that exist within this frame.
     * @return
     */
    public ArrayList<String> getImportantTerms() {
        if(mImportantTerms == null) {
            return  new ArrayList<String>();
        } else {
            return mImportantTerms;
        }
    }

    /**
     * Adds a term to the list of important terms
     * @param term
     */
    public void addImportantTerm(String term) {
        if(mImportantTerms == null) mImportantTerms = new ArrayList<String>();
        if(!mImportantTerms.contains(term)) {
            mImportantTerms.add(term);
        }
    }

    /**
     * Stores the translated frame text
     * @param translation the translated text
     */
    public void setTranslation(String translation) {
        // the default is to use the project's target language
        setTranslation(translation, mChapter.getProject().getSelectedTargetLanguage());
    }

    /**
     * Stores the translated frame text.
     * Important! You should almost always use setTranslation(String translation) instead.
     * Only use this method if you know what you are doing.
     * @param translation the translated text
     * @param targetLanguage the language the text was translated to
     */
    public void setTranslation(String translation, Language targetLanguage) {
        if(mTranslation != null && !mTranslation.isLanguage(targetLanguage) && !mTranslation.isSaved()) {
            save();
        } else if(mTranslation != null && translation.equals(mTranslation.getText())){
            // don't do anything if the translation hasn't changed
            return;
        }
        mTranslation = new Translation(targetLanguage, translation);
    }

    /**
     * Returns this frames translation
     * @return
     */
    public Translation getTranslation() {
        if(mTranslation == null || !mTranslation.isLanguage(mChapter.getProject().getSelectedTargetLanguage())) {
            if(mTranslation != null) {
                save();
            }
            // load translation from disk
            try {
                String text = FileUtilities.getStringFromFile(getFramePath());
                mTranslation = new Translation(mChapter.getProject().getSelectedTargetLanguage(), text);
                mTranslation.isSaved(true);
            } catch (Exception e) {
                Logger.e(this.getClass().getName(), "failed to load the translation from disk", e);
            }
        }
        return mTranslation;
    }

    /**
     * Specifies the chapter this frame belongs to.
     * This can only be set once.
     * @param chapter
     */
    public void setChapter(Chapter chapter) {
        if(mChapter == null) mChapter = chapter;
    }

    /**
     * Returns the chapter this frame belongs to
     * @return
     */
    public Chapter getChapter() {
        return mChapter;
    }

    /**
     * Returns the path to the image file
     * @return
     */
    public String getImagePath() {
        return "sourceTranslations/"+getChapter().getProject().getId()+"/"+getChapter().getProject().getSelectedSourceLanguage().getId()+"/"+getChapter().getProject().getSelectedSourceLanguage().getSelectedResource().getId()+"/images/"+getChapterFrameId()+".jpg";
    }

    public String getDefaultImagePath() {
        return "sourceTranslations/"+getChapter().getProject().getId()+"/en/"+getChapter().getProject().getSelectedSourceLanguage().getSelectedResource().getId()+"/images/"+getChapterFrameId()+".jpg";
    }

    /**
     * Returns the frame text
     * @return
     */
    public String getText() {
        return mText;
    }

    /**
     * Returns the chapter-frame id
     * @return
     */
    public String getChapterFrameId() {
        return mChapterFrameId;
    }

    /**
     * Returns the frame id
     * @return
     */
    @Override
    public String getId() {
        return mId;
    }

    @Override
    public String getTitle() {
        if(mCachedTitle == null) {
            if(format == Format.USX) {
                // locate verse range
                Pattern pattern = Pattern.compile(VerseSpan.PATTERN);
                Matcher matcher = pattern.matcher(mText);
                int numVerses = 0;
                int startVerse = 0;
                int endVerse = 0;
                VerseSpan verse = null;
                while(matcher.find()) {
                    verse = new VerseSpan(matcher.group(1));
                    if(numVerses == 0) {
                        // first verse
                        startVerse = verse.getVerseNumber();
                    }
                    numVerses ++;
                }
                if(verse != null) {
                    endVerse = verse.getVerseNumber();
                }
                if(startVerse == 0 || endVerse == 0) {
                    mCachedTitle = mId;
                } else if(startVerse == endVerse) {
                    mCachedTitle = startVerse + "";
                } else {
                    mCachedTitle = startVerse + "-" + endVerse;
                }
            } else {
                mCachedTitle = mId;
            }
        }
        return mCachedTitle;
    }

    @Override
    public CharSequence getDescription() {
        if(mCachedDescription == null) {
            RenderingEngine renderer;
            if (format == Format.USX) {
                renderer = new USXRenderer();
            } else {
                renderer = new DefaultRenderer();
            }
            CharSequence out = renderer.render(mText);
            if (out.length() > 93) {
                mCachedDescription = out.subSequence(0, 90).toString().trim() + "...";
            } else {
                mCachedDescription = out.toString();
            }
        }
        return mCachedDescription;
    }

    /**
     * Returns the chapter id
     * @return
     */
    public String getChapterId() {
        return mChapterId;
    }

    /**
     * Saves the frame to the disk
     */
    public void save() {
        if(mTranslation != null && !mTranslation.isSaved()) {
            mTranslation.isSaved(true);
            File file = new File(getFramePath(mTranslation.getLanguage()));
            if(mTranslation.getText().isEmpty()) {
                // delete empty file
                if(file.exists()) {
                    file.delete();
                    MainContext.getEventBus().post(new FrameTranslationStatusChangedEvent());
                    mChapter.cleanDir(mTranslation.getLanguage());
                }
            } else {
                // write translation
                if(!file.exists()) {
                    file.getParentFile().mkdirs();
                }  try {
                    Boolean notifyTranslationChanged = false;
                    if(!file.exists()) {
                        notifyTranslationChanged = true;
                    }
                    file.createNewFile();
                    PrintStream ps = new PrintStream(file);
                    ps.print(mTranslation.getText());
                    ps.close();
                    if(notifyTranslationChanged) {
                        MainContext.getEventBus().post(new FrameTranslationStatusChangedEvent());
                        mChapter.cleanDir(mTranslation.getLanguage());
                    }
                } catch (IOException e) {
                    Logger.e(this.getClass().getName(), "failed to write the translation to disk", e);
                }
            }
        }
    }

    /**
     * Returns the path to the frame file
     * @param projectId
     * @param languageId
     * @param chapterId
     * @param frameId
     * @return
     */
    public static String getFramePath(String projectId, String languageId, String chapterId, String frameId) {
        return Project.getRepositoryPath(projectId, languageId) + chapterId + "/" + frameId + ".txt";
    }

    /**
     * Returns the path to the frame file
     * @param targetLanguage
     * @return
     */
    private String getFramePath(Language targetLanguage) {
        return Frame.getFramePath(mChapter.getProject().getId(), targetLanguage.getId(), mChapterId, getId());
    }

    /**
     * Returns the path to the frame file for the currently selected target language
     * @return
     */
    private String getFramePath() {
        return Frame.getFramePath(mChapter.getProject().getId(), mChapter.getProject().getSelectedTargetLanguage().getId(), mChapterId, getId());
    }

    /**
     * Check if the frame is currently being translated
     * @return
     */
    public boolean isTranslating() {
        return !getTranslation().getText().isEmpty();
    }

    @Override
    public boolean isTranslatingGlobal() {
        return false;
    }

    @Override
    public String getType() {
        return "frame";
    }

    /**
     * Checks if this is the currently selected frame.
     * @return
     */
    @Override
    public boolean isSelected() {
        Project p = MainContext.getContext().getSharedProjectManager().getSelectedProject();
        if(p == null) return false;
        Chapter c = p.getSelectedChapter();
        if(c == null) return false;
        Frame f = c.getSelectedFrame();
        if(f == null) return false;
        return p.getId().equals(getChapter().getProject().getId()) && c.getId().equals(getChapter().getId()) && f.getId().equals(getId());
    }
}
