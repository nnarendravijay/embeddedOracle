package com.nnarendravijay;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.CreateContainerResponse;
import com.github.dockerjava.api.model.ExposedPort;
import com.github.dockerjava.api.model.Ports;
import com.github.dockerjava.core.DockerClientBuilder;
import com.github.dockerjava.core.DockerClientConfig;
import com.github.dockerjava.core.command.PullImageResultCallback;
import oracle.jdbc.pool.OracleDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class EmbeddedOracle {

  private static Logger logger = LoggerFactory.getLogger(EmbeddedOracle.class);

  private DockerClient dockerClient;
  private final Map<Integer, Integer> portBindings;
  private CreateContainerResponse container;
  private static final String USERNAME = "system";
  private static final String PASSWORD = "oracle";
  private String jdbcUrl;
  private static final int MACHINEPORT = 2376;


  public EmbeddedOracle(String dockerImage, Map<Integer, Integer> portBindings)
      throws InterruptedException, SQLException, ClassNotFoundException, IOException {

    this.portBindings = portBindings;
    startContainer(dockerImage);
    testConnectivity(dockerImage);

  }

  private void startContainer(String oracleImage) throws InterruptedException,
      IOException {

    Optional<String> machineIp = (Optional<String>) DockerMachineManagement.getDefaultMachine();
    assert machineIp.isPresent();
    assert portBindings.containsKey(1521);
    int oraclePort = portBindings.get(1521);
    jdbcUrl = "jdbc:oracle:thin:@" + machineIp.get() + ":" + oraclePort + "/xe";

    String certPath = (String) DockerMachineManagement.getCertPath();

    String machineUri = "https://" + machineIp.get() + ":" + MACHINEPORT;
    DockerClientConfig config = DockerClientConfig.createDefaultConfigBuilder()
        .withVersion("1.19")
        .withUri(machineUri)
        .withDockerCertPath(certPath)
        .build();
    dockerClient = DockerClientBuilder.getInstance(config).build();


    logger.info("Pulling the image: {}; if it isn't already cached", oracleImage);

    PullImageResultCallback pullImageResultCallback = new PullImageResultCallback();
    dockerClient.pullImageCmd(oracleImage).exec(pullImageResultCallback).awaitSuccess();

    List<ExposedPort> exposedPorts = new ArrayList<>();
    Ports ports = new Ports();
    portBindings.entrySet().stream().forEach(portMapping -> {
      ports.bind(ExposedPort.tcp(portMapping.getKey()), Ports.Binding(portMapping.getValue()));
      exposedPorts.add(ExposedPort.tcp(portMapping.getKey()));
    });

    ExposedPort[] exposedPortArray = new ExposedPort[exposedPorts.size()];
    exposedPortArray = exposedPorts.toArray(exposedPortArray);
    container = dockerClient.createContainerCmd(oracleImage)
        .withExposedPorts(exposedPortArray)
        .withPortBindings(ports)
        .exec();

    logger.info("Starting the container ...");
    dockerClient.startContainerCmd(container.getId()).exec();
  }

  private void testConnectivity(String oracleImage)
      throws SQLException, InterruptedException, ClassNotFoundException, IOException {
    Class.forName("oracle.jdbc.driver.OracleDriver");
    boolean connectSuccessful = false;
    Connection connection = null;
    for (int i = 0; i < 30; i++) {
      try {
        logger.info("Testing the connection to Oracle to ensure it has come up");
        connection = DriverManager.getConnection(jdbcUrl, USERNAME, PASSWORD);
        connectSuccessful = true;
        break;
      } catch (Exception e) {
        Thread.sleep(1000);
      } finally {
        if (connection != null) {
          connection.close();
        }
      }
    }
    if (!connectSuccessful)  {
      logger.error("After several attempts, connect to Oracle failed; Happens when there are docker machine problems" +
          ", hence resetting the default machine");
      DockerMachineManagement.resetDefaultMachine();
      startContainer(oracleImage);
      testConnectivity(oracleImage);
    }
  }


  public OracleDataSource getOracleDS() throws SQLException {
    OracleDataSource dataSource = new OracleDataSource();
    dataSource.setURL(jdbcUrl);
    dataSource.setUser(USERNAME);
    dataSource.setPassword(PASSWORD);
    return dataSource;
  }

  public void stopContainer() {
    logger.info("Stopping the container ...");
    dockerClient.stopContainerCmd(container.getId()).exec();
    dockerClient.removeContainerCmd(container.getId()).withForce(true).exec();
  }


}
