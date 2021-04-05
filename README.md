# Documentation for the Oracle NoSQL Database SDK for Spring Data

This is a README for the gh-pages branch of the
[Oracle NoSQL Database SDK for Spring Data repository](https://github.com/oracle/nosql-spring-sdk). This branch is used to publish documentation on GitHub via GitHub pages

## Building and Publishing Documentation

Generated documentation is published on
[GitHub Pages](https://oracle.github.io/nosql-spring-sdk/) using the GitHub Pages
facility. Publication is automatic based on changes pushed to this (gh-pages)
branch of the
[Oracle NoSQL Database SDK for Spring Data](https://github.com/oracle/nosql-spring-sdk)
repository.

In these instructions <nosql-spring-sdk> is the path to a current clone from
which to publish the documentation and <nosql-spring-sdk-doc> is the path to
a fresh clone of the gh-pages branch (see instructions below).

Clone the gh-pages branch of the SDK repository

``` bash
$ git clone --single-branch --branch gh-pages https://github.com/oracle/nosql-spring-sdk.git nosql-spring-sdk-doc
```

Generate documentation in the master (or other designated) branch of the
repository

``` bash
$ cd <nosql-spring-sdk>
$ mvn clean javadoc:javadoc
```

The doc ends up in driver/target/apidocs, copy it to the gh-pages branch

``` bash
$ cp -r <nosql-spring-sdk>/target/site/apidocs/* <nosql-spring-sdk-doc>
```

Commit and push after double-checking the diff in the nosql-spring-sdk-doc
repository

``` bash
 $ cd <nosql-spring-sdk-doc>
 $ git add .
 $ git commit
 $ git push
```

The new documentation will automatically be published to
[GitHub Pages](https://oracle.github.io/nosql-spring-sdk).
