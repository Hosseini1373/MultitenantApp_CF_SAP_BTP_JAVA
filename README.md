1- In POM I added the XSUAA dependencies
2- Added the xs-app.json
3- Added the content of the xs-security to the service in BTP (xs-security.json)
4- Added the content of the saas-registry-config.json to the service in BTP (saas-registry-config.json)
5- Npm install the app router application
6- There must be a destination called cf_api on the provider side that takes care of 
   authentication with https://api.cf.us10-001.hana.ondemand.com/v3. We need this api
   to get the domain and app guids. Unfortunately, there is no out-of-the-box access of this API when you are in 
   the context of an application (refer to https://sap.github.io/cloud-sdk/docs/js/tutorials/multi-tenant-application). 
7- There must be a destination called myDestination in the customer tenants
8- We must have WebSecurityConfig.java for the checkDestination API to work. If this API is not needed,
   we could comment this file out and other APIs would work normally
   
