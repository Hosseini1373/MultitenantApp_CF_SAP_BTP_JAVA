/**
 * This is the WebSecurityConfig class
 * this is used when we need authorization for the application
 * via xsuaa service and not via the identity provider
 */


 package com.concircle.multitenantapp;

 import com.sap.cloud.security.xsuaa.XsuaaServiceConfiguration;
 import com.sap.cloud.security.xsuaa.token.TokenAuthenticationConverter;

import lombok.extern.log4j.Log4j2;

import org.slf4j.Logger;
 import org.slf4j.LoggerFactory;
 import org.springframework.beans.factory.annotation.Autowired;
 import org.springframework.context.annotation.Bean;
 import org.springframework.context.annotation.Configuration;
 import org.springframework.core.convert.converter.Converter;
 import org.springframework.security.authentication.AbstractAuthenticationToken;
 import org.springframework.security.config.annotation.web.builders.HttpSecurity;
 import org.springframework.security.config.http.SessionCreationPolicy;
 import org.springframework.security.oauth2.jwt.Jwt;
 import org.springframework.security.web.SecurityFilterChain;
 


 @Log4j2
 @Configuration
 public class WebSecurityConfig {
 
     @Autowired
     XsuaaServiceConfiguration xsuaaServiceConfiguration;
 
 
     @Bean
     SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
         log.info("Configuring HTTP security filter chain");
 
         return http
                 .sessionManagement(management -> {
                     log.info("Setting session creation policy to STATELESS");
                     management.sessionCreationPolicy(SessionCreationPolicy.STATELESS);
                 })
                 .authorizeHttpRequests(registry -> {
                     log.info("Configuring request matchers for authentication");
                     registry
                         // .requestMatchers("/**").hasAuthority("Display")
                         .requestMatchers("/**").authenticated()
                         .anyRequest().denyAll();
                 })
                 .oauth2ResourceServer(oauth2Config -> oauth2Config
                         .jwt(jwtConfig -> {
                             log.info("Configuring JWT authentication converter");
                             jwtConfig.jwtAuthenticationConverter(getJwtAuthoritiesConverter());
                         }))
                 .build();
     }
 
     /**
      * Customizes how GrantedAuthority are derived from a Jwt
      *
      * @return jwt converter
      */
     Converter<Jwt, AbstractAuthenticationToken> getJwtAuthoritiesConverter() {
         log.info("Creating new TokenAuthenticationConverter");
         TokenAuthenticationConverter converter = new TokenAuthenticationConverter(xsuaaServiceConfiguration);
         converter.setLocalScopeAsAuthorities(true);
         log.info("Converter set to use local scope as authorities");
         return converter;
     }
 }
 

































// ********************************************************************************************************************
// Old configuration code:
// @Configuration

// public class WebSecurityConfig {

// 	@Autowired
// 	XsuaaServiceConfiguration xsuaaServiceConfiguration;

// 	@Bean
//     public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {


//         return http
//                 .sessionManagement(management -> management.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

//                 .authorizeHttpRequests(registry -> registry
//                         // .requestMatchers("/**").hasAuthority("Display")
//                         .requestMatchers("/**").authenticated()
//                         .anyRequest().denyAll())

//                 .oauth2ResourceServer(oauth2Config -> oauth2Config
//                         .jwt(jwtConfig -> jwtConfig
//                                 .jwtAuthenticationConverter(getJwtAuthoritiesConverter()) ))   // Adjust the converter to represent your use case
//                 // Use MyCustomHybridTokenAuthenticationConverter when IAS and XSUAA is used
//                 // Use MyCustomIasTokenAuthenticationConverter when only IAS is used
//                 .build();
//     }

// 	/**
// 	 * Customizes how GrantedAuthority are derived from a Jwt
// 	 *
// 	 * @returns jwt converter
// 	 */
// 	Converter<Jwt, AbstractAuthenticationToken> getJwtAuthoritiesConverter() {
// 		TokenAuthenticationConverter converter = new TokenAuthenticationConverter(xsuaaServiceConfiguration);
// 		converter.setLocalScopeAsAuthorities(true);
// 		return converter;
// 	}






//     //Workaround for hybrid use case until Cloud Authorization Service is globally available.
//     class MyCustomHybridTokenAuthenticationConverter implements Converter<Jwt, AbstractAuthenticationToken>
//     {
//         @Override
//         public AbstractAuthenticationToken convert( Jwt jwt )
//         {
//             if( jwt.hasClaim(TokenClaims.XSUAA.EXTERNAL_ATTRIBUTE) ) {
//                 return authConverter.convert(jwt);
//             }
//             return new AuthenticationToken(jwt, deriveAuthoritiesFromGroup(jwt));
//         }

//         private Collection<GrantedAuthority> deriveAuthoritiesFromGroup( Jwt jwt )
//         {
//             Collection<GrantedAuthority> groupAuthorities = new ArrayList<>();
//             if( jwt.hasClaim(TokenClaims.GROUPS) ) {
//                 List<String> groups = jwt.getClaimAsStringList(TokenClaims.GROUPS);
//                 for( String group : groups ) {
//                     groupAuthorities.add(new SimpleGrantedAuthority(group.replace("IASAUTHZ_", "")));
//                 }
//             }
//             return groupAuthorities;
//         }
//     }

//     //Workaround for IAS only use case until Cloud Authorization Service is globally available.
//     class MyCustomIasTokenAuthenticationConverter implements Converter<Jwt, AbstractAuthenticationToken>
//     {
//         @Override
//         public AbstractAuthenticationToken convert( Jwt jwt )
//         {
//             final List<String> groups = jwt.getClaimAsStringList(TokenClaims.GROUPS);
//             final List<GrantedAuthority>
//                     groupAuthorities =
//                     groups == null ?
//                             Collections.emptyList() :
//                             groups.stream().map(SimpleGrantedAuthority::new).collect(Collectors.toList());
//             return new AuthenticationToken(jwt, groupAuthorities);
//         }
//     }
// }


