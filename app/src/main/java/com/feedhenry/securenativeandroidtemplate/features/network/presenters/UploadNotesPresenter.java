package com.feedhenry.securenativeandroidtemplate.features.network.presenters;

import android.os.AsyncTask;
import android.util.Log;

import com.feedhenry.securenativeandroidtemplate.R;
import com.feedhenry.securenativeandroidtemplate.domain.Constants;
import com.feedhenry.securenativeandroidtemplate.domain.configurations.ApiServerConfiguration;
import com.feedhenry.securenativeandroidtemplate.domain.configurations.AppConfiguration;
import com.feedhenry.securenativeandroidtemplate.domain.models.Note;
import com.feedhenry.securenativeandroidtemplate.domain.repositories.NoteRepository;
import com.feedhenry.securenativeandroidtemplate.features.network.views.UploadNotesView;
import com.feedhenry.securenativeandroidtemplate.mvp.presenters.BasePresenter;

import org.aerogear.mobile.auth.AuthService;
import org.aerogear.mobile.auth.user.UserPrincipal;
import org.aerogear.mobile.core.MobileCore;
import org.aerogear.mobile.core.http.HttpRequest;
import org.aerogear.mobile.core.http.HttpResponse;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import javax.inject.Inject;

import okhttp3.MediaType;

/**
 * Created by weili on 27/10/2017.
 */

public class UploadNotesPresenter extends BasePresenter<UploadNotesView> {

    private static final String TAG = "UploadNotesPresenter";
    public static final MediaType JSON
            = MediaType.parse("application/json; charset=utf-8");

    private NoteRepository noteRepository;
    private ApiServerConfiguration apiServerConfiguration;

    private UploadNotesTask uploadNotesTask;

    @Inject
    AuthService authService;

    @Inject
    MobileCore mobileCore;      // TODO: constructor inject or field inject?

    @Inject
    public UploadNotesPresenter(NoteRepository noteRepos, AppConfiguration appConfiguration){
        this.noteRepository = noteRepos;
        this.apiServerConfiguration = appConfiguration.getAPIServerConfiguration();
    }

    public void checkClientStatus() {
        new ClientStatusCheckerTask().execute();
    }

    public void uploadNotes() {
        uploadNotesTask = new UploadNotesTask();
        uploadNotesTask.execute();
    }

    public void cancelUpload() {
        if (uploadNotesTask != null) {
            uploadNotesTask.cancel(true);
        }
    }

    private class ClientStatus {
        private boolean hasPermission;
        private long numberOfNotes;

        ClientStatus(boolean hasPerm, long numberOfNotes) {
            this.hasPermission = hasPerm;
            this.numberOfNotes = numberOfNotes;
        }
    }

    private class ClientStatusCheckerTask extends AsyncTask<Void, Void, ClientStatus> {

        private Exception error;
        String requiredRole = Constants.ACCESS_CONTROL_ROLES.ROLE_API_ACCESS;

        @Override
        protected void onPreExecute() {
            if (view != null) {
                view.showLoading();
            }
        }

        // tag::checkPermission[]
        @Override
        protected ClientStatus doInBackground(Void... voids) {
            ClientStatus status = null;
            try {
                UserPrincipal user = authService.currentUser();
                if (user != null ) {
                    boolean hasPermission = user.hasRealmRole(requiredRole);
                    long numberOfNotes = noteRepository.count();
                    status = new ClientStatus(hasPermission, numberOfNotes);
                    return status;
                }
            } catch (Exception e) {
                Log.e(TAG, "Error - Exception", e);
                this.error = e;
            }
            return status;
        }
        // end::checkPermission[]

        @Override
        protected void onPostExecute(ClientStatus clientStatus) {
            if (view != null) {
                view.hideLoading();
                if (this.error != null) {
                    view.showMessage(this.error.getMessage());
                } else {
                    view.renderPermissionMessage(clientStatus.hasPermission, requiredRole);
                    view.renderNotesMessage(clientStatus.numberOfNotes);
                }
            }
        }
    }

    private class UploadNotesTask extends AsyncTask<Void, Long, Long> {

        private AtomicInteger uploaded = new AtomicInteger(0);
        private Exception error;

        @Override
        protected void onPreExecute() {
            if (view != null) {
                view.showProgressBar();
            }
        }

        // tag::invokeAPI[]
        @Override
        protected Long doInBackground(Void... voids) {
            String apiUrl = apiServerConfiguration.getNoteAPIUrl();

            UserPrincipal user = authService.currentUser();
            if (user != null ) {
                String accessToken = user.getAccessToken();
                try {
                    List<Note> notes = noteRepository.listNotes();
                    long totalNumber = notes.size();
                    final AtomicLong currentCount = new AtomicLong(0);
                    for (Note note : notes) {
                        if (isCancelled() || this.error != null) {
                            break;
                        }
                        Note readNote = noteRepository.readNote(note.getId());
                        HttpRequest httpRequest = mobileCore.getHttpLayer().newRequest();
                        httpRequest.addHeader("Authorization", String.format("Bearer %s", accessToken));
                        httpRequest.post(apiUrl, readNote.toJson(true).toString().getBytes("UTF-8"));
                        HttpResponse httpResponse = httpRequest.execute();

                        httpResponse.onError(() -> {
                            UploadNotesTask.this.error = new Exception(httpResponse.stringBody());
                        });
                        httpResponse.onSuccess(() -> {
                            uploaded.incrementAndGet();
                        });
                        httpResponse.onComplete(() -> {
                            long progress = currentCount.incrementAndGet();
                            publishProgress(progress, totalNumber);
                        });

                        httpResponse.waitForCompletionAndClose();
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Error - Exception", e);
                    this.error = e;
                }
            }
            return uploaded.longValue();
        }
        // end::invokeAPI[]

        @Override
        protected void onPostExecute(Long completed) {
            if (view != null) {
                view.hideProgressBar();
                if (this.error != null) {
                    view.updateProgressDesc(this.error.getMessage());
                    view.showMessage(this.error.getMessage());
                } else {
                    view.showMessage(R.string.upload_complete, completed);
                }
            }
        }

        @Override
        protected void onProgressUpdate(Long... progress) {
            long completed = progress[0];
            long total = progress[1];
            if (view != null) {
                view.updateProgress(completed, total);
            }
        }

        @Override
        protected void onCancelled(Long completed) {
            if (view != null) {
                view.hideProgressBar();
                view.showMessage(R.string.upload_cancelled, completed);
            }
        }

    }
}
