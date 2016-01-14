package com.nnarendravijay

import org.slf4j.Logger
import org.slf4j.LoggerFactory;

class DockerMachineManagement {

    private static Logger logger = LoggerFactory.getLogger(DockerMachineManagement.class);

    static def getDefaultMachine() {

        Process process = runMachineCmd('/usr/local/bin/docker-machine', 'status', 'default')
        List<String> soutAndSerr = process.inputStream.readLines()
        logCmdResult(soutAndSerr)
        if (!soutAndSerr.contains('Running')) {

            if (soutAndSerr.contains('Stopped')) {
                logger.info('Default machine is stopped; Starting it now')
                process = runMachineCmd('/usr/local/bin/docker-machine', 'start', 'default')
                logCmdResult(process.inputStream.readLines())
                assert process.exitValue() == 0
                Thread.sleep(5000)
            } else if (soutAndSerr.stream().anyMatch({ out -> out.contains('Host does not exist') })) {
                logger.info('Default machine not found; Trying to create one')
                process = runMachineCmd('/usr/local/bin/docker-machine', 'create', '--driver', 'VirtualBox', 'default')
                logCmdResult(process.inputStream.readLines())
                assert process.exitValue() == 0
            } else {
                logger.info('The error code while finding machine status is NOT understood, removing and creating new ')
                runMachineCmd('/usr/local/bin/docker-machine', 'rm', 'default')
                process = runMachineCmd('/usr/local/bin/docker-machine', 'create', '--driver', 'VirtualBox', 'default')
                logCmdResult(process.inputStream.readLines())
                assert process.exitValue() == 0
            }
        }
        logger.info("Trying to find the IP of the default machine")
        process = runMachineCmd('/usr/local/bin/docker-machine', 'ip', 'default')
        soutAndSerr = process.inputStream.readLines()
        logCmdResult(soutAndSerr)
        assert process.exitValue() == 0
        return soutAndSerr.stream().findFirst();
    }

    static def getCertPath() {

        logger.info('Finding the CertDir of the Docker Machine for the Client to interact with it')
        Process process = runMachineCmd('/usr/local/bin/docker-machine', 'inspect',
                                                    "--format='{{.HostOptions.AuthOptions.CertDir}}'", 'default')
        List<String> soutAndSerr = process.inputStream.readLines()
        logCmdResult(soutAndSerr)
        assert process.exitValue() == 0
        Optional<String> certPath = soutAndSerr.stream().findFirst();
        assert certPath.isPresent()
        return certPath.get().trim().replace("'", "")

    }

    static def resetDefaultMachine() {
        logger.info("Removing the existing default machine and creating new one")
        runMachineCmd('/usr/local/bin/docker-machine', 'rm', 'default')
        Process process = runMachineCmd('/usr/local/bin/docker-machine', 'create', '--driver', 'VirtualBox', 'default')
        logCmdResult(process.inputStream.readLines())
        assert process.exitValue() == 0

    }

    static def runMachineCmd(String... cmdWithArgs) {
        ProcessBuilder p = new ProcessBuilder(cmdWithArgs)
        p.redirectErrorStream(true)
        String newPath = p.environment().get("PATH") + ":/usr/local/bin";
        p.environment().put("PATH", newPath)
        def process = p.start();
        process.waitFor();
        return process
    }

    static def logCmdResult(List<String> soutAndSerr) {
        logger.debug("SOUT or SERR for the last command: ")
        soutAndSerr.stream().forEach({ out ->
            logger.debug(out);
        })

    }
}