package com.nnarendravijay;

import com.nnarendravijay.junit.EmbeddedOracleRule;
import oracle.jdbc.pool.OracleDataSource;
import org.junit.Assert;
import org.junit.ClassRule;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public class EmbeddedOracleRuleTest {

  private static Logger logger = LoggerFactory.getLogger(EmbeddedOracleRuleTest.class);

  @ClassRule
  public static EmbeddedOracleRule db = new EmbeddedOracleRule();

  @Test
  public void testEmbeddedOracleRule() throws SQLException {

    OracleDataSource dataSource = db.getOracleDatabase();
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
