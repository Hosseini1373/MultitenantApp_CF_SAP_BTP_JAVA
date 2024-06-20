package com.concircle.multitenantapp;


import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.boot.web.servlet.ServletComponentScan;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.security.core.annotation.AuthenticationPrincipal;

import com.auth0.jwt.interfaces.DecodedJWT;
import com.sap.cloud.sdk.cloudplatform.connectivity.Destination;
import com.sap.cloud.sdk.cloudplatform.connectivity.DestinationAccessor;
import com.sap.cloud.sdk.cloudplatform.connectivity.DestinationLoader;
import com.sap.cloud.sdk.cloudplatform.security.AuthToken;
import com.sap.cloud.sdk.cloudplatform.security.AuthTokenAccessor;
import com.sap.cloud.sdk.cloudplatform.security.AuthTokenFacade;
import com.sap.cloud.security.xsuaa.token.SpringSecurityContext;
import com.sap.cloud.security.xsuaa.token.Token;


import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.sap.cloud.security.xsuaa.token.Token;
import com.sap.cloud.sdk.cloudplatform.connectivity.DestinationOptions;
import com.sap.cloud.sdk.cloudplatform.connectivity.DestinationServiceOptionsAugmenter;
import com.sap.cloud.sdk.cloudplatform.connectivity.DestinationServiceRetrievalStrategy;
import com.sap.cloud.sdk.cloudplatform.connectivity.DestinationServiceTokenExchangeStrategy;
import com.sap.cloud.sdk.cloudplatform.connectivity.ServiceBindingDestinationOptions.Options;
import com.sap.cloud.sdk.cloudplatform.requestheader.RequestHeaderAccessor;
import com.sap.cloud.sdk.cloudplatform.requestheader.RequestHeaderContainer;
import com.sap.cloud.sdk.cloudplatform.security.AuthTokenFacade;
import java.util.Optional;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.log4j.Log4j2;

import io.pivotal.cfenv.core.CfEnv;
import io.pivotal.cfenv.core.CfService;
import io.vavr.control.Try;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;


@Log4j2

@RestController
@RequestMapping(path = "")

@ComponentScan({"com.sap.cloud.sdk"})
@ServletComponentScan({"com.sap.cloud.sdk"})

public class UIController {

    private final List<String> relevantServices = new ArrayList<>();

    @Autowired
    private SubscriptionService subscriptionService;


    @GetMapping(path = "")
    public ResponseEntity<String> readAll(@AuthenticationPrincipal Token token) {
        // if (!token.getAuthorities()
        // .contains(new SimpleGrantedAuthority("Display"))
        // ) {
        //     throw new NotAuthorizedException("This operation requires \"Display\" scope");
        // }
        log.info("readAll, Hello World!");
        return new ResponseEntity<String>("Hello World!", HttpStatus.OK);
    }   

    @GetMapping("/authTest")
    public ResponseEntity<String> authTest() {
        log.info("readAll, Hello World!");
        return new ResponseEntity<String>("Hello World!", HttpStatus.OK);
    }    




    public void yourBusinessLogic() {
    try{
        DestinationLoader loader = DestinationAccessor.getLoader();
        DestinationOptions.Builder optionsBuilder = DestinationOptions.builder();
        optionsBuilder.augmentBuilder(
            DestinationServiceOptionsAugmenter.augmenter().retrievalStrategy(DestinationServiceRetrievalStrategy.CURRENT_TENANT)
        );


        optionsBuilder.augmentBuilder(
            DestinationServiceOptionsAugmenter.augmenter().tokenExchangeStrategy(DestinationServiceTokenExchangeStrategy.FORWARD_USER_TOKEN)
        );

        DestinationOptions options = optionsBuilder.build();

        Try<Destination> destinationTry = loader.tryGetDestination("myDestination", options);

        if (destinationTry.isSuccess()) {
            Destination destination = destinationTry.get();
            log.info("Destination retrieved successfully: " + destination.getPropertyNames());
        } else {
            Throwable exception = destinationTry.getCause();
            log.error("Error retrieving destination: ", exception);
        }
    } catch (Exception e) {
        log.error("Error processing request: ", e);
    }
    }

    @GetMapping("/welcome")
    public @ResponseBody String showWelcomeMessage(@AuthenticationPrincipal Token token) {
        RequestHeaderContainer headers = RequestHeaderAccessor.getHeaderContainer();
        List<String> authorization =headers.getHeaderValues("authorization"); // will return ["Bearer DUMMY_JWT"]
        List<String> cookie =headers.getHeaderValues("set-cookie"); // will return ["cookie-1; cookie-2"]
        List<String> language =headers.getHeaderValues("accept-language"); // will return ["en-US"]
        List<String> appHeader =headers.getHeaderValues("x-app-specific-header"); // will return ["customer-value"]
        log.info("Authorization: "+authorization+"; Cookie: "+cookie+"; Language: "+language+"; AppHeader: "+appHeader+"; ");
        // RequestHeaderContainer updatedHeaders =
        // headers
        //     .toBuilder()
        //     .withoutHeader("set-cookie")
        //     .withHeader("x-app-specific-header", "application-value")
        //     .replaceHeader("accept-language", "de-DE")
        //     .build();
        RequestHeaderAccessor.executeWithHeaderContainer(headers, () -> yourBusinessLogic());


        String line1 = "Hello " + token.getLogonName();
        String line2 = "your tenant sub-domain is " + token.getSubdomain();
        String line3 = "your tenant zone id is " + token.getZoneId();
        log.info(line1+"; "+line2+"; "+line3+"; ");
        return line1 + "; " + line2 + "; " + line3+ "; "+"Authorization: "+authorization+"; Cookie: "+cookie+"; Language: "+language+"; AppHeader: "+appHeader+"; ";
    }

    @GetMapping("/checkDestination")
    public ResponseEntity<String> checkDestination(@AuthenticationPrincipal Token token) {
        try {
            //TODO: Unfortunatelly the token is somehow null here, so we use 
            // the SpringSecurityContext.getToken() method to get the token
            

            Token tokenDirect = SpringSecurityContext.getToken();
            log.info("The token_direct is: " + tokenDirect.getAppToken());
            String tokenZid = tokenDirect.getZoneId();
            String tenantText;

            if ("f48ae449-d6a8-48ca-bcc7-bd0c4027846a".equals(tokenZid)) {
                tenantText = "You are on provider tenant: " + tokenZid;
                log.info("You are on provider tenant.");
            } else {
                tenantText = "You are on customer tenant: " + tokenZid;
                log.info("You are on customer tenant.");
            }
    
            log.info("The token_direct zone id is: " + tokenZid);
            log.info(tenantText);
    
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
                    DestinationServiceOptionsAugmenter.augmenter().retrievalStrategy(DestinationServiceRetrievalStrategy.CURRENT_TENANT)
                );
            // }
    
            optionsBuilder.augmentBuilder(
                DestinationServiceOptionsAugmenter.augmenter().tokenExchangeStrategy(DestinationServiceTokenExchangeStrategy.FORWARD_USER_TOKEN)
            );
    
            DestinationOptions options = optionsBuilder.build();
    
            Try<Destination> destinationTry = loader.tryGetDestination("myDestination", options);
    
            if (destinationTry.isSuccess()) {
                Destination destination = destinationTry.get();
                log.info("Destination retrieved successfully.");
                return ResponseEntity.ok(tenantText + ". The destination description is: " + destination.getPropertyNames());
            } else {
                Throwable exception = destinationTry.getCause();
                log.error("Error retrieving destination: ", exception);
                return ResponseEntity.internalServerError().body("Failed to retrieve destination: " + exception.getMessage());
            }
        } catch (Exception e) {
            log.error("Error processing request: ", e);
            return ResponseEntity.internalServerError().body("Error processing request: " + e.getMessage());
        }
    }
    


    // @GetMapping("/checkDestination")
    // public ResponseEntity<String> checkDestination(@AuthenticationPrincipal Token token) {
    //     try {
    //     //     AuthToken authToken;
    //     //     try{// Fetch the current JWT token
    //     //         authToken = AuthTokenAccessor.getCurrentToken();
    //     //     } catch (Exception e) {
    //     //         log.warn("No JWT provided in the request, using provider tenant");
    //     //         return ResponseEntity.ok("You are on provider tenant.");
    //     //     }
    //         log.info(token.toString());
    //         String tenantText = "You are on tenant: " + token.getAppToken();
    //         log.info(tenantText);

    //         // Setup the destination loader with JWT-based options
    //         DestinationLoader loader = DestinationAccessor.getLoader();
    //         DestinationOptions options =
    //             DestinationOptions
    //                 .builder()
    //                 .augmentBuilder(
    //                     DestinationServiceOptionsAugmenter
    //                         .augmenter()
    //                         .retrievalStrategy(DestinationServiceRetrievalStrategy.ONLY_SUBSCRIBER))
    //                 .augmentBuilder(
    //                     DestinationServiceOptionsAugmenter
    //                         .augmenter()
    //                         .tokenExchangeStrategy(DestinationServiceTokenExchangeStrategy.FORWARD_USER_TOKEN))
    //                 .build();

    //         // Attempt to fetch the destination using the provided JWT
    //         Try<Destination> destinationTry = loader.tryGetDestination("myDestination", options);

    //         if (destinationTry.isSuccess()) {
    //             // If the retrieval is successful, log and respond with success
    //             Destination destination = destinationTry.get();
    //             log.info("Destination retrieved successfully.");
    //             return ResponseEntity.ok(tenantText + ". The destination description is: " + destination.getPropertyNames());
    //         } else {
    //             // If the retrieval fails, log the error and respond accordingly
    //             Throwable exception = destinationTry.getCause();
    //             log.error("Error retrieving destination: ", exception);
    //             return ResponseEntity.internalServerError().body("Failed to retrieve destination: " + exception.getMessage());
    //         }
    //     } catch (Exception e) {
    //         // Log any exceptions that occur and respond with internal server error
    //         log.error("Error processing request: ", e);
    //         return ResponseEntity.internalServerError().body("Error processing request: " + e.getMessage());
    //     }
    // }


    
    @GetMapping("/dependencies")
    public ResponseEntity<List<Map<String, String>>> getDependencies(@AuthenticationPrincipal Token token) {
        log.info("Accessing getDependencies with token authorities");
        try {
            List<Map<String, String>> dependencies = subscriptionService.getDependencies();
            return ResponseEntity.ok(dependencies);
        } catch (Exception e) {
            log.error("Error retrieving dependencies: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // @GetMapping("/subscriptions")
    // public ResponseEntity<String> subscribions(@AuthenticationPrincipal Token token) {
    //     return null;
    // }


    @PutMapping("/subscription/{subscriberTenantId}")
    public ResponseEntity<String> subscribeRoute(@RequestBody Map<String, Object> body, @AuthenticationPrincipal Token token) {
        log.info("Request body received: {}", body);
        try {
            String subscribedSubdomain = (String) body.get("subscribedSubdomain");
            log.info("subscribedSubdomain received from user: " + subscribedSubdomain);
            if (subscribedSubdomain == null) {
                return ResponseEntity.badRequest().body("subscribedSubdomain is required");
            }
            String subscriberRoute = subscriptionService.subscribeRoute(subscribedSubdomain);
            return ResponseEntity.ok(subscriberRoute);
        } catch (Exception e) {
            log.error("Error processing subscription", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(e.getMessage());
        }
    }


    // @PutMapping("/subscription/{subscriberTenantId}")
    // public ResponseEntity<String> subscribeRoute(@RequestBody Map<String, String> body, @AuthenticationPrincipal Token token) {
    //     //TODO: Check if the user has the required scope
    //     // if (!token.getAuthorities().contains(new SimpleGrantedAuthority("Subscribe"))) {
    //     //     return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
    //     // }
    //     String subscribedSubdomain = body.get("subscribedSubdomain");
    //     log.info("subscribedSubdomain received from user: "+subscribedSubdomain);
    //     try {
    //         String subscriberRoute = subscriptionService.subscribeRoute(subscribedSubdomain);
    //         return ResponseEntity.ok(subscriberRoute);
    //     } catch (Exception e) {
    //         return ResponseEntity.status(500).body(e.getMessage());
    //     }
    // }

    @DeleteMapping("/subscription/{subscriberTenantId}")
    public ResponseEntity<String> unsubscribeRoute(@RequestBody Map<String, Object> body, @AuthenticationPrincipal Token token) {
        //TODO: Implement the check for the "Unsubscribe" scope
        // if (!token.getAuthorities().contains(new SimpleGrantedAuthority("Unsubscribe"))) {
        //     return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        // }
        try {
        String subscribedSubdomain = (String) body.get("subscribedSubdomain");
        log.info("subscribedSubdomain received from user: " + subscribedSubdomain);
        if (subscribedSubdomain == null) {
                return ResponseEntity.badRequest().body("subscribedSubdomain is required");
            }
        
            String subscriberRoute =  subscriptionService.unsubscribeRoute(subscribedSubdomain);
            return ResponseEntity.ok(subscriberRoute);
        } catch (Exception e) {
            return ResponseEntity.status(500).body(e.getMessage());
        }
    }



    // @PostMapping("/subscriptionTest/{subscriberTenantId}")
    // public ResponseEntity<String> subscribeRouteTest(@PathVariable String subscriberTenantId, @RequestBody Map<String, String> body, @AuthenticationPrincipal Token token) {
    //     //TODO: Check if the user has the required scope
    //     // if (!token.getAuthorities().contains(new SimpleGrantedAuthority("Subscribe"))) {
    //     //     return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
    //     // }
    //     String subscribedSubdomain = body.get("subscribedSubdomain");
    //     try {
    //         String subscriberRoute = subscriptionService.subscribeRoute(subscribedSubdomain);
    //         return ResponseEntity.ok(subscriberRoute);
    //     } catch (Exception e) {
    //         return ResponseEntity.status(500).body(e.getMessage());
    //     }
    // }

    // @DeleteMapping("/subscriptionTest/{subscriberTenantId}")
    // public ResponseEntity<String> unsubscribeRouteTest(@PathVariable String subscriberTenantId, @RequestBody Map<String, String> body, @AuthenticationPrincipal Token token) {
    //     //TODO: Implement the check for the "Unsubscribe" scope
    //     // if (!token.getAuthorities().contains(new SimpleGrantedAuthority("Unsubscribe"))) {
    //     //     return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
    //     // }
    //     String subscribedSubdomain = body.get("subscribedSubdomain");
    //     try {
    //         subscriptionService.unsubscribeRoute(subscribedSubdomain);
    //         return ResponseEntity.ok("Unsubscribed.");
    //     } catch (Exception e) {
    //         return ResponseEntity.status(500).body(e.getMessage());
    //     }
    // }



}

