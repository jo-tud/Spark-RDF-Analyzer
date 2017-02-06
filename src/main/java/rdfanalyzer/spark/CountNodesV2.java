/*
 * Copyright (C) 2016 University of Freiburg.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package rdfanalyzer.spark;

import java.util.List;

import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;

/**
 * This class uses a detailed and advanced method to calculate number of nodes.
 */
public class CountNodesV2 {
	public static String main(String[] args) throws Exception {
		// Read graph from parquet
		Dataset<Row> graphFrame = Service.spark().read().parquet(Configuration.storage() + args[0] + ".parquet");
		graphFrame.cache().createOrReplaceTempView("Graph");

		// Run SQL over loaded Graph.
		Dataset<Row> resultsFrame = Service.spark()
				.sql("SELECT COUNT(DISTINCT MyTable1.subject) FROM (SELECT subject FROM Graph"
						+ " UNION ALL SELECT object FROM Graph WHERE object NOT LIKE '\"%' " + " ) MyTable1");

		// Get the literals.
		List<Row> rows = resultsFrame.collectAsList();
		String Literals = Long.toString(rows.get(0).getLong(0));

		// Run SQL over loaded Graph.
		resultsFrame = Service.spark().sql("SELECT COUNT(object) FROM Graph WHERE object LIKE '\"%'");

		// Get the Objects, calculate SUM and format results desired format.
		rows = resultsFrame.collectAsList();

		String Objects = Long.toString(rows.get(0).getLong(0));
		String Sum = Integer.toString(Integer.parseInt(Literals) + Integer.parseInt(Objects));

		return "<h3><span class=\"glyphicon glyphicon-file\" aria-hidden=\"true\"></span>&nbsp;Objects: " + Objects
				+ "</h3><h3><span class=\"glyphicon glyphicon-menu-hamburger\" aria-hidden=\"true\"></span>&nbsp;Literals: "
				+ Literals
				+ "</h3><h3><span class=\"glyphicon glyphicon-globe\" aria-hidden=\"true\"></span>&nbsp;All: " + Sum
				+ "</h3>";
	}
}
