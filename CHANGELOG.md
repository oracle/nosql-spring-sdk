# Change Log
All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](http://keepachangelog.com/).

## [Unreleased]
### Changed
- Update copyrights to 2024
- Upgrade NoSQL Java SDK dependency to version 5.4.12.

## [2.0.0] - 2023-08-29
### Changed
- Upgrade dependencies:
  - Spring framework: 5.3.27 to 6.0.11
  - Spring Data: 2.7.0 to 3.1.2
  - Apache commons-lang3: 3.12.0 to 3.13.0
  - Reactor core: 3.4.19 to 3.5.8
  - Spring boot starter: 2.7.0 to 3.1.2
  - Slf4j: 1.7.36 to 2.0.7
  - maven-compiler-plugin: 2.3.2 to 3.11.0
  - maven-javadoc-plugin: 3.4.0 to 3.5.0
  - maven-checkstyle-plugin: 2.17 to 3.0.0
  - maven-help-plugin: 3.1.0 to 3.4.0
  - maven-jar-plugin: 3.2.0 to 3.3.0
  - exec-maven-plugin: 3.0.0 to 3.1.0
  - maven-surefire-plugin: 3.0.0-M5 to 3.1.2
- Requirements moved to JDK 17+.

## [1.7.1] - 2023-07-26
### Added
- Added the checks to verify entity definition matches with corresponding 
  table in the database during table creation.

### Changed
- Upgrade NoSQL Java SDK dependency to version 5.4.11.

## [1.6.0] - 2023-05-05
### Added
- Added support for composite primary keys.

### Changed
- Upgrade NoSQL Java SDK dependency to version 5.4.10.

## [1.5.0] - 2023-03-06
### Added
- Added support for java.util.Map and similar types as mapping types.
- Added support for evaluating SpEl expressions on NosqlTable.tableName annotation.
- Added support for table level default TTL.

### Changed
- Updated documentation links.

## [1.4.1] - 2022-07-27
### Added
- Add a way to return the library version using NosqlDbFactory.getLibraryVersion().
- Add NoSQL-SpringSDK/version as extension to http user agent.
- Added LICENSE.txt and THIRD_PARTY_LICENSES.txt to runtime jar file and LICENSE.txt to sources and javadoc jars.

## [1.4.0] - 2022-06-27
### Added
- On-premise only, added support for setting durability option on writes.
- Add durability setter/getter on NosqlRepository and ReactiveNosqlRepository interfaces and implementing classes.
- Cloud only, added support for on-demand tables by setting NosqlTable.capacityMode to NosqlCapacityMode.ON_DEMAND.
- Enable TableLimits defaults when no NosqlTable annotation is explicitly used.
- Add getters for default storageGB, capacityMode, readUnits and writeUnits in NosqlDbConfig and NosqlDbFactory with default values of PROVISIONED, 25GB, 50 read units and 50 write units.
- Avoid entity checking atomic field classes, this enables running under jdk 17.
- Consider entity if NosqlTable annotation is present and don't account for Persistent annotation.

### Changed
- Better error message when method parameters don't match the expected param for the query part.
- Add file with configuration properties for test AppConfig (config properties can be changed without rebuilding code).
- Update javadoc related to default table limits.
- Update library dependency versions.

## [1.3.0] - 2022-04-19
### Changed
- Updated library dependency versions and updated THIRD_PARTY_LICENSES.txt 
and THIRD_PARTY_LICENSES_DEV.txt.
- Updated copyrights to 2022

### Added
- Support for deleteAllById() in Simple[Reactive]NosqlRepository classes.

## [1.2.0] - 2021-04-06
### Added
- Exception message for instantiate failures
- New profile in pom.xml for cloud testing
- Support for fields of type java enumeration
- Support for IgnoreCase and AllIgnoreCase keywords in derived query method 
  names.
- Projections
  - Implemented support for projections to POJOs with limited set of fields 
    and interfaces.

### Changed
- Updated copyrights to 2021

### Fixed
- Generation of projecting queries to avoid selecting the same property twice
- Optimize types outside of loop in find queries
- Avoid creating extra stream when executing queries.

## [1.1.0] - 2020-12-20
The initial release of Oracle NoSQL SDK for Spring Data.
