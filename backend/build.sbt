name := "BookSwap"

scalaVersion := "2.13.3"

libraryDependencies ++= {
  val cats = Seq(
    "org.typelevel" %% "cats-core",
    "org.typelevel" %% "cats-effect"
  ).map(_ % "2.2.0")

  val circe = Seq(
    "io.circe" %% "circe-core",
    "io.circe" %% "circe-parser"
  ).map(_ % "0.12.3")

  val doobie = Seq(
    "org.tpolecat" %% "doobie-core"     ,
    "org.tpolecat" %% "doobie-hikari"   ,
    "org.tpolecat" %% "doobie-postgres" ,
    "org.tpolecat" %% "doobie-scalatest"
  ).map(_ % "0.9.0")

  val enumeratum = Seq(
    "com.beachape"  %% "enumeratum"        % "1.6.1",
    "com.beachape"  %% "enumeratum-doobie" % "1.6.0",
  )

  val scalatest = Seq(
    "org.scalatest" %% "scalatest" % "3.2.0" % Test
  )

  cats ++ circe ++ doobie ++ enumeratum ++ scalatest
}

scalacOptions ++= Seq(
  "-deprecation",
  "-Xfatal-warnings",
  "-Ywarn-dead-code",
  "-Ywarn-numeric-widen",
  "-Ywarn-value-discard",
  "-Ywarn-unused"
)

addCompilerPlugin("com.olegpy" %% "better-monadic-for" % "0.3.1")
addCompilerPlugin("org.typelevel" %% "kind-projector" % "0.11.0" cross CrossVersion.full)
