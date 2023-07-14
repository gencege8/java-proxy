import java.util.StringTokenizer;

public class HttpResponse {
	int statusCode;
    String reasonPhrase;
    MimeHeader mh;

    public HttpResponse(String request) {
    	StringTokenizer st = new StringTokenizer(request, "\r\n");
    	String s=st.nextToken();
        statusCode = //ADD -> get statusCode from first line
        reasonPhrase = //ADD -> get reasonPhrase from first line
        String raw_mime_header = //ADD-> the rest of the response
        mh = new MimeHeader(raw_mime_header);
    }

    public HttpResponse(int code, String reason, MimeHeader m) {
        statusCode = code;
        reasonPhrase = reason;
        mh = m;
        mh.put("Connection", "close");
    }

    public String toString() {
        return "HTTP/1.1 " + statusCode + " " + reasonPhrase + "\r\n" + mh + "\r\n";
    }
}