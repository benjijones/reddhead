package kuhn

import collection.JavaConverters._
import org.neo4j.tooling.GlobalGraphOperations
import org.neo4j.graphdb.factory.GraphDatabaseFactory
import org.neo4j.cypher.ExecutionEngine

package object graph {
  type TF = collection.mutable.ListBuffer[() ⇒ Unit]

  //  val `child relationship type` = DynamicRelationshipType.withName("child")

  def tx[T](f: TF ⇒ T): T = {
    val tx = graph.beginTx
    val tf = new collection.mutable.ListBuffer[() ⇒ Unit]
    val t = try {
      val t = f(tf)
      tx.success()
      t
    }
    catch {
      case ex: Exception ⇒
        tx.failure()
        throw ex
    }
    finally {
      tx.close()
    }
    tf.foreach(f ⇒ f())
    t
  }

  def createNode = graph.createNode

  def getNode(k: String, v: String) = optNode(k, v).getOrElse(sys.error(s"node $k=$v not found"))

  def optNode(k: String, v: String) = Option(index.get(k, v).getSingle)

  def allNodes = GlobalGraphOperations.at(graph).getAllNodes.asScala

  def nodes(kind: String) = {
    val e = new ExecutionEngine(graph)
    val r = e.execute("START n=nodes(*) WHERE n.kind = {k} RETURN n", Map[String, Any]("k" → kind).asJava)
    println(r)
  }

  def shutdown() {
    graph.shutdown()
  }

  val graph = {
    val g = new GraphDatabaseFactory().newEmbeddedDatabaseBuilder("data").newGraphDatabase
    g.index.getNodeAutoIndexer.setEnabled(true)
    g.index.getNodeAutoIndexer.startAutoIndexingProperty("name")
    g
  }

  private val index = graph.index.getNodeAutoIndexer.getAutoIndex
}