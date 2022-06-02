val epicsServiceVersion      = "1.0.7"
val logbackVersion           = "1.2.5"
val jaxbVersion              = "2.3.1"
val slf4jVersion             = "1.7.32"
val scalaMockVersion         = "5.2.0"
val xmlUnitVersion           = "1.6"
val munitVersion             = "0.7.29"
val gmpCommandRecordsVersion = "0.7.7"

// Gemini repository
ThisBuild / resolvers += "Gemini Repository".at(
  "https://github.com/gemini-hlsw/maven-repo/raw/master/releases"
)

inThisBuild(
  Seq(
    homepage                      := Some(url("https://github.com/gemini-hlsw/acm")),
    Global / onChangedBuildSource := ReloadOnSourceChanges
  ) ++ lucumaPublishSettings
)

lazy val acm = project
  .in(file("."))
  .settings(
    name                    := "acm",
    libraryDependencies ++= Seq(
      "ch.qos.logback"     % "logback-core"         % logbackVersion,
      "edu.gemini.epics"   % "epics-service"        % epicsServiceVersion,
      "org.slf4j"          % "slf4j-api"            % slf4jVersion,
      "javax.xml.bind"     % "jaxb-api"             % jaxbVersion,
      "org.glassfish.jaxb" % "jaxb-runtime"         % jaxbVersion,
      "org.scalamock"     %% "scalamock"            % scalaMockVersion         % Test,
      "xmlunit"            % "xmlunit"              % xmlUnitVersion           % Test,
      "org.scalameta"     %% "munit"                % munitVersion             % Test,
      "edu.gemini.gmp"     % "gmp-commands-records" % gmpCommandRecordsVersion % Test
    ),
    testFrameworks += new TestFramework("munit.Framework"),
    javacOptions += "-Xlint:unchecked",
    Compile / doc / sources := Seq(),
    compileOrder            := CompileOrder.JavaThenScala,
    Compile / sourceGenerators += Def.task {
      import scala.sys.process._
      val pkg = "edu.gemini.epics.acm.generated"
      val log = state.value.log
      val gen = (Compile / sourceManaged).value
      val out = pkg.split("\\.").foldLeft(gen)(_ / _)
      val xsd = sourceDirectory.value / "main" / "resources" / "CaSchema.xsd"
      val cmd = List("xjc", "-d", gen.getAbsolutePath, "-p", pkg, xsd.getAbsolutePath)
      val mod = xsd.getParentFile.listFiles.map(_.lastModified).max
      val cur =
        if (out.exists && out.listFiles.nonEmpty) out.listFiles.map(_.lastModified).min
        else Int.MaxValue
      if (mod > cur) {
        out.mkdirs
        val err = cmd.run(ProcessLogger(log.info(_), log.error(_))).exitValue
        if (err != 0) sys.error("xjc failed")
      }
      out.listFiles.toSeq
    }.taskValue
  )
  .configure(_.enablePlugins(AutomateHeaderPlugin))
