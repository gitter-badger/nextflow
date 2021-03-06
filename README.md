Nextflow
========

*"Dataflow variables are spectacularly expressive in concurrent programming"*
<br>[Henri E. Bal , Jennifer G. Steiner , Andrew S. Tanenbaum](http://citeseerx.ist.psu.edu/viewdoc/summary?doi=10.1.1.145.7873)

Rationale
---------

With the arise of big data, techniques to analyse and run experiments on large datasets are increasingly necessary.

Parallelization and distributed computing are the best ways to tackle this kind of problem, but the tools commonly available
to the bioinformaticians community, traditionally lack good support for these techniques, or provide a model that fits
badly with the specific requirements in the bioinformatics domain and, most of the time, require the knowledge
of complex tools or low-level APIs.

Nextflow framework is based on the *dataflow* programming model, which greatly simplifies writing parallel and distributed pipelines
without adding unnecessary complexity and letting you concentrate on the flow of data, i.e. the functional logic of the application/algorithm.

It doesn't aim to be another pipeline scripting language yet, but it is built around the idea that the Linux platform 
is the *lingua franca* of data science, since it provides many simple command line and scripting tools, which by themselves 
are powerful, but when chained together facilitate complex data manipulations. 

In practice, this means that a Nextflow script is defined by composing  many different processes. 
Each process can be written in any scripting language that can be executed by the Linux platform (BASH, Perl, Ruby, Python, etc), 
to which is added the ability to coordinate and synchronize the processes execution by simply specifying their inputs and outputs.   

Quick start
-----------

Nextflow does not require any installation procedure, just download the distribution package by copying and pasting
this command in your terminal:

    wget -qO- get.nextflow.io | bash

It creates the ``nextflow`` executable file in the current directory. You may want to move it to a folder accessible from your ``$PATH``.

Create a file named `hello.nf` with the following content and copy it to the path where you downloaded the Nextflow package.

    process sayHello {
    
        """
        printf 'Hello world! \n'
        """
    }



Launch the above example by typing the following command on your terminal console:

    ./nextflow run -process.echo true hello.nf


Congratulations! You have just run your first program with Nextflow.


Something more useful
---------------------

Let's see a more real example: execute a BLAST search, get the top 10 hits, extract the found protein sequences and align them.

Copy the following example into a file named `pipeline.nf` .


    params.query = "$HOME/sample.fa"
    params.db = "$HOME/tools/blast-db/pdb/pdb"

    process blast {
        output:
         file top_hits

        """
        blastp -db ${params.db} -query ${params.query} -outfmt 6 > blast_result
        cat blast_result | head -n 10 | cut -f 2 > top_hits
        """
    }

    process extract {
        input:
         file top_hits
        output:
         file sequences

        "blastdbcmd -db ${params.db} -entry_batch $top_hits > sequences"
    }

    process align {
        input:
         file sequences
        echo true

        "t_coffee $sequences 2>&- | tee align_result"
    }


The `input` and `output` declarations in each process, define what it is expecting to receive as input and what file(s)
are going to be produced as output.

Since the two variables `query` and `db` are prefixed by the `params` qualifier, their values can be overridden quickly
when the script is launched, by simply adding them on the Nextflow command line and prefixing them with the `--` characters.
For example:

    $ ./nextflow run pipeline.nf --db=/path/to/blast/db --query=/path/to/query.fasta


Mixing scripting languages
--------------------------

Processes in your pipeline can be written in any scripting language supported by the underlying Linux platform. To use a scripting
other than Linux BASH (e.g. Perl, Python, Ruby, R, etc), simply start your process script with the corresponding
<a href='http://en.wikipedia.org/wiki/Shebang_(Unix)' target='_bank'>shebang</a> declaration. For example:

    process perlStuff {

        """
        #!/usr/bin/env perl

        print 'Hi there!' . '\n';
        """
    }

    process pyStuff {
        """
        #!/usr/bin/env python

        x = 'Hello'
        y = 'world!'
        print "%s - %s" % (x,y)
        """
    }


Cluster Resource Managers support
---------------------------------

*Nextflow* provides an abstraction between the pipeline functional logic and the underlying processing system. 
Thus it is possible to write your pipeline once and have it running on your computer or a cluster resource
manager without modifying it. 

Currently the following clusters are supported: 
  
  + Oracle Grid Engine (SGE)
  + Platform LSF
  + SLURM (beta)
  + PBS/Torque (beta)


By default processes are parallelized by spanning multiple threads in the machine where the pipeline is launched.

To submit the execution to a SGE cluster create a file named `nextflow.config`, in the directory
where the pipeline is going to be launched, with the following content: 

    process {
      executor='sge'
      queue='<your execution queue>'
    }

In doing that, processes will be executed as SGE jobs by using the `qsub` command, and so your pipeline will behave like any 
other SGE job script, with the benefit that *Nextflow* will automatically and transparently manage the processes 
synchronisation, file(s) staging/un-staging, etc.  

Alternatively the same declaration can be defined in the file `$HOME/.nextflow/config`, which is supposed to hold 
the global *Nextflow* configuration.


Cloud support
-------------

*Nextflow* provides an experimental support for [DNAnexus](http://www.dnanexus.com) cloud platform.

Since this requires some extra runtime dependencies, to have Nextflow running on the DNAnexus you will need
to compile and build *Nextflow* from source using the following command:

    ./gradlew dnanexus


Read more about the building procedure in the following section.



Build from source
-----------------

*Nextflow* is written in [Groovy](http://groovy.codehaus.org) (a scripting language for the JVM). A precompiled,
ready-to-run, package is available at the [Github releases page](https://github.com/nextflow-io/nextflow/releases),
thus it is not necessary to compile it in order to use it.

If you are interested in modifying the source code, or contributing to the project, it worth knowing that 
the build process is based on the [Gradle](http://www.gradle.org/) build automation system. 

You can compile *Nextflow* by typing the following command in the project home directory on your computer:

    $ ./gradlew compile

The very first time you run it, it will automatically download all the libraries required by the build process. 
It may take some minutes to complete.

When complete, execute the program by using the `launch.sh` script in the project directory.

The self-contained runnable Nextflow package can be created by using the following command:

    $ ./gradlew pack

Note: if the compilation stops reporting the error: `java.lang.VerifyError: Bad <init> method call from inside of a branch`,
this is due to a bug affecting the following Java JDK:

- 1.7.0 update 55
- 1.7.0 update 65
- 1.7.0 update 67
- 1.8.0 update 11
- 1.8.0 update 20

Upgrade to a newer JDK to avoid to this issue. Alternatively a possible workaround is to define the following variable
in your environment:

    _JAVA_OPTIONS='-Xverify:none'

Read more at these links:

- https://bugs.openjdk.java.net/browse/JDK-8051012
- https://jira.codehaus.org/browse/GROOVY-6951


Required dependencies
---------------------

Java 7 or higher


Documentation
--------------

Nextflow documentation is available at this link http://docs.nextflow.io

Community
----------

You can post questions, or report problems by using the Nextflow Google group available
at this link https://groups.google.com/forum/#!forum/nextflow

Build servers 
--------------

  * [Travis-CI](https://travis-ci.org/nextflow-io/nextflow) 
  * [Groovy Joint build](http://ci.groovy-lang.org/project.html?projectId=JointBuilds_Nextflow&guest=1) 

License
-------

The *Nextflow* framework source code is released under the GNU GPL3 License.


Credits
-------

Nextflow is built on two great pieces of open source software, namely <a href='http://groovy.codehaus.org' target='_blank'>Groovy</a>
and <a href='http://www.gpars.org/' target='_blank'>Gpars</a>

YourKit is kindly supporting this open source project with its full-featured Java Profiler.
Read more http://www.yourkit.com

