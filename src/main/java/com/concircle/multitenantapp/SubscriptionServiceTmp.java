// package com.concircle.multitenantapp;

// import org.apache.hc.core5.http.HttpEntity;
// import org.apache.hc.core5.http.ParseException;
// import org.apache.hc.core5.http.io.entity.EntityUtils;
// import org.apache.http.client.methods.CloseableHttpResponse;
// import org.apache.http.client.methods.HttpGet;
// import org.apache.http.client.methods.HttpPost;
// import org.apache.http.entity.StringEntity;
// import org.apache.http.impl.client.CloseableHttpClient;
// import org.apache.http.impl.client.HttpClients;
// import org.springframework.beans.factory.annotation.Autowired;
// import org.springframework.stereotype.Service;

// import com.fasterxml.jackson.databind.JsonNode;
// import com.fasterxml.jackson.databind.ObjectMapper;

// import io.pivotal.cfenv.core.CfEnv;
// import io.pivotal.cfenv.core.CfService;
// import lombok.extern.log4j.Log4j;

// import java.io.IOException;
// import java.net.http.HttpResponse;
// import java.nio.charset.StandardCharsets;
// import java.util.ArrayList;
// import java.util.Arrays;
// import java.util.HashMap;
// import java.util.List;
// import java.util.Map;
// import lombok.extern.log4j.Log4j2;

// @Log4j2

// @Service
// public class SubscriptionServiceTmp {

//     @Autowired
//     private CfEnv cfEnv;


//     private final List<String> relevantServices = new ArrayList<>();
//     private final ObjectMapper objectMapper = new ObjectMapper();
//     private final CloseableHttpClient httpClient = HttpClients.createDefault();
//     private String cfApiBaseUri; // Replace with actual API base URI
//     private final String appRouterName = "approuter";
//     private final String routePrefix = "route-prefix";
//     private final String cfApiDestinationName = "cf-api";
//     private final List<String> services = new ArrayList<>(Arrays.asList("destination_multi"));


//     private org.apache.http.HttpResponse response;






//     public SubscriptionServiceTmp() {
//         cfEnv = new CfEnv();
//         cfApiBaseUri=cfEnv.getApp().getMap().getOrDefault(cfApiDestinationName, "https://api.default.cf.example.com").toString();
//         services.forEach(service -> {
//             relevantServices.add(service);
//         });
//     }


//     public Map<String, Object> getDependencies() {
//         Map<String, Object> dependencies = new HashMap<>();
//         relevantServices.forEach(serviceLabel -> {
//             try {
//                 log.info("searching for service: " + serviceLabel);
//                 CfService service = cfEnv.findServiceByName(serviceLabel);
//                 if (service != null) {
//                     Map<String, String> serviceDetails = new HashMap<>();
//                     String xsappname= service.getCredentials().getString("xsappname");
//                     log.info("Found service: " + xsappname);
//                     serviceDetails.put("appId", xsappname);
//                     serviceDetails.put("appName", serviceLabel);
//                     dependencies.put(serviceLabel, serviceDetails);
//                 }
//             } catch (Exception e) {
//                 log.error("Failed to find service: " + serviceLabel, e);
//             }
//         });

//         return dependencies;
//     }




//     public Map<String, String> getCfGuids(String appName) throws IOException, ParseException {
        
//         String orgGuid=cfEnv.getApp().getMap().getOrDefault("organization_id", "https://api.default.cf.example.com").toString();
//         String spaceGuid = cfEnv.getApp().getSpaceId();
//         Map<String, String> guids = new HashMap<>();

//         HttpGet request = new HttpGet(cfApiBaseUri + "/v3/apps?organization_guids=" + orgGuid + "&space_guids=" + spaceGuid + "&names=" + appName);
//         HttpResponse response = (HttpResponse) httpClient.execute(request);
//         String json = EntityUtils.toString((HttpEntity) ((org.apache.http.HttpResponse) response).getEntity());
//         JsonNode appNode = objectMapper.readTree(json).path("resources").get(0);
//         guids.put("appGuid", appNode.path("guid").asText());

//         try {
//             request = new HttpGet(cfApiBaseUri + "/v3/domains?names=" + encodeLandscape());
//             response = (HttpResponse) httpClient.execute(request);
//             json = EntityUtils.toString((HttpEntity) ((org.apache.http.HttpResponse) response).getEntity());
//             JsonNode domainNode = objectMapper.readTree(json).path("resources").get(0);
//             guids.put("domainGuid", domainNode.path("guid").asText());
//         } catch (Exception e) {
//             log.error("Error fetching domain GUID: ", e);
//         }

//         guids.put("spaceGuid", spaceGuid);
//         return guids;
//     }

//     private String encodeLandscape() {
//         String landscape = cfEnv.getApp().getUris().get(0).split("\\.")[1];
//         return landscape.replaceAll("\\.", "-");
//     }

//     public String createRoute(String subscribedSubdomain, Map<String, String> guids) throws IOException, ParseException {
//         String routePath = routePrefix + "-" + subscribedSubdomain;
//         String jsonBody = String.format("{\"host\":\"%s\",\"relationships\":{\"space\":{\"data\":{\"guid\":\"%s\"}},\"domain\":{\"data\":{\"guid\":\"%s\"}}}}",
//                 routePath, guids.get("spaceGuid"), guids.get("domainGuid"));

//         HttpPost post = new HttpPost(cfApiBaseUri + "/v3/routes");
//         post.setEntity(new StringEntity(jsonBody));
//         HttpResponse response = (HttpResponse) httpClient.execute(post);
//         jsonBody = EntityUtils.toString((HttpEntity) ((org.apache.http.HttpResponse) response).getEntity());
//         return objectMapper.readTree(jsonBody).path("guid").asText();


//     }
// }

