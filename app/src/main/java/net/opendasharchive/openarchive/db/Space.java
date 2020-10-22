package net.opendasharchive.openarchive.db;

import android.text.TextUtils;

import com.orm.SugarRecord;

import net.opendasharchive.openarchive.util.Prefs;

import java.util.Iterator;

public class Space extends SugarRecord {

    public int type;
    public String name;

    public String username;
    public String password;

    public String host;

    public final static int TYPE_WEBDAV = 0;
    public final static int TYPE_INTERNET_ARCHIVE = 1;
    public final static int TYPE_PIRATEBOX = 2;
    public final static int TYPE_DROPBOX = 3;
    public final static int TYPE_DAT = 4;
    public final static int TYPE_SCP = 5;

    public static Iterator<Space> getAllAsList() {
        return Space.findAll(Space.class);
    }

    public static Space getCurrentSpace ()
    {
        long spaceId = Prefs.getCurrentSpaceId();
        if (spaceId != -1L) {
            Space space = Space.findById(Space.class,spaceId);

            if (space != null) {
                if (TextUtils.isEmpty(space.name))
                    space.name = space.username;

                return space;
            }
        }

        return null;
    }
}
