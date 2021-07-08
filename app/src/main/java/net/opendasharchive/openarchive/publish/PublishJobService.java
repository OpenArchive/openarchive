package net.opendasharchive.openarchive.publish;

import android.app.job.JobParameters;
import android.app.job.JobService;
import android.content.Intent;
import android.os.Build;

import androidx.annotation.RequiresApi;
import androidx.core.content.ContextCompat;

import net.opendasharchive.openarchive.db.Media;

import java.util.List;

@RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
public class PublishJobService extends JobService {

    @Override
    public boolean onStartJob(JobParameters jobParameters) {

        List<Media> results = Media.find(Media.class, "status = ?", Media.STATUS_QUEUED + "");

        //if (results.size() > 0)
            //ContextCompat.startForegroundService(this, new Intent(this, PublishService.class));

        return true;
    }

    @Override
    public boolean onStopJob(JobParameters jobParameters) {
        return false;
    }


}
