package server.game; 

import com.google.gson.stream.JsonReader; 
import messages.JsonUtils;
import java.io.FileReader;
import java.io.IOException; 

public class GameLoader{
    private final JsonReader reader; // Lettura sequenziale per token
    private int nextGameIndex = 0; 

    public GameLoader(String path, int startIndex) throws IOException{
        reader = new JsonReader(new FileReader(path));
        reader.beginArray(); // Cursore su primo elemento

        for(int i = 0; i < startIndex; i++){
            reader.skipValue();
            nextGameIndex++; 
        }
    }

    public synchronized Game loadNext() throws IOException{
        if(reader.hasNext()){
            nextGameIndex++; 
            return JsonUtils.GSON.fromJson(reader, Game.class);
        }
        return null; 
    }

    public int getNextGameIndex(){
        return nextGameIndex; 
    }

    public void close() throws IOException{
        reader.close(); 
    }
}
