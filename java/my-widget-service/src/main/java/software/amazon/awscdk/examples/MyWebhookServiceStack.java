package software.amazon.awscdk.examples;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import software.amazon.awscdk.core.Construct;
import software.amazon.awscdk.core.ConstructNode;
import software.amazon.awscdk.core.Duration;
import software.amazon.awscdk.core.ResourceEnvironment;
import software.amazon.awscdk.core.Stack;
import software.amazon.awscdk.services.apigateway.ApiKey;
import software.amazon.awscdk.services.apigateway.ApiKeyOptions;
import software.amazon.awscdk.services.apigateway.IApiKey;
import software.amazon.awscdk.services.apigateway.IRestApi;
import software.amazon.awscdk.services.apigateway.LambdaIntegration;
import software.amazon.awscdk.services.apigateway.MethodOptions;
import software.amazon.awscdk.services.apigateway.Resource;
import software.amazon.awscdk.services.apigateway.RestApi;
import software.amazon.awscdk.services.apigateway.Stage;
import software.amazon.awscdk.services.apigateway.UsagePlan;
import software.amazon.awscdk.services.apigateway.UsagePlanPerApiStage;
import software.amazon.awscdk.services.apigateway.UsagePlanProps;
import software.amazon.awscdk.services.iam.IManagedPolicy;
import software.amazon.awscdk.services.iam.ManagedPolicy;
import software.amazon.awscdk.services.iam.Role;
import software.amazon.awscdk.services.iam.ServicePrincipal;
import software.amazon.awscdk.services.lambda.Code;
import software.amazon.awscdk.services.lambda.Function;
import software.amazon.awscdk.services.lambda.Runtime;
import software.amazon.awscdk.services.s3.Bucket;

public class MyWebhookServiceStack extends Stack {
  public MyWebhookServiceStack(final Construct scope, final String id) {
    super(scope, id, null);

    Bucket bucket = Bucket.Builder.create(this, "webhookevents").build();

    RestApi api =
        RestApi.Builder.create(this, "webhook-ingestion-api")
            .restApiName("Webhook ingestion Service")
            .description("This service ingests FusionAuth webhook events.")
            .build();

    List<IManagedPolicy> managedPolicyArray = new ArrayList<IManagedPolicy>();
    managedPolicyArray.add(
        (IManagedPolicy) ManagedPolicy.fromAwsManagedPolicyName("AmazonS3FullAccess"));

    Role restApiRole =
        Role.Builder.create(this, "RestAPIRole")
            .assumedBy(new ServicePrincipal("apigateway.amazonaws.com"))
            .managedPolicies(managedPolicyArray)
            .build();

    Map<String, String> environmentVariables = new HashMap<String, String>();
    environmentVariables.put("BUCKET", bucket.getBucketName());

    Function lambdaFunction =
        Function.Builder.create(this, "WebhookHandler")
            .code(Code.fromAsset("resources"))
            .handler("webhook.main")
            .timeout(Duration.seconds(30))
            .runtime(Runtime.NODEJS_10_X)
            .environment(environmentVariables)
            .build();

    bucket.grantReadWrite(lambdaFunction);

    Map<String, String> lambdaIntegrationMap = new HashMap<String, String>();
    lambdaIntegrationMap.put("application/json", "{ \"statusCode\": \"200\" }");

    LambdaIntegration postIntegration = new LambdaIntegration(lambdaFunction);
    
	MethodOptions options = new MethodOptions() {
		@Override
		public @Nullable Boolean getApiKeyRequired() {
			return true;
		}
	};
	api.getRoot().addMethod("POST", postIntegration, options );
	
	UsagePlanProps props = new UsagePlanProps() {
		
		public @Nullable String getName() {
			return "webhook";
		}
	};
	
	UsagePlan plan = api.addUsagePlan("forwebhook",props);

//	ApiKeyOptions apiKeyOptions = new ApiKeyOptions() {
//		@Override
//		public @Nullable String getValue() {
//			return "E5A656AD-DE37-4664-9C20-EA16D7E70B1E";
//		}
//	};
	IApiKey key = api.addApiKey("for-webhook-secret-key" );
	
	plan.addApiKey(key);

	UsagePlanPerApiStage apiStage = new UsagePlanPerApiStage() {
		@Override
		public @Nullable Stage getStage() {
			return api.getDeploymentStage();
		}
		@Override
		public @Nullable IRestApi getApi() {
			return api;

		}
	};
	plan.addApiStage(apiStage);

  }
}
