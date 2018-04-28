
package mccode.qdup.QueryModels;

import android.os.Parcel;
import android.os.Parcelable;
import android.os.Parcelable.Creator;

public class Artist implements Parcelable
{

    private External_urls external_urls;
    private String href;
    private String id;
    private String name;
    private String type;
    private String uri;
    public final static Parcelable.Creator<Artist> CREATOR = new Creator<Artist>() {


        @SuppressWarnings({
            "unchecked"
        })
        public Artist createFromParcel(Parcel in) {
            return new Artist(in);
        }

        public Artist[] newArray(int size) {
            return (new Artist[size]);
        }

    }
    ;

    protected Artist(Parcel in) {
        this.external_urls = ((External_urls) in.readValue((External_urls.class.getClassLoader())));
        this.href = ((String) in.readValue((String.class.getClassLoader())));
        this.id = ((String) in.readValue((String.class.getClassLoader())));
        this.name = ((String) in.readValue((String.class.getClassLoader())));
        this.type = ((String) in.readValue((String.class.getClassLoader())));
        this.uri = ((String) in.readValue((String.class.getClassLoader())));
    }

    /**
     * No args constructor for use in serialization
     * 
     */
    public Artist() {
    }

    /**
     * 
     * @param id
     * @param external_urls
     * @param name
     * @param type
     * @param uri
     * @param href
     */
    public Artist(External_urls external_urls, String href, String id, String name, String type, String uri) {
        super();
        this.external_urls = external_urls;
        this.href = href;
        this.id = id;
        this.name = name;
        this.type = type;
        this.uri = uri;
    }

    public External_urls getExternal_urls() {
        return external_urls;
    }

    public void setExternal_urls(External_urls external_urls) {
        this.external_urls = external_urls;
    }

    public String getHref() {
        return href;
    }

    public void setHref(String href) {
        this.href = href;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getUri() {
        return uri;
    }

    public void setUri(String uri) {
        this.uri = uri;
    }

    public void writeToParcel(Parcel dest, int flags) {
        dest.writeValue(external_urls);
        dest.writeValue(href);
        dest.writeValue(id);
        dest.writeValue(name);
        dest.writeValue(type);
        dest.writeValue(uri);
    }

    public int describeContents() {
        return  0;
    }

}
