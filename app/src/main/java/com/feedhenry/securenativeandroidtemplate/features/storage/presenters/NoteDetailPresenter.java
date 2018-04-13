package com.feedhenry.securenativeandroidtemplate.features.storage.presenters;

import com.feedhenry.securenativeandroidtemplate.R;
import com.feedhenry.securenativeandroidtemplate.domain.callbacks.CallbackHandler;
import com.feedhenry.securenativeandroidtemplate.domain.models.Note;
import com.feedhenry.securenativeandroidtemplate.domain.services.NoteCrudlService;
import com.feedhenry.securenativeandroidtemplate.features.storage.views.NoteDetailAppView;
import com.feedhenry.securenativeandroidtemplate.mvp.presenters.BasePresenter;

import javax.inject.Inject;

/**
 * Implement the presenter for the note details view.
 * It will be responsible for read/update/save the note in the repository, and update the view accordingly.
 */

public class NoteDetailPresenter extends BasePresenter<NoteDetailAppView> {

    private NoteCrudlService noteCrudlService;

    @Inject
    NoteDetailPresenter(NoteCrudlService noteCrudlService) {
        this.noteCrudlService = noteCrudlService;
    }

    public void createNote(String noteTitle, String noteContent, int storeType) {
        this.view.showLoading();
        Note note = new Note(noteTitle, noteContent);
        this.noteCrudlService.createNote(note, storeType, new CallbackHandler<Note>() {
            @Override
            public void onSuccess(Note note) {
                view.hideLoading();
                view.showMessage(R.string.info_note_saved);
                view.onNoteSaved(note);
            }

            @Override
            public void onError(Throwable error) {
                view.hideLoading();
                view.showMessage(R.string.error_failed_save_note);
            }
        });
        for (int i = 0; i < 100; i++) {
            this.noteCrudlService.createNote(new Note("aa" + i, "bb" + i), storeType, new CallbackHandler<Note>() {
                @Override
                public void onSuccess(Note note) {
//                    view.hideLoading();
//                    view.showMessage(R.string.info_note_saved);
//                    view.onNoteSaved(note);
                }

                @Override
                public void onError(Throwable error) {
//                    view.hideLoading();
//                    view.showMessage(R.string.error_failed_save_note);
                }
            });

        }
    }

    public void loadNoteWithId(String noteId, int storeType) {
        this.view.showLoading();
        this.noteCrudlService.readNote(noteId, storeType, new CallbackHandler<Note>() {
            @Override
            public void onSuccess(Note note) {
                view.hideLoading();
                view.loadNote(note);
            }

            @Override
            public void onError(Throwable error) {
                view.hideLoading();
                view.showMessage(R.string.error_failed_read_note);
            }
        });
    }

    public void updateNote(Note note) {
        this.view.showLoading();
        this.noteCrudlService.updateNote(note, new CallbackHandler<Note>() {
            @Override
            public void onSuccess(Note updatedNote) {
                view.hideLoading();
                view.showMessage(R.string.info_note_saved);
                view.onNoteSaved(updatedNote);
            }

            @Override
            public void onError(Throwable error) {
                view.showMessage(R.string.error_failed_save_note);
            }
        });
    }

    public void deleteNote(final Note note) {
        this.view.showLoading();
        this.noteCrudlService.deleteNote(note, new CallbackHandler<Note>() {
            @Override
            public void onSuccess(Note models) {
                view.hideLoading();
                view.showMessage(R.string.info_note_deleted);
                view.onNoteSaved(note);
            }

            @Override
            public void onError(Throwable error) {
                view.showMessage(R.string.error_failed_delete_note);
            }
        });
    }
}
