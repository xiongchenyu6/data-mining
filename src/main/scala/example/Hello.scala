package example

import slick.jdbc.MySQLProfile.api._

import scala.concurrent.Await
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global
import java.sql.Timestamp
import scala.collection.JavaConverters._
import com.hankcs.hanlp.tokenizer._
import com.hankcs.hanlp.corpus.tag._
import com.hankcs.hanlp.dictionary._
import org.apache.spark.{SparkContext, SparkConf}
import org.apache.spark.rdd.RDD
import org.apache.spark.mllib.fpm.FPGrowth

object Hello extends App {

  final case class Box(
    box_id : Int,
    content: String,
    number:String,
    weight:String,
    company:String,
    name:String,
    user_id:Int,
    username:String,
    order_id:Int,
    createAt:Timestamp,
    status:Int,
    message:String,
    group_id:Int
  )

  // Schema for the "message" table:
  final class BoxTable(tag: Tag) extends Table[Box](tag, "box") {
    def box_id      = column[Int]("box_id", O.PrimaryKey, O.AutoInc)
    def content = column[String]("content")
    def number = column[String]("number")
    def weight = column[String]("weight")
    def company = column[String]("company")
    def name = column[String]("name")
    def user_id = column[Int]("user_id")
    def username = column[String]("username")
    def order_id = column[Int]("order_id")
    def createAt = column[Timestamp]("company")
    def status = column[Int]("status")
    def message = column[String]("message")
    def group_id = column[Int]("group_id")

    def * = (box_id,content,number,weight,company,name,user_id,username,order_id,createAt,status,message,group_id).mapTo[Box]
  }

  // Base query for querying the messages table:
  lazy val boxs = TableQuery[BoxTable]

  // Create an in-memory H2 database;
  val db = Database.forConfig("database")

  // Helper method for running a query in this example file:
  def exec[T](program: DBIO[T]): T = Await.result(db.run(program), 2 seconds)

  CustomDictionary.add("手机膜","n 1")
  CustomDictionary.add("手机壳","n 1")
  CustomDictionary.remove("粉")

  // Run the test query and print the results:
  println("\nSelecting all messages:")
  val table =  exec( boxs.map(t => (t.content,t.user_id)).result )
  val groupedTable = table.groupBy(_._2)
  val tableList = groupedTable.map(_._2.map( c => NLPTokenizer.segment(c._1).asScala))
  val flatternTableList = tableList.map(_.flatten.filter(f=>f.nature == Nature.n).map(_.word))

  val flatternTableArray = flatternTableList.map(_.distinct.toArray).toSeq
  val conf = new SparkConf().setAppName("spark").setMaster("local[4]")
  val sc = new SparkContext(conf)
  sc.setLogLevel("ERROR")
  val transactions: RDD[Array[String]] = sc.parallelize(flatternTableArray)
  val fpg = new FPGrowth().setMinSupport(0.03).setNumPartitions(10)
  val model = fpg.run(transactions)

  model.freqItemsets.collect().foreach { itemset => println(itemset.items.mkString("[", ",", "]") + ", " + itemset.freq) }

  val minConfidence = 0.4
  model.generateAssociationRules(minConfidence).collect().foreach { rule => println(rule.antecedent.mkString("[", ",", "]") + " => " + rule.consequent .mkString("[", ",", "]") + ", " + rule.confidence) }
  println(model.generateAssociationRules(minConfidence).collect().size)
  sc.stop()
}
