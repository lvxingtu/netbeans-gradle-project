package org.netbeans.gradle.build;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import org.gradle.api.Action;
import org.gradle.api.JavaVersion;
import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.api.plugins.JavaPluginConvention;
import org.gradle.api.tasks.compile.CompileOptions;
import org.gradle.api.tasks.compile.GroovyCompile;
import org.gradle.api.tasks.compile.JavaCompile;
import org.gradle.jvm.toolchain.JavaToolChain;

public final class CompilerUtils {
    public static JavaVersion getTargetCompatibility(Project project) {
        JavaPluginConvention javaPlugin = project.getConvention().findPlugin(JavaPluginConvention.class);
        return javaPlugin.getTargetCompatibility();
    }

    public static File findToolsJar(Project project, JavaVersion javaVersion) {
        String explicitToolsJarProperty = "jdk" + javaVersion.getMajorVersion() + "ToolsJar";
        if (project.hasProperty(explicitToolsJarProperty)) {
            return new File(project.property(explicitToolsJarProperty).toString().trim());
        }

        String foundToolsJar = null;
        String explicitJavaCompiler = tryGetExplicitJdkCompiler(project, javaVersion);
        if (explicitJavaCompiler != null) {
            foundToolsJar = extractToolsJarFromCompiler(explicitJavaCompiler);
        }

        if (foundToolsJar == null) {
            String javaHome = System.getProperty("java.home");
            foundToolsJar = extractToolsJarFromJavaHome(javaHome);
        }

        if (foundToolsJar == null) {
            throw new IllegalStateException("Unable to find the JDK's tools.jar.");
        }

        return new File(foundToolsJar);
    }

    public static String tryGetCompilerVersion(JavaCompile compileTask) {
        JavaToolChain toolChain = compileTask.getToolChain();
        return toolChain.getVersion();
    }

    public static void configureJavaCompilers(Project project) {
        project.getTasks().withType(JavaCompile.class, new Action<JavaCompile>() {
            @Override
            public void execute(JavaCompile compileTask) {
                configureJavaCompiler(compileTask);
            }
        });
        project.getTasks().withType(GroovyCompile.class, new Action<GroovyCompile>() {
            @Override
            public void execute(GroovyCompile compileTask) {
                configureJavaCompiler(compileTask);
            }
        });
    }

    private static void configureJavaCompiler(final JavaCompile compileTask) {
        configureJavaCompiler(compileTask, compileTask.getOptions());
    }

    private static void configureJavaCompiler(final GroovyCompile compileTask) {
        configureJavaCompiler(compileTask, compileTask.getOptions());
    }

    private static void configureJavaCompiler(final Task compileTask, final CompileOptions compilerOptions) {
        compilerOptions.setEncoding("UTF-8");
        addCompilerArgs(compilerOptions, "-Xlint");

        TaskConfigurations.lazilyConfiguredTask(compileTask, new Action<Task>() {
            @Override
            public void execute(Task task) {
                configureJavacNow(compileTask, compilerOptions);
            }
        });
    }

    public static void addCompilerArgs(CompileOptions options, String... newArgs) {
        List<String> prevArgs = options.getCompilerArgs();

        List<String> args = new ArrayList<>(prevArgs.size() + newArgs.length);
        args.addAll(prevArgs);
        args.addAll(Arrays.asList(newArgs));

        options.setCompilerArgs(args);
    }

    private static void configureJavacNow(Task compileTask, CompileOptions compilerOptions) {
        final JavaVersion targetCompatibility = getTargetCompatibility(compileTask.getProject());
        if (Objects.equals(JavaVersion.current(), targetCompatibility)) {
            return;
        }

        final Project project = compileTask.getProject();
        String explicitJavaCompiler = tryGetExplicitJdkCompiler(project, targetCompatibility);
        if (explicitJavaCompiler != null) {
            compilerOptions.setFork(true);
            compilerOptions.getForkOptions().setExecutable(explicitJavaCompiler);
        }
        else {
            compileTask.doFirst(new Action<Task>() {
                @Override
                public void execute(Task t) {
                    String jdkProperty = getJdkPropertyName(targetCompatibility);
                    project.getLogger().warn("Warning: " + jdkProperty + " property is missing and"
                            + " not compiling with Java " + targetCompatibility
                            + ". Using " + JavaVersion.current());
                }
            });
        }
    }

    private static Path subPath(Path base, String... subPaths) {
        if (base == null) {
            return null;
        }

        Path result = base;
        for (String subPath: subPaths) {
            result = result.resolve(subPath);
        }
        return result;
    }

    private static Path getParent(Path base, int level) {
        Path result = base;
        for (int i = 0; i < level && result != null; i++) {
            result = result.getParent();
        }
        return result;
    }

    private static String extractToolsJarFromJDKHome(Path jdkHome) {
        Path toolsJar = subPath(jdkHome, "lib", "tools.jar");
        if (toolsJar == null) {
            return null;
        }

        if (!Files.isRegularFile(toolsJar)) {
            return null;
        }

        return toolsJar.toString();
    }

    private static String extractToolsJarFromJavaHome(String javaHome) {
        if (javaHome == null) {
            return null;
        }

        Path jdkHome = Paths.get(javaHome).getParent();
        return extractToolsJarFromJDKHome(jdkHome);
    }

    private static String extractToolsJarFromCompiler(String javac) {
        if (javac == null) {
            return null;
        }

        Path jdkHome = getParent(Paths.get(javac), 2);
        return extractToolsJarFromJDKHome(jdkHome);
    }

    private static String getJdkPropertyName(JavaVersion javaVersion) {
        return "jdk" + javaVersion.getMajorVersion() + "Compiler";
    }

    private static String tryGetExplicitJdkCompiler(Project project, JavaVersion javaVersion) {
        String jdkProperty = getJdkPropertyName(javaVersion);
        if (project.hasProperty(jdkProperty)) {
            return project.property(jdkProperty).toString().trim();
        }
        else {
            return null;
        }
    }

    private CompilerUtils() {
        throw new AssertionError();
    }

}
