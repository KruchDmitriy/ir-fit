package search;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.util.concurrent.ConcurrentSkipListSet;

public class Document {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    public final String url;
    public final Double starsCount;
    public final ConcurrentSkipListSet<String> addresses;
    public final String text ;

    public Document(String url, Double starsCount, ConcurrentSkipListSet<String> addresses, String text) {
        this.url = url;
        this.starsCount = starsCount;
        this.addresses = addresses;
        this.text = text;
    }

    @Override
    public String toString() {
        return GSON.toJson(this);
    }
}
