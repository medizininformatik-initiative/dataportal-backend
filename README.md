[![Build](https://github.com/medizininformatik-initiative/dataportal-backend/actions/workflows/ci.yml/badge.svg)](https://github.com/medizininformatik-initiative/dataportal-backend/actions/workflows/ci.yml)
[![OpenSSF Scorecard](https://api.scorecard.dev/projects/github.com/medizininformatik-initiative/dataportal-backend/badge)](https://scorecard.dev/viewer/?uri=github.com/medizininformatik-initiative/dataportal-backend)
[![codecov](https://codecov.io/gh/medizininformatik-initiative/dataportal-backend/graph/badge.svg?token=0E6UPL7I5K)](https://codecov.io/gh/medizininformatik-initiative/dataportal-backend)
[![GitHub Release](https://img.shields.io/github/v/release/medizininformatik-initiative/dataportal-backend?sort=semver&display_name=tag&style=flat&logo=github&label=current)]()

# Dataportal Backend

## Api Documentation

The OpenAPI documentation is available at <baseurl>/api/v5/swagger-ui/index.html

## Configuration Base

| EnvVar                                  | Description                                                                                                                                                   | Example            | Default                                           |
|-----------------------------------------|---------------------------------------------------------------------------------------------------------------------------------------------------------------|--------------------|---------------------------------------------------|
| LOG_LEVEL                               | Sets the log level being used. Possible values are: `error`, `warn`, `info`, `debug` and `trace`.                                                             |                    | `warn`                                            |
| HIBERNATE_SHOW_SQL                      | Show the sql statements hibernate executes.                                                                                                                   |                    | `false`                                           |
| LOG_LEVEL_SQL                           | The log level for hibernate.                                                                                                                                  |                    | `warn`                                            |
| BROKER_CLIENT_MOCK_ENABLED              | Enables the mock client. Possible values are `true` and `false`.                                                                                              |                    | `true`                                            |
| BROKER_CLIENT_DIRECT_ENABLED            | Enables the direct client. Possible values are `true` and `false`.                                                                                            |                    | `false`                                           |
| BROKER_CLIENT_AKTIN_ENABLED             | Enables the aktin client. Possible values are `true` and `false`.                                                                                             |                    | `false`                                           |
| BROKER_CLIENT_DSF_ENABLED               | Enables the dsf client. Possible values are `true` and `false`.                                                                                               |                    | `false`                                           |
| KEYCLOAK_BASE_URL_ISSUER                | Base URL the keycloak instance uses in the issuer claim                                                                                                       |                    | `http://localhost:8080`                           |
| KEYCLOAK_BASE_URL_JWK                   | Base URL for the JWK Set URI of the keycloak instance                                                                                                         |                    | `http://localhost:8080`                           |
| KEYCLOAK_REALM                          | Realm to be used for checking bearer tokens.                                                                                                                  |                    | `dataportal`                                      |
| KEYCLOAK_ALLOWED_ROLE                   | The name of the role a user needs to have basic access to the dataportal function.                                                                            |                    | `DataportalUser`                                  |
| KEYCLOAK_POWER_ROLE                     | Optional role that can be assigned to a user to free them from being subject to any hard limits (see _PRIVACY_QUOTA_HARD.*_ EnvVars).                         |                    | `DataportalPowerUser`                             |
| KEYCLOAK_ADMIN_ROLE                     | Role that gives admin rights to a user. Admins do not fall under any limits and can also see un-obfuscated site names.                                        |                    | `DataportalAdmin`                                 |
| SPRING_DATASOURCE_URL                   | The JDBC URL of the Postgres dataportal database.                                                                                                             |                    | `jdbc:postgresql://dataportal-db:5432/dataportal` |
| SPRING_DATASOURCE_USERNAME              | Username to connect to the Postgres dataportal database.                                                                                                      |                    | `dataportaluser`                                  |
| SPRING_DATASOURCE_PASSWORD              | Password to connect to the Postgres dataportal database.                                                                                                      |                    | `dataportalpw`                                    |
| ONTOLOGY_DB_MIGRATION_FOLDER            | The folder containing SQL migration scripts used by Flyway.                                                                                                   |                    | `ontology/migration`                              |
| MAPPINGS_FILE                           | The file containing the mappings for CQL translation.                                                                                                         |                    | `ontology/mapping_cql.json`                       |
| CONCEPT_TREE_FILE                       | The file containing the mapping tree for CQL translation.                                                                                                     |                    | `ontology/mapping_tree.json`                      |
| DSE_PROFILE_TREE_FILE                   | The file containing the tree of the profiles needed for **d**ata **s**election and **e**xtraction.                                                            |                    | `ontology/dse/profile_tree.json`                  |
| TERMINOLOGY_SYSTEMS_FILE                | The file containing mappings between terminology system urls and "normal" names.                                                                              |                    | `ontology/terminology_systems.json`               |
| CQL_TRANSLATE_ENABLED                   | When set to `true`, queries will be translated to CQL in addition to the CCDL representation.                                                                 |                    | `true`                                            |
| FHIR_TRANSLATE_ENABLED                  | When set to `true`, queries will be translated to fhir search in addition to the CCDL representation.                                                         |                    | `false`                                           |
| FLARE_WEBSERVICE_BASE_URL               | URL of the local FLARE webservice - needed for FHIR query translation and when running the DIRECT path                                                        |                    | `http://localhost:5000`                           |
| CQL_SERVER_BASE_URL                     | URL of the local FHIR server that handles CQL requests                                                                                                        |                    | `http://cql`                                      |
| QUERY_VALIDATION_ENABLED                | When enabled, any CCDL submitted via the `run-query` endpoint is validated against the JSON schema located in `src/main/resources/query/query-schema.json`    | `true` / `false`   | `true`                                            |
| QUERYRESULT_EXPIRY                      | For what duration should query results be kept in memory? (ISO 8601 duration)                                                                                 | `PT5M`             | `PT5M`                                            |
| QUERYRESULT_PUBLIC_KEY                  | The public key in Base64-encoded DER format without banners and line breaks. Mandatory if _QUERYRESULT_DISABLE_LOG_FILE_ENCRYPTION_ is _false_                |                    |                                                   |
| QUERYRESULT_DISABLE_LOG_FILE_ENCRYPTION | Disable encryption of the result log file.                                                                                                                    | `true` / `false`   |                                                   |
| ALLOWED_ORIGINS                         | Allowed origins for cross-origin requests. This should at least cover the frontend address.                                                                   |                    | `http://localhost`                                |
| MAX_SAVED_QUERIES_PER_USER              | How many slots does a user have to store saved queries.                                                                                                       |                    | `10`                                              |
| EXPORT_CSV_DELIMITER                    | The delimiter used when exporting dataqueries as csv files.                                                                                                   |                    | `;`                                               |
| EXPORT_CSV_TEXTWRAPPER                  | The wrapper char used to wrap an entry in the csv export of a dataquery.                                                                                      |                    | `"`                                               |
| PURGE_EXPIRED_QUERIES                   | Cron expression to schedule when to check for (and delete) expired queries                                                                                    |                    | `0 0 * * * *`                                     |


### Running the DIRECT Path

The DIRECT path can be run **either** with FLARE **or** with a CQL compatible server, not with both.
Result counts from the direct path can be obfuscated for privacy reasons. The current implementation
handles obfuscation by adding or subtracting a random number <=5.

| EnvVar                                        | Description                                                                                     | Example                               | Default |
|-----------------------------------------------|-------------------------------------------------------------------------------------------------|---------------------------------------|---------|
| BROKER_CLIENT_DIRECT_AUTH_BASIC_USERNAME      | Username to use to connect to flare or directly to the FHIR server via CQL                      | `dataportal-user`                     |         |
| BROKER_CLIENT_DIRECT_AUTH_BASIC_PASSWORD      | Password for that user                                                                          | `verysecurepassword`                  |         |
| BROKER_CLIENT_DIRECT_AUTH_OAUTH_ISSUER_URL    | Issuer URL of OpenID Connect provider for authenticating access to OAuth2 protected FHIR server | `https://auth.example.com/realms/foo` |         |
| BROKER_CLIENT_DIRECT_AUTH_OAUTH_CLIENT_ID     | Client ID to use when authenticating at OpenID Connect provider                                 | `foo_client`                          |         |
| BROKER_CLIENT_DIRECT_AUTH_OAUTH_CLIENT_SECRET | Client secret to use when authenticating at OpenID Connect provider                             | `verysecurepassword`                  |         |
| BROKER_CLIENT_DIRECT_USE_CQL                  | Whether to use a CQL server or not.                                                             |                                       | `false` |
| BROKER_CLIENT_DIRECT_CQL_USE_ASYNC            | Whether to use the FHIR Async Request Pattern when using a CQL server.                          |                                       | `false` |
| BROKER_CLIENT_DIRECT_TIMEOUT                  | Maximum time waiting for response from FLARE or FHIR server (ISO 8601 duration)                 | `PT24H`                               | `PT20S` |
| BROKER_CLIENT_OBFUSCATE_RESULT_COUNT          | Whether the result counts retrieved from the direct broker shall be obfuscated                  |                                       | `false` |

This is irrelevant if _BROKER_CLIENT_DIRECT_ENABLED_ is set to false.

#### Running the DIRECT Path with Local FLARE

To run the backend using the DIRECT broker path with FLARE,
the _FLARE_WEBSERVICE_BASE_URL_ environment variable needs to be set to a running instance of a FLARE
instance the backend is allowed to connect to.

#### Running the DIRECT Path with Local CQL Server

To run the backend using the DIRECT broker path with CQL,
the _CQL_SERVER_BASE_URL_ environment variable needs to be set to a running instance of a CQL compatible
FHIR server.


### Running the AKTIN Broker Path

To run the backend using the AKTIN broker path, the following environment variables need to be set:

| EnvVar                | Description                                              | Example                         | Default |
|-----------------------|----------------------------------------------------------|---------------------------------|---------|
| AKTIN_BROKER_BASE_URL | Base URL for the AKTIN RESTful API                       | `http://localhost:8080/broker/` |         |
| AKTIN_BROKER_API_KEY  | API key for the broker RESTful API with admin privileges | `xxxAdmin1234`                  |         |

When using API-key authentication, please make sure that the broker server has a
corresponding API-key entry with `OU=admin` contained in the DN-string.


### Running the DSF Path

To run the backend using the DSF path, the following environment variables need to be set:

| EnvVar                               | Description                                                                                                           | Example              | Default |
|--------------------------------------|-----------------------------------------------------------------------------------------------------------------------|----------------------|---------|
| DSF_SECURITY_CACERT                  | Certificate chain (`PEM` encoded) required for secured communication with the DSF middleware.                         |                      |         |
| DSF_SECURITY_CLIENT_CERTIFICATE_FILE | Client certificate (`PEM` encoded) required for authentication with the DSF middleware.                               |                      |         |
| DSF_SECURITY_CLIENT_KEY_FILE         | Client private key (`PEM` encoded) required for authentication with the DSF middleware.                               |                      |         |
| DSF_SECURITY_CLIENT_KEY_PASSWORD     | Password for the encrypted client private key (required if key is password-protected).                                |                      |         |
| DSF_PROXY_HOST                       | Proxy host to be used.                                                                                                |                      |         |
| DSF_PROXY_USERNAME                   | Proxy username to be used.                                                                                            |                      |         |
| DSF_PROXY_PASSWORD                   | Proxy password to be used.                                                                                            |                      |         |
| DSF_WEBSERVICE_BASE_URL              | Base URL pointing to the local ZARS FHIR server.                                                                      | `https://zars/fhir`  |         |
| DSF_WEBSERVICE_LOG_REQUESTS          | Log webservice client communication at log level INFO or below (**WARNING**: potentially contains sensitive data)     | `true`               | `false` |
| DSF_WEBSOCKET_URL                    | URL pointing to the local ZARS FHIR server websocket endpoint.                                                        | `wss://zars/fhir/ws` |         |
| DSF_ORGANIZATION_ID                  | Identifier for the local organization this backend is part of.                                                        | `MY ZARS`            |         |

### Privacy and Obfuscation

To prevent potentially malicious attempts to obtain critical patient data, several
countermeasures have been implemented. Users are restricted to creating a certain number of queries per timeframe.
Permanently pushing this limit will get a user blacklisted and needs manual intervention if the user shall be de-listed again.
Moreover, retrieving detailed results for queries (including a breakdown by (obfuscated) site names) is also limited.

If the number of responding sites is below the configured threshold, only the total number of results will be provided.
If the number of total results is below the threshold, no result will be provided.


| EnvVar                                                 | Description                                                                                                                                            | Example | Default |
|--------------------------------------------------------|--------------------------------------------------------------------------------------------------------------------------------------------------------|---------|---------|
| PRIVACY_QUOTA_SOFT_CREATE_AMOUNT                       | Amount of queries a user can create in the interval defined in _PRIVACY_QUOTA_SOFT_CREATE_INTERVAL_.                                                   |         | `3`     |
| PRIVACY_QUOTA_SOFT_CREATE_INTERVAL                     | (see description above)                                                                                                                                |         | `PT1M`  |
| PRIVACY_QUOTA_HARD_CREATE_AMOUNT                       | Amount of queries a user can create in the interval defined in _PRIVACY_QUOTA_HARD_CREATE_INTERVAL_ before being blacklisted.                          |         | `50`    |
| PRIVACY_QUOTA_HARD_CREATE_INTERVAL                     | (see description above)                                                                                                                                |         | `P7D`   |
| PRIVACY_QUOTA_READ_SUMMARY_POLLINGINTERVAL             | Interval in which a user can read the summary query result endpoint.                                                                                   |         | `PT5S`  |
| PRIVACY_QUOTA_READ_DETAILED_OBFUSCATED_POLLINGINTERVAL | Interval in which a user can read the detailed obfuscated query result endpoint.                                                                       |         | `PT10S` |
| PRIVACY_QUOTA_READ_DETAILED_OBFUSCATED_AMOUNT          | Amount of times a user can create a distinct detailed obfuscated result in the interval defined in _PRIVACY_QUOTA_READ_DETAILED_OBFUSCATED_INTERVAL _. |         | `10`    |
| PRIVACY_QUOTA_READ_DETAILED_OBFUSCATED_INTERVAL        | (see description above)                                                                                                                                |         | `PT3S`  |
| PRIVACY_THRESHOLD_RESULTS                              | If the total number of results is below this number, return an empty result instead.                                                                   |         | `3`     |
| PRIVACY_THRESHOLD_SITES                                | If the number of responding sites (above PRIVACY_THRESHOLD_SITES_RESULT) is below this number, only respond with a total amount of patients            |         | `20`    |
| PRIVACY_THRESHOLD_SITES_RESULT                         | Any site that reports a number below this threshold is considered as non-responding (or zero) in regard to PRIVACY_THRESHOLD_SITES                     |         | `20`    |

### Elasticsearch Connection

Searching the ontology/ontologies will move away from the local files towards an external (still provided by us) elastic
search service. For now, the configuration of that service has to be done manually. So additional steps are necessary to
use it. (NOT YET DOCUMENTED)

To configure the location of the external service, use the following parameters.


| EnvVar                        | Description                                     | Example            | Default                          |
|-------------------------------|-------------------------------------------------|--------------------|----------------------------------|
| ELASTIC_SEARCH_ENABLED        | Toggle elastic search connection                | `false`            | `true`                           |
| ELASTIC_SEARCH_HOST           | Host and port of the elastic search endpoint    | `192.168.1.1:9200` | `localhost:9200`                 |
| ELASTIC_SEARCH_FILTER         | Which parameters can be used to filter results  | `foo,bar,baz`      | `context,terminology,kds_module` |  

### Passthrough Variables

There are a few variables that are not used in the backend itself, but are forwarded to the frontend if requested.


| EnvVar                     | Description                                                                                                        | Example           | Default                                                                                                         |
|----------------------------|--------------------------------------------------------------------------------------------------------------------|-------------------|-----------------------------------------------------------------------------------------------------------------|
| PT_CCDL_VERSION            | The used version of the Clinical Cohort Definition Language                                                        | ``                | `unknown`                                                                                                       |
| PT_PORTAL_LINK             | URL to the portal page                                                                                             | `https://foo.bar` | `https://antrag.forschen-fuer-gesundheit.de`                                                                    |
| PT_DSE_PATIENT_PROFILE_URL | URL of the patient profile used in data selection and extraction                                                   | `foo,bar,baz`     | `https://www.medizininformatik-initiative.de/fhir/core/modul-person/StructureDefinition/PatientPseudonymisiert` |  
| PT_POLLING_TIME_UI         | How long should the UI poll for a result                                                                           | `PT10S`           | `PT1M`                                                                                                          |
| PT_POLLING_SUMMARY         | How often should the UI poll for summary results. Must be longer than `PRIVACY_QUOTA_READ_SUMMARY_POLLINGINTERVAL`  | `PT20S`           | `PT10S`                                                                                                         |

## Support for self-signed certificates

The dataportal backend supports the use of self-signed certificates from your own CAs.
On each startup, the dataportal backend will search through the folder /app/certs inside the container, add all found
CA *.pem files to a java truststore and start the application with this truststore.

Using docker-compose, mount a folder from your host (e.g.: ./certs) to the /app/certs folder,
add your *.pem files (one for each CA you would like to support) to the folder and ensure that they
have the .pem extension.

## Setting up Development

To run this project, the following steps need to be followed:

1. Add GitHub package repositories
2. Build the project
3. Setup database


### Adding GitHub Package Repositories

This project uses dependencies ([sq2cql](https://github.com/medizininformatik-initiative/sq2cql)) which are not hosted on maven central but on GitHub.

To download artifacts from GitHub package repositories, you need to add your GitHub login credentials to your central maven config file.

For more information, take a look at this GitHub documentation about [authentication](https://docs.github.com/en/free-pro-team@latest/packages/using-github-packages-with-your-projects-ecosystem/configuring-apache-maven-for-use-with-github-packages#authenticating-to-github-packages).

To install the packages using maven in your own projects, you need a personal GitHub access token. This [GitHub documentation](https://docs.github.com/en/free-pro-team@latest/github/authenticating-to-github/creating-a-personal-access-token) shows you how to generate one.

After that, add the following `<server>` configurations to the `<servers>` section in your local _.m2/settings.xml_. Replace `USERNAME` with your GitHub username and `TOKEN` with the previously generated personal GitHub access token. The token needs at least the scope `read:packages`.

```
<settings xmlns="http://maven.apache.org/SETTINGS/1.0.0"
  xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
  xsi:schemaLocation="http://maven.apache.org/SETTINGS/1.0.0
                      http://maven.apache.org/xsd/settings-1.0.0.xsd">
  <servers>
    <server>
      <id>mii</id>
      <username>USERNAME</username>
      <password>TOKEN</password>
    </server>
  </servers>
</settings>
```

### Building the Project

Since Release 4.0.0, this project is packaged without ontology files. They have to be downloaded first from the corresponding
[GitHub repository](https://github.com/medizininformatik-initiative/fhir-ontology-generator). This can be done by enabling
the maven profile "download-ontology" when building the project. This wipes any existing ontology files in your project.
So if you are working with your own ontology files, do **not** execute this.

Navigate to the root of this repository and execute `mvn install -Pdownload-ontology` (or omit the -Pdownload-ontology part
when working with your own).

You can change your run configuration in intellij to execute maven goals before running. So if you want to always just
grab the latest ontology from GitHub, you can Edit your run configuration, go to `modify options` and select `add before launch task`
and `run maven goal` with `clean package -Pdownload-ontology`. This is however not necessary, and your mileage may vary
in other IDEs if they offer such an option.

Be aware that Step 1 "Add GitHub package repositories" needs to be executed before.

### Setting up the Database

The project requires a PSQL database. The easiest way to set this up is to use the docker-compose file provided:

`docker-compose up -d`

Note that this starts an empty psql database as well as a containerized version of the backend.
The containerized version of the backend will then connect to the backend database.
One can then connect to the same database when starting the backend in an IDE.

## Working with the Backend

This backend provides a rest webservice which connects the [Dataportal GUI](https://github.com/medizininformatik-initiative/feasibility-gui)
and the corresponding middlewares.

To send a feasibility query to the backend, use the following example query:

```
curl --location --request POST 'http://localhost:8090/api/v5/query/feasibility' \
--header 'Content-Type: application/json' \
--data-raw '{
    "version": "http://to_be_decided.com/draft-1/schema#",
    "display": "",
    "inclusionCriteria": [
      [
        {
          "termCode": {
            "code": "29463-7",
            "system": "http://loinc.org",
            "version": "v1",
            "display": "Body Weight"
        },
        "valueFilter": {
            "type": "quantity-comparator",
            "unit": {
              "code": "kg",
              "display": "kilogram"
            },
            "comparator": "gt",
            "value": 90
          }
        }
      ]
    ]
  }'
```
another example
```
curl --location --request POST 'http://localhost:8090/api/v5/query/feasibility' \
--header 'Content-Type: application/json' \
--data-raw '{
    "version": "http://to_be_decided.com/draft-1/schema#",
    "display": "xxx",
    "inclusionCriteria": [
      [
        {
          "termCode": {
            "code": "J98.4",
            "system": "urn:oid:1.2.276.0.76.5.409",
            "version": "v1",
            "display": "xxx"
        }

        }
      ]
    ]
  }'
```


The response to this call will return a location header, which links to the endpoint where the result
for the query can be collected with one of the available sub-paths.
For a full description of the api, please refer to the swagger documentation (either in static/v3/api-docs/swagger.yaml
or at http://localhost:8090/api/v5/swagger-ui/index.html when running)


## Starting with Docker

### Creating the Docker Image
```
mvn install
docker build -t dataportal-backend .
```

### Starting the Backend and the Database
```
docker-compose up -d
```

**Note:** _If you need the database to run using another port than 5432 then set the corresponding environment variable like:_
```
DATAPORTAL_DATABASE_PORT=<your-desired-port> docker-compose up -d
```

### Testing if the Container is Running Properly
```
GET http://localhost:8090/api/v5/actuator/health
```

Should reply with status 200 and a JSON object

## Query Result Log Encryption

### Generating a Public/Private Key Pair

According to [BSI TR-02102-1][1], we have to use RSA keys with a minimum size of 3000 bit. We will use 3072 because that is the next possible value.

Generate the private key:

```sh
openssl genrsa -out key.pem 3072
```

Extract the public key from the private key in Base64-encoded DER format to put into `QUERYRESULT_PUBLIC_KEY`:

```sh
openssl rsa -in key.pem -outform DER -pubout | base64
```

If you like to use the `Decryptor` class, you have to convert the private key into the [PKCS#8](https://www.rfc-editor.org/rfc/rfc5208) format:

```sh
openssl pkcs8 -topk8 -inform PEM -outform DER -in key.pem -nocrypt | base64
```

You can use the following Java code to create a `PrivateKey` class for use with `Decryptor`:

```java
var keyFactory = KeyFactory.getInstance("RSA");
var privateKey = keyFactory.generatePrivate(new PKCS8EncodedKeySpec(Base64.getDecoder().decode("...")));
```

[1]: <https://www.bsi.bund.de/DE/Themen/Unternehmen-und-Organisationen/Standards-und-Zertifizierung/Technische-Richtlinien/TR-nach-Thema-sortiert/tr02102/tr02102_node.html>
