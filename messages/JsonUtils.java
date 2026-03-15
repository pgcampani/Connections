package messages; 

import com.google.gson.*; 

public class JsonUtils{

    public static final Gson GSON = new Gson(); 

    public static final Gson GSON_PRETTY = new GsonBuilder().setPrettyPrinting().create(); 
}
