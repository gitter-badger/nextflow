/*
 * Copyright (c) 2013-2014, Centre for Genomic Regulation (CRG).
 * Copyright (c) 2013-2014, Paolo Di Tommaso and the respective authors.
 *
 *   This file is part of 'Nextflow'.
 *
 *   Nextflow is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU General Public License as published by
 *   the Free Software Foundation, either version 3 of the License, or
 *   (at your option) any later version.
 *
 *   Nextflow is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU General Public License for more details.
 *
 *   You should have received a copy of the GNU General Public License
 *   along with Nextflow.  If not, see <http://www.gnu.org/licenses/>.
 */

package nextflow.script
import static test.TestParser.parse

import groovyx.gpars.dataflow.DataflowQueue
import nextflow.processor.TaskProcessor
import spock.lang.Specification
/**
 *
 * @author Paolo Di Tommaso <paolo.ditommaso@gmail.com>
 */
class ParamsOutTest extends Specification {


    // ==============================================================
    //                  test *output* parameters
    // ==============================================================

    def testOutParam() {

        setup:
        def text = '''
            process hola {
              output:
              val x
              val p into q

              return ''
            }
            '''

        def binding = [:]
        TaskProcessor process = parse(text, binding).run()

        when:
        def out1 = process.taskConfig.getOutputs().get(0)
        def out2 = process.taskConfig.getOutputs().get(1)

        // it MUST
        // - create a value out parameter named 'x'
        // - create in the script context (binding) a new variable of type DataflowQueue named 'x'
        then:
        process.taskConfig.getOutputs().size() == 2

        out1.class == ValueOutParam
        out1.name == 'x'
        out1.outChannel instanceof DataflowQueue
        binding.containsKey('x')

        out2.class == ValueOutParam
        out2.name == 'p'
        out2.outChannel instanceof DataflowQueue
        !binding.containsKey('p')
        binding.containsKey('q')

    }

    def testFileOutParams() {

        setup:
        def text = '''
            process hola {
              output:
              file x
              file 'y'  mode flatten
              file p into q mode standard

              return ''
            }
            '''

        def binding = [:]
        TaskProcessor process = parse(text, binding).run()

        when:
        def out1 = process.taskConfig.getOutputs().get(0)
        def out2 = process.taskConfig.getOutputs().get(1)
        def out3 = process.taskConfig.getOutputs().get(2)

        // it MUST
        // - create a value out parameter named 'x'
        // - create in the script context (binding) a new variable of type DataflowQueue named 'x'
        then:
        process.taskConfig.getOutputs().size() == 3

        out1.class == FileOutParam
        out1.name == 'x'
        out1.outChannel instanceof DataflowQueue
        out1.outChannel == binding.'x'
        out1.mode == BasicMode.standard

        out2.class == FileOutParam
        out2.name == 'y'
        out2.outChannel instanceof DataflowQueue
        out2.outChannel == binding.y
        out2.mode == BasicMode.flatten

        out3.class == FileOutParam
        out3.name == 'p'
        out3.outChannel instanceof DataflowQueue
        out3.outChannel == binding.q
        out3.mode == BasicMode.standard
        !binding.containsKey('p')
    }

    def testFileOutParamsWithVariables() {

        setup:
        def text = '''

            process hola {
              output:
              file "${x}_name" into channel1
              file "${x}_${y}.fa" into channel2
              file "simple.txt" into channel3
              file "${z}.txt:${x}.fa" into channel4
              set "${z}.txt:${x}.fa" into channel5
              set file("${z}.txt:${x}.fa") into channel6

              return ''
            }
            '''

        def binding = [:]
        TaskProcessor process = parse(text, binding).run()
        def ctx = [x: 'hola', y:99, z:'script_file']

        when:
        FileOutParam out1 = process.taskConfig.getOutputs().get(0)
        FileOutParam out2 = process.taskConfig.getOutputs().get(1)
        FileOutParam out3 = process.taskConfig.getOutputs().get(2)
        FileOutParam out4 = process.taskConfig.getOutputs().get(3)
        SetOutParam out5 = process.taskConfig.getOutputs().get(4)
        SetOutParam out6 = process.taskConfig.getOutputs().get(5)

        then:
        process.taskConfig.getOutputs().size() == 6

        out1.name == null
        out1.getFilePatterns(ctx) == ['hola_name']
        out1.outChannel instanceof DataflowQueue
        out1.outChannel == binding.channel1
        out1.isParametric()

        out2.name == null
        out2.getFilePatterns(ctx) == ['hola_99.fa']
        out2.outChannel instanceof DataflowQueue
        out2.outChannel == binding.channel2
        out2.isParametric()

        out3.name == 'simple.txt'
        out3.getFilePatterns(ctx) == ['simple.txt']
        out3.outChannel instanceof DataflowQueue
        out3.outChannel == binding.channel3
        !out3.isParametric()

        out4.name == null
        out4.getFilePatterns(ctx) == ['script_file.txt','hola.fa']
        out4.outChannel instanceof DataflowQueue
        out4.outChannel == binding.channel4
        out4.isParametric()

        out5.outChannel instanceof DataflowQueue
        out5.outChannel == binding.channel5
        out5.inner[0] instanceof FileOutParam
        (out5.inner[0] as FileOutParam) .getFilePatterns(ctx) == ['script_file.txt','hola.fa']
        (out5.inner[0] as FileOutParam) .isParametric()

        out6.outChannel instanceof DataflowQueue
        out6.outChannel == binding.channel6
        out6.inner[0] instanceof FileOutParam
        (out6.inner[0] as FileOutParam) .getFilePatterns(ctx) == ['script_file.txt','hola.fa']
        (out6.inner[0] as FileOutParam) .isParametric()

    }

    def testFileOutWithGString2 () {

        setup:
        def text = '''

            process hola {
              output:
              file x
              file "$y" into q
              set file(z) into p
              file u

              return ''
            }
            '''

        def binding = [:]
        TaskProcessor process = parse(text, binding).run()
        def ctx = [ x: 'hola', y:'hola_2', z: 'hola_z' ]

        when:
        FileOutParam out1 = process.taskConfig.getOutputs().get(0)
        FileOutParam out2 = process.taskConfig.getOutputs().get(1)
        SetOutParam out3 = process.taskConfig.getOutputs().get(2)
        FileOutParam out4 = process.taskConfig.getOutputs().get(3)


        then:
        out1.name == 'x'
        out1.getFilePatterns(ctx) == ['hola']
        out1.outChannel instanceof DataflowQueue
        out1.outChannel == binding.x

        out2.name == null
        out2.getFilePatterns(ctx) == ['hola_2']
        out2.outChannel instanceof DataflowQueue
        out2.outChannel == binding.q

        out3.inner[0] instanceof FileOutParam
        (out3.inner[0] as FileOutParam).name == 'z'
        (out3.inner[0] as FileOutParam).getFilePatterns(ctx) == ['hola_z']

        out4.name == 'u'
        out4.getFilePatterns(ctx) == ['u']
        out4.outChannel instanceof DataflowQueue
        out4.outChannel == binding.u
    }

    def testFileOutWithGString3 () {

        setup:
        def text = '''

            process hola {
              output:
              file "$x"

              return ''
            }
            '''

        def binding = [:]
        TaskProcessor process = parse(text, binding).run()

        when:
        FileOutParam out1 = process.taskConfig.getOutputs().get(0)
        def x = out1.outChannel

        then:
        thrown(IllegalArgumentException)



    }



    def testSetOutParams() {

        setup:
        def text = '''
            process hola {
              output:
                set(x) into p
                set(y,'-', '*.fa') into q mode flatten
                set(stdout, z) into t mode combine

              return ''
            }
            '''

        def binding = [:]
        TaskProcessor process = parse(text, binding).run()

        when:
        SetOutParam out1 = process.taskConfig.getOutputs().get(0)
        SetOutParam out2 = process.taskConfig.getOutputs().get(1)
        SetOutParam out3 = process.taskConfig.getOutputs().get(2)

        then:
        process.taskConfig.getOutputs().size() == 3

        out1.outChannel instanceof DataflowQueue
        out1.outChannel == binding.p
        out1.inner.size() == 1
        out1.inner[0] instanceof ValueOutParam
        out1.inner[0].name == 'x'
        out1.inner[0].index == 0
        out1.mode == BasicMode.standard

        out2.outChannel instanceof DataflowQueue
        out2.outChannel == binding.q
        out2.inner[0] instanceof ValueOutParam
        out2.inner[0].name == 'y'
        out2.inner[0].index == 1
        out2.inner[1] instanceof StdOutParam
        out2.inner[1].name == '-'
        out2.inner[1].index == 1
        out2.inner[2] instanceof FileOutParam
        out2.inner[2].name == '*.fa'
        out2.inner[2].index == 1
        out2.inner.size() ==3
        out2.mode == BasicMode.flatten

        out3.outChannel instanceof DataflowQueue
        out3.outChannel == binding.t
        out3.inner.size() == 2
        out3.inner[0] instanceof StdOutParam
        out3.inner[0].name == '-'
        out3.inner[0].index == 2
        out3.inner[1] instanceof ValueOutParam
        out3.inner[1].name == 'z'
        out3.inner[1].index == 2
        out3.mode == SetOutParam.CombineMode.combine

    }

    def testSetOutParams2() {

        setup:
        def text = '''
            process hola {
              output:
                set val(x) into p
                set val(y), stdout, file('*.fa') into q mode flatten
                set stdout, val(z) into t mode combine

              return ''
            }
            '''

        def binding = [:]
        TaskProcessor process = parse(text, binding).run()

        when:
        SetOutParam out1 = process.taskConfig.getOutputs().get(0)
        SetOutParam out2 = process.taskConfig.getOutputs().get(1)
        SetOutParam out3 = process.taskConfig.getOutputs().get(2)

        then:
        process.taskConfig.getOutputs().size() == 3

        out1.outChannel instanceof DataflowQueue
        out1.outChannel == binding.p
        out1.inner.size() == 1
        out1.inner[0] instanceof ValueOutParam
        out1.inner[0].name == 'x'
        out1.inner[0].index == 0
        out1.mode == BasicMode.standard

        out2.outChannel instanceof DataflowQueue
        out2.outChannel == binding.q
        out2.inner[0] instanceof ValueOutParam
        out2.inner[0].name == 'y'
        out2.inner[0].index == 1
        out2.inner[1] instanceof StdOutParam
        out2.inner[1].name == '-'
        out2.inner[1].index == 1
        out2.inner[2] instanceof FileOutParam
        out2.inner[2].name == '*.fa'
        out2.inner[2].index == 1
        out2.inner.size() ==3
        out2.mode == BasicMode.flatten

        out3.outChannel instanceof DataflowQueue
        out3.outChannel == binding.t
        out3.inner.size() == 2
        out3.inner[0] instanceof StdOutParam
        out3.inner[0].name == '-'
        out3.inner[0].index == 2
        out3.inner[1] instanceof ValueOutParam
        out3.inner[1].name == 'z'
        out3.inner[1].index == 2
        out3.mode == SetOutParam.CombineMode.combine

    }



    def testStdOut() {

        setup:
        def text = '''
            process hola {
              output:
              stdout into p

              return ''
            }
            '''

        def binding = [:]
        TaskProcessor process = parse(text, binding).run()

        when:
        def out1 = process.taskConfig.getOutputs().get(0)

        then:
        process.taskConfig.getOutputs().size() == 1

        out1.class == StdOutParam

    }


    def testOutList () {

        setup:
        def bind = new Binding()
        def outs = new OutputsList()

        when:
        def v1 = new ValueOutParam(bind,outs)
        def o1 = new StdOutParam(bind,outs)
        def v2 = new ValueOutParam(bind,outs)
        def s1 = new SetOutParam(bind,outs)
        def s2 = new SetOutParam(bind,outs)

        then:
        outs.size() == 5
        outs.ofType(StdOutParam) == [o1]
        outs.ofType(ValueOutParam) == [v1,v2]
        outs.ofType(SetOutParam,StdOutParam) == [o1,s1,s2]

    }


    def testModeParam() {

        setup:
        def p = new SetOutParam(new Binding(), [])
        when:
        p.mode(value)
        then:
        p.getMode() == expected

        where:
        value                       | expected
        'combine'                   | SetOutParam.CombineMode.combine
        new TokenVar('combine')    | SetOutParam.CombineMode.combine
        'flatten'                   | BasicMode.flatten
        new TokenVar('flatten')    | BasicMode.flatten

    }

    def testWrongMode() {

        when:
        def p = new SetOutParam(new Binding(), [])
        p.mode('unknown')
        then:
        thrown(IllegalArgumentException)

    }

    def testDefaultMode() {

        setup:
        def bind = new Binding()
        def list = []

        expect:
        new StdOutParam(bind, list).mode == BasicMode.standard
        new ValueOutParam(bind, list).mode == BasicMode.standard
        new FileOutParam(bind, list).mode == BasicMode.standard
        new SetOutParam(bind, list).mode == BasicMode.standard

    }

}
