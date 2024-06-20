// package com.concircle.multitenantapp;
// import org.springframework.security.core.annotation.AuthenticationPrincipal;
// import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
// import org.springframework.web.bind.annotation.GetMapping;
// import org.springframework.web.bind.annotation.RequestBody;
// import org.springframework.web.bind.annotation.RequestMapping;
// import org.springframework.web.bind.annotation.RestController;


// import java.io.IOException;
// import java.io.InputStream;
// import java.net.HttpURLConnection;
// import java.nio.charset.StandardCharsets;
// import java.util.ArrayList;
// import java.util.HashMap;
// import java.util.List;
// import java.util.Map;
// import java.util.UUID;
// import java.util.concurrent.CompletableFuture;
// import java.util.concurrent.TimeUnit;
// import java.util.concurrent.TimeoutException;

// import org.springframework.beans.factory.annotation.Autowired;
// import org.springframework.boot.context.properties.bind.DefaultValue;
// import org.springframework.context.ApplicationEventPublisher;
// import org.springframework.core.env.Environment;
// import org.springframework.http.HttpHeaders;
// import org.springframework.http.HttpStatus;
// import org.springframework.http.MediaType;
// import org.springframework.http.ResponseEntity;
// import org.springframework.web.bind.annotation.DeleteMapping;
// import org.springframework.web.bind.annotation.GetMapping;
// import org.springframework.web.bind.annotation.PathVariable;
// import org.springframework.web.bind.annotation.PostMapping;
// import org.springframework.web.bind.annotation.RequestBody;
// import org.springframework.web.bind.annotation.RequestMapping;
// import org.springframework.web.bind.annotation.RestController;
// import org.springframework.security.core.authority.SimpleGrantedAuthority;
// import org.springframework.security.core.annotation.AuthenticationPrincipal;
// import com.sap.cloud.security.xsuaa.token.Token;


// import org.springframework.http.HttpStatus;
// import org.springframework.http.ResponseEntity;
// import org.springframework.security.core.authority.SimpleGrantedAuthority;
// import org.springframework.web.bind.annotation.GetMapping;
// import org.springframework.web.bind.annotation.RequestMapping;
// import org.springframework.web.bind.annotation.RestController;
// import org.springframework.security.core.annotation.AuthenticationPrincipal;
// import com.sap.cloud.security.xsuaa.token.Token;

// import jakarta.servlet.http.HttpServletResponse;
// import lombok.extern.log4j.Log4j2;

// import io.pivotal.cfenv.core.CfEnv;
// import io.pivotal.cfenv.core.CfService;



// @Log4j2

// @RestController

// @RequestMapping("/subscription")
// public class SubscriptionController {

//     @Autowired
//     private SubscriptionService subscriptionService;
//     private final List<String> relevantServices = new ArrayList<>();

//     public SubscriptionController() {
//         relevantServices.add("destination_multi");

//     }

//     @PostMapping("/subscribe")
//     public ResponseEntity<String> subscribeRoute(@RequestBody Map<String, String> body, @AuthenticationPrincipal JwtAuthenticationToken token) {
//         String subscribedSubdomain = body.get("subscribedSubdomain");
//         try {
//             String subscriberRoute = subscriptionService.subscribeRoute(subscribedSubdomain);
//             return ResponseEntity.ok(subscriberRoute);
//         } catch (Exception e) {
//             return ResponseEntity.status(500).body(e.getMessage());
//         }
//     }

//     @PostMapping("/unsubscribe")
//     public ResponseEntity<String> unsubscribeRoute(@RequestBody Map<String, String> body, @AuthenticationPrincipal JwtAuthenticationToken token) {
//         String subscribedSubdomain = body.get("subscribedSubdomain");
//         try {
//             subscriptionService.unsubscribeRoute(subscribedSubdomain);
//             return ResponseEntity.ok("Unsubscribed.");
//         } catch (Exception e) {
//             return ResponseEntity.status(500).body(e.getMessage());
//         }
//     }

//     @GetMapping("/dependencies")
//     public ResponseEntity<Map<String, Object>> getDependencies(@AuthenticationPrincipal JwtAuthenticationToken token) {
//         Map<String, Object> dependencies = new HashMap<>();
//         CfEnv cfEnv = new CfEnv();
//         relevantServices.forEach(serviceLabel -> {
//             try {
//                 CfService service = cfEnv.findServiceByName(serviceLabel);
//                 if (service != null) {
//                     Map<String, String> serviceDetails = new HashMap<>();
//                     serviceDetails.put("appId", service.getCredentials().getString("xsappname"));
//                     serviceDetails.put("appName", serviceLabel);
//                     dependencies.put(serviceLabel, serviceDetails);
//                 }
//             } catch (Exception e) {
//                 log.error("Failed to find service: " + serviceLabel, e);
//             }
//         });

//         return (ResponseEntity<Map<String, Object>>) dependencies;
//     }
    



// }
