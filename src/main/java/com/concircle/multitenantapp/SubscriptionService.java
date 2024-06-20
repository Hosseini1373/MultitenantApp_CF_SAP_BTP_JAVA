package com.concircle.multitenantapp;

import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.PathVariable;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sap.cloud.sdk.cloudplatform.connectivity.Destination;
import com.sap.cloud.sdk.cloudplatform.connectivity.DestinationAccessor;
import com.sap.cloud.sdk.cloudplatform.connectivity.DestinationLoader;
import com.sap.cloud.sdk.cloudplatform.connectivity.DestinationOptions;
import com.sap.cloud.sdk.cloudplatform.connectivity.DestinationServiceOptionsAugmenter;
import com.sap.cloud.sdk.cloudplatform.connectivity.DestinationServiceRetrievalStrategy;
import com.sap.cloud.sdk.cloudplatform.connectivity.DestinationServiceTokenExchangeStrategy;
import com.sap.cloud.sdk.cloudplatform.connectivity.HttpClientAccessor;
import com.sap.cloud.sdk.cloudplatform.connectivity.HttpDestination;

import io.pivotal.cfenv.core.CfEnv;
import io.pivotal.cfenv.core.CfService;
import io.vavr.API;
import io.vavr.control.Try;
import lombok.extern.log4j.Log4j;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URLEncoder;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.HttpClient;
import org.apache.http.HttpEntity;


// import java.net.http.HttpClient;
// import java.net.http.HttpRequest;
// import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import lombok.extern.log4j.Log4j2;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;

@Log4j2

@Service
public class SubscriptionService {

    private final List<String> relevantServices = new ArrayList<>();
    private final String APP_ROUTER_NAME = "approuter";
    private final String ROUTE_PREFIX = "route-prefix";
    // Unfortunately, there is no out-of-the-box access of this API when you are in the context of 
    // an application. The code assumes a destination with the name cf_api in the sample implementation 
    // which contains the access data for the CF API
    private final String cfApiDestinationName = "cf_api";
    private final List<String> services = new ArrayList<>(Arrays.asList("destination_multi"));


    // private final HttpClient httpClient = HttpClient.newHttpClient();
    private final ObjectMapper objectMapper = new ObjectMapper();

    private JSONObject getVcapServices() {
        String vcapServices = System.getenv("VCAP_SERVICES");
        return new JSONObject(vcapServices);
    }

    private JSONObject getVcapApplication() {
        String vcapApplication = System.getenv("VCAP_APPLICATION");
        return new JSONObject(vcapApplication);
    }

    private String getCfApiUri() {
        JSONObject vcapApplication = getVcapApplication();
        return vcapApplication.getString(cfApiDestinationName);
    }

    private String getOrganizationGuid() {
        JSONObject vcapApplication = getVcapApplication();
        return vcapApplication.getString("organization_id");
    }

    
    private void logEnvironmentDetails() {
        // Retrieve and log VCAP_SERVICES environment variable safely
        String vcapServicesJson = System.getenv("VCAP_SERVICES");
        if (vcapServicesJson != null) {
            JSONObject vcapServices = new JSONObject(vcapServicesJson);
            // Assuming you want to log details about the saas-registry, but sanitize credentials
            JSONObject saasRegistry = vcapServices.getJSONArray("saas-registry").getJSONObject(0);
            saasRegistry.getJSONObject("credentials").remove("clientsecret"); // Remove sensitive data
            log.info("VCAP_SERVICES (saas-registry): {}", saasRegistry.toString());
        } else {
            log.warn("VCAP_SERVICES environment variable is not set.");
        }
    
        // Retrieve and log VCAP_APPLICATION environment variable safely
        String vcapApplicationJson = System.getenv("VCAP_APPLICATION");
        if (vcapApplicationJson != null) {
            JSONObject vcapApplication = new JSONObject(vcapApplicationJson);
            log.info("VCAP_APPLICATION: {}", vcapApplication.toString());
        } else {
            log.warn("VCAP_APPLICATION environment variable is not set.");
        }
    }

    public SubscriptionService() {
        log.info(APP_ROUTER_NAME + " is the app router name");
        log.info(cfApiDestinationName + " is the CF API destination name");
        services.forEach(service -> relevantServices.add(service));
        log.info("Relevant services: " + relevantServices);
    }

    public String subscribeRoute(@PathVariable String subdomain) throws IOException {
        String subscriberRoute = "https://" + ROUTE_PREFIX + "-" + subdomain + "." + getLandscape();
        log.info("Subscribing to route: " + subscriberRoute);

        Guids guids = getCfGuids(APP_ROUTER_NAME); // Await the completion of the CompletableFuture and retrieve the result
        String routeGuid = createRoute(subdomain, guids);
        bindRoute(routeGuid, guids);

        return subscriberRoute;
    }

    public String unsubscribeRoute(@PathVariable String subdomain) throws IOException {
        String subscriberRoute = "https://" + ROUTE_PREFIX + "-" + subdomain + "." + getLandscape();
        log.info("Unsubscribing from route: " + subdomain);
        deleteRoute(subdomain);
        return subscriberRoute;
    }

    public List<Map<String, String>> getDependencies() {
        List<Map<String, String>> dependenciesList = new ArrayList<>();
        relevantServices.forEach(serviceLabel -> {
            try {
                log.info("Searching for service: " + serviceLabel);
                CfEnv cfEnv = new CfEnv();
                CfService service = cfEnv.findServiceByName(serviceLabel);
                log.info("Credentials of service: "+service.getCredentials().toString());
                log.info("serviceLabel: "+serviceLabel + " service found: " + service.getLabel()+ " "+service.getCredentials().getString("xsappname"));
                if (service != null) {
                    Map<String, String> serviceDetails = new HashMap<>();
                    String xsappname = service.getCredentials().getString("xsappname");
                    log.info("Found service: " + xsappname);
                    // serviceDetails.put("appId", xsappname);
                    // serviceDetails.put("appName", serviceLabel);
                    serviceDetails.put("xsappname", xsappname);
                    dependenciesList.add(serviceDetails);
                }
            } catch (Exception e) {
                log.error("Failed to find service: " + serviceLabel, e);
            }
        });
        return dependenciesList;
    }



    // private Guids getCfGuids(String appName) throws IOException {
    //     log.info("Fetching GUIDs for application: {}", appName);

    //     JSONObject vcapServices = getVcapServices();
    //     // Assuming the service name matches exactly with the one needed
    //     JSONObject service = vcapServices.getJSONArray("destination").getJSONObject(0);
    //     // String appGuid = service.getJSONObject("credentials").getString("instanceid");
    //     // String domainGuid = service.getJSONObject("credentials").getString("url");  // Example, adjust as needed
    //     String spaceGuid = getVcapApplication().getString("space_id");
        
    //     String orgGuid = getVcapApplication().getString("organization_id");

    //     JSONObject vcapApplication = getVcapApplication();
    //     String cfApiBaseUri = vcapApplication.getString(cfApiDestinationName);

    //     // CfEnv cfEnv = new CfEnv();
    //     // cfApiBaseUri = cfEnv.getApp().getMap().getOrDefault(cfApiDestinationName, "https://api.cf.us10-001.hana.ondemand.com").toString();
    //     log.info("cfApiBaseUri",cfApiBaseUri);
    //     // String orgGuid=cfEnv.getApp().getMap().getOrDefault("organization_id", "https://api.default.cf.example.com").toString();
    //     // String spaceGuid = cfEnv.getApp().getSpaceId();


    //     CloseableHttpClient client = HttpClients.createDefault();
    //     HttpGet request = new HttpGet(cfApiBaseUri + "/v3/apps?organization_guids=" + orgGuid + "&space_guids=" + spaceGuid + "&names=" + appName);
    //     CloseableHttpResponse response = client.execute(request);
    //     log.info("response: {}", response.toString());

    //     // Log the status and response for debugging
    //     log.info("Response status: {}", response.getStatusLine());
    //     // Actual parsing logic needs to be implemented to extract guids
    //     String appGuid = "extracted-app-guid";  // Example placeholder
    //     String domainGuid = "extracted-domain-guid";  // Example placeholder

    //     log.info("Extracted GUIDs - App: {}, Domain: {}", appGuid, domainGuid);
    //     return new Guids(appGuid, domainGuid, spaceGuid);
    // }




    //We get the spaceGuid and orgGuid from the VCAP_APPLICATION and the appGuid and domainGuid from the CF API
    public Guids getCfGuids(String appName) throws IOException {
        // JSONObject vcapServices = getVcapServices();
        JSONObject vcapApplication = getVcapApplication();
        String spaceGuid = vcapApplication.getString("space_id");
        String orgGuid = vcapApplication.getString("organization_id");
        String cfApiBaseUri = vcapApplication.getString(cfApiDestinationName);

        Guids guids = new Guids("", "", "");
        // HttpDestination destination = DestinationAccessor.getDestination("cfApiDestination").asHttp();
        guids.setSpaceGuid(spaceGuid);
        log.info(cfApiBaseUri+ "/v3/apps?organization_guids=" + orgGuid + "&space_guids=" + spaceGuid + "&names=" + appName);
        guids.setAppGuid(fetchGuid(cfApiBaseUri+ "/v3/apps?organization_guids=" + orgGuid + "&space_guids=" + spaceGuid + "&names=" + appName));

        log.info(cfApiBaseUri+ "/v3/domains?names=" + URLEncoder.encode(getLandscape(), StandardCharsets.UTF_8.toString()));
        guids.setDomainGuid(fetchGuid(cfApiBaseUri+ "/v3/domains?names=" + URLEncoder.encode(getLandscape(), StandardCharsets.UTF_8.toString())));

        log.info("The guids are: "+guids.getAppGuid()+" "+guids.getDomainGuid()+" "+guids.getSpaceGuid());
        return guids;
    }

    private String fetchGuid(String path) throws IOException {
        JSONObject vcapApplication = getVcapApplication();
        String cfApiBaseUri = vcapApplication.getString(cfApiDestinationName);
        // String cfApiBaseUri="https://api.cf.us10-001.hana.ondemand.com";
        log.info("cfApiBaseUri: "+cfApiBaseUri);
        log.info("cfApiDestinationName:"+cfApiDestinationName);
        // HttpDestination destination = DestinationAccessor.getDestination(cfApiDestinationName).asHttp();

            Destination destination=null;
            DestinationLoader loader = DestinationAccessor.getLoader();
            DestinationOptions.Builder optionsBuilder = DestinationOptions.builder();
    
            // if ("f48ae449-d6a8-48ca-bcc7-bd0c4027846a".equals(tokenZid)) {
            //     // Handle provider tenant scenario
            //     optionsBuilder.augmentBuilder(
            //         DestinationServiceOptionsAugmenter.augmenter().retrievalStrategy(DestinationServiceRetrievalStrategy.ALWAYS_PROVIDER)
            //     );
            // } else {
                // Handle customer tenant scenario
                optionsBuilder.augmentBuilder(
                    DestinationServiceOptionsAugmenter.augmenter().retrievalStrategy(DestinationServiceRetrievalStrategy.ALWAYS_PROVIDER)
                );
            // }
    
            optionsBuilder.augmentBuilder(
                DestinationServiceOptionsAugmenter.augmenter().tokenExchangeStrategy(DestinationServiceTokenExchangeStrategy.FORWARD_USER_TOKEN)
            );
    
            DestinationOptions options = optionsBuilder.build();
    
            Try<Destination> destinationTry = loader.tryGetDestination(cfApiDestinationName, options);
    
            if (destinationTry.isSuccess()) {
                destination = destinationTry.get();
                log.info("Destination retrieved successfully.");
            } else {
                Throwable exception = destinationTry.getCause();
                log.error("Error retrieving destination: ", exception);
            }


        log.info("The destination constructor worked");
        HttpClient httpClient = HttpClientAccessor.getHttpClient(destination);


        HttpGet request = new HttpGet(path);
        log.info("request to url: {} in fetchGuid", request.getURI());
        HttpResponse response = httpClient.execute(request);
        String responseBody = EntityUtils.toString(response.getEntity());
        log.info("response from the fetchGuid request: {}", responseBody);

        return parseGuid(responseBody);
    }


    private String parseGuid(String json) {
        JSONObject jsonObject = new JSONObject(json);

        // Access the "resources" array
        JSONArray resourcesArray = jsonObject.getJSONArray("resources");
      
        // Check if there are any resources
        if (resourcesArray.length() == 0) {
          throw new JSONException("No resources found in the JSON response");
        }
      
        // Get the first resource (assuming only one domain is expected)
        JSONObject resourceObject = resourcesArray.getJSONObject(0);
      
        // Extract the GUID from the "guid" field
        String guid = resourceObject.getString("guid");
        log.info("The parsed guid is: "+guid);
      
        return guid;
    }

    // private CompletableFuture<String> fetchAppGuid(String orgGuid, String spaceGuid, String appName) {
    //     JSONObject vcapApplication = getVcapApplication();
    //     String cfApiBaseUri = vcapApplication.getString(cfApiDestinationName);
    //     Guids guids = new Guids("", "", "");
    //     HttpDestination destination = DestinationAccessor.getDestination("cfApiDestination").asHttp();
    //     guids.setSpaceGuid(spaceGuid);
    //     guids.setAppGuid(fetchGuid(destination, "/v3/apps?organization_guids=" + orgGuid + "&space_guids=" + spaceGuid + "&names=" + appName));
    //     guids.setDomainGuid(fetchGuid(destination, "/v3/domains?names=" + URLEncoder.encode(getLandscape(), StandardCharsets.UTF_8.toString())));

    //     // String url = String.format("/v3/apps?organization_guids=%s&space_guids=%s&names=%s", orgGuid, spaceGuid, appName);
    //     // HttpRequest request = HttpRequest.newBuilder()
    //     //         .uri(URI.create(cfApiBaseUri + url))
    //     //         .GET()
    //     //         .build();

    //     // return httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
    //     //         .thenApply(HttpResponse::body)
    //     //         .thenApply(this::parseAppGuid);
    // }
    // private String fetchGuid(HttpDestination destination, String path) throws IOException {
    //     HttpClient httpClient = HttpClientAccessor.getHttpClient(destination);
    //     HttpGet request = new HttpGet(destination.getUri() + path);
    //     log.info("request to url: {} in fetchGuid", request.getURI());
    //     HttpResponse response = httpClient.execute(request);
    //     String responseBody = EntityUtils.toString(response.getEntity());

    //     return parseGuid(responseBody);
    // }





    // private String parseAppGuid(String responseBody) {
    //     try {
    //         JsonNode node = objectMapper.readTree(responseBody);
    //         return node.at("/resources/0/guid").asText();
    //     } catch (Exception e) {
    //         throw new RuntimeException("Failed to parse application GUID", e);
    //     }
    // }

    // private CompletableFuture<Guids> fetchDomainGuid(String appGuid, String appName) throws UnsupportedEncodingException {
    //     JSONObject vcapApplication = getVcapApplication();
    //     String cfApiBaseUri = vcapApplication.getString(cfApiDestinationName);

    //     String url = String.format("/v3/domains?names=%s", URLEncoder.encode(getLandscape(), StandardCharsets.UTF_8.toString()));
    //     HttpRequest request = HttpRequest.newBuilder()
    //             .uri(URI.create(cfApiBaseUri + url))
    //             .GET()
    //             .build();

    //     return httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
    //             .thenApply(HttpResponse::body)
    //             .thenApply(responseBody -> parseDomainGuid(responseBody, appGuid));
    // }

    // private Guids parseDomainGuid(String responseBody, String appGuid) {
    //     try {
    //         JsonNode node = objectMapper.readTree(responseBody);
    //         String domainGuid = node.at("/resources/0/guid").asText();
    //         return new Guids(appGuid, domainGuid, System.getenv("CF_SPACE_ID"));
    //     } catch (Exception e) {
    //         throw new RuntimeException("Failed to parse domain GUID", e);
    //     }
    // }






    public String getLandscape() {
        JSONObject vcapApplication = getVcapApplication(); // Ensure this method returns the parsed JSON object of VCAP_APPLICATION.
        String uri = vcapApplication.getJSONArray("application_uris").getString(0);
        String[] parts = uri.split("\\.");
        String[] domainParts = Arrays.copyOfRange(parts, 1, parts.length);
        return String.join(".", domainParts);
    }

    private String getRoutePath(String subscribedSubdomain) {
        String routePath = ROUTE_PREFIX + "-" + subscribedSubdomain;
        log.info("Generated route path: {}", routePath);
        return routePath;
    }


 


    private void bindRoute(String routeGuid, Guids guids) throws IOException {
        log.info("binding route with GUID: {}", routeGuid);
        log.info("the guids in bindRoute are: "+guids.getAppGuid()+" "+guids.getDomainGuid()+" "+guids.getSpaceGuid());
        JSONObject vcapApplication = getVcapApplication();
        String cfApiBaseUri = vcapApplication.getString(cfApiDestinationName);
        // HttpDestination destination = DestinationAccessor.getDestination(cfApiDestinationName).asHttp();
        // log.info(DestinationAccessor.getDestination(cfApiDestinationName).getPropertyNames().toString());
        Destination destination=null;
            DestinationLoader loader = DestinationAccessor.getLoader();
            DestinationOptions.Builder optionsBuilder = DestinationOptions.builder();
    
            // if ("f48ae449-d6a8-48ca-bcc7-bd0c4027846a".equals(tokenZid)) {
            //     // Handle provider tenant scenario
            //     optionsBuilder.augmentBuilder(
            //         DestinationServiceOptionsAugmenter.augmenter().retrievalStrategy(DestinationServiceRetrievalStrategy.ALWAYS_PROVIDER)
            //     );
            // } else {
                // Handle customer tenant scenario
                optionsBuilder.augmentBuilder(
                    DestinationServiceOptionsAugmenter.augmenter().retrievalStrategy(DestinationServiceRetrievalStrategy.ALWAYS_PROVIDER)
                );
            // }
    
            optionsBuilder.augmentBuilder(
                DestinationServiceOptionsAugmenter.augmenter().tokenExchangeStrategy(DestinationServiceTokenExchangeStrategy.FORWARD_USER_TOKEN)
            );
    
            DestinationOptions options = optionsBuilder.build();
    
            Try<Destination> destinationTry = loader.tryGetDestination(cfApiDestinationName, options);
    
            if (destinationTry.isSuccess()) {
                destination = destinationTry.get();
                log.info("Destination retrieved successfully.");
            } else {
                Throwable exception = destinationTry.getCause();
                log.error("Error retrieving destination: ", exception);
            }

        HttpClient httpClient = HttpClientAccessor.getHttpClient(destination);
        log.info("Binding route with GUID: {}", routeGuid);
 
        JSONObject bindRouteBody = new JSONObject();
        
    
        
        JSONArray destinationArray = new JSONArray();
        JSONObject destinationObject = new JSONObject();
        JSONObject app = new JSONObject();
        app.put("guid", guids.getAppGuid());
        destinationObject.put("app", app);
        destinationArray.put(destinationObject);
        bindRouteBody.put("destinations", destinationArray);
        log.info("Body of post request to bindRoute:"+bindRouteBody.toString());



        HttpPost request = new HttpPost(cfApiBaseUri + "/v3/routes/" + routeGuid + "/destinations");
        request.setHeader("Content-Type", "application/json");
        request.setEntity(new StringEntity(bindRouteBody.toString(), ContentType.APPLICATION_JSON));

        log.info("request to url: {} in bindRoute", request.getURI());
        HttpResponse response = httpClient.execute(request);
        String responseBody = EntityUtils.toString(response.getEntity());
        log.info("response from the bindRoute post request: {}", responseBody);



        // CfEnv cfEnv = new CfEnv();
        // cfApiBaseUri = cfEnv.getApp().getMap().getOrDefault(cfApiDestinationName, "https://api.cf.us10-001.hana.ondemand.com").toString();
        // log.info("cfApiBaseUri",cfApiBaseUri);
        // log.info("Binding route with GUID: {}", routeGuid);
        // CloseableHttpClient client = HttpClients.createDefault();
        // HttpPost request = new HttpPost(cfApiBaseUri + "/v3/routes/" + routeGuid + "/destinations");
        // log.info("request to url: {} in bindRoute", request.getURI());

        // Additional headers and body should be added here
        // client.execute(request);
        log.info("Route bound successfully");
    }

    // private String createRoute(String subscribedSubdomain, Guids guids) throws IOException {
    //     log.info("Creating route for subdomain: {}", subscribedSubdomain);
    //     JSONObject vcapApplication = getVcapApplication();
    //     String cfApiBaseUri = vcapApplication.getString(cfApiDestinationName);
    //     // CfEnv cfEnv = new CfEnv();
    //     // log.info("Application details from cfEnv: {}", cfEnv.getApp());
    //     // log.info("Service details from cfEnv: {}", cfEnv.findAllServices());
    
    //     // logEnvironmentDetails();
    
    //     // cfApiBaseUri = cfEnv.getApp().getMap().getOrDefault(cfApiDestinationName, "https://api.cf.us10-001.hana.ondemand.com").toString();
    //     log.info("Using cfApiBaseUri: {}", cfApiBaseUri);
    
    //     CloseableHttpClient client = HttpClients.createDefault();
    //     HttpPost request = new HttpPost(cfApiBaseUri + "/v3/routes");
    //     log.info("request to url: {} in createRoute", request.getURI());
    //     // Additional headers and body should be added here
    //     CloseableHttpResponse response = client.execute(request);
    //     log.info("HTTP response status: {}", response.getStatusLine());
    
    //     // Parsing and route creation logic
    //     String routeGuid = "extracted-route-guid";  // Example placeholder
    //     log.info("Route created with GUID: {}", routeGuid);
    
    //     return routeGuid;
    // }
    

    public String createRoute(String subscribedSubdomain, Guids guids) throws IOException {
        log.info("Creating route for subdomain: {}", subscribedSubdomain);

        JSONObject vcapApplication = getVcapApplication();
        String cfApiBaseUri = vcapApplication.getString(cfApiDestinationName);
        // HttpDestination destination = DestinationAccessor.getDestination(cfApiDestinationName).asHttp();
        Destination destination=null;
            DestinationLoader loader = DestinationAccessor.getLoader();
            DestinationOptions.Builder optionsBuilder = DestinationOptions.builder();
    
            // if ("f48ae449-d6a8-48ca-bcc7-bd0c4027846a".equals(tokenZid)) {
            //     // Handle provider tenant scenario
            //     optionsBuilder.augmentBuilder(
            //         DestinationServiceOptionsAugmenter.augmenter().retrievalStrategy(DestinationServiceRetrievalStrategy.ALWAYS_PROVIDER)
            //     );
            // } else {
                // Handle customer tenant scenario
                optionsBuilder.augmentBuilder(
                    DestinationServiceOptionsAugmenter.augmenter().retrievalStrategy(DestinationServiceRetrievalStrategy.ALWAYS_PROVIDER)
                );
            // }
    
            optionsBuilder.augmentBuilder(
                DestinationServiceOptionsAugmenter.augmenter().tokenExchangeStrategy(DestinationServiceTokenExchangeStrategy.FORWARD_USER_TOKEN)
            );
    
            DestinationOptions options = optionsBuilder.build();
    
            Try<Destination> destinationTry = loader.tryGetDestination(cfApiDestinationName, options);
    
            if (destinationTry.isSuccess()) {
                destination = destinationTry.get();
                log.info("Destination retrieved successfully.");
            } else {
                Throwable exception = destinationTry.getCause();
                log.error("Error retrieving destination: ", exception);
            }
        HttpClient httpClient = HttpClientAccessor.getHttpClient(destination);


        String routePath = getRoutePath(subscribedSubdomain);
        JSONObject createRouteBody = new JSONObject();
        
    
        createRouteBody.put("host", routePath);
        JSONObject spaceData = new JSONObject();
        spaceData.put("guid", guids.getSpaceGuid());
        JSONObject space = new JSONObject();
        space.put("data", spaceData);
        createRouteBody.put("relationships", new JSONObject().put("space", space));
        JSONObject domainData = new JSONObject();
        domainData.put("guid", guids.getDomainGuid());
        JSONObject domain = new JSONObject();
        domain.put("data", domainData);
        createRouteBody.put("relationships", createRouteBody.getJSONObject("relationships").put("domain", domain));
        log.info("Body of post request to routes:"+createRouteBody.toString());

        HttpPost request = new HttpPost(cfApiBaseUri + "/v3/routes");
        request.setHeader("Content-Type", "application/json");
        request.setEntity(new StringEntity(createRouteBody.toString(), ContentType.APPLICATION_JSON));
    
        log.info("Request to URL: {} in createRoute", request.getURI());
    
        HttpResponse response = httpClient.execute(request);
        try {
            int statusCode = response.getStatusLine().getStatusCode();
            HttpEntity responseEntity = response.getEntity();
            String responseString = EntityUtils.toString(responseEntity);
            log.info("HTTP response status in createRoute: {}", response.getStatusLine());
            log.info("HTTP response body in createRoute: {}", responseString);
    
            if (statusCode == HttpStatus.SC_CREATED) { // Assuming 201 is the status code for a successful creation
                JSONObject jsonResponse = new JSONObject(responseString);
                String routeGuid = jsonResponse.getString("guid");
                log.info("Route created with GUID in createRoute: {}", routeGuid);
                return routeGuid;
            } else {
                log.error("Failed to create route in createRoute, status: {}, response: {}", statusCode, responseString);
                throw new IOException("Failed to create route in createRoute: " + responseString);
            }
        } finally {
            EntityUtils.consume(response.getEntity()); // Ensuring the entity content is fully consumed and resources released
        }
    }
    
    
    private List<String> deleteParseGuid(String json) {
        JSONObject jsonObject = new JSONObject(json);

        // Access the "resources" array
        JSONArray resourcesArray = jsonObject.getJSONArray("resources");
        List<String> guids = new ArrayList<>();
        for (int i = 0; i < resourcesArray.length(); i++) {
            guids.add(resourcesArray.getJSONObject(i).getString("guid"));
        }
        return guids;
    }

    private void deleteRoute(String subscribedSubdomain) throws IOException {
        log.info("Deleting route for subdomain: {}", subscribedSubdomain);
        // CloseableHttpClient client = HttpClients.createDefault();

        JSONObject vcapApplication = getVcapApplication();
        String cfApiBaseUri = vcapApplication.getString(cfApiDestinationName);
        // HttpDestination destination = DestinationAccessor.getDestination(cfApiDestinationName).asHttp();
        Destination destination=null;
            DestinationLoader loader = DestinationAccessor.getLoader();
            DestinationOptions.Builder optionsBuilder = DestinationOptions.builder();
    
            // if ("f48ae449-d6a8-48ca-bcc7-bd0c4027846a".equals(tokenZid)) {
            //     // Handle provider tenant scenario
            //     optionsBuilder.augmentBuilder(
            //         DestinationServiceOptionsAugmenter.augmenter().retrievalStrategy(DestinationServiceRetrievalStrategy.ALWAYS_PROVIDER)
            //     );
            // } else {
                // Handle customer tenant scenario
                optionsBuilder.augmentBuilder(
                    DestinationServiceOptionsAugmenter.augmenter().retrievalStrategy(DestinationServiceRetrievalStrategy.ALWAYS_PROVIDER)
                );
            // }
    
            optionsBuilder.augmentBuilder(
                DestinationServiceOptionsAugmenter.augmenter().tokenExchangeStrategy(DestinationServiceTokenExchangeStrategy.FORWARD_USER_TOKEN)
            );
    
            DestinationOptions options = optionsBuilder.build();
    
            Try<Destination> destinationTry = loader.tryGetDestination(cfApiDestinationName, options);
    
            if (destinationTry.isSuccess()) {
                destination = destinationTry.get();
                log.info("Destination retrieved successfully.");
            } else {
                Throwable exception = destinationTry.getCause();
                log.error("Error retrieving destination: ", exception);
            }
        HttpClient httpClient = HttpClientAccessor.getHttpClient(destination);


        Guids guids=getCfGuids(APP_ROUTER_NAME);
        String appGuid=guids.getAppGuid();

        String url = String.format("%s/v3/apps/%s/routes?hosts=%s", cfApiBaseUri, appGuid, getRoutePath(subscribedSubdomain));

        HttpGet request = new HttpGet(url);
        log.info("request to url: {} in deleteRoute", request.getURI());
        HttpResponse response = httpClient.execute(request);
        log.info("response: {}", response.toString());
        // Actual deletion logic needs to be implemented
        log.info("Route deletion initiated for subdomain: {}", subscribedSubdomain);



          // Check for successful response
        if (response.getStatusLine().getStatusCode() != HttpStatus.SC_OK) {
            throw new IOException("Failed to retrieve routes: " + response.getStatusLine());
        }

        // Parse response to extract route GUIDs
        String responseBody = EntityUtils.toString(response.getEntity());
        List<String> routeGuids = deleteParseGuid(responseBody);
        log.info("Route GUIDs to delete: {}", routeGuids);
        // Delete each route using DELETE requests
        for (String routeGuid : routeGuids) {
            String deleteUrl = String.format("%s/v3/routes/%s", cfApiBaseUri, routeGuid);
            HttpDelete deleteRequest = new HttpDelete(deleteUrl);
            HttpResponse deleteResponse = httpClient.execute(deleteRequest);
            if (deleteResponse.getStatusLine().getStatusCode() != HttpStatus.SC_ACCEPTED) {
            throw new IOException("Failed to delete route " + routeGuid + ": " + deleteResponse.getStatusLine());
            }
        }

        log.info("Route deletion successful for subdomain: {}", subscribedSubdomain);

    }



    private static class Guids {
        private String appGuid;
        private String domainGuid;
        private String spaceGuid;

        public Guids(String appGuid, String domainGuid, String spaceGuid) {
            this.appGuid = appGuid;
            this.domainGuid = domainGuid;
            this.spaceGuid = spaceGuid;
        }

        public String getAppGuid() {
            return appGuid;
        }

        public String getDomainGuid() {
            return domainGuid;
        }

        public String getSpaceGuid() {
            return spaceGuid;
        }

        public void setSpaceGuid(String spaceGuid) {
            this.spaceGuid = spaceGuid;
        }
        public void setAppGuid(String appGuid) {
            this.appGuid = appGuid;
        }
        public void setDomainGuid(String domainGuid) {
            this.domainGuid = domainGuid;
        }
    }
}
