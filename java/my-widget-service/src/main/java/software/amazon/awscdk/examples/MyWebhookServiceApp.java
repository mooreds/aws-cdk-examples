package software.amazon.awscdk.examples;

import software.amazon.awscdk.core.App;

public class MyWebhookServiceApp {
  public static void main(final String argv[]) {
    App app = new App();

    new MyWebhookServiceStack(app, "MyWebhookServiceStack");

    app.synth();
    
  }
}
