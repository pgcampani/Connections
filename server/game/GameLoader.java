package server.game; 

import com.google.gson.stream.JsonReader; 
import messages.JsonUtils;
import java.io.FileReader;
import java.io.IOException; 

public class GameLoader{
    private final JsonReader reader; // Lettura sequenziale per token
    
    public GameLoader(String path) throws IOException{
        reader = new JsonReader(new FileReader(path));
        reader.beginArray(); // Cursore su primo elemento
    }

    public synchronized  Game loadNext() throws IOException{
        if(reader.hasNext()){
            return JsonUtils.GSON.fromJson(reader, Game.class);
        }
        return null; 
    }

    public void close() throws IOException{
        reader.close(); 
    }
}
