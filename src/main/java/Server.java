import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import org.apache.commons.io.IOUtils;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.URL;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class Server
{
    private static final int port = 8000;
    private static HttpServer server;

    public static void main(String[] args) throws Exception
    {
        int port = 8000;

        server = HttpServer.create(new InetSocketAddress(port), 300);
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
            for (String key : headers.keySet())
            {
                for (String value : headers.get(key))
                {
//                    System.out.println(key + " : " + value);
                    httpURLConnection.setRequestProperty(key, value);
                }
            }

            // Setting request method
            String requestMethod = exchange.getRequestMethod();
            httpURLConnection.setRequestMethod(requestMethod);
            if (requestMethod == "POST" || requestMethod == "PUT")
            {
                httpURLConnection.setDoOutput(true);
                httpURLConnection.setDoInput(true);

                OutputStream toServerStream = null;

                try
                {
                    // Sending request body to server
                    byte[] bytes = IOUtils.toByteArray(exchange.getRequestBody());

                    toServerStream = httpURLConnection.getOutputStream();
                    toServerStream.write(bytes);
                } catch (Exception e)
                {
                    e.printStackTrace();
                } finally
                {
                    toServerStream.close();
                }
            } else
            {
                httpURLConnection.setDoOutput(false);
                httpURLConnection.setDoInput(true);
            }

            httpURLConnection.setUseCaches(false);
            httpURLConnection.connect();

            Map<String, List<String>> responseMap = httpURLConnection.getHeaderFields();
            for (Iterator iterator = responseMap.keySet().iterator(); iterator.hasNext(); )
            {
                String key = (String) iterator.next();
                String value = responseMap.get(key).get(0);

                // block Transfer-Encoding header
                if(key != null && !key.equals("Transfer-Encoding"))
                    exchange.getResponseHeaders().set(key, value);
            }

            InputStream inputStream = null;
            OutputStream os = null;
            try
            {
                inputStream = httpURLConnection.getInputStream();

                byte[] bytes = IOUtils.toByteArray(inputStream);
                os = exchange.getResponseBody();


                exchange.sendResponseHeaders(httpURLConnection.getResponseCode(), bytes.length);
                os.write(bytes);
            } catch (Exception e)
            {
                e.printStackTrace();
            } finally
            {
                os.close();
                inputStream.close();
                httpURLConnection.disconnect();
            }
        }
    }
}