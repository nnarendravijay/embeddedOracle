package com.nnarendravijay;

import oracle.jdbc.pool.OracleDataSource;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import javax.sql.DataSource;
import java.io.IOException;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Map;

public class EmbeddedOracleTest {

  private String oracleImage = "wnameless/oracle-xe-11g";
  private static Map<Integer, Integer> portBindings = new HashMap<>();

  @BeforeClass
  public static void setup() {
    portBindings.put(22, 59180);
    portBindings.put(1521, 59181);
    portBindings.put(8080, 59182);
  }

  @Test
  public void testMachineDown() throws InterruptedException, SQLException, ClassNotFoundException, IOException {

    DockerMachineManagement.runMachineCmd("/usr/local/bin/docker-machine", "rm", "default");
    Thread.sleep(5000);
    EmbeddedOracle embeddedOracle = new EmbeddedOracle(oracleImage, portBindings);
    OracleDataSource dataSource = embeddedOracle.getOracleDS();
    testSQL(dataSource);
    embeddedOracle.stopContainer();
  }

  @Test
  public void testImageNotCached() throws InterruptedException, SQLException, ClassNotFoundException, IOException {
    DockerMachineManagement.runMachineCmd("/usr/local/bin/docker", "rmi", "-f", oracleImage);
    EmbeddedOracle embeddedOracle = new EmbeddedOracle(oracleImage, portBindings);
    OracleDataSource dataSource = embeddedOracle.getOracleDS();
    testSQL(dataSource);
    embeddedOracle.stopContainer();
  }

  @Test
  public void testMachineStopped() throws InterruptedException, SQLException, ClassNotFoundException, IOException {
    DockerMachineManagement.runMachineCmd("/usr/local/bin/docker-machine", "stop", "default");
    EmbeddedOracle embeddedOracle = new EmbeddedOracle(oracleImage, portBindings);
    OracleDataSource dataSource = embeddedOracle.getOracleDS();
    testSQL(dataSource);
    embeddedOracle.stopContainer();
  }

  private void testSQL(DataSource dataSource) throws SQLException {
    Connection connection = dataSource.getConnection();
    Statement s = connection.createStatement();
    ResultSet rs = s.executeQuery("select tname from tab where rownum <= 2");
    Assert.assertTrue(rs.next());
    closeResources(connection, s, rs);
  }

  private void closeResources(Connection c, Statement s, ResultSet rs) throws SQLException {
    if (c != null) {
      c.close();
    }
    if (s != null) {
      s.close();
    }
    if (rs != null) {
      rs.close();
    }
  }

}
