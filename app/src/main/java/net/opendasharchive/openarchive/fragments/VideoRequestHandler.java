package net.opendasharchive.openarchive.fragments;

import android.content.Context;
import android.graphics.Bitmap;
import android.media.MediaMetadataRetriever;
import android.net.Uri;

import com.squareup.picasso.Picasso;
import com.squareup.picasso.Request;
import com.squareup.picasso.RequestHandler;

import java.io.IOException;

public class VideoRequestHandler extends RequestHandler{
    public final static String SCHEME_VIDEO="video";
    private Context mContext;

    public VideoRequestHandler(Context context)
    {
        mContext = context;
    }

    @Override
    public boolean canHandleRequest(Request data)
    {
        String scheme = data.uri.getScheme();
        return (SCHEME_VIDEO.equals(scheme));
    }

    @Override
    public Result load(Request data, int arg1) throws IOException
    {

        Bitmap bm = null;
        try {
            bm = retrieveVideoFrameFromVideo(mContext, Uri.parse(data.uri.toString().substring(6)));
            if (bm != null)
                return new RequestHandler.Result(bm, Picasso.LoadedFrom.DISK);
        } catch (Throwable throwable) {
            throwable.printStackTrace();
        }

        return null;

    }

    public static Bitmap retrieveVideoFrameFromVideo(Context context, Uri videoPath)throws Throwable
    {
        Bitmap bitmap = null;
        MediaMetadataRetriever mediaMetadataRetriever = null;
        try
        {
            mediaMetadataRetriever = new MediaMetadataRetriever();
            mediaMetadataRetriever.setDataSource(context,videoPath);
            bitmap = mediaMetadataRetriever.getFrameAtTime(1, MediaMetadataRetriever.OPTION_CLOSEST);
        }
        catch (Exception e)
        {
            e.printStackTrace();
            throw new Throwable("Exception in retriveVideoFrameFromVideo(String videoPath)"+ e.getMessage());
        }
        finally
        {
            if (mediaMetadataRetriever != null)
            {
                mediaMetadataRetriever.release();
            }
        }
        return bitmap;
    }
}