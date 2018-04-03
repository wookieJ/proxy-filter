public class Domain
{
    private long id;
    private String url;
    private long requestNumber;
    private long bytesSent;
    private long bytesReceived;

    private static long id_number = 1;

    public long getId()
    {
        return id;
    }

    public void setId(long id)
    {
        this.id = id;
    }

    public String getUrl()
    {
        return url;
    }

    public void setUrl(String url)
    {
        this.url = url;
    }

    public long getRequestNumber()
    {
        return requestNumber;
    }

    public void setRequestNumber(long requestNumber)
    {
        this.requestNumber = requestNumber;
    }

    public long getBytesSent()
    {
        return bytesSent;
    }

    public void setBytesSent(long bytesSent)
    {
        this.bytesSent = bytesSent;
    }

    public long getBytesReceived()
    {
        return bytesReceived;
    }

    public void setBytesReceived(long bytesReceived)
    {
        this.bytesReceived = bytesReceived;
    }

    public Domain(String url, long requestNumber, long bytesSent, long bytesReceived)
    {
        this.id = id_number++;
        this.url = url;
        this.requestNumber = requestNumber;
        this.bytesSent = bytesSent;
        this.bytesReceived = bytesReceived;
    }

    public Domain (String fromCSV)
    {
        String[] data = fromCSV.split(";");

        this.id = Long.parseLong(data[0]);
        this.url = data[1];
        this.requestNumber = Long.parseLong(data[2]);
        this.bytesSent = Long.parseLong(data[3]);
        this.bytesReceived = Long.parseLong(data[4]);
    }

    @Override
    public String toString()
    {
        return id + ";" + url + ";" + requestNumber + ";" + bytesSent + ";" + bytesReceived;
    }
}
