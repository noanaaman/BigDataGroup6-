

object matrix {
  def main(args: Array[String]): Unit = {
    val lines = sc.textFile(args(0));
    val vals = lines.map(l =>
      val fields = l.split(",")
      (fields(1), fields(0), fields(2)) //userid, prodid, rating
    })
   
      // mapping from userid to integer id
    val userIdToInt: RDD[(String, L)ong)] = 
     vals.map(vals(0).distinct().zipWithUniqueId())

     // mapping from product id to integer id
    val prodIdToInt: RDD[(String, Long)] =
      vals.map(vals(1).distinct().zipWithUniqueId())
      
    val rating: RDD[Rating] = vals.map { r =>
    Rating(userIdToInt.lookup(r(0)).head.toInt, prodIdToInt.lookup(r(1)).head.toInt, r(2))
    }
    
    
    // Build the recommendation model using ALS
    val rank = 10
    val numIterations = 10
    val model = ALS.train(ratings, rank, numIterations, 0.01)
    
    val usersProducts = ratings.map { case Rating(user, product, rate) =>
      (user, product)
    }  
    val predictions =
      model.predict(usersProducts).map { case Rating(user, product, rate) =>
      ((user, product), rate)
    }
    val ratesAndPreds = ratings.map { case Rating(user, product, rate) =>
      ((user, product), rate)
    }.join(predictions)
    val MSE = ratesAndPreds.map { case ((user, product), (r1, r2)) =>
      val err = (r1 - r2)
        err * err
      }.mean()
    println("Mean Squared Error = " + MSE)

    // Save and load model
    model.save(sc, "target/tmp/myCollaborativeFilter")
    val sameModel = MatrixFactorizationModel.load(sc, "target/tmp/myCollaborativeFilter")
  }
}