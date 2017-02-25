package ranking;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import scala.Tuple2;
import scala.Tuple3;
import scala.Tuple4;
import scala.collection.Iterator;
import scala.runtime.AbstractFunction1;
import scala.runtime.BoxedUnit;

import org.apache.spark.api.java.JavaPairRDD;
import org.apache.spark.api.java.JavaRDD;
import org.apache.spark.api.java.function.Function;
import org.apache.spark.api.java.function.PairFunction;
import org.apache.spark.sql.DataFrame;
import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Encoder;
import org.apache.spark.sql.Encoders;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.types.StructType;

import rdfanalyzer.spark.Service;

public class DataFramePartitionLooper extends AbstractFunction1<Iterator<Row>, BoxedUnit> implements Serializable {

	private static final long serialVersionUID = -1919222653470217466L;
	
	private boolean starter = true;
	private APSP apsp ;
	private JavaPairRDD<Long, Tuple4<List<Long>,Integer,Integer, Integer>> adjacencyMatrix;
	private JavaPairRDD<Long, Tuple3<List<Long>, List<Integer>, List<Integer>>> result;

	public DataFramePartitionLooper(DataFrame relations) {
		
		apsp = new APSP();
		
		JavaPairRDD<Long,Long> rdd = DFToRDD(relations);
		
		// get the adjacency matrix first.
		adjacencyMatrix = apsp.reduceToAdjacencyMatrix(rdd);
		adjacencyMatrix.cache();
		
	}

	private JavaRDD<APSPCase> ConvertPairRDDToCaseRDD(JavaPairRDD<Long, Tuple3<List<Long>, List<Integer>, List<Integer>>> result){
		return result.map(new Function<Tuple2<Long,Tuple3<List<Long>, List<Integer>, List<Integer>>>, APSPCase>() {

			@Override
			public APSPCase call(Tuple2<Long,Tuple3<List<Long>, List<Integer>, List<Integer>>> line) throws Exception {
				
				APSPCase apspcase = new APSPCase();
				apspcase.setSourceNodes(line._1);
				apspcase.setNodeDistances(line._2._2());
				apspcase.setNodeShortestPaths(line._2._3());
				apspcase.setDestinationNodes(line._2._1());
				
				return apspcase;
			}
		});

	}

	
	public void WriteDataToFile(){
		JavaRDD<APSPCase> apspRDD = ConvertPairRDDToCaseRDD(result);
		WriteInfoToParquet(apspRDD);
	}
	
	private void WriteInfoToParquet(JavaRDD<APSPCase> finalData){

		try{
			org.apache.spark.sql.catalyst.encoders.OuterScopes.addOuterScope(this);

			Encoder<APSPCase> encoder = Encoders.bean(APSPCase.class);
			Dataset<APSPCase> javaBeanDS = Service.sqlCtx().createDataset(
			  finalData.collect(),
			  encoder
			);
			javaBeanDS.toDF().write().parquet(rdfanalyzer.spark.Configuration.storage() + "sib200APSPAll.parquet");
		}
		catch(NullPointerException e){
			System.out.println("We are in the error");
			System.out.println(e.getMessage());
		}
	}

	private JavaPairRDD<Long,Long> DFToRDD(DataFrame relations){
		return relations.select("subId","objId").toJavaRDD().mapToPair(new PairFunction<Row, Long, Long>() {

			@Override
			public Tuple2<Long, Long> call(Row arg0) throws Exception {
				return new Tuple2<Long,Long>(arg0.getLong(0),arg0.getLong(1));
			}
		});
	}
	
	
	public void ApplyBFS(long subject){
		
		 if(starter){
			starter = false;
			result = this.apsp.applyBFSForNode(subject, adjacencyMatrix);
		 }
		 else{
			 
			result = result.union(this.apsp.applyBFSForNode(subject, adjacencyMatrix));
		 }
	}


	@Override
	public BoxedUnit apply(Iterator<Row> iterator) {
		
		System.out.println("iterator called");
		
		while (iterator.hasNext()) {
			 Row row = iterator.next();
			 
			 long subject = row.getLong(1);
			 
			 if(starter){
				starter = false;
				result = this.apsp.applyBFSForNode(subject, adjacencyMatrix);
			 }
			 else{
				 
				result = result.union(this.apsp.applyBFSForNode(subject, adjacencyMatrix));
			 }
		}
		
		return BoxedUnit.UNIT;
	}

}