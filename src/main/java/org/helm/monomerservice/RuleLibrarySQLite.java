/*******************************************************************************
 * Copyright C 2017, The Pistoia Alliance
 *
 * Permission is hereby granted, free of charge, to any person obtaining
 * a copy of this software and associated documentation files (the
 * "Software"), to deal in the Software without restriction, including
 * without limitation the rights to use, copy, modify, merge, publish,
 * distribute, sublicense, and/or sell copies of the Software, and to
 * permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.
 * IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY
 * CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT,
 * TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
 * SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 ******************************************************************************/

package org.helm.monomerservice;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.util.ArrayList;

//import com.sun.javafx.tools.packager.Log;

/**
 * Provides access to SQLite database
 * <p>
 * Description of methods in IConfigLoaderRulesLibrary
 */

public class RuleLibrarySQLite implements IRuleLibrary {

	private static final Logger LOG = LoggerFactory.getLogger(RuleLibrarySQLite.class);

	private static String forname = "org.sqlite.JDBC";
	private static String connection = "jdbc:sqlite:"
			+ MonomerLibrarySQLite.class.getResource("resources/MonomerLib2.0.db").toString();

	private final Validation validation = new Validation(LOG);

	public int deleteRule(int id) throws Exception {
		Connection c = null;
		Statement stmt = null;

		try {
			c = getConnection();
			stmt = c.createStatement();

			ResultSet rs = stmt.executeQuery("SELECT * from RULES where ID = " + id);

			if (rs.next()) {

				String sql = "DELETE from RULES where ID = " + id;
				stmt.executeUpdate(sql);
			} else {
				LOG.info("Rule with ID = " + id + " was not found");
				return -1;
			}

			LOG.info("Rule with ID = " + id + " deleted");

			try {
				stmt.close();
				c.commit();
			} catch (Exception e1) {
				throw e1;
			}
		} catch (Exception e2) {
			c.rollback();
			throw e2;
		} finally {
			c.close();
			LOG.info("Closed database ..");
		}
		return id;
	}

	public Rule showRule(int id) throws Exception {
		Connection c = null;
		Statement stmt = null;
		Rule rule = null;

		try {
			c = getConnection();
			stmt = c.createStatement();

			ResultSet rs = stmt.executeQuery("SELECT * FROM RULES WHERE ID = " + id + ";");

			rule = new Rule(id, rs.getString("CATEGORY"), rs.getString("NAME"), rs.getString("DESCRIPTION"),
					rs.getString("SCRIPT"), rs.getString("AUTHOR"));
			// rule.set_id(rs.getString("_ID"));

			LOG.info("Rule with ID: " + id + " shown");

			try {
				stmt.close();
			} catch (Exception e1) {
				throw e1;
			}
		} catch (Exception e2) {
			c.rollback();
			throw e2;
		} finally {
			c.close();
			LOG.info("Closed database ..");
		}
		return rule;
	}

	public ArrayList<Rule> showAllRules() throws Exception {
		Connection c = null;
		Statement stmt = null;
		Rule rule;
		ArrayList<Rule> list = new ArrayList<Rule>();

		try {
			c = getConnection();
			stmt = c.createStatement();

			ResultSet rs = stmt.executeQuery("SELECT * FROM RULES");

			while (rs.next()) {
				int id = rs.getInt("ID");

				rule = new Rule(id, rs.getString("CATEGORY"), rs.getString("NAME"), rs.getString("DESCRIPTION"),
						rs.getString("SCRIPT"), rs.getString("AUTHOR"));
				// rule.set_id(rs.getString("_ID"));

				list.add(rule);

				LOG.info("ShowAllRules - Rule with ID: " + id + " shown");
			}

			try {
				stmt.close();
			} catch (Exception e1) {
				throw e1;
			}
		} catch (Exception e2) {
			c.rollback();
			throw e2;
		} finally {
			c.close();
			LOG.info("Closed database ..");
		}
		return list;
	}

	public int insertOrUpdateRule(Rule rule) throws Exception {
		Connection c = null;

		try {
			c = getConnection();

			// Check rule
			if (validation.checkRule(rule) != 0) {
				return validation.checkRule(rule);
			}

			// check if rule already exists
			PreparedStatement pstmt = c.prepareStatement("SELECT * from rules where ID = ?");
			pstmt.setInt(1, rule.getId());
			ResultSet rs = pstmt.executeQuery();

			if (rs.next()) {
				pstmt = c.prepareStatement("SELECT * from rules where NAME = ?;)");
				pstmt.setString(1, rule.getName());
				ResultSet rs1 = pstmt.executeQuery();

				while (rs1.next()) {
					if (!rs1.getString("ID").equals(rule.getId().toString())) {
						LOG.info("Rule already exists with ID: " + rs1.getString("ID"));
						return -1;
					}
				}

				pstmt = c.prepareStatement("update rules set category = ?, name = ?, author = ?, description = ?, "
						+ "script = ? where id = ?");
				pstmt.setString(1, rule.getCategory());
				pstmt.setString(2, rule.getName());
				pstmt.setString(3, rule.getAuthor());
				pstmt.setString(4, rule.getDescription());
				pstmt.setString(5, rule.getScript());// .replaceAll("'", "''"));
				pstmt.setInt(6, rule.getId());
				pstmt.execute();
			} else {
				pstmt.close();
				pstmt = c.prepareStatement("SELECT * from rules where NAME = ?;");
				pstmt.setString(1, rule.getName());
				ResultSet rs1 = pstmt.executeQuery();

				if (rs1.next()) {
					LOG.info("Rule already exists with ID: " + rs1.getString("ID"));
					return -1;
				}

				pstmt = c.prepareStatement(
						"INSERT INTO RULES (CATEGORY,NAME,AUTHOR,DESCRIPTION,SCRIPT) " + "VALUES(?,?,?,?,?)"); //"INSERT INTO RULES (CATEGORY,NAME,AUTHOR,DESCRIPTION,SCRIPT,ID) " + "VALUES(?,?,?,?,?,?)");
				pstmt.setString(1, rule.getCategory());
				pstmt.setString(2, rule.getName());
				pstmt.setString(3, rule.getAuthor());
				pstmt.setString(4, rule.getDescription());
				pstmt.setString(5, rule.getScript());// .replaceAll("'", "''"));
				//pstmt.setInt(6, rule.getId());
				pstmt.execute();
				rs = pstmt.getGeneratedKeys();
				rs.next();
				int id = rs.getInt(1);
				rule.setId(id);
				LOG.info("Rule was inserted with ID: " + id);
			}
			try {
				pstmt.close();
				c.commit();
			} catch (Exception e1) {
				throw e1;
			}
		} catch (Exception e2) {
			c.rollback();
			throw e2;
		} finally {
			c.close();
			LOG.info("Closed database ..");
		}

		return 0;
	}

	private Connection getConnection() throws Exception {
		Connection c = null;
		try {
			Class.forName(forname);
			c = DriverManager.getConnection(connection);
			c.setAutoCommit(false);
			LOG.info("Opened database ..");
		} catch (Exception e) {
			throw e;
		}
		return c;
	}
}
