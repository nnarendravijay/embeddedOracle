Problem Statement: For DAO Layer unit testing, there isn't an embedded simulation of Oracle. There is H2, but there are several differences between the H2 simulation and Oracle.

Solution: This project solves the problem by providing the Oracle instance as a Docker Image and spinning up a container with Oracle in it. 

    <dependency>
        <groupId>com.nnarendravijay</groupId>
        <artifactId>embeddedOracle</artifactId>
        <version>0.0.1</version>
    </dependency>
 
More Details: It nealy takes about a minute of two for starting up the default VM and caching the Docker Image you first run this on a machine (but, completely depends on the network bandwidth). After the first run, the Docker image is cached and it takes a maximum of 15 seconds for the container and Oracle to startup and available for accepting connections.

Prerequisites: Docker installed on your machine.

Database Versioning/Migrations: You can use your own migrations and could use Flyway or Liquibase, whichever you prefer for setting up your data. 

