# Change Log
All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](http://keepachangelog.com/).

## [1.4.0]
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

## [1.3.0]
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
