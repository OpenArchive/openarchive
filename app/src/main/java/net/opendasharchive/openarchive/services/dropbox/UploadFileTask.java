package net.opendasharchive.openarchive.services.dropbox;

import android.content.Context;
import android.net.Uri;

import com.dropbox.core.DbxException;
import com.dropbox.core.NetworkIOException;
import com.dropbox.core.RetryException;
import com.dropbox.core.v2.DbxClientV2;
import com.dropbox.core.v2.files.CommitInfo;
import com.dropbox.core.v2.files.FileMetadata;
import com.dropbox.core.v2.files.UploadSessionCursor;
import com.dropbox.core.v2.files.UploadSessionFinishErrorException;
import com.dropbox.core.v2.files.UploadSessionLookupErrorException;
import com.dropbox.core.v2.files.WriteMode;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Date;

/**
 * Async task to upload a file to a directory
 */
public class UploadFileTask {

    private final Context mContext;
    private final DbxClientV2 mDbxClient;
    private final Callback mCallback;
    private Exception mException;
    private String mLocalUri;

    private String mRemoteProjectPath;
    private String mRemoteFolderPath;
    private String mRemoteFileName;

    // Adjust the chunk size based on your network speed and reliability. Larger chunk sizes will
    // result in fewer network requests, which will be faster. But if an error occurs, the entire
    // chunk will be lost and have to be re-uploaded. Use a multiple of 4MiB for your chunk size.
    private static final long CHUNKED_UPLOAD_CHUNK_SIZE = 1000 * 500; // 500KB
    private static final int CHUNKED_UPLOAD_MAX_ATTEMPTS = 5;

    private boolean mKeepUploading = true;

    public interface Callback {
        void onUploadComplete(FileMetadata result);
        void onError(Exception e);
        void onProgress(long progress);
    }

    public UploadFileTask(Context context, DbxClientV2 dbxClient, Callback callback) {
        mContext = context;
        mDbxClient = dbxClient;
        mCallback = callback;
    }

    public void upload (String localUri, String remoteFileName, String remoteFolderPath, String remoteProjectPath)
    {
        mLocalUri = localUri;
        mRemoteProjectPath = "/" + remoteProjectPath;
        mRemoteFolderPath = "/" + remoteFolderPath;
        mRemoteFileName = remoteFileName;

        File localFile = UriHelpers.getFileForUri(mContext, Uri.parse(mLocalUri));

        if (localFile.length() < CHUNKED_UPLOAD_CHUNK_SIZE)
            upload(localFile);
        else
            chunkedUploadFile(localFile);
    }

    public void cancel ()
    {
        mKeepUploading = false;
    }

    private void upload (File localFile) {


        if (localFile != null) {


            FileMetadata result = null;

            try {

                //the only way to see if you need to create a folder, is to list it. kinda crazy, but so it is.
                try {
                    mDbxClient.files().listFolder(mRemoteProjectPath);
                }
                catch (Exception e)
                {
                    mDbxClient.files().createFolderV2(mRemoteProjectPath);
                }

                try {
                    mDbxClient.files().listFolder(mRemoteProjectPath + mRemoteFolderPath);
                }
                catch (Exception e)
                {
                    mDbxClient.files().createFolderV2(mRemoteProjectPath + mRemoteFolderPath);
                }

                try (InputStream inputStream = new FileInputStream(localFile)) {

                    result = mDbxClient.files().uploadBuilder(mRemoteProjectPath + mRemoteFolderPath + "/" + mRemoteFileName)
                            .withMode(WriteMode.OVERWRITE).uploadAndFinish(inputStream);

                    mCallback.onUploadComplete(result);

                } catch (DbxException | IOException e) {
                    mException = e;

                    if (mException != null) {
                        mCallback.onError(mException);
                    } else if (result == null) {
                        mCallback.onError(null);
                    }
                }


            } catch (Exception e) {
                mException = e;
                if (mException != null) {
                    mCallback.onError(mException);
                } else if (result == null) {
                    mCallback.onError(null);
                }
            }


        }

    }

    /**
     * Uploads a file in chunks using multiple requests. This approach is preferred for larger files
     * since it allows for more efficient processing of the file contents on the server side and
     * also allows partial uploads to be retried (e.g. network connection problem will not cause you
     * to re-upload all the bytes).
     *
     */
    private boolean chunkedUploadFile(File localFile) {

        String dropboxPath = mRemoteProjectPath + mRemoteFolderPath + "/" + mRemoteFileName;

        long size = localFile.length();

        // assert our file is at least the chunk upload size. We make this assumption in the code
        // below to simplify the logic.
        if (size < CHUNKED_UPLOAD_CHUNK_SIZE) {
            //System.err.println("File too small, use upload() instead.");
            return false;
        }

        long uploaded = 0L;
        DbxException thrown = null;



        // Chunked uploads have 3 phases, each of which can accept uploaded bytes:
        //
        //    (1)  Start: initiate the upload and get an upload session ID
        //    (2) Append: upload chunks of the file to append to our session
        //    (3) Finish: commit the upload and close the session
        //
        // We track how many bytes we uploaded to determine which phase we should be in.
        String sessionId = null;
        for (int i = 0; i < CHUNKED_UPLOAD_MAX_ATTEMPTS; ++i) {
            if (i > 0) {
                System.out.printf("Retrying chunked upload (%d / %d attempts)\n", i + 1, CHUNKED_UPLOAD_MAX_ATTEMPTS);
            }

            try (InputStream in = new FileInputStream(localFile)) {
                // if this is a retry, make sure seek to the correct offset
                in.skip(uploaded);

                // (1) Start
                if (sessionId == null) {
                    sessionId = mDbxClient.files().uploadSessionStart()
                            .uploadAndFinish(in, CHUNKED_UPLOAD_CHUNK_SIZE)
                            .getSessionId();
                    uploaded += CHUNKED_UPLOAD_CHUNK_SIZE;


                    mCallback.onProgress(uploaded);

                }

                UploadSessionCursor cursor = new UploadSessionCursor(sessionId, uploaded);

                // (2) Append
                while ((size - uploaded) > CHUNKED_UPLOAD_CHUNK_SIZE) {
                    mDbxClient.files().uploadSessionAppendV2(cursor)
                            .uploadAndFinish(in, CHUNKED_UPLOAD_CHUNK_SIZE);
                    uploaded += CHUNKED_UPLOAD_CHUNK_SIZE;
                    mCallback.onProgress(uploaded);
                    cursor = new UploadSessionCursor(sessionId, uploaded);
                    if (!mKeepUploading)
                    {
                        return false;
                    }

                }

                // (3) Finish
                long remaining = size - uploaded;
                CommitInfo commitInfo = CommitInfo.newBuilder(dropboxPath)
                        .withMode(WriteMode.ADD)
                        .withClientModified(new Date(localFile.lastModified()))
                        .build();
                FileMetadata metadata = mDbxClient.files().uploadSessionFinish(cursor, commitInfo)
                        .uploadAndFinish(in, remaining);

                mCallback.onUploadComplete(metadata);

                //System.out.println(metadata.toStringMultiline());
                return true;
            } catch (RetryException ex) {
                thrown = ex;
                // RetryExceptions are never automatically retried by the client for uploads. Must
                // catch this exception even if DbxRequestConfig.getMaxRetries() > 0.
           //     sleepQuietly(ex.getBackoffMillis());
                continue;
            } catch (NetworkIOException ex) {
                thrown = ex;
                // network issue with Dropbox (maybe a timeout?) try again
                continue;
            } catch (UploadSessionLookupErrorException ex) {
                if (ex.errorValue.isIncorrectOffset()) {
                    thrown = ex;
                    // server offset into the stream doesn't match our offset (uploaded). Seek to
                    // the expected offset according to the server and try again.
                    uploaded = ex.errorValue
                            .getIncorrectOffsetValue()
                            .getCorrectOffset();
                    continue;
                } else {
                    // Some other error occurred, give up.
                   // System.err.println("Error uploading to Dropbox: " + ex.getMessage());

                    return false;
                }
            } catch (UploadSessionFinishErrorException ex) {
                if (ex.errorValue.isLookupFailed() && ex.errorValue.getLookupFailedValue().isIncorrectOffset()) {
                    thrown = ex;
                    // server offset into the stream doesn't match our offset (uploaded). Seek to
                    // the expected offset according to the server and try again.
                    uploaded = ex.errorValue
                            .getLookupFailedValue()
                            .getIncorrectOffsetValue()
                            .getCorrectOffset();
                    continue;
                } else {
                    // some other error occurred, give up.

                    return false;
                }
            } catch (DbxException ex) {
                //System.err.println("Error uploading to Dropbox: " + ex.getMessage());

                return false;
            } catch (IOException ex) {
                //System.err.println("Error reading from file \"" + localFile + "\": " + ex.getMessage());

                return false;
            }
        }

        // if we made it here, then we must have run out of attempts
        //System.err.println("Maxed out upload attempts to Dropbox. Most recent error: " + thrown.getMessage());
        return false;
    }
}