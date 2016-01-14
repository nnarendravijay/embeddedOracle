package com.nnarendravijay.junit;

import com.nnarendravijay.EmbeddedOracle;
import oracle.jdbc.pool.OracleDataSource;
import org.junit.rules.ExternalResource;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

public class EmbeddedOracleRule extends ExternalResource {

  private EmbeddedOracle embeddedOracle;

  @Override
  protected void before() throws Throwable {
    super.before();

    String oracleImage = "wnameless/oracle-xe-11g";
    Map<Integer, Integer> portBindings = new HashMap<>();
    portBindings.put(22, 59180);
    portBindings.put(1521, 59181);
    portBindings.put(8080, 59182);
    embeddedOracle = new EmbeddedOracle(oracleImage, portBindings);
  }

  @Override
  protected void after() {
    embeddedOracle.stopContainer();
    super.after();
  }

  public OracleDataSource getOracleDatabase() throws SQLException {
    return embeddedOracle.getOracleDS();
  }

}
