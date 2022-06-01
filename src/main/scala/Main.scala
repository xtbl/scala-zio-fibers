import zio.console._
import zio.{UIO, ZIO, Exit}
import zio.duration._

@main def hello: Unit =
  println("Hello world!")
  println(msg)

def msg = "I was compiled by Scala 3. :)"

object MyApp extends zio.App {

  def run(args: List[String]) =
    myAppLogic.exitCode

  val myAppLogic =
    for {
      _ <- putStrLn("Hello! What is your name?")
      name <- getStrLn
      _ <- putStrLn(s"Hello, ${name}, welcome to ZIO!")
    } yield ()
}

object ZioFibers extends zio.App {

  val showerTime = ZIO.succeed("Taking a shower")
  val boilingWater = ZIO.succeed("Boiling some water")
  val preparingCoffe = ZIO.succeed("Prepare some coffee")

  def printThread = s"[${Thread.currentThread.getName}]"

  def synchronousRoutine() = for {
    _ <- showerTime.debug(printThread)
    _ <- boilingWater.debug(printThread)
    _ <- preparingCoffe.debug(printThread)
  } yield ()

  // fiber = schedulable computation
  // Fiber[E, A]

  def concurrentShowerWhileBoilingWater() = for {
    _ <- showerTime.debug(printThread).fork
    _ <- boilingWater.debug(printThread)
    _ <- preparingCoffe.debug(printThread)
  } yield ()

  def concurrentRoutine() = for {
    showerFiber <- showerTime.debug(printThread).fork
    boilingWaterFiber <- boilingWater.debug(printThread).fork
    zippedFiber = showerFiber.zip(boilingWaterFiber)
    result <- zippedFiber.join.debug(printThread)
    _ <- ZIO.succeed(s"$result done").debug(printThread) *> preparingCoffe
      .debug(printThread)
  } yield ()

  val callFromAlice = ZIO.succeed("Call from Alice")
  val boilingWaterWithTime = boilingWater.debug(printThread)
    *> ZIO.sleep(5.seconds) *> ZIO.succeed("Boiled water")

  def concurrentRoutineWithAliceCall() = for {
    showerFiber <- showerTime.debug(printThread)
    boilingWaterFiber <- boilingWater.fork
    _ <- callFromAlice.debug(printThread).fork *> ZIO.sleep(
      5.seconds
    ) *> boilingWaterFiber.interrupt.debug(printThread)
    _ <- ZIO
      .succeed("Screw that coffee, going with Alice")
      .debug(printThread)
  } yield ()

  // *> can be read as "and then"

  val prepareCoffeeWithTime = preparingCoffe.debug(printThread) *> ZIO.sleep(5.seconds) 
      *> ZIO.succeed("Coffee ready")

  def concurrentRoutineWithCoffeAtHome() = for {
    _ <- showerTime.debug(printThread)
    _ <- boilingWater.debug(printThread)
    coffeFiber <- prepareCoffeeWithTime.debug(printThread).fork.uninterruptible
    result <- callFromAlice.debug(printThread).fork *> coffeFiber.interrupt.debug(printThread)
    _ <- result match {
      case Exit.Success(value) => 
        ZIO.succeed("Sorry Alice, making breakfast at home").debug(printThread)
      case _ => ZIO.succeed("Going to a cafe with Alice").debug(printThread)
    }
  } yield ()


  // main method for ZIO apps
  override def run(args: List[String]) =
    concurrentRoutineWithCoffeAtHome().exitCode

}
