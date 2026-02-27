package  messages; 

import java.io.*; 
import java.nio.ByteBuffer; 
import java.nio.channels.SocketChannel; 
import java.nio.charset.StandardCharsets; 

public class NetworkUtils{
    public static void NIOsend(SocketChannel channel, ByteBuffer buffer, Object obj) throws IOException{
        String json = JsonUtils.GSON.toJson(obj) + "\n"; 
        buffer.clear(); 
        buffer.put(json.getBytes(StandardCharsets.UTF_8));
        buffer.flip(); 
        channel.write(buffer); 
    }

    public static String NIOreceive(SocketChannel channel, ByteBuffer buffer) throws IOException{
        buffer.clear(); 
        StringBuilder sb = new StringBuilder(); 
        int bytesRead = channel.read(buffer);

        if(bytesRead == -1){
            throw new IOException("Server disconnesso"); 
        }

        buffer.flip();
        while(buffer.hasRemaining()){
            char c = (char) buffer.get(); 
            if(c == '\n') break; 
            sb.append(c); 
        }
        return sb.toString(); 
    }

    public static void TCPsend(PrintWriter out, Object obj){
        out.println(JsonUtils.GSON.toJson(obj));
    }

    public static String TCPreceive(BufferedReader in) throws IOException{
        return in.readLine(); 
    }
}
