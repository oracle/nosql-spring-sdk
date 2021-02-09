# Oracle NoSQL Database SDK for Spring Data 1.1.0

## About

Oracle NoSQL SDK for Spring Data provides a Spring Data
implementation module to connect to an 
[Oracle NoSQL Database](https://www.oracle.com/database/technologies/related/nosql.html) 
cluster or to
[Oracle NoSQL Cloud Service](https://www.oracle.com/database/nosql-cloud.html).


## Usage

* Download the latest release from 
[Oracle NoSQL Database downloads](https://www.oracle.com/database/technologies/nosql-database-server-downloads.html)
page.

* Manually install the downloaded project into your local maven repository 
(-sources and -javadoc files are optional):

    ```
    mvn install:install-file \
    -DpomFile=spring-data-oracle-nosql-1.1.0.pom \
    -Dfile=spring-data-oracle-nosql-1.1.0.jar \
    -Dsources=spring-data-oracle-nosql-1.1.0-sources.jar \
    -Djavadoc=spring-data-oracle-nosql-1.1.0-javadoc.jar
    ```
  
* To use the SDK in your project add maven dependency to your project's pom.xml:

    ```xml
    <dependency>
        <groupId>com.oracle.nosql.sdk</groupId>
        <artifactId>spring-data-oracle-nosql</artifactId>
        <version>x.y.z</version>
    </dependency>
    ```
    
* The example below also requires an additional dependency:
    
    ```xml
    <dependency>
      <groupId>org.springframework.boot</groupId>
      <artifactId>spring-boot-starter</artifactId>
      <version>2.3.4.RELEASE</version>
    </dependency>
    ``` 

* Define an AppConfig class that provides a nosqlDBConfig bean that returns an
Oracle NoSQL DB configuration:

    ```java
    package org.example.app;

    import com.oracle.nosql.spring.data.config.AbstractNosqlConfiguration;
    import com.oracle.nosql.spring.data.config.NosqlDbConfig;
    import com.oracle.nosql.spring.data.repository.config.EnableNosqlRepositories;
    
    import org.springframework.context.annotation.Bean;
    import org.springframework.context.annotation.Configuration;
    
    import oracle.nosql.driver.kv.StoreAccessTokenProvider;

    @Configuration
    @EnableNosqlRepositories
    public class AppConfig extends AbstractNosqlConfiguration {
       
        @Bean
        public NosqlDbConfig nosqlDbConfig() {
            return new NosqlDbConfig(
                "localhost:8080",                   // endpoint URL
                new StoreAccessTokenProvider());    // AuthorizationProvider
        }
    }
    ```

Note: Depending on individual scenario use the appropriate AuthorizationProvider:
 - For cloud configuration use the following example or see 
    [documentation](https://docs.oracle.com/en/cloud/paas/nosql-cloud/csnsd/connecting-using-java.html):

    ```java
    new oracle.nosql.driver.iam.SignatureProvider(
                        tenantId,             // OCID
                        userId,               // OCID
                        fingerprint,          // String
                        File privateKeyFile,
                        char[] passphrase)
    ```

 - For cloud simulator use: 

    ```java
    com.oracle.nosql.spring.data.NosqlDbFactory.CloudSimProvider.getProvider()
    ```

 - For on-prem configuration use one of the following examples or see 
    [documentation](https://docs.oracle.com/en/database/other-databases/nosql-database/20.2/admin/creating-nosql-handle.html):
   * For unsecure example: 

     ```java
     new oracle.nosql.driver.kv.StoreAccessTokenProvider()
     ```

   * For secure example use: 

     ```java
     new oracle.nosql.driver.kv.StoreAccessTokenProvider("username", "password".toCharArray())
     ```

Note: For convenience one can use the following 
    com.oracle.nosql.spring.data.NosqlDbConfig methods:
    
  - for cloud: NosqlDbConfig.createCloudConfig("endpoint", configFile);
  - for cloud simulator: NosqlDbConfig.createCloudSimConfig("endpoint");
  - for on-prem unsecure store: NosqlDbConfig.createProxyConfig("endpoint");
  - for on-prem secure store: NosqlDbConfig.createProxyConfig("endpoint", user, password);

* Define the entity class:

  ```java
  package org.example.app;

  import com.oracle.nosql.spring.data.core.mapping.NosqlId;
  
  public class Customer {
      @NosqlId(generated = true)
      long customerId;
      String firstName;
      String lastName;
  
      @Override
      public String toString() {
          return "Customer{" +
              "customerId=" + customerId +
              ", firstName='" + firstName + '\'' +
              ", lastName='" + lastName + '\'' +
              '}';
      }
  }
  ```

* Declare a repository that extends NosqlRepository:

    ```java
    package org.example.app;

    import com.oracle.nosql.spring.data.repository.NosqlRepository;
    
    public interface CustomerRepository
        extends NosqlRepository<Customer, Long>
    {
        Iterable<Customer> findByLastName(String lastname);
    }
    ```
    
* Write the main application class. This requires adding dependencies to org
.springframework.boot:spring-boot and org.springframework
.boot:spring-boot-autoconfigure. 

  ```java
  package org.example.app;

  import org.springframework.beans.factory.annotation.Autowired;
  import org.springframework.boot.CommandLineRunner;
  import org.springframework.boot.SpringApplication;
  import org.springframework.boot.autoconfigure.SpringBootApplication;
  import org.springframework.context.ConfigurableApplicationContext;
  
  @SpringBootApplication
  public class App implements CommandLineRunner
  {
      @Autowired
      private CustomerRepository repo;
  
      public static void main( String[] args )
      {
          ConfigurableApplicationContext
              ctx = SpringApplication.run(App.class, args);
          SpringApplication.exit(ctx, () -> 0);
          ctx.close();
          System.exit(0);
      }
  
      @Override
      public void run(String... args) throws Exception {
  
          repo.deleteAll();
  
          Customer s1 = new Customer();
          s1.firstName = "John";
          s1.lastName = "Doe";
  
          repo.save(s1);
          System.out.println("\nsaved: " + s1); // customerId contains generated value
          
          Customer s2 = new Customer();
          s2.firstName = "John";
          s2.lastName = "Smith";
  
          repo.save(s2);
          System.out.println("\nsaved: " + s2); // customerId contains generated value
  
          System.out.println("\nfindAll:");
          Iterable<Customer> customers = repo.findAll();
  
          for (Customer s : customers) {
              System.out.println("  Customer: " + s);
          }
  
          System.out.println("\nfindByLastName: Smith");
          customers = repo.findByLastName("Smith");
  
          for (Customer s : customers) {
              System.out.println("  Customer: " + s);
          }
      }
  }
  ```

## Build and run the example code

Example code requires an Oracle NoSQL DB instance and a local http proxy running
 on port 8080.

Start a kvlite instance with helperHosts "localhost:5000":

```
java -jar /path_to/kvstore.jar kvlite -root kvroot -host localhost -port 5000 -store kvstore -secure-config disable &
```

Start http proxy with endpoint URL "localhost:8080":

```
java -jar /path_to/httpproxy.jar -storeName kvstore -httpPort 8080 -helperHosts localhost:5000 -verbose true &
```

Execute the example code:

```
mvn exec:java -Dexec.mainClass="org.example.app.App"
```

To log the internally generated queries, one has to enable the debug level by
 adding following logging flag:

```
mvn exec:java -Dexec.mainClass="org.example.app.App" -Dlogging.level.com.oracle.nosql.spring.data=DEBUG
```

## Run unit tests

Running tests require a running store and proxy. The test.serverType and 
test.endpoint system properties must be specified.
```
mvn test -Dtest.serverType=onprem -Dtest.endpoint=http://127.0.0.1:8080
```
By default, if no option is specified, onprem serverType and http://127.0.0
.1:8080 endpoint is assumed. 

Tests can be also be run on:
 - onprem:  
    ```
    mvn -B -Ptest-onprem test -DargLine="-Dtest.endpoint=$ONPREM_ENDPOINT"
    ```
 - onprem-secure: 
    Must specify the user, password, trustfile and trust file access password. 
    ```
    mvn -B -Ptest-onprem-secure test -DargLine="-Dtest.endpoint=$ONPREM_SEC_ENDPOINT -Dtest.user=$DRIVER_USER -Dtest.trust=$DRIVER_TRUST_FILE -Dtest.password=$DRIVER_PASS -Dtest.trust.password=$DRIVER_TRUST_PASS"
    ```
 - cloudsim:
    ```
    mvn -B -Ptest-cloudsim test -DargLine="-Dtest.endpoint=$CLOUDSIM_ENDPOINT"
    ```
 
 
Enjoy.