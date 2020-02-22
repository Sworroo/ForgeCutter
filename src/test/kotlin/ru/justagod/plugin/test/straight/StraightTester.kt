package ru.justagod.plugin.test.straight

import org.junit.Test
import ru.justagod.mincer.Mincer
import ru.justagod.mincer.filter.WalkThroughFilter
import ru.justagod.mincer.pipeline.Pipeline
import ru.justagod.mincer.processor.SubMincer
import ru.justagod.mincer.util.join
import ru.justagod.mincer.util.makeFirstSimple
import ru.justagod.model.ClassTypeReference
import ru.justagod.plugin.data.CutterTaskData
import ru.justagod.plugin.data.SideName
import ru.justagod.plugin.processing.CutterPipelines
import ru.justagod.plugin.test.common.ClassSearcherMincer
import ru.justagod.plugin.test.common.TestVerifierMincer
import ru.justagod.plugin.test.test1.Test1Verifier
import ru.justagod.plugin.test.test2.Test2Verifier
import ru.justagod.plugin.test.test3.Test3Verifier
import ru.justagod.plugin.test.test4.Test4Verifier
import ru.justagod.plugin.test.test5.Test5Verifier
import ru.justagod.plugin.test.test6.Test6Verifier
import ru.justagod.plugin.test.test7.Test7Verifier
import ru.justagod.plugin.test.test8.Test8Verifier
import ru.justagod.plugin.test.test9.Test9Runner

class StraightTester : StraightCommon() {

    @Test
    fun test1() {
        test("test1", "CLIENT", Test1Verifier("client", "server"))
        test("test1", "SERVER", Test1Verifier("server", "client"))
    }

    @Test
    fun test2() {
        test("test2", "CLIENT", Test2Verifier(false))
        test("test2", "SERVER", Test2Verifier(true))
    }

    @Test
    fun test3() {
        test("test3", "CLIENT", Test3Verifier(false))
        test("test3", "SERVER", Test3Verifier(true))
    }

    @Test
    fun test4() {
        test("test4", "CLIENT", Test4Verifier(false))
        test("test4", "SERVER", Test4Verifier(true))
    }

    @Test
    fun test5() {
        test("test5", "CLIENT", Test5Verifier())
    }

    @Test
    fun test6() {
        test("test6", "CLIENT", Test6Verifier())
    }

    @Test
    fun test7() {
        test("test7", "CLIENT", Test7Verifier(false))
        test("test7", "SERVER", Test7Verifier(true))
    }

    @Test
    fun test8() {
        test("test8", "CLIENT", Test8Verifier())
    }

    @Test
    fun test9() {
        Test9Runner.run()
    }


    private fun test(name: String, side: String, verifier: TestVerifierMincer) {
        return compileAndProcess(resolve(name)) {
            Mincer.Builder(it, false)
                    .registerSubMincer(
                            CutterPipelines.makePipeline(
                                    "anno.SideOnly",
                                    CutterTaskData("SERVER")
                                            .also {
                                                it.primalSides = listOf(
                                                        SideName.make("SERVER"),
                                                        SideName.make("CLIENT")
                                                )
                                                it.targetSides = listOf(
                                                        SideName.make(side)
                                                )
                                            }
                            )
                    )
                    .build()
        }.validate {
            Mincer.Builder(it, false)
                    .registerSubMincer(
                            Pipeline.makeFirstSimple(
                                    verifier,
                                    WalkThroughFilter,
                                    Unit
                            ).join(
                                    ClassSearcherMincer(verifier.mandatoryClasses()),
                                    WalkThroughFilter,
                                    Unit
                            )
                    )
                    .build()
        }
    }

}