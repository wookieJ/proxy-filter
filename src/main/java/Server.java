import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import org.apache.commons.io.IOUtils;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.URL;
import java.util.List;
import java.util.Map;

public class Server
{
    private static final int port = 8000;
    private static HttpServer server;

    public static void main(String[] args) throws Exception
    {
        int port = 8000;

        server = HttpServer.create(new InetSocketAddress(port), 0);
        server.createContext("/", new RootHandler());
        server.start();

        System.out.println("Started server on port: " + port);
    }

    static class RootHandler implements HttpHandler
    {
        public void handle(HttpExchange exchange) throws IOException
        {
            // Setting URL
            URL url = exchange.getRequestURI().toURL();
            HttpURLConnection httpURLConnection = (HttpURLConnection) url.openConnection();
            httpURLConnection.setInstanceFollowRedirects(false);
            // Setting headers
            Headers headers = exchange.getRequestHeaders();

            for (Map.Entry<String, List<String>> header : headers.entrySet()) {
                for(String value : header.getValue()){
                    httpURLConnection.setRequestProperty(header.getKey(), value);
                }

            }

            // Setting request method
            String requestMethod = exchange.getRequestMethod();
            httpURLConnection.setRequestMethod(requestMethod);
            if(requestMethod == "POST" || requestMethod == "PUT")
            {
                System.out.println("POST !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
                httpURLConnection.setDoOutput(true);
                httpURLConnection.setDoInput(true);

                // Sending request body to server
                byte[] bytes = IOUtils.toByteArray(exchange.getRequestBody());

                OutputStream toServerStream = httpURLConnection.getOutputStream();
                toServerStream.write(bytes);
                toServerStream.close();
            }
            else
            {
                httpURLConnection.setDoOutput(false);
                httpURLConnection.setDoInput(true);
            }

            httpURLConnection.setUseCaches(false);
            httpURLConnection.connect();

            InputStream inputStream = null;
            inputStream = httpURLConnection.getInputStream();

            if(httpURLConnection.getResponseCode() >= 200 && httpURLConnection.getResponseCode() < 300)
                System.out.println("OK");
            else
            {
                System.out.println("Error!");
            }

            byte[] bytes = IOUtils.toByteArray(inputStream);
            OutputStream os = exchange.getResponseBody();

            for (Map.Entry<String, List<String>> header : httpURLConnection.getHeaderFields().entrySet()) {
                for(String value : header.getValue()){
                    if(header.getKey() != null && header.getKey() != "Transfer-Encoding")
                    {
                        exchange.getResponseHeaders().set(header.getKey(), value);
                    }
                }
            }

            exchange.sendResponseHeaders(httpURLConnection.getResponseCode(), bytes.length);
            os.write(bytes);
            os.close();
            inputStream.close();

            httpURLConnection.disconnect();
        }
    }
}