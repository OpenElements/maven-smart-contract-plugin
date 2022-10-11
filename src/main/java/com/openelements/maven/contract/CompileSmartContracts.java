package com.openelements.maven.contract;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

@Mojo(name = "compile", defaultPhase = LifecyclePhase.COMPILE)
public class CompileSmartContracts extends AbstractMojo {

	@Parameter(defaultValue = "${project}", required = true, readonly = true)
	MavenProject project;

	public void execute() throws MojoExecutionException {
		try {
			getLog().debug("Starting to compile smart contracts");

			final Path contractsSourceDir = Paths.get(project.getBasedir().getAbsolutePath(), "src/main/contracts");
			final Path contractsTargetDir = Paths.get(project.getBuild().getOutputDirectory(), "contracts");

			if (!contractsSourceDir.toFile().exists() || !contractsSourceDir.toFile().isDirectory()) {
				getLog().warn("Directory src/main/contracts does not exist. Will end tasks");
				return;
			}

			final Set<Path> contracts = new HashSet<>();
			try (final Stream<Path> stream = Files.walk(contractsSourceDir)) {
				stream.filter(Files::isRegularFile)
						.filter(file -> file.toFile().getName().toLowerCase().endsWith(".sol"))
						.forEach(file -> contracts.add(file));
			}
			getLog().debug("Found " + contracts.size() + " contracts");

			contracts.stream().forEach(contract -> {
				try {
					final Path targetDirForContract = Optional.ofNullable(
									contractsSourceDir.relativize(contract).getParent())
							.map(dir -> contractsTargetDir.resolve(dir))
							.orElse(contractsTargetDir);
					getLog().debug(
							"Compiling " + contract.toFile().getAbsolutePath() + " to " + targetDirForContract.toFile().getAbsolutePath());

					final ProcessBuilder processBuilder = new ProcessBuilder("solc", "--overwrite", "-o",
							targetDirForContract.toFile().getAbsolutePath(), "--bin", "--abi",
							contract.toFile().getAbsolutePath());
					processBuilder.directory(contractsSourceDir.toFile());
					processBuilder.redirectErrorStream(true);
					processBuilder.inheritIO();
					final Process process = processBuilder.start();
					final int result = process.waitFor();
					if (result != 0) {
						throw new IllegalStateException(
								"Error in compiling " + contract.toFile().getAbsolutePath() + ". Process ended with " + result);
					}
				} catch (final Exception e) {
					throw new IllegalStateException("Error in compiling " + contract.toFile().getAbsolutePath(), e);
				}
			});
		} catch (final Exception e) {
			throw new MojoExecutionException("Error in executing task", e);
		}
	}

}