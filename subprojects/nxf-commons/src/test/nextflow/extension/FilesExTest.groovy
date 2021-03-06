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

package nextflow.extension
import java.nio.file.Files
import java.nio.file.Paths

import spock.lang.Specification
/**
 *
 * @author Paolo Di Tommaso <paolo.ditommaso@gmail.com>
 */
class FilesExTest extends Specification {



    def testGetExtension() {

        expect:
        new File('file.txt').getExtension() == 'txt'
        Paths.get('file.txt').getExtension() == 'txt'
        new File('file_name').getExtension() == ''
        Paths.get('file_name').getExtension() == ''

    }

    def testBaseName() {

        expect:
        new File('/path/file.txt').getBaseName() == 'file'
        Paths.get('/path/file.txt').getBaseName() == 'file'

        new File('/path/file_name').getBaseName() == 'file_name'
        Paths.get('/path/file_name').getBaseName() == 'file_name'

        new File('/path/').getBaseName() == 'path'
        Paths.get('/path/').getBaseName() == 'path'

        new File('/').getBaseName() == ''
        Paths.get('/').getBaseName() == ''

    }


    def testGetName() {

        expect:
        new File('/path/file.txt').getName() == 'file.txt'
        Paths.get('/path/file.txt').getName() == 'file.txt'

        new File('/path/file_name').getName() == 'file_name'
        Paths.get('/path/file_name').getName() == 'file_name'

        new File('/path/').getName() == 'path'
        Paths.get('/path/').getName() == 'path'

        new File('/').getName() == ''
        Paths.get('/').getName() == ''

    }

    def testPathMakeDir() {

        when:
        def path = Paths.get('xyz')
        then:
        path.mkdir()

        cleanup:
        path.deleteDir()


    }


    def 'test baseName' () {

        expect:
        new File('a/b/file.text').baseName == 'file'
        new File('.').baseName == ''

    }

    def 'test file extension' () {

        expect:
        new File('a/b/file.txt').extension == 'txt'
        new File('.').baseName == ''

    }

    def testCopyTo () {

        setup:
        def source = File.createTempFile('test','source')
        source.text = 'Hello'

        when:
        def copy = source.copyTo( File.createTempFile('test','target') )
        then:
        copy.text == 'Hello'
        source.exists()

        when:
        def folder = Files.createTempDirectory('test').toFile()
        def copy2 = source.copyTo(folder)
        then:
        copy2.text == 'Hello'
        copy2.name == source.name
        source.exists()

        cleanup:
        source?.delete()
        copy?.delete()
        folder?.deleteDir()
    }


    def 'test copyTo path' () {

        setup:
        def source = Files.createTempFile('test','source')
        source.text = 'Hello'

        when:
        def copy = source.copyTo( Files.createTempFile('test','target') )
        then:
        copy.text == 'Hello'
        source.exists()

        when:
        def folder = Files.createTempDirectory('testCopyTo')
        def copy2 = source.copyTo(folder)
        then:
        copy2.text == 'Hello'
        copy2.name == source.name
        source.exists()

        cleanup:
        source?.delete()
        copy?.delete()
        folder?.deleteDir()
    }

    def 'test copyTo directory with symlinks'  () {

        given:
        def folder = Files.createTempDirectory('test')
        Files.createDirectories(folder.resolve('myFiles'))
        folder.resolve('myFiles').resolve('file1').text = 'Hello'
        folder.resolve('myFiles').resolve('file2').text = 'Hola'

        Files.createSymbolicLink( folder.resolve('linkDir'), folder.resolve('myFiles') )

        when:
        FilesEx.copyTo(folder.resolve('linkDir'), folder.resolve('target'))

        then:
        Files.isDirectory(folder.resolve('target'))
        folder.resolve('target').resolve('file1').text == 'Hello'
        folder.resolve('target').resolve('file2').text == 'Hola'

        cleanup:
        folder?.deleteDir()

    }

    def 'test copy source dir to existing dir'  () {

        given:
        def folder = Files.createTempDirectory('test')
        folder.resolve('alpha').mkdir()
        folder.resolve('alpha/file_1.txt').text = 'file 1'
        folder.resolve('alpha/file_2.txt').text = 'file 2'
        folder.resolve('alpha/link_3.txt').symlinkTo(folder.resolve('alpha/file_2.txt'))

        folder.resolve('alpha/dir2').mkdir()
        folder.resolve('alpha/dir2/file_4').text = 'Hello'
        folder.resolve('alpha/dir2/file_5').text = 'Hola'

        /*
         * copy the dir 'alpha' with all its content to 'beta' directory
         * => beta will contain the 'alpha' dir
         */
        when:
        folder.resolve('beta').mkdir()
        FilesEx.copyTo(folder.resolve('alpha'), folder.resolve('beta'))
        then:
        folder.resolve('beta').isDirectory()
        folder.resolve('beta/alpha').isDirectory()
        folder.resolve('beta/alpha/file_1.txt').text == 'file 1'
        folder.resolve('beta/alpha/file_2.txt').text == 'file 2'
        folder.resolve('beta/alpha/link_3.txt').text == 'file 2'
        // note: this is coherent with Linux 'cp -r' command i.e. the link target file is copied not the link itself
        !folder.resolve('beta/alpha/link_3.txt').isSymlink()
        folder.resolve('beta/alpha/dir2').isDirectory()
        folder.resolve('beta/alpha/dir2/file_4').text == 'Hello'
        folder.resolve('beta/alpha/dir2/file_5').text == 'Hola'

        /*
         * when the 'beta' directory does not exist the source dir content is
         * copied to it
         *
         * note: this is coherent with the cp -r linux command
         */
        when:
        folder.resolve('beta').deleteDir()
        FilesEx.copyTo(folder.resolve('alpha'), folder.resolve('beta'))
        then:
        folder.resolve('beta').isDirectory()
        folder.resolve('beta/file_1.txt').text == 'file 1'
        folder.resolve('beta/file_2.txt').text == 'file 2'
        folder.resolve('beta/link_3.txt').text == 'file 2'
        // note: this is coherent with 'cp -r' linux command i.e. the link target file is copied not the link itself
        !folder.resolve('beta/link_3.txt').isSymlink()
        folder.resolve('beta/dir2').isDirectory()
        folder.resolve('beta/dir2/file_4').text == 'Hello'
        folder.resolve('beta/dir2/file_5').text == 'Hola'

        when:
        folder.resolve('beta').deleteDir()
        FilesEx.copyTo(folder.resolve('alpha/dir2'), folder.resolve('beta/alpha/dir2'))
        then:
        folder.resolve('beta/alpha/dir2').isDirectory()
        folder.resolve('beta/alpha/dir2/file_4').text == 'Hello'
        folder.resolve('beta/alpha/dir2/file_5').text == 'Hola'

        cleanup:
        folder?.deleteDir()

    }

    def 'test moveTo' () {

        setup:
        // source1
        def source1 = Files.createTempFile('test','source')
        source1.text = 'Hello 1'


        /*
         * test moving to a file with a different name
         */
        when:
        def file1 = source1.moveTo( Files.createTempFile('targetFile',null) )
        then:
        !source1.exists()
        file1.text == 'Hello 1'

        /*
         * test moving to a FOLDER
         */
        when:
        def folder = Files.createTempDirectory('testMoveTo')
        def source2 = Files.createTempFile('test','source')
        source2.text = 'Hello 2'
        def file2 = source2.moveTo(folder)
        then:
        file2.text == 'Hello 2'
        file2.name == source2.name
        !source2.exists()


        cleanup:
        file1?.delete()
        file2?.delete()
        folder?.deleteDir()

    }

    def 'test moveTo (directory) ' () {

        given:
        def sourceFolder =  Files.createTempDirectory('folderToCopy')
        def path1 = sourceFolder.resolve('file1')
        def path2 = sourceFolder.resolve('file2')
        def path3 = sourceFolder.resolve('file3')
        def sub1 = Files.createTempDirectory(sourceFolder, 'sub1')
        def path4 = sub1.resolve('file4')
        path1.text = 'Hello1'
        path2.text = 'Hello2'
        path3.text = 'Hello3'
        path4.text = 'Hello4'
        def targetFolder = Files.createTempDirectory('targetFolder')

        when:
        targetFolder = sourceFolder.moveTo(targetFolder)
        then:
        !sourceFolder.exists()
        targetFolder.getName() == sourceFolder.getName()
        targetFolder.resolve('file1').text == 'Hello1'
        targetFolder.resolve('file2').text == 'Hello2'
        targetFolder.resolve('file3').text == 'Hello3'
        targetFolder.resolve(sub1.getName()).resolve('file4').text == 'Hello4'

        cleanup:
        sourceFolder?.deleteDir()
        targetFolder.getParent()?.deleteDir()


    }

    def testFileShit() {

        setup:
        def file = new File('test_file_shift')
        def path = Paths.get('test_path_shift')

        when:
        file << 'hola'
        path << 'hello'

        then:
        file.text == 'hola'
        path.text == 'hello'

        cleanup:
        file.delete()
        path.delete()

    }

    def testFileTail() {

        setup:
        def file = File.createTempFile('tailtest','txt')
        file.deleteOnExit()

        when:
        file.text = '''
        1
        22
        333
        4444
        55555
        666666
        7777777
        88888888
        999999999
        '''.stripIndent()

        then:
        file.tail(1).toString() == '999999999'
        file.tail(2).toString() == '88888888\n999999999'
        file.tail(2,null,8).toString() == '88888888\n999999999'
        file.tail(2,null,9).toString() == '88888888\n999999999'
        file.tail(2,null,10).toString() == '88888888\n999999999'
        file.tail(2,null,11).toString() == '88888888\n999999999'
        file.tail(2,null,12).toString() == '88888888\n999999999'

        when:
        def file2 = File.createTempFile('tailtest','txt')
        file2.deleteOnExit()
        file2.text = '''
        1
        22
        333
        4444
        55555
        666666
        7777777


        88888888

        999999999'''.stripIndent()

        then:
        file2.tail(1).toString() == '999999999'
        file2.tail(3).toString() == '88888888\n\n999999999'
        file2.tail(6).toString() == '7777777\n\n\n88888888\n\n999999999'
        file2.tail(6,null,8).toString() == '7777777\n\n\n88888888\n\n999999999'
        file2.tail(6,null,9).toString() == '7777777\n\n\n88888888\n\n999999999'
        file2.tail(6,null,10).toString() == '7777777\n\n\n88888888\n\n999999999'
        file2.tail(6,null,11).toString() == '7777777\n\n\n88888888\n\n999999999'
        file2.tail(6,null,12).toString() == '7777777\n\n\n88888888\n\n999999999'

    }

    def testReaderTail() {

        when:
        def text = '''
        1
        22
        333
        4444
        55555
        666666
        7777777
        88888888
        999999999
        '''.stripIndent()

        then:
        new StringReader(text).tail(1).toString() == '999999999'
        new StringReader(text).tail(2).toString() == '88888888\n999999999'
        new StringReader(text).tail(3).toString() == '7777777\n88888888\n999999999'

        when:
        text = '''
        1
        22
        333
        4444
        55555
        666666
        7777777


        88888888

        999999999'''.stripIndent()

        then:
        new StringReader(text).tail(1).toString() == '999999999'
        new StringReader(text).tail(3).toString() == '88888888\n\n999999999'
        new StringReader(text).tail(6).toString() == '7777777\n\n\n88888888\n\n999999999'

    }


    def testFileHead() {

        setup:
        def file = File.createTempFile('tailtest','txt')
        file.deleteOnExit()

        when:
        file.text = '''\
        1
        22
        333


        4444
        55555
        666666
        7777777
        88888888
        999999999
        '''.stripIndent()

        then:
        file.head(1).toString() == '1'
        file.head(2).toString() == '1\n22'
        file.head(6).toString() == '1\n22\n333\n\n\n4444'

    }

    def testFileList() {

        given:
        def sourceFolder =  Files.createTempDirectory('folderToCopy')
        def path1 = sourceFolder.resolve('file1')
        def path2 = sourceFolder.resolve('file2')
        def path3 = sourceFolder.resolve('file3')
        def sub1 = Files.createTempDirectory(sourceFolder, 'sub1')
        def path4 = sub1.resolve('file4')
        def path5 = sub1.resolve('file5')
        path1.text = 'Hello1'
        path2.text = 'Hello2'
        Files.createSymbolicLink(path3, path2)
        path4.text = 'Hello4'
        path5.text = 'Hello5'

        when:
        def files = sourceFolder.toFile().listFiles()
        def paths = sourceFolder.listFiles()
        def strings1 = sourceFolder.toFile().list()
        def strings2 = sourceFolder.list()

        then:
        files.size() == 4
        files.contains new File(sourceFolder.toString(), 'file1')
        files.contains new File(sourceFolder.toString(), 'file2')
        files.contains new File(sourceFolder.toString(), 'file3')
        files.contains sub1.toFile()

        paths.size() == 4
        paths.contains path1
        paths.contains path2
        paths.contains path3
        paths.contains sub1

        strings1.size() == 4
        strings1.sort() == strings2.sort()

        expect:
        path1.toFile().listFiles() == null
        path1.listFiles() == null

        cleanup:
        sourceFolder.deleteDir()
    }


    def testSetReadonly() {

        setup:
        def file1 = File.createTempFile('testfile',null)
        def file2 = File.createTempFile('testfile',null)
        file1.deleteOnExit()
        file2.deleteOnExit()

        when:
        file1.setReadOnly()
        def OK = file2.toPath().setReadOnly()

        then:
        OK
        file1.toPath().getPermissions() == file2.toPath().getPermissions()

        expect:
        !Paths.get('none').setReadOnly()
    }

    def testSetExecutable () {
        setup:
        def file1 = File.createTempFile('testfile',null)
        def file2 = File.createTempFile('testfile',null)
        file1.deleteOnExit()
        file2.deleteOnExit()

        when:
        file1.setExecutable(true,false)
        def OK = file2.toPath().setExecutable(true,false)
        println "file1: ${file1.toPath().getPermissions()}"
        println "file2: ${file2.toPath().getPermissions()}"
        then:
        OK
        file1.toPath().getPermissions() == file2.toPath().getPermissions()

        when:
        file1.setExecutable(true)
        file2.toPath().setExecutable(true)
        println "file1: ${file1.toPath().getPermissions()}"
        println "file2: ${file2.toPath().getPermissions()}"
        then:
        file1.toPath().getPermissions() == file2.toPath().getPermissions()

        when:
        file1.setExecutable(false)
        file2.toPath().setExecutable(false)
        println "file1: ${file1.toPath().getPermissions()}"
        println "file2: ${file2.toPath().getPermissions()}"
        then:
        file1.toPath().getPermissions() == file2.toPath().getPermissions()

        when:
        file1.setExecutable(false,false)
        file2.toPath().setExecutable(false,false)
        println "file1: ${file1.toPath().getPermissions()}"
        println "file2: ${file2.toPath().getPermissions()}"
        then:
        file1.toPath().getPermissions() == file2.toPath().getPermissions()

        expect:
        !Paths.get('none').setExecutable(true)

    }

    def testSetWritable() {
        setup:
        def file1 = File.createTempFile('testfile',null)
        def file2 = File.createTempFile('testfile',null)
        file1.deleteOnExit()
        file2.deleteOnExit()

        when:
        file1.setWritable(true,false)
        def OK = file2.toPath().setWritable(true,false)
        println "file1: ${file1.toPath().getPermissions()}"
        println "file2: ${file2.toPath().getPermissions()}"
        then:
        OK
        file1.toPath().getPermissions() == file2.toPath().getPermissions()

        when:
        file1.setWritable(true)
        file2.toPath().setWritable(true)
        println "file1: ${file1.toPath().getPermissions()}"
        println "file2: ${file2.toPath().getPermissions()}"
        then:
        file1.toPath().getPermissions() == file2.toPath().getPermissions()

        when:
        file1.setWritable(false)
        file2.toPath().setWritable(false)
        println "file1: ${file1.toPath().getPermissions()}"
        println "file2: ${file2.toPath().getPermissions()}"
        then:
        file1.toPath().getPermissions() == file2.toPath().getPermissions()

        when:
        file1.setWritable(false,false)
        file2.toPath().setWritable(false,false)
        println "file1: ${file1.toPath().getPermissions()}"
        println "file2: ${file2.toPath().getPermissions()}"
        then:
        file1.toPath().getPermissions() == file2.toPath().getPermissions()

        expect:
        !Paths.get('none').setWritable(true)


    }

    def testSetReadable() {
        setup:
        def file1 = File.createTempFile('testfile',null)
        def file2 = File.createTempFile('testfile',null)
        file1.deleteOnExit()
        file2.deleteOnExit()

        when:
        file1.setReadable(true,false)
        def OK = file2.toPath().setReadable(true,false)
        println "file1: ${file1.toPath().getPermissions()}"
        println "file2: ${file2.toPath().getPermissions()}"
        then:
        OK
        file1.toPath().getPermissions() == file2.toPath().getPermissions()

        when:
        file1.setReadable(true)
        file2.toPath().setReadable(true)
        println "file1: ${file1.toPath().getPermissions()}"
        println "file2: ${file2.toPath().getPermissions()}"
        then:
        file1.toPath().getPermissions() == file2.toPath().getPermissions()

        when:
        file1.setReadable(false)
        file2.toPath().setReadable(false)
        println "file1: ${file1.toPath().getPermissions()}"
        println "file2: ${file2.toPath().getPermissions()}"
        then:
        file1.toPath().getPermissions() == file2.toPath().getPermissions()

        when:
        file1.setReadable(false,false)
        file2.toPath().setReadable(false,false)
        println "file1: ${file1.toPath().getPermissions()}"
        println "file2: ${file2.toPath().getPermissions()}"
        then:
        file1.toPath().getPermissions() == file2.toPath().getPermissions()


        expect:
        !Paths.get('none').setReadable(true)

    }

    def testDigitToPerm() {

        expect:
        FilesEx.digitToPerm(1).toString() == '--x'
        FilesEx.digitToPerm(2).toString() == '-w-'
        FilesEx.digitToPerm(3).toString() == '-wx'
        FilesEx.digitToPerm(4).toString() == 'r--'
        FilesEx.digitToPerm(5).toString() == 'r-x'
        FilesEx.digitToPerm(6).toString() == 'rw-'
        FilesEx.digitToPerm(7).toString() == 'rwx'

    }

    def testSetPermission() {
        setup:
        def file1 = File.createTempFile('testfile',null)
        file1.deleteOnExit()

        when:
        file1.setReadable(true,false)
        def ok = file1.toPath().setPermissions(6,4,4)
        then:
        ok
        file1.toPath().getPermissions() == 'rw-r--r--'

        expect:
        !Paths.get('none').setPermissions(6,6,6)

    }

    def testSetPermissionString() {
        setup:
        def file1 = File.createTempFile('testfile',null)
        file1.deleteOnExit()

        when:
        file1.setReadable(true,false)
        def ok = file1.toPath().setPermissions('rw-rw-rw-')
        then:
        ok
        file1.toPath().getPermissions() == 'rw-rw-rw-'

        expect:
        !Paths.get('none').setPermissions('rw-rw-rw-')

    }


    def testReadLink() {
        when:
        def path = Paths.get('/path/to/notExitingFile')
        then:
        path.resolveSymLink() == path

        when:
        def file = Files.createTempFile('testFile',null)
        file.text = 'hola'
        def link1 = Paths.get('link1'); link1.delete()
        def link2 = Paths.get('link2'); link2.delete()
        Files.createSymbolicLink(link1, file)
        Files.createSymbolicLink( link2, link1 )

        then:
        file.text == 'hola'
        link1.text == 'hola'
        link2.text == 'hola'

        file.resolveSymLink() == file
        link1.resolveSymLink() == file
        link2.resolveSymLink() == file

        cleanup:
        file?.delete()
        link1?.delete()
        link2?.delete()
    }

    def testCheckFolder() {

        when:
        def folder1 = Paths.get('some/path')
        folder1.createDirIfNotExists()
        then:
        folder1.exists()

        when:
        def folder2 = Files.createTempDirectory('some-temp-dir')
        folder2.createDirIfNotExists( )
        then:
        folder2.exists()

        when:
        def file3 = Files.createTempFile('some-file',null)
        file3.createDirIfNotExists()
        then:
        thrown(IOException)


        cleanup:
        folder1?.deleteDir()
        folder2?.deleteDir()
        file3.delete()
    }

    def testEmptyAndIsEmpty() {

        when:
        def file = File.createTempFile('hello','file')
        then:
        file.empty()
        //file.isEmpty() == this is tested in NextflowDelegatingMetaClassTest
        when:
        file.text = 'hello'
        then:
        !file.empty()
        //!file.isEmpty() == == this is tested in NextflowDelegatingMetaClassTest

        when:
        def path = Files.createTempFile('hello','path')
        then:
        path.empty()
        // path.isEmpty() == this is tested in NextflowDelegatingMetaClassTest
        when:
        path.text = 'hello'
        then:
        !path.empty()
        // !path.isEmpty() == this is tested in NextflowDelegatingMetaClassTest

        cleanup:
        file?.delete()
        path?.delete()
    }

    def testPathAdd() {

        expect:
        Paths.get('file') + '.txt' == Paths.get('file.txt')
        Paths.get('file') + '' == Paths.get('file')
        Paths.get('/file') + '_name' == Paths.get('/file_name')
        Paths.get('/') + 'file.txt' == Paths.get('/file.txt')
        Paths.get('some/path/file') + '.txt' == Paths.get('some/path/file.txt')
        Paths.get('some/path') + '/file.txt' == Paths.get('some/path/file.txt')
        Paths.get('some/path/') + '/file.txt' == Paths.get('some/path/file.txt')
        Paths.get('some/path/') + '//file.txt' == Paths.get('some/path/file.txt')
        Paths.get('/some/path/') + '//file.txt' == Paths.get('/some/path/file.txt')
    }

    def testPathResolve() {

        expect:
        Paths.get('/') / 'file.txt' == Paths.get('/file.txt')
        Paths.get('file') / 'txt' == Paths.get('file/txt')
        Paths.get('/some/file') / '.txt' == Paths.get('/some/file/.txt')
        Paths.get('/some/file/') / '.txt' == Paths.get('/some/file/.txt')
    }

    def testPathMinus() {

        expect:
        Paths.get('/') -1 == null
        Paths.get('/some/path') -1 == Paths.get('/some')
        Paths.get('/some/path/file.txt') -1 == Paths.get('/some/path')
        Paths.get('/some/path/file.txt') -2 == Paths.get('/some')
        Paths.get('/some/path/file.txt') -3 == Paths.get('/')
        Paths.get('/some/path/file.txt') -4 == null
//        Paths.get('/some/path/file.txt') - 'file.txt' == Paths.get('/some/path')

    }

    def testPathSibling() {

        expect:
        (Paths.get('/some/path') | 'hello') == Paths.get('/some/hello')
        (Paths.get('/some/path/') | 'hello') == Paths.get('/some/hello')
        (Paths.get('/some/path/file.txt') | 'ciao.txt') == Paths.get('/some/path/ciao.txt')

    }

    def testRollFile() {

        given:
        def folder = Files.createTempDirectory('roll-test')
        folder.resolve('A').text = 'a'
        folder.resolve('B').text = 'b0'
        folder.resolve('B.1').text = 'b1'
        folder.resolve('B.2').text = 'b2'
        folder.resolve('C1').text = 'c1'
        folder.resolve('C.2').text = 'c1'

        when:
        def file1 = folder.resolve('A')
        file1.rollFile()
        file1.text = 'ciao'
        then:
        file1.exists()
        file1.text == 'ciao'
        folder.resolve('A.1').text == 'a'
        !folder.resolve('A.2').exists()

        when:
        def file2 = folder.resolve("B")
        file2.rollFile()
        file2.text = 'hello'
        then:
        file2.text == 'hello'
        folder.resolve('B.1').text == 'b0'
        folder.resolve('B.2').text == 'b1'
        folder.resolve('B.3').text == 'b2'
        !folder.resolve('B.4').exists()

        when:
        def file3 = folder.resolve("C")
        file3.rollFile()
        file3.text = 'Ciao'
        then:
        file3.text == 'Ciao'
        folder.resolve('C1').text == 'c1'
        folder.resolve('C.2').text == 'c1'
        !folder.resolve('C.1').exists()

        cleanup:
        folder?.deleteDir()
    }

    def testPathComplete() {

        when:
        def here = Paths.get('.').toAbsolutePath().normalize().toString()
        then:
        FilesEx.complete(Paths.get('./xxx/../hola')).isAbsolute()
        FilesEx.complete(Paths.get('./xxx/../hola')).toString() == "$here/hola"

        when:
        def folder = Files.createTempDirectory('test')
        folder.resolve('file1').text = 'hello'
        Files.createSymbolicLink(folder.resolve('link1'), folder.resolve('file1'))

        then:
        // should NOT resolve symbolic link
        folder.resolve('link1').exists()
        folder.resolve('link1').isSymlink()
        FilesEx.complete( folder.resolve('./xxx/../link1') ) == folder.resolve('link1')

        cleanup:
        folder?.deleteDir()
    }

}
