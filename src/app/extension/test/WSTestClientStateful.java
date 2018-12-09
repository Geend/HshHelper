package extension.test;

import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URLEncodedUtils;
import play.libs.ws.WSClient;
import play.libs.ws.WSCookie;
import play.libs.ws.WSResponse;
import play.test.WSTestClient;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

public class WSTestClientStateful {
    private WSClient ws;
    private List<WSCookie> cookies;
    private boolean followRedirects;

    public WSTestClientStateful(int port) {
        this.ws =  WSTestClient.newClient(port);
        this.cookies = new ArrayList<>();
        followRedirects = false;
    }

    private void updateCookies(WSResponse response) {
        List<WSCookie> responseCookies = response.getCookies();

        for(WSCookie cookie : responseCookies) {
            cookies.removeIf(x -> x.getName().equals(cookie.getName()));
            cookies.add(cookie);
        }
    }

    public WSResponse get(String url) throws ExecutionException, InterruptedException {
        WSResponse response = ws.url(url).setFollowRedirects(followRedirects).setCookies(cookies).get().toCompletableFuture().get();
        updateCookies(response);
        return response;
    }

    public WSResponse post(String url, List<NameValuePair> parameter) throws ExecutionException, InterruptedException {
        String encParameters = URLEncodedUtils.format(parameter, "utf-8");
        WSResponse response = ws.url(url).setFollowRedirects(followRedirects).setCookies(cookies).setContentType("application/x-www-form-urlencoded").post(encParameters).toCompletableFuture().get();
        updateCookies(response);
        return response;
    }

    public void close() throws IOException {
        ws.close();
    }

    public void setFollowRedirects(boolean value) {
        followRedirects = value;
    }
}
