package mill.contrib.avrolib

import mill.util.{TestEvaluator, TestUtil}
import utest.framework.TestPath
import utest.{TestSuite, Tests, assert, _}

object TutorialTests extends TestSuite {

  trait TutorialBase extends TestUtil.BaseModule {
    override def millSourcePath: os.Path = TestUtil.getSrcPathBase() / millOuterCtx.enclosing.split('.')
  }

  trait TutorialModule extends AvroModule {
    override def scalaVersion = "2.12.4"
    override def avroHuggerVersion = "1.0.0-RC15"
  }

  object Tutorial extends TutorialBase {
    object core extends TutorialModule {
      override def avroHuggerVersion = "1.0.0-RC15"
    }
  }

  val resourcePath: os.Path = os.pwd / 'contrib / 'avrolib / 'test / 'avro / 'tutorial

  def avroOutPath(eval: TestEvaluator): os.Path =
    eval.outPath / 'core / 'compileAvro / 'dest / 'com / 'example / 'tutorial

  def workspaceTest[T](m: TestUtil.BaseModule)(t: TestEvaluator => T)
                      (implicit tp: TestPath): T = {
    val eval = new TestEvaluator(m)
    os.remove.all(m.millSourcePath)
    println(s"Source ${m.millSourcePath}")
    os.remove.all(eval.outPath)
    println(s"Removed ${eval.outPath}")
    os.makeDir.all(m.millSourcePath / 'core / 'avro)
    os.copy(resourcePath, m.millSourcePath / 'core / 'avro / 'tutorial)
    t(eval)
  }

  def compiledSourcefiles: Seq[os.RelPath] = Seq[os.RelPath](
    "Person.scala"
  )

  def tests: Tests = Tests {
    'avroVersion - {
      'fromBuild - workspaceTest(Tutorial) { eval =>
        val Right((result, evalCount)) = eval.apply(Tutorial.core.avroHuggerVersion)

        assert(
          result == "1.0.0-RC15",
          evalCount > 0
        )
      }
    }

    'compileAvro - {
      'calledDirectly - workspaceTest(Tutorial) { eval =>
        val Right((result, evalCount)) = eval.apply(Tutorial.core.compileAvro)

        val outPath = avroOutPath(eval)

        val outputFiles = os.walk(result.path).filter(os.isFile)

        val expectedSourcefiles = compiledSourcefiles.map(outPath / _)

        assert(
          result.path == eval.outPath / 'core / 'compileAvro / 'dest,
          outputFiles.nonEmpty,
          outputFiles.forall(expectedSourcefiles.contains),
          outputFiles.size == 1,
          evalCount > 0
        )

        // don't recompile if nothing changed
        val Right((_, unchangedEvalCount)) = eval.apply(Tutorial.core.compileAvro)

        assert(unchangedEvalCount == 0)
      }
      // This throws a NullPointerException in coursier somewhere
      //
//       'triggeredByScalaCompile - workspaceTest(Tutorial) { eval =>
//         val Right((_, evalCount)) = eval.apply(Tutorial.core.compile)
//
//         val outPath = avroOutPath(eval)
//
//         val outputFiles = os.walk(outPath).filter(os.isFile)
//
//         val expectedSourcefiles = compiledSourcefiles.map(outPath / _)
//
//         assert(
//           outputFiles.nonEmpty,
//           outputFiles.forall(expectedSourcefiles.contains),
//           outputFiles.size == 3,
//           evalCount > 0
//         )

         // don't recompile if nothing changed
//         val Right((_, unchangedEvalCount)) = eval.apply(Tutorial.core.compile)
//
//         assert(unchangedEvalCount == 0)
//       }

    }

  }
}
