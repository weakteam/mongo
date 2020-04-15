import java.time.Instant

import Keys._
import com.typesafe.sbt.GitPlugin.autoImport._
import sbt.Keys._
import sbt._
import sbtbuildinfo.BuildInfoKeys._
import sbtbuildinfo.{BuildInfoPlugin, _}

object BuildInfo {

  implicit class BuildInfoOps(val project: Project) extends AnyVal {
    def withBuildInfo: Project =
      project
        .enablePlugins(BuildInfoPlugin)
        .settings(
          buildCommit := git.gitHeadCommit.value.getOrElse("unknown"),
          buildBranch := git.gitCurrentBranch.value,
          buildTime := Instant.now,
          buildNumber := sys.props.getOrElse("BUILD_NUMBER", "0").toInt
        )
        .settings(
          buildInfoKeys := {
            Seq[BuildInfoKey](
              name,
              version,
              scalaVersion,
              sbtVersion,
              buildCommit,
              buildBranch,
              buildTime,
              buildNumber,
              projectVersionMinor,
              projectVersionMajor
            )
          },
          buildInfoPackage := "io.github.weakteam",
          buildInfoOptions += BuildInfoOption.ToMap,
          buildInfoOptions += BuildInfoOption.ToJson
        )
  }
}
