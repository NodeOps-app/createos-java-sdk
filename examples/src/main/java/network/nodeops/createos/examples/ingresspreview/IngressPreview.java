package network.nodeops.createos.examples.ingresspreview;

import java.io.ByteArrayInputStream;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import network.nodeops.createos.CreateOsClient;
import network.nodeops.createos.Sandbox;
import network.nodeops.createos.model.CreateProcessRequest;
import network.nodeops.createos.model.CreateSandboxRequest;
import network.nodeops.createos.model.RunCommandRequest;

/** Starts a web server and fetches it through the sandbox public preview URL. */
public final class IngressPreview {
  private IngressPreview() {}

  /** Runs the example. */
  public static void main(String[] arguments) throws Exception {
    CreateOsClient client = CreateOsClient.builder().build();
    Sandbox sandbox =
        client.createSandbox(
            CreateSandboxRequest.builder("s-1vcpu-256mb")
                .rootFileSystem("devbox:1")
                .ingressEnabled(true)
                .build());
    try {
      sandbox.runCommand(RunCommandRequest.of("mkdir", "-p", "/srv"));
      byte[] page =
          "<h1>hello from CreateOS Sandbox preview URL</h1>".getBytes(StandardCharsets.UTF_8);
      sandbox.files().upload("/srv/index.html", new ByteArrayInputStream(page));
      sandbox
          .processes()
          .create(
              new CreateProcessRequest(
                  "python3",
                  List.of("-m", "http.server", "8080", "--bind", "0.0.0.0"),
                  "/srv",
                  Map.of(),
                  null));
      sandbox.waitForPort("127.0.0.1", 8080, Duration.ofSeconds(10));
      var previewUrl = sandbox.previewUrl(8080);
      System.out.println("URL: " + previewUrl);
      var request = HttpRequest.newBuilder(previewUrl).GET().build();
      var response = previewHttpClient().send(request, HttpResponse.BodyHandlers.ofString());
      System.out.println(response.body());
    } finally {
      sandbox.destroy();
    }
  }

  private static HttpClient previewHttpClient() throws GeneralSecurityException {
    // Preview ingress currently uses a self-signed certificate. Keep this
    // relaxed trust policy isolated to this example's one-purpose client.
    SSLContext context = SSLContext.getInstance("TLS");
    context.init(null, new TrustManager[] {new PreviewTrustManager()}, new SecureRandom());
    return HttpClient.newBuilder().sslContext(context).build();
  }

  private static final class PreviewTrustManager implements X509TrustManager {
    @Override
    public void checkClientTrusted(X509Certificate[] chain, String authenticationType) {}

    @Override
    public void checkServerTrusted(X509Certificate[] chain, String authenticationType) {}

    @Override
    public X509Certificate[] getAcceptedIssuers() {
      return new X509Certificate[0];
    }
  }
}
