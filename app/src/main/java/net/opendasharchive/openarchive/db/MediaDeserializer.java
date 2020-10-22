package net.opendasharchive.openarchive.db;

import android.util.Log;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;

import java.lang.reflect.Type;
import java.text.DateFormat;
import java.text.ParseException;

/**
 * Created by n8fr8 on 9/15/16.
 */
public class MediaDeserializer  implements JsonDeserializer<Media> {
    @Override
    public Media deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {

        Media mediaResult = new Media ();

        JsonObject jobj = (JsonObject) json;

        if (jobj.has("mimeType"))
            mediaResult.setMimeType(jobj.get("mimeType").getAsString());

        if (jobj.has("title"))
            mediaResult.setTitle(jobj.get("title").getAsString());

        if (jobj.has("description"))
            mediaResult.setDescription(jobj.get("description").getAsString());

        if (jobj.has("serverUrl"))
            mediaResult.setServerUrl(jobj.get("serverUrl").getAsString());

        if (jobj.has("location"))
            mediaResult.setLocation(jobj.get("location").getAsString());

        if (jobj.has("tags"))
            mediaResult.setTags(jobj.get("tags").getAsString());

        if (jobj.has("licenseUrl"))
            mediaResult.setLicenseUrl(jobj.get("licenseUrl").getAsString());

        if (jobj.has("author"))
            mediaResult.setAuthor(jobj.get("author").getAsString());

        DateFormat ft = DateFormat.getDateTimeInstance(DateFormat.FULL,DateFormat.FULL);

        try {
            if (jobj.has("createDate"))
                mediaResult.setCreateDate(ft.parse(jobj.get("createDate").getAsString()));
        }
        catch (ParseException e)
        {
            Log.e("MediaDez","unable to parse date",e);
        }

        try {
            if (jobj.has("updateDate"))
                mediaResult.setUpdateDate(ft.parse(jobj.get("updateDate").getAsString()));
        }
        catch (ParseException e)
        {
            Log.e("MediaDez","unable to parse date",e);
        }

        return mediaResult;
    }
}

/**
 *   public String originalFilePath;
 public String scrubbedFilePath;
 public String mimeType;
 public String thumbnailFilePath;
 public Date createDate;
 public Date updateDate;
 public String serverUrl;

 public String title;
 public String description;
 public String author;
 public String location;
 public String tags;

 public String licenseUrl;
 */
