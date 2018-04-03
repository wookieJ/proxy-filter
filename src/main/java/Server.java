import com.sun.java.browser.plugin2.DOM;
import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import org.apache.commons.io.IOUtils;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.URL;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class Server
{
    private static final int port = 8000;
    private static HttpServer server;
    private static List<String> blackList;
    private static List<Domain> domainList;
    private static long id = 1;

    public static void main(String[] args) throws Exception
    {
        int port = 8000;
        blackList = new ArrayList<>();
        domainList = new ArrayList<>();

        // reading strings from file and adding them to blackList list
        FileReader fileReader = null;
        BufferedReader bufferedReader = null;

        try
        {
            fileReader = new FileReader("resource/blackList.txt");
            bufferedReader = new BufferedReader(fileReader);
            String line;

            while ((line = bufferedReader.readLine()) != null)
            {
                blackList.add(line);
            }
        } catch (IOException e)
        {
            e.printStackTrace();
        } finally
        {
            bufferedReader.close();
            fileReader.close();
        }

        // Read domains statistics and load them to list. If file doesn't exist, it is creating
        try
        {
            File file = new File("resource/statistics.csv");
            if (!file.exists())
            {
                System.out.println("Creating new statistics.csv");
                file.createNewFile();

                updateCSV();
            } else
            {
                System.out.println("Reading statistics.csv file");
                fileReader = new FileReader("resource/statistics.csv");
                bufferedReader = new BufferedReader(fileReader);
                String line;

                int i = 0;
                while ((line = bufferedReader.readLine()) != null)
                {
                    if (i != 0)
                    {
                        Domain domain = new Domain(line);
                        domainList.add(domain);
                    }
                    i++;
                }
            }
        } catch (IOException e)
        {
            e.printStackTrace();
        }

        System.out.println(domainList);

        server = HttpServer.create(new InetSocketAddress(port), 400);
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
            System.out.println(exchange.getRequestURI());

            if (!blackList.contains(exchange.getRequestURI().toString()))
            {
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
                Long outputLength = new Long(0);
                if (requestMethod == "POST" || requestMethod == "PUT")
                {
                    httpURLConnection.setDoOutput(true);
                    httpURLConnection.setDoInput(true);

                    OutputStream toServerStream = null;

                    try
                    {
                        // Sending request body to server
                        byte[] bytes = IOUtils.toByteArray(exchange.getRequestBody());
                        outputLength = Long.valueOf(bytes.length);
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
                    if (key != null && !key.equals("Transfer-Encoding"))
                        exchange.getResponseHeaders().set(key, value);
                }

                InputStream inputStream = null;
                OutputStream os = null;
                Long inputLength = null;

                try
                {
                    inputStream = httpURLConnection.getInputStream();

                    byte[] bytes = IOUtils.toByteArray(inputStream);
                    os = exchange.getResponseBody();

                    inputLength = Long.valueOf(bytes.length);
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

                String domainUrl = exchange.getRequestURI().toString();
                Domain domain = null;

                for(Domain d: domainList)
                {
                    if(d.getUrl().equals(domainUrl))
                        domain = d;
                }

                // domain exists
                if (domain != null)
                {
                    System.out.println("Domain " + domainUrl + " exists");
                } else
                {
                    // creating new domain
                    if (inputLength != null)
                    {
                        System.out.println("Creating new domain!");
                        domain = new Domain(domainUrl, 1, inputLength, outputLength);
                        domainList.add(domain);

                        System.out.println(domainList);

                        updateCSV();
                    }
                }
            } else
            {
                byte[] responseHtml = ("<h1>This URL is on the black list and isn't allowed").getBytes();
                exchange.getResponseHeaders().set("Content-Type", "text/html");
                exchange.sendResponseHeaders(423, responseHtml.length);
                OutputStream os = exchange.getResponseBody();
                os.write(responseHtml);
                os.close();

                System.out.println("NOT ALLOWED!");
            }
        }
    }

    private static void updateCSV()
    {
        PrintWriter printWriter = null;
        StringBuilder stringBuilder = new StringBuilder();

        try
        {
            File file = new File("resource/statistics.csv");
            printWriter = new PrintWriter(file);

            stringBuilder.append("Id");
            stringBuilder.append(";");
            stringBuilder.append("Domain");
            stringBuilder.append(";");
            stringBuilder.append("Request number");
            stringBuilder.append(";");
            stringBuilder.append("Bytes sent");
            stringBuilder.append(";");
            stringBuilder.append("Bytes received");
            stringBuilder.append("\n");

            domainList.forEach(domain -> stringBuilder.append(domain + "\n"));

            printWriter.write(stringBuilder.toString());
        } catch (IOException e)
        {
            e.printStackTrace();
        } finally
        {
            printWriter.close();
        }
    }
}