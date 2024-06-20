# Project README

## Overview
This project integrates XSUAA authentication and multi-tenancy support into an SAP Business Technology Platform (BTP) application. It includes the necessary configurations and dependencies to secure the application and manage tenant-specific data access.

## Prerequisites
- SAP BTP account
- Cloud Foundry CLI
- Node.js and npm installed

## Configuration Steps

### 1. XSUAA Dependencies
Add XSUAA service dependencies to your project's POM file to handle authentication and authorization.

### 2. Application Configuration
Create an `xs-app.json` file in your project's root directory to configure the application routes and authentication scopes.

### 3. Security Configuration
Add the content of `xs-security.json` to the XSUAA service on SAP BTP to define the security profile of the application.

### 4. SaaS Registry Configuration
Update the `saas-registry-config.json` in the SAP BTP to configure the SaaS application properties.

### 5. Install Dependencies
Run `npm install` to install the necessary Node.js dependencies including the app router.

### 6. Destination Configuration for Provider
Ensure a destination named `cf_api` is configured on the provider side. This destination is essential for authenticating and interacting with SAP Cloud Foundry APIs to retrieve domain and application GUIDs.

### 7. Destination Configuration for Customers
Setup a destination called `myDestination` in customer tenants to handle tenant-specific routing.

### 8. Web Security Configuration
Include `WebSecurityConfig.java` in your project to manage security configurations. If the `checkDestination` API is not required, this file can be commented out to ensure other APIs function normally.

## Deployment Commands
Use the following Cloud Foundry commands to log in and push your application to SAP BTP:

```bash
cf login -a https://api.cf.<domain>.hana.ondemand.com
cf push -f manifest.yml
```

## Additional Information
Refer to the [SAP Cloud SDK Documentation](https://sap.github.io/cloud-sdk/docs/js/tutorials/multi-tenant-application) for more details on multi-tenant application development.

This README provides a structured approach to deploying and managing your SAP BTP application with multi-tenancy and security configurations. Adjust the domain in the deployment commands to match your specific SAP Cloud Foundry environment.