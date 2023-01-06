package com.openelements.maven.contract;

import com.openelements.jsolidity.compiler.SolidityCompiler;
import com.openelements.jsolidity.compiler.SolidityCompilerOutputType;
import com.openelements.jsolidity.compiler.SolidityCompilerResult;
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

	public static final String SRC_PATH = "src/main/contracts";

	public static final String TARGET_PATH = "src/main/resources";

	public static final String SOL_FILE_SUFFIX = ".sol";

	public static final String BIN_FILE_SUFFIX = ".bin";

	public static final String ABI_FILE_SUFFIX = ".abi";

	@Parameter(defaultValue = "${project}", required = true, readonly = true)
	MavenProject project;

	public void execute() throws MojoExecutionException {
		try {
			getLog().debug("Starting to compile smart contracts");

			final Path contractsSourceDir = Paths.get(project.getBasedir().getAbsolutePath(), SRC_PATH);
			final Path contractsTargetDir = Paths.get(project.getBasedir().getAbsolutePath(), TARGET_PATH);

			if (!contractsSourceDir.toFile().exists() || !contractsSourceDir.toFile().isDirectory()) {
				getLog().warn("Directory " + SRC_PATH + " does not exist. Will end tasks");
				return;
			}

			final Set<Path> contracts = new HashSet<>();
			try (final Stream<Path> stream = Files.walk(contractsSourceDir)) {
				stream.filter(Files::isRegularFile)
						.filter(file -> file.toFile().getName().toLowerCase().endsWith(SOL_FILE_SUFFIX))
						.forEach(file -> contracts.add(file));
			}
			getLog().debug("Found " + contracts.size() + " contracts");

			contracts.stream().forEach(contract -> {
				try {
					final Path targetDirForContract = Optional.ofNullable(
									contractsSourceDir.relativize(contract).getParent())
							.map(dir -> contractsTargetDir.resolve(dir))
							.orElse(contractsTargetDir);
					targetDirForContract.toFile().mkdirs();

					getLog().debug(
							"Compiling " + contract.toFile().getAbsolutePath() + " to " + targetDirForContract.toFile().getAbsolutePath());

					SolidityCompiler compiler = new SolidityCompiler(contractsSourceDir.toFile(), contract.toFile().getAbsolutePath(), Set.of(SolidityCompilerOutputType.values()));
					SolidityCompilerResult result = compiler.compile();

					final String contractName = contract.toFile().getName().substring(0, contract.toFile().getName().length() - SOL_FILE_SUFFIX.length());
					final Path binFile = Path.of(targetDirForContract.toFile().getAbsolutePath(), contractName + BIN_FILE_SUFFIX);
					Files.writeString(binFile, result.contracts().iterator().next().bin());

					final Path abiFile = Path.of(targetDirForContract.toFile().getAbsolutePath(), contractName + ABI_FILE_SUFFIX);
					Files.writeString(abiFile, result.contracts().iterator().next().rawAbi());

				} catch (final Exception e) {
					throw new IllegalStateException("Error in compiling " + contract.toFile().getAbsolutePath(), e);
				}
			});
		} catch (final Exception e) {
			throw new MojoExecutionException("Error in executing task", e);
		}
	}

}