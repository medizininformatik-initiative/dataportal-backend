# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/)

## [8.0.0] - 2025-12-05

- Based on ontology **[v3.9.2](https://github.com/medizininformatik-initiative/fhir-ontology-generator/releases/tag/v3.9.2)**
- Known issue: The api documentation does not reflect the changes from ([#734](https://github.com/medizininformatik-initiative/dataportal-backend/issues/734))

### Added
- Search endpoint for exact bulk search for criteria and value sets ([#690](https://github.com/medizininformatik-initiative/dataportal-backend/issues/690))
### Changed
- Project was renamed from `feasibility-backend` to `dataportal-backend`
- Change base package name to `de.medizininformatikinitiative.dataportal.backend` ([#760](https://github.com/medizininformatik-initiative/dataportal-backend/issues/760))
- Provide Client Certificate in PEM format Instead of Java Keystore ([#497](https://github.com/medizininformatik-initiative/dataportal-backend/issues/497))
### Fixed
- Reconnect Websocket to DSF after Connection Failure ([#738](https://github.com/medizininformatik-initiative/dataportal-backend/issues/738))
### Security
- Updated dependencies and GitHub actions

## [7.6.0] - 2025-11-14

- Based on ontology **[v3.9.2](https://github.com/medizininformatik-initiative/fhir-ontology-generator/releases/tag/v3.9.2)**
- Known issue: The api documentation does not reflect the changes from ([#734](https://github.com/medizininformatik-initiative/dataportal-backend/issues/734))

### Added
- Settings endpoint was added to provide configuration settings to the GUI ([#663](https://github.com/medizininformatik-initiative/dataportal-backend/issues/663))
- Endpoint to retrieve all ui profiles was added ([#721](https://github.com/medizininformatik-initiative/dataportal-backend/issues/721))
- An endpoint was added to convert crtdl to a zip file with csv files ([#721](https://github.com/medizininformatik-initiative/dataportal-backend/issues/721))
- Relationship entries (parents/children/related_items) in terminology search now contain selectable, termcode and terminology attributes ([#664](https://github.com/medizininformatik-initiative/dataportal-backend/issues/664), [#734](https://github.com/medizininformatik-initiative/dataportal-backend/issues/734))
### Changed
- Ontology was updated to [v3.9.2](https://github.com/medizininformatik-initiative/fhir-ontology-generator/releases/tag/v3.9.2) ([#734](https://github.com/medizininformatik-initiative/dataportal-backend/issues/734))
- The endpoint that serves criteria profile data no longer contains full ui profiles but only the ids ([#709](https://github.com/medizininformatik-initiative/dataportal-backend/issues/709))
- Query schema validation was rewritten to provide better error messages if a submitted content does not fit the json schema [#724](https://github.com/medizininformatik-initiative/dataportal-backend/issues/724)
### Security
- Updated dependencies and GitHub actions

## [7.5.1] - 2025-10-16

- Based on ontology **[v3.9.1](https://github.com/medizininformatik-initiative/fhir-ontology-generator/releases/tag/v3.9.1)**

### Changed
- Update Ontology to v3.9.1
### Security
- Updated dependencies and GitHub actions

## [7.5.0] - 2025-09-24

- Based on ontology **[v3.9.0](https://github.com/medizininformatik-initiative/fhir-ontology-generator/releases/tag/v3.9.0)**

### Changed
- Update UI profile model to allow support of multiple value sets ([#599](https://github.com/medizininformatik-initiative/dataportal-backend/issues/599))
### Fixed
- Correctly display the git branch in the info actuator ([#645](https://github.com/medizininformatik-initiative/dataportal-backend/issues/645))
### Security
- Updated dependencies and GitHub actions

## [7.4.1] - 2025-09-04

- Based on ontology **[v3.8.3](https://github.com/medizininformatik-initiative/fhir-ontology-generator/releases/tag/v3.8.3)**

### Fixed
- Correctly display all filter options when searching for criteria (was restricted to 10) ([#628](https://github.com/medizininformatik-initiative/dataportal-backend/issues/628))
- Correctly find terminology entries when translations are empty strings ([#626](https://github.com/medizininformatik-initiative/dataportal-backend/issues/626))
### Security
- Updated dependencies and GitHub actions

## [7.4.0] - 2025-08-12

- Based on ontology **[v3.8.3](https://github.com/medizininformatik-initiative/fhir-ontology-generator/releases/tag/v3.8.3)**

### Added
- Improved test coverage ([#552](https://github.com/medizininformatik-initiative/dataportal-backend/issues/552))
### Changed
- Update ontology to 3.8.3 ([#605](https://github.com/medizininformatik-initiative/dataportal-backend/issues/605))
### Fixed
- Fixed flaky test in MockBrokerClientIT ([#607](https://github.com/medizininformatik-initiative/dataportal-backend/issues/607))
### Security
- Updated dependencies and GitHub actions

## [7.3.0] - 2025-07-07

- Based on ontology **[v3.8.1](https://github.com/medizininformatik-initiative/fhir-ontology-generator/releases/tag/v3.8.1)**

### Added
- OpenSSF Scorecard and badges
### Changed
- Move management endpoints under default api path (/api/v5) ([#547](https://github.com/medizininformatik-initiative/dataportal-backend/issues/547))
### Fixed
- Enable CORS for management endpoints ([#547](https://github.com/medizininformatik-initiative/dataportal-backend/issues/547))
### Security
- Pinned workflow dependencies to specific hashes ([#549](https://github.com/medizininformatik-initiative/dataportal-backend/issues/549))

## [7.2.0] - 2025-07-02

- Based on ontology **[v3.8.1](https://github.com/medizininformatik-initiative/fhir-ontology-generator/releases/tag/v3.8.1)**

### Added
- CQL execution via direct broker can now be done asynchronously ([#530](https://github.com/medizininformatik-initiative/dataportal-backend/issues/530))
- Timeouts for direct broker are configurable (for both flare and CQL) ([#531](https://github.com/medizininformatik-initiative/dataportal-backend/issues/531))
- Structured queries can be translated to CQL via POST ([#516](https://github.com/medizininformatik-initiative/dataportal-backend/issues/516))
- Formerly undocumented parameters have been added to the README ([#542](https://github.com/medizininformatik-initiative/dataportal-backend/issues/542))
### Changed
- Deprecated spring security methods have been replaced by current ones ([#526](https://github.com/medizininformatik-initiative/dataportal-backend/issues/526))
- Docker images have been updated (blaze 1.0.3, keycloak 26.2, flare 2.5.0)
- OpenAPI documentation path has been moved under the default api path ([#537](https://github.com/medizininformatik-initiative/dataportal-backend/issues/537))
- Docker base image has been updated to eclipse-temurin:24-jre ([#542](https://github.com/medizininformatik-initiative/dataportal-backend/issues/542))
- Direct broker configuration in application.yml has been moved to one place ([#544](https://github.com/medizininformatik-initiative/dataportal-backend/issues/544))
### Removed
- Unused tables and derived entities have been removed ([#359](https://github.com/medizininformatik-initiative/dataportal-backend/issues/359))
- Unused parameters have been removed from the README and the example compose file ([#542](https://github.com/medizininformatik-initiative/dataportal-backend/issues/542))
### Fixed
- Searching for terminology entries while a filter is selected no longer returns false positives ([#533](https://github.com/medizininformatik-initiative/dataportal-backend/issues/533))
- Retrieving the query list of a user by an admin account correctly contains the resultSize now (if set) ([#540](https://github.com/medizininformatik-initiative/dataportal-backend/issues/540))
### Security
- Updated dependencies (postgresql, opencsv)
- Updated spring boot to 3.5.3 ([#534](https://github.com/medizininformatik-initiative/dataportal-backend/issues/534))

## [7.1.2] - 2025-06-06

- Based on ontology **[v3.7.0](https://github.com/medizininformatik-initiative/fhir-ontology-generator/releases/tag/v3.7.0)**

### Fixed
- Dataqueries can be read by admin users ([#524](https://github.com/medizininformatik-initiative/dataportal-backend/issues/524))

## [7.1.1] - 2025-05-27

- Based on ontology **[v3.7.0](https://github.com/medizininformatik-initiative/fhir-ontology-generator/releases/tag/v3.7.0)**

### Fixed
- Durations with larger timeunits than days are now correctly parsed ([#519](https://github.com/medizininformatik-initiative/dataportal-backend/issues/519))

## [7.1.0] - 2025-05-26

- Based on ontology **[v3.7.0](https://github.com/medizininformatik-initiative/fhir-ontology-generator/releases/tag/v3.7.0)**

### Changed
- Update ontology to 3.7.0
### Security
- Spring boot was updated to 3.5.0

## [7.0.1] - 2025-05-20

### Fixed
- Parent/Child relations for terminology entries are correctly returned with translations ([#461](https://github.com/medizininformatik-initiative/dataportal-backend/issues/461))

## [7.0.0] - 2025-05-16

- Based on ontology **[v3.6.0](https://github.com/medizininformatik-initiative/fhir-ontology-generator/releases/tag/v3.6.0)**

### Added
- Dataqueries are introduced ([#374](https://github.com/medizininformatik-initiative/dataportal-backend/issues/374))
- CSV Export for dataqueries ([#487](https://github.com/medizininformatik-initiative/dataportal-backend/issues/487))
- Dataqueries can be created for users (from an admin role) ([#419](https://github.com/medizininformatik-initiative/dataportal-backend/issues/419))
### Changed
- REST API changed to v5 ([#374](https://github.com/medizininformatik-initiative/dataportal-backend/issues/374))
- Configurable variables dealing with time intervals have their format changed to ISO 8601 Durations ([#477](https://github.com/medizininformatik-initiative/dataportal-backend/issues/477))
- Log output when blacklisting a user has been improved ([#499](https://github.com/medizininformatik-initiative/dataportal-backend/issues/499))
- DSE Profile representation has changed ([#496](https://github.com/medizininformatik-initiative/dataportal-backend/issues/496))
### Removed
- Query Templates have been removed ([#374](https://github.com/medizininformatik-initiative/dataportal-backend/issues/374))
- Availability update script has been moved to [FHIR Ontology Generator](https://github.com/medizininformatik-initiative/fhir-ontology-generator/) ([#480](https://github.com/medizininformatik-initiative/dataportal-backend/issues/480))
### Security
- Spring boot was updated to 3.4.5
- Dependency updates ([#507](https://github.com/medizininformatik-initiative/dataportal-backend/issues/507))

## [7.0.0-alpha.1] - 2025-04-07

### Changed
- REST API changed to v5
### Removed
- Query Templates have been removed

## [6.2.2] - 2025-03-18

- Based on ontology **[v3.2.2](https://github.com/medizininformatik-initiative/fhir-ontology-generator/releases/tag/v3.2.2)**

### Changed
- Update ontology to 3.2.2

## [6.2.1] - 2025-03-17

- Based on ontology **[v3.2.1](https://github.com/medizininformatik-initiative/fhir-ontology-generator/releases/tag/v3.2.1)**

### Changed
- Update ontology to 3.2.1

## [6.2.0] - 2025-03-14

- Based on ontology **[v3.2.0](https://github.com/medizininformatik-initiative/fhir-ontology-generator/releases/tag/v3.2.0)**

### Added
- Info actuator endpoint to provide information about git parameters and ontology version ([#463](https://github.com/medizininformatik-initiative/dataportal-backend/issues/463))
- Endpoint to query the feasibility query quota usage ([#460](https://github.com/medizininformatik-initiative/dataportal-backend/issues/460))
- Feasibility FHIR Resource Version added to Profile URLs 
### Changed
- Update sq2cql to 1.0.0
- Update ontology to 3.2.0
### Fixed
- Swagger and health/info endpoints can be queried when authenticated ([#473](https://github.com/medizininformatik-initiative/dataportal-backend/issues/473))

## [6.1.1] - 2025-03-03

### Changed
- Management endpoint configuration updated ([#462](https://github.com/medizininformatik-initiative/dataportal-backend/issues/462))
### Fixed
- The search in translations/originals in the ontology was not working as intended ([#455](https://github.com/medizininformatik-initiative/dataportal-backend/issues/455))
- The amount of allowed calls to detailed obfuscated records was not read correctly ([#457](https://github.com/medizininformatik-initiative/dataportal-backend/issues/457))
### Security
- Update Spring Boot to 3.4.3

## [6.1.0] - 2025-02-14

- Based on ontology **[v3.1.0](https://github.com/medizininformatik-initiative/fhir-ontology-generator/releases/tag/v3.1.0)**

### Added
- Added translations to ui profiles ([#430](https://github.com/medizininformatik-initiative/dataportal-backend/issues/430))
### Changed
- Update sq2cql to 0.8.0
- Search in original display text if no translations are found ([#446](https://github.com/medizininformatik-initiative/dataportal-backend/issues/446))
- Change display and translation structure for criteria and concepts ([#382](https://github.com/medizininformatik-initiative/dataportal-backend/issues/382))
- Change codeable-concept/entry endpoint to take a list of ids as query param instead of just one id as path param ([#433](https://github.com/medizininformatik-initiative/dataportal-backend/issues/433))
- Replace @MockBean annotation with @MockitoBean due to deprecation ([#434](https://github.com/medizininformatik-initiative/dataportal-backend/issues/434))
- Replace deprecated calls to bucket4j library ([#424](https://github.com/medizininformatik-initiative/dataportal-backend/issues/424))
- Read ids from downloaded files in integration tests to remove the need to change those each time a new ontology is used ([#428](https://github.com/medizininformatik-initiative/dataportal-backend/issues/428))
### Removed
- @Data Annotation removed from JPA classes ([#332](https://github.com/medizininformatik-initiative/dataportal-backend/issues/332))
### Fixed
- Missing Parameters for DSE profile data added in open api doc ([#431](https://github.com/medizininformatik-initiative/dataportal-backend/issues/431))
### Security
- Update Spring Boot to 3.4.2 ([#437](https://github.com/medizininformatik-initiative/dataportal-backend/issues/437))

## [6.1.0-alpha.2] - 2025-02-12

### Changed
- Update sq2cql to 0.8.0-alpha.1 ([#450](https://github.com/medizininformatik-initiative/dataportal-backend/issues/450))
- Search in original display text if no translations are found ([#446](https://github.com/medizininformatik-initiative/dataportal-backend/issues/446))

## [6.0.5] - 2025-02-03

### Changed
- Update sq2cql to 0.7.0 ([#444](https://github.com/medizininformatik-initiative/dataportal-backend/issues/444))
### Security
- Update Spring Boot to 3.4.2

## [6.1.0-alpha.1] - 2025-01-24

### Added
- Added translations to ui profiles ([#430](https://github.com/medizininformatik-initiative/dataportal-backend/issues/430))
### Changed
- Change display and translation structure for criteria and concepts ([#382](https://github.com/medizininformatik-initiative/dataportal-backend/issues/382))
- Change codeable-concept/entry endpoint to take a list of ids as query param instead of just one id as path param ([#433](https://github.com/medizininformatik-initiative/dataportal-backend/issues/433))
- Replace @MockBean annotation with @MockitoBean due to deprecation ([#434](https://github.com/medizininformatik-initiative/dataportal-backend/issues/434))
- Replace deprecated calls to bucket4j library ([#424](https://github.com/medizininformatik-initiative/dataportal-backend/issues/424))
- Read ids from downloaded files in integration tests to remove the need to change those each time a new ontology is used ([#428](https://github.com/medizininformatik-initiative/dataportal-backend/issues/428))
### Removed
- @Data Annotation removed from JPA classes ([#332](https://github.com/medizininformatik-initiative/dataportal-backend/issues/332))
### Fixed
- Missing Parameters for DSE profile data added in open api doc ([#431](https://github.com/medizininformatik-initiative/dataportal-backend/issues/431))
### Security
- Update Spring Boot to 3.4.2 ([#437](https://github.com/medizininformatik-initiative/dataportal-backend/issues/437))

## [6.0.4] - 2025-01-10

### Fixed
- Time Restriction validation was broken when only one of beforeDate or afterDate was set ([#421](https://github.com/medizininformatik-initiative/dataportal-backend/issues/421))
### Security
- Update Spring Boot to 3.4.1

## [6.0.3] - 2024-12-10

### Changed
- Download path for ontology files has changed

## [6.0.2] - 2024-12-10

### Fixed
- Update Sq2CQL to v0.6.1
- Update Ontology to v3.0.1

## [6.0.1] - 2024-11-29

### Fixed
- Update Sq2CQL ([#401](https://github.com/medizininformatik-initiative/dataportal-backend/issues/401))
### Security
- Update Spring Boot ([#403](https://github.com/medizininformatik-initiative/dataportal-backend/issues/403))
- Update Fhir R4 Structures ([#405](https://github.com/medizininformatik-initiative/dataportal-backend/issues/405))

## [6.0.0] - 2024-10-21

- Based on ontology **[v3.0.0](https://github.com/medizininformatik-initiative/fhir-ontology-generator/releases/tag/v3.0.0)**

### Added
- Added recommended and required to dse fields
- Added DSE profile tree field as list of values
- Added referencedProfiles to dse profile fields
- Update cql aliases
- JVM options configurable in Dockerimage
- Terminology search implemented via external elastic search service ([#307](https://github.com/medizininformatik-initiative/dataportal-backend/issues/307))
- Endpoints to query profile data for data selection and extraction (ES) ([#321](https://github.com/medizininformatik-initiative/dataportal-backend/issues/321))
- Endpoints to search for codeable concepts (ES) ([#324](https://github.com/medizininformatik-initiative/dataportal-backend/issues/324))
### Changed
- moved rest api from v3 to v4
- codex and or feasibility references are replaced by dataportal (not in package names though)
- openapi documentation updated to 3.1.0
- Updated sq2cql and Ontology dependencies
- Allow empty search
- Change structure of dse profile and details for new translation structure
- **breaking** Consistent naming for endpoints and filenames (kebab-case for rest endpoints, camelCase for json parameters, snake_case for elastic search variables)

### Removed
- Unused endpoints from the /terminology path

### Security
- Updated Spring Boot to 3.3.2 ([#317](https://github.com/medizininformatik-initiative/dataportal-backend/issues/317))

## [6.0.0-alpha.3] - 2024-10-21

### Changed
- Allow empty search
- Change structure of dse profile and details for new translation structure

## [6.0.0-alpha.2] - 2024-09-04

### Added
- JVM options configurable in Dockerimage
- 
### Changed
- moved rest api from v3 to v4
- codex and or feasibility references are replaced by dataportal (not in package names though)
- openapi documentation updated to 3.1.0
### Removed
- Unused endpoints from the /terminology path

## [6.0.0-alpha.1] - 2024-09-02

### Added
- Terminology search implemented via external elastic search service ([#307](https://github.com/medizininformatik-initiative/dataportal-backend/issues/307))
- Endpoints to query profile data for data selection and extraction (ES) ([#321](https://github.com/medizininformatik-initiative/dataportal-backend/issues/321))
- Endpoints to search for codeable concepts (ES) ([#324](https://github.com/medizininformatik-initiative/dataportal-backend/issues/324))
### Changed
- **breaking** Consistent naming for endpoints and filenames (kebab-case for rest endpoints, camelCase for json parameters, snake_case for elastic search variables)
### Security
- Updated Spring Boot to 3.3.2 ([#317](https://github.com/medizininformatik-initiative/dataportal-backend/issues/317))

## [5.0.1] - 2024-06-29

### Fixed
- Injection of environment variable value for OAuth client id was broken ([#308](https://github.com/medizininformatik-initiative/dataportal-backend/issues/308))

## [5.0.0] - 2024-06-26

### Added
- Added an endpoint to validate uploaded structured queries. ([#258](https://github.com/medizininformatik-initiative/dataportal-backend/issues/258))
- OpenID Connect authentication for direct broker ([#302](https://github.com/medizininformatik-initiative/dataportal-backend/issues/302))
### Changed
- Validation for structured queries has been reworked. ([#260](https://github.com/medizininformatik-initiative/dataportal-backend/issues/260)), ([#266](https://github.com/medizininformatik-initiative/dataportal-backend/issues/266))
- Updated sq2cql to v0.3.0
- Updated ontology to version v2.2.0 ([#299](https://github.com/medizininformatik-initiative/dataportal-backend/issues/299))
### Fixed
- Increased timeout in MockBrokerClientIT to avoid occasional test failures ([#276](https://github.com/medizininformatik-initiative/dataportal-backend/issues/276))
- OPS codes with lowercase letters are now correctly found ([#292](https://github.com/medizininformatik-initiative/dataportal-backend/issues/292))
### Security
- updated spring boot to 3.3.1
- updated undertow to 2.3.14.Final to fix [CVE-2024-6162](https://avd.aquasec.com/nvd/2024/cve-2024-6162/) ([#304](https://github.com/medizininformatik-initiative/dataportal-backend/issues/304))
- Updated netty-codec-http to 4.1.108.Final to fix [CVE-2024-29025](https://avd.aquasec.com/nvd/cve-2024-29025) ([#279](https://github.com/medizininformatik-initiative/dataportal-backend/issues/279))
- Updated nimbus-jose-jwt to 9.37.3 to fix [CVE-2023-52428](https://avd.aquasec.com/nvd/cve-2023-52428) ([#275](https://github.com/medizininformatik-initiative/dataportal-backend/issues/275))
- Updated xnio to 3.8.14.Final to fix [CVE-2023-5685](https://avd.aquasec.com/nvd/cve-2023-5685) ([#274](https://github.com/medizininformatik-initiative/dataportal-backend/issues/274))

## [5.0.0-rc.1] - 2024-06-17

### Changed
- Updated ontology to version v2.2.0 ([#299](https://github.com/medizininformatik-initiative/dataportal-backend/issues/299))

## [5.0.0-alpha.3] - 2024-06-14

### Changed
- Updated sq2cql to version v0.3.0-rc.1 ([#294](https://github.com/medizininformatik-initiative/dataportal-backend/issues/294))
- Updated ontology to version v2.2.0-RC2 ([#293](https://github.com/medizininformatik-initiative/dataportal-backend/issues/293))
### Fixed
- OPS codes with lowercase letters are now correctly found ([#292](https://github.com/medizininformatik-initiative/dataportal-backend/issues/292))
### Security
- Updated spring boot to 3.3.0 ([#290](https://github.com/medizininformatik-initiative/dataportal-backend/issues/290))

## [5.0.0-alpha.2] - 2024-04-30

### Fixed
- Increased timeout in MockBrockerClientIT to avoid occasional test failures ([#276](https://github.com/medizininformatik-initiative/dataportal-backend/issues/276))
### Security
- Updated spring boot to 3.2.5 ([#282](https://github.com/medizininformatik-initiative/dataportal-backend/issues/282))
- Updated netty-codec-http to 4.1.108.Final to fix [CVE-2024-29025](https://avd.aquasec.com/nvd/cve-2024-29025) ([#279](https://github.com/medizininformatik-initiative/dataportal-backend/issues/279)
- Updated nimbus-jose-jwt to 9.37.3 to fix [CVE-2023-52428](https://avd.aquasec.com/nvd/cve-2023-52428) ([#275](https://github.com/medizininformatik-initiative/dataportal-backend/issues/275))
- Updated xnio to 3.8.14.Final to fix [CVE-2023-5685](https://avd.aquasec.com/nvd/cve-2023-5685) ([#274](https://github.com/medizininformatik-initiative/dataportal-backend/issues/274))

## [5.0.0-alpha.1] - 2024-04-03

### Added
- Added an endpoint to validate uploaded structured queries. ([#258](https://github.com/medizininformatik-initiative/dataportal-backend/issues/258))
### Changed
- Validation for structured queries has been reworked. ([#260](https://github.com/medizininformatik-initiative/dataportal-backend/issues/260)), ([#266](https://github.com/medizininformatik-initiative/dataportal-backend/issues/266))
### Security
- Updated spring boot to 3.2.4 ([#262](https://github.com/medizininformatik-initiative/dataportal-backend/issues/262))

The full changelog can be found [here](https://github.com/medizininformatik-initiative/dataportal-backend/milestone/9?closed=1).

## [4.3.0] - 2024-02-02

### Added
- Basic auth for direct broker ([#210](https://github.com/medizininformatik-initiative/dataportal-backend/issues/210))
### Changed
- Updated sq2cql to 0.2.14 ([#253](https://github.com/medizininformatik-initiative/dataportal-backend/issues/253))
- Reduce verbosity of DSF Webservice client ([#247](https://github.com/medizininformatik-initiative/dataportal-backend/issues/247))
### Security
- Updated spring boot to 3.2.2 ([#251](https://github.com/medizininformatik-initiative/dataportal-backend/issues/251))

## [4.2.0] - 2023-11-17

### Changed
- Updated ontology and sq2cql to new version

## [4.1.0] - 2023-11-09

### Added
- Make order of main categories configurable ([#219](https://github.com/medizininformatik-initiative/dataportal-backend/issues/219))
- Save and load query results for saved queries ([#199](https://github.com/medizininformatik-initiative/dataportal-backend/issues/199))
- Support CRUD for query templates ([#214](https://github.com/medizininformatik-initiative/dataportal-backend/issues/214))

### Fixed
- Fix code scanning alert - parser confusion leads to OOM ([#221](https://github.com/medizininformatik-initiative/dataportal-backend/issues/221))
- Fix code scanning alert - Missing Override annotation ([#223](https://github.com/medizininformatik-initiative/dataportal-backend/issues/223))
- Fix code scanning alert - Unread local variable ([#222](https://github.com/medizininformatik-initiative/dataportal-backend/issues/222))

### Security
- Update Spring Boot to 3.1.5 ([#227](https://github.com/medizininformatik-initiative/dataportal-backend/issues/227))

## [4.0.0] - 2023-10-06

### Added
- Support for self-signed certificates ([#203](https://github.com/medizininformatik-initiative/dataportal-backend/issues/203))
- New DB tables  ([#180](https://github.com/medizininformatik-initiative/dataportal-backend/issues/180))
- v3 api endpoint ([#190](https://github.com/medizininformatik-initiative/dataportal-backend/issues/190))
### Changed
- Ontology is loaded from GitHub ([#201](https://github.com/medizininformatik-initiative/dataportal-backend/issues/201))
- Error handling changed to provide more information to the GUI  ([#116](https://github.com/medizininformatik-initiative/dataportal-backend/issues/116))
### Removed
- v1 and v2 api endpoints ([#190](https://github.com/medizininformatik-initiative/dataportal-backend/issues/190))
### Security
- Update Spring Boot to 3.1.3 ([#188](https://github.com/medizininformatik-initiative/dataportal-backend/issues/188))

The full changelog can be found [here](https://github.com/medizininformatik-initiative/dataportal-backend/milestone/6?closed=1).

## [3.1.3] - 2023-07-13

### Changed
- Obfuscated site ids are no longer consistent over multiple requests of the same result
### Security
- Update Spring Boot to 3.1.1

## [3.1.2] - 2023-07-11

### Security
- Fix potential input resource leak ([#155](https://github.com/medizininformatik-initiative/dataportal-backend/issues/155))

## [3.1.1] - 2023-05-24

### Fixed
- Database configuration changed

## [3.1.0] - 2023-05-24

### Changed
- Result Log files are encrypted ([#124](https://github.com/medizininformatik-initiative/dataportal-backend/pull/124))
### Security
- Update Spring Boot to 3.1.0 ([#130](https://github.com/medizininformatik-initiative/dataportal-backend/pull/130))
- Update Spring Security to 6.1.0 ([#129](https://github.com/medizininformatik-initiative/dataportal-backend/pull/129))
- Update JSON lib to 20230227 ([#131](https://github.com/medizininformatik-initiative/dataportal-backend/pull/131))

## [3.0.0] - 2023-03-29

### Added
- Structured queries can be validated against the JSON schema via a REST endpoint ([#91](https://github.com/medizininformatik-initiative/dataportal-backend/pull/91))
- Limits on how often query results can be retrieved are imposed on users ([#77](https://github.com/medizininformatik-initiative/dataportal-backend/pull/77)), ([#69](https://github.com/medizininformatik-initiative/dataportal-backend/pull/69)), ([#94](https://github.com/medizininformatik-initiative/dataportal-backend/pull/94))
- Direct broker CQL compatibility ([#48](https://github.com/medizininformatik-initiative/dataportal-backend/pull/48))
### Changed
- Limit amount of queries a user can post before being locked out ([#101](https://github.com/medizininformatik-initiative/dataportal-backend/pull/101))
- Query results are no longer persisted but only kept in memory for a configurable time ([#62](https://github.com/medizininformatik-initiative/dataportal-backend/pull/62)), ([#80](https://github.com/medizininformatik-initiative/dataportal-backend/pull/80)), ([#87](https://github.com/medizininformatik-initiative/dataportal-backend/pull/87))
- Return restricted results if certain thresholds are not surpassed ([#63](https://github.com/medizininformatik-initiative/dataportal-backend/pull/63)), ([#64](https://github.com/medizininformatik-initiative/dataportal-backend/pull/64))
### Removed
* Remove obsolete REST endpoints under /api/v1/ ([#109](https://github.com/medizininformatik-initiative/dataportal-backend/pull/109))
### Fixed
- Fix codesystem alias for consent ([#85](https://github.com/medizininformatik-initiative/dataportal-backend/pull/85)).
### Security
* Update Spring Boot to v3.0.5 ([#104](https://github.com/medizininformatik-initiative/dataportal-backend/pull/104))
* Update HAPI to 6.4.2 ([#73](https://github.com/medizininformatik-initiative/dataportal-backend/pull/73))

The full changelog can be found [here](https://github.com/medizininformatik-initiative/dataportal-backend/milestone/4?closed=1).
