# Change Log
All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](http://keepachangelog.com/).

## [Unreleased]

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