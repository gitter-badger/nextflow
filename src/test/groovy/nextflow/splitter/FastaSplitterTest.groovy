package nextflow.splitter

import nextflow.Channel
import spock.lang.Specification
/**
 *
 * @author Paolo Di Tommaso <paolo.ditommaso@gmail.com>
 */
class FastaSplitterTest extends Specification {



    def testFastaRecord() {
        def fasta = /
            ;

            >gi|5524211|gb|AAD44166.1| cytochrome b
            NLFVALYDFVASGDNTLSITKGEKLRVLGYNHNGEWCEAQTKNGQGWVPS
            NYITPVN
            /.stripIndent()

        expect:
        FastaSplitter.parseFastaRecord(fasta, [id:true])
                .id == 'gi|5524211|gb|AAD44166.1|'

        FastaSplitter.parseFastaRecord(fasta, [sequence:true])
                .sequence == 'NLFVALYDFVASGDNTLSITKGEKLRVLGYNHNGEWCEAQTKNGQGWVPS\nNYITPVN\n'

        FastaSplitter.parseFastaRecord(fasta, [sequence:true, width: 20 ])
                .sequence == 'NLFVALYDFVASGDNTLSIT\nKGEKLRVLGYNHNGEWCEAQ\nTKNGQGWVPSNYITPVN\n'

        FastaSplitter.parseFastaRecord(fasta, [header:true])
                .header == 'gi|5524211|gb|AAD44166.1| cytochrome b'

        FastaSplitter.parseFastaRecord(fasta, [seqString:true])
                .seqString == 'NLFVALYDFVASGDNTLSITKGEKLRVLGYNHNGEWCEAQTKNGQGWVPSNYITPVN'

        FastaSplitter.parseFastaRecord(fasta, [text:true])
                .text == fasta

        FastaSplitter.parseFastaRecord(fasta, [desc:true])
                .desc == 'cytochrome b'

    }



    def testSplitFasta () {

        when:
        def fasta = """\
                >prot1
                LCLYTHIGRNIYYGS1
                EWIWGGFSVDKATLN
                ;
                ; comment
                ;
                >prot2
                LLILILLLLLLALLS
                GLMPFLHTSKHRSMM
                IENY
                """.stripIndent()

        def count = 0
        def q = new FastaSplitter().options(each:{ count++; it }).target(fasta).channel()

        then:
        count == 2
        q.val == ">prot1\nLCLYTHIGRNIYYGS1\nEWIWGGFSVDKATLN\n"
        q.val == ">prot2\nLLILILLLLLLALLS\nGLMPFLHTSKHRSMM\nIENY\n"
        q.val == Channel.STOP

    }

    def testSplitFastaRecord() {

        given:
        def fasta = """\
                >1aboA
                NLFVALYDFVASGDNTLSITKGEKLRVLGYNHNGEWCEAQTKNGQGWVPS
                NYITPVN
                >1ycsB
                KGVIYALWDYEPQNDDELPMKEGDCMTIIHREDEDEIEWWWARLNDKEGY
                VPRNLLGLYP
                ; comment
                >1pht
                GYQYRALYDYKKEREEDIDLHLGDILTVNKGSLVALGFSDGQEARPEEIG
                WLNGYNETTGERGDFPGTYVE
                YIGRKKISP
                """.stripIndent()

        when:
        def q = new FastaSplitter().options(record: [id:true, seqString:true]).target(fasta) .channel()

        then:
        q.val == [id:'1aboA', seqString: 'NLFVALYDFVASGDNTLSITKGEKLRVLGYNHNGEWCEAQTKNGQGWVPSNYITPVN']
        q.val == [id:'1ycsB', seqString: 'KGVIYALWDYEPQNDDELPMKEGDCMTIIHREDEDEIEWWWARLNDKEGYVPRNLLGLYP']
        q.val == [id:'1pht', seqString: 'GYQYRALYDYKKEREEDIDLHLGDILTVNKGSLVALGFSDGQEARPEEIGWLNGYNETTGERGDFPGTYVEYIGRKKISP']
        q.val == Channel.STOP

    }

    def testSplitFastaFile () {

        setup:
        def file = File.createTempFile('chunk','test')
        file.deleteOnExit()
        def fasta = """\
                >prot1
                AA
                >prot2
                BB
                CC
                >prot3
                DD
                >prot4
                EE
                FF
                GG
                >prot5
                LL
                NN
                """.stripIndent()


        when:
        def result = new FastaSplitter().options(by:2).target(fasta).list()

        then:
        result[0] == ">prot1\nAA\n>prot2\nBB\nCC\n"
        result[1] == ">prot3\nDD\n>prot4\nEE\nFF\nGG\n"
        result[2] == ">prot5\nLL\nNN\n"


        when:
        def result2 = new FastaSplitter()
                .options(record: [id: true, seqString: true], each:{ [ it.id, it.seqString.size() ]} )
                .target(fasta)
                .list()

        then:
        result2[0] == [ 'prot1', 2 ]
        result2[1] == [ 'prot2', 4 ]
        result2[2] == [ 'prot3', 2 ]
        result2[3] == [ 'prot4', 6 ]
        result2[4] == [ 'prot5', 4 ]
        result2.size() == 5

    }




}
