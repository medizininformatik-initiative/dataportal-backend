# Configuration

The dataportal backend is configured via environment variables. See this sections for available variables and their defaults

#### `LOG_LEVEL`

Sets the log level being used. Possible values are: `error`, `warn`, `info`, `debug` and `trace`.

**Default:** `warn`

#### `HIBERNATE_SHOW_SQL`

Show the sql statements hibernate executes.

**Default:** `false`

#### `LOG_LEVEL_SQL`

The log level for hibernate.

**Default:** `warn`

#### `BROKER_CLIENT_MOCK_ENABLED`

Enables the mock client. Possible values are `true` and `false`.

**Default:** `true`

#### `BROKER_CLIENT_DIRECT_ENABLED`

Enables the direct client. Possible values are `true` and `false`.

**Default:** `false`

#### `BROKER_CLIENT_AKTIN_ENABLED`

Enables the aktin client. Possible values are `true` and `false`.

**Default:** `false`

#### `BROKER_CLIENT_DSF_ENABLED`

Enables the dsf client. Possible values are `true` and `false`.

**Default:** `false`

#### `KEYCLOAK_BASE_URL_ISSUER`

Base URL the keycloak instance uses in the issuer claim

**Default:** `http://localhost:8080`

#### `KEYCLOAK_BASE_URL_JWK`

Base URL for the JWK Set URI of the keycloak instance

**Default:** `http://localhost:8080`

#### `KEYCLOAK_REALM`

Realm to be used for checking bearer tokens.

**Default:** `dataportal`

#### `KEYCLOAK_ALLOWED_ROLE`

The name of the role a user needs to have basic access to the dataportal function.

**Default:** `DataportalUser`

#### `KEYCLOAK_POWER_ROLE`

Optional role that can be assigned to a user to free them from being subject to any hard limits (see _PRIVACY_QUOTA_HARD.*_ EnvVars).

**Default:** `DataportalPowerUser`

#### `KEYCLOAK_ADMIN_ROLE`

Role that gives admin rights to a user. Admins do not fall under any limits and can also see un-obfuscated site names.

**Default:** `DataportalAdmin`

#### `SPRING_DATASOURCE_URL`

The JDBC URL of the Postgres dataportal database.

**Default:** `jdbc:postgresql://dataportal-db:5432/dataportal`

#### `SPRING_DATASOURCE_USERNAME`

Username to connect to the Postgres dataportal database.

**Default:** `dataportaluser`

#### `SPRING_DATASOURCE_PASSWORD`

Password to connect to the Postgres dataportal database.

**Default:** `dataportalpw`

#### `ONTOLOGY_DB_MIGRATION_FOLDER`

The folder containing SQL migration scripts used by Flyway.

**Default:** `ontology/migration`

#### `MAPPINGS_FILE`

The file containing the mappings for CQL translation.

**Default:** `ontology/mapping_cql.json`

#### `CONCEPT_TREE_FILE`

The file containing the mapping tree for CQL translation.

**Default:** `ontology/mapping_tree.json`

#### `DSE_PROFILE_TREE_FILE`

The file containing the tree of the profiles needed for **d**ata **s**election and **e**xtraction.

**Default:** `ontology/dse/profile_tree.json`

#### `TERMINOLOGY_SYSTEMS_FILE`

The file containing mappings between terminology system urls and "normal" names.

**Default:** `ontology/terminology_systems.json`

#### `CQL_TRANSLATE_ENABLED`

When set to `true`, queries will be translated to CQL in addition to the CCDL representation.

**Default:** `true`

#### `FHIR_TRANSLATE_ENABLED`

When set to `true`, queries will be translated to fhir search in addition to the CCDL representation.

**Default:** `false`

#### `FLARE_WEBSERVICE_BASE_URL`

URL of the local FLARE webservice - needed for FHIR query translation and when running the DIRECT path

**Default:** `http://localhost:5000`

#### `CQL_SERVER_BASE_URL`

URL of the local FHIR server that handles CQL requests

**Default:** `http://cql`

#### `API_BASE_URL`

Sets the base URL of the webservice. This is necessary if the webservice is running behind a proxy server. If not filled, the API base URL is the request URL

**Default:** – (none)

#### `QUERY_VALIDATION_ENABLED`

When enabled, any CCDL submitted via the `run-query` endpoint is validated against the JSON schema located in `src/main/resources/query/query-schema.json`

**Default:** `true`

#### `QUERYRESULT_EXPIRY`

For what duration should query results be kept in memory? (ISO 8601 duration)

**Default:** `PT5M`

#### `QUERYRESULT_PUBLIC_KEY`

The public key in Base64-encoded DER format without banners and line breaks. Mandatory if _QUERYRESULT_DISABLE_LOG_FILE_ENCRYPTION_ is _false_

**Default:** – (none)

#### `QUERYRESULT_DISABLE_LOG_FILE_ENCRYPTION`

Disable encryption of the result log file.

**Default:** – (none)

#### `ALLOWED_ORIGINS`

Allowed origins for cross-origin requests. This should at least cover the frontend address.

**Default:** `http://localhost`

#### `MAX_SAVED_QUERIES_PER_USER`

How many slots does a user have to store saved queries.

**Default:** `10`

#### `EXPORT_CSV_DELIMITER`

The delimiter used when exporting dataqueries as csv files.

**Default:** `;`

#### `EXPORT_CSV_TEXTWRAPPER`

The wrapper char used to wrap an entry in the csv export of a dataquery.

**Default:** `"`

#### `PURGE_EXPIRED_QUERIES`

Cron expression to schedule when to check for (and delete) expired queries

**Default:** `0 0 * * * *`

#### `BROKER_CLIENT_DIRECT_AUTH_BASIC_USERNAME`

Username to use to connect to flare or directly to the FHIR server via CQL

**Default:** – (none)

#### `BROKER_CLIENT_DIRECT_AUTH_BASIC_PASSWORD`

Password for that user

**Default:** – (none)

#### `BROKER_CLIENT_DIRECT_AUTH_OAUTH_ISSUER_URL`

Issuer URL of OpenID Connect provider for authenticating access to OAuth2 protected FHIR server

**Default:** – (none)

#### `BROKER_CLIENT_DIRECT_AUTH_OAUTH_CLIENT_ID`

Client ID to use when authenticating at OpenID Connect provider

**Default:** – (none)

#### `BROKER_CLIENT_DIRECT_AUTH_OAUTH_CLIENT_SECRET`

Client secret to use when authenticating at OpenID Connect provider

**Default:** – (none)

#### `BROKER_CLIENT_DIRECT_USE_CQL`

Whether to use a CQL server or not.

**Default:** `false`

#### `BROKER_CLIENT_DIRECT_CQL_USE_ASYNC`

Whether to use the FHIR Async Request Pattern when using a CQL server.

**Default:** `false`

#### `BROKER_CLIENT_DIRECT_TIMEOUT`

Maximum time waiting for response from FLARE or FHIR server (ISO 8601 duration)

**Default:** `PT20S`

#### `BROKER_CLIENT_OBFUSCATE_RESULT_COUNT`

Whether the result counts retrieved from the direct broker shall be obfuscated

**Default:** `false`

#### `AKTIN_BROKER_BASE_URL`

Base URL for the AKTIN RESTful API

**Default:** – (none)

#### `AKTIN_BROKER_API_KEY`

API key for the broker RESTful API with admin privileges

**Default:** – (none)

#### `DSF_SECURITY_CACERT`

Certificate chain (`PEM` encoded) required for secured communication with the DSF middleware.

**Default:** – (none)

#### `DSF_SECURITY_CLIENT_CERTIFICATE_FILE`

Client certificate (`PEM` encoded) required for authentication with the DSF middleware.

**Default:** – (none)

#### `DSF_SECURITY_CLIENT_KEY_FILE`

Client private key (`PEM` encoded) required for authentication with the DSF middleware.

**Default:** – (none)

#### `DSF_SECURITY_CLIENT_KEY_PASSWORD`

Password for the encrypted client private key (required if key is password-protected).

**Default:** – (none)

#### `DSF_PROXY_HOST`

Proxy host to be used.

**Default:** – (none)

#### `DSF_PROXY_USERNAME`

Proxy username to be used.

**Default:** – (none)

#### `DSF_PROXY_PASSWORD`

Proxy password to be used.

**Default:** – (none)

#### `DSF_WEBSERVICE_BASE_URL`

Base URL pointing to the local ZARS FHIR server.

**Default:** – (none)

#### `DSF_WEBSERVICE_LOG_REQUESTS`

Log webservice client communication at log level INFO or below (**WARNING**: potentially contains sensitive data)

**Default:** `false`

#### `DSF_WEBSOCKET_URL`

URL pointing to the local ZARS FHIR server websocket endpoint.

**Default:** – (none)

#### `DSF_ORGANIZATION_ID`

Identifier for the local organization this backend is part of.

**Default:** – (none)

#### `PRIVACY_QUOTA_SOFT_CREATE_AMOUNT`

Amount of queries a user can create in the interval defined in _PRIVACY_QUOTA_SOFT_CREATE_INTERVAL_.

**Default:** `3`

#### `PRIVACY_QUOTA_SOFT_CREATE_INTERVAL`

(see description above)

**Default:** `PT1M`

#### `PRIVACY_QUOTA_HARD_CREATE_AMOUNT`

Amount of queries a user can create in the interval defined in _PRIVACY_QUOTA_HARD_CREATE_INTERVAL_ before being blacklisted.

**Default:** `50`

#### `PRIVACY_QUOTA_HARD_CREATE_INTERVAL`

(see description above)

**Default:** `P7D`

#### `PRIVACY_QUOTA_READ_SUMMARY_POLLINGINTERVAL`

Interval in which a user can read the summary query result endpoint.

**Default:** `PT5S`

#### `PRIVACY_QUOTA_READ_DETAILED_OBFUSCATED_POLLINGINTERVAL`

Interval in which a user can read the detailed obfuscated query result endpoint.

**Default:** `PT10S`

#### `PRIVACY_QUOTA_READ_DETAILED_OBFUSCATED_AMOUNT`

Amount of times a user can create a distinct detailed obfuscated result in the interval defined in _PRIVACY_QUOTA_READ_DETAILED_OBFUSCATED_INTERVAL _.

**Default:** `10`

#### `PRIVACY_QUOTA_READ_DETAILED_OBFUSCATED_INTERVAL`

(see description above)

**Default:** `PT3S`

#### `PRIVACY_THRESHOLD_RESULTS`

If the total number of results is below this number, return an empty result instead.

**Default:** `3`

#### `PRIVACY_THRESHOLD_SITES`

If the number of responding sites (above PRIVACY_THRESHOLD_SITES_RESULT) is below this number, only respond with a total amount of patients

**Default:** `20`

#### `PRIVACY_THRESHOLD_SITES_RESULT`

Any site that reports a number below this threshold is considered as non-responding (or zero) in regard to PRIVACY_THRESHOLD_SITES

**Default:** `20`

#### `ELASTIC_SEARCH_ENABLED`

Toggle elastic search connection

**Default:** `true`

#### `ELASTIC_SEARCH_HOST`

Host and port of the elastic search endpoint

**Default:** `localhost:9200`

| ELASTIC_SEARCH_FILTER         | Which parameters can be used to filter results  | `foo,bar,baz`      | `context,terminology,kds_module` |
#### `PT_CCDL_VERSION`

The used version of the Clinical Cohort Definition Language

**Default:** `unknown`

#### `PT_PORTAL_LINK`

URL to the portal page

**Default:** `https://antrag.forschen-fuer-gesundheit.de`

| PT_DSE_PATIENT_PROFILE_URL | URL of the patient profile used in data selection and extraction                                                   | `foo,bar,baz`     | `https://www.medizininformatik-initiative.de/fhir/core/modul-person/StructureDefinition/PatientPseudonymisiert` |
#### `PT_POLLING_TIME_UI`

How long should the UI poll for a result

**Default:** `PT1M`

#### `PT_POLLING_SUMMARY`

How often should the UI poll for summary results. Must be longer than `PRIVACY_QUOTA_READ_SUMMARY_POLLINGINTERVAL`

**Default:** `PT10S`                                                                                                         

