/*
 * Copyright (c) 2018 by Andrew Charneski.
 *
 * The author licenses this file to you under the
 * Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance
 * with the License.  You may obtain a copy
 * of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import com.google.inject.AbstractModule;
import org.apache.commons.io.FileUtils;
import org.apache.maven.RepositoryUtils;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.InvalidRepositoryException;
import org.apache.maven.artifact.handler.manager.ArtifactHandlerManager;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.repository.ArtifactRepositoryPolicy;
import org.apache.maven.artifact.repository.MavenArtifactRepository;
import org.apache.maven.artifact.repository.layout.ArtifactRepositoryLayout;
import org.apache.maven.artifact.repository.layout.DefaultRepositoryLayout;
import org.apache.maven.artifact.resolver.ArtifactResolutionRequest;
import org.apache.maven.artifact.resolver.ArtifactResolutionResult;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.Plugin;
import org.apache.maven.model.Repository;
import org.apache.maven.project.*;
import org.apache.maven.repository.ArtifactDoesNotExistException;
import org.apache.maven.repository.ArtifactTransferFailedException;
import org.apache.maven.repository.ArtifactTransferListener;
import org.apache.maven.repository.RepositorySystem;
import org.apache.maven.repository.internal.MavenRepositorySystemUtils;
import org.apache.maven.settings.Mirror;
import org.apache.maven.settings.Proxy;
import org.apache.maven.settings.Server;
import org.codehaus.plexus.ContainerConfiguration;
import org.codehaus.plexus.DefaultContainerConfiguration;
import org.codehaus.plexus.DefaultPlexusContainer;
import org.codehaus.plexus.PlexusContainerException;
import org.codehaus.plexus.classworlds.ClassWorld;
import org.codehaus.plexus.component.repository.exception.ComponentLookupException;
import org.eclipse.aether.ConfigurationProperties;
import org.eclipse.aether.DefaultRepositoryCache;
import org.eclipse.aether.DefaultRepositorySystemSession;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.internal.impl.DefaultRepositorySystem;
import org.eclipse.aether.repository.LocalRepository;
import org.eclipse.aether.repository.RepositoryPolicy;
import org.eclipse.aether.resolution.ResolutionErrorPolicy;
import org.eclipse.aether.util.repository.SimpleResolutionErrorPolicy;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.FileASTRequestor;
import org.eclipse.jdt.internal.compiler.classfmt.ClassFileConstants;
import org.eclipse.jdt.internal.compiler.impl.CompilerOptions;
import org.slf4j.ILoggerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.stream.Stream;

public class SimpleMavenProject {
  public static final File repositoryLocation = new File(System.getProperty("user.home"), ".m2/repository");
  protected static final Logger logger = LoggerFactory.getLogger(SimpleMavenProject.class);
  public final DefaultPlexusContainer container;
  public final DefaultRepositorySystemSession session;
  public final MavenProject project;
  public final String projectRoot;
  
  public SimpleMavenProject(final String projectRoot) throws IOException, PlexusContainerException, ComponentLookupException, ProjectBuildingException {
    this.projectRoot = projectRoot;
    Map<Object, Object> configProps = new LinkedHashMap<>();
    configProps.put(ConfigurationProperties.USER_AGENT, "Maven+SimiaCryptus");
    configProps.put(ConfigurationProperties.INTERACTIVE, false);
    configProps.putAll(System.getProperties());
    this.container = getPlexusContainer(repositoryLocation);
    this.session = getSession(repositoryLocation, false, configProps, container);
    this.project = getMavenProject(container, session);
  }
  
  public static void main(String[] args) throws Exception {
    SimpleMavenProject maven = new SimpleMavenProject(args[0]);
    Stream.concat(
      maven.project.getTestCompileSourceRoots().stream(),
      maven.project.getCompileSourceRoots().stream()
    ).forEach(sourceRoot -> {
      try {
        logger.info("Code: " + new File(sourceRoot).getCanonicalFile());
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
    });
    maven.resolve().getDependencies().forEach(dependency -> {
      try {
        logger.info("Dep: " + dependency.getArtifact().getFile().getCanonicalFile());
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
    });
  }
  
  public HashMap<String, CompilationUnit> parse() throws IOException, PlexusContainerException, ComponentLookupException, ProjectBuildingException, DependencyResolutionException {
    final String root = projectRoot;
    ASTParser astParser = ASTParser.newParser(AST.JLS9);
    astParser.setKind(ASTParser.K_EXPRESSION);
    astParser.setResolveBindings(true);
    HashMap<String, String> compilerOptions = new HashMap<>();
    compilerOptions.put(CompilerOptions.OPTION_Source, CompilerOptions.versionFromJdkLevel(ClassFileConstants.JDK1_8));
    compilerOptions.put(CompilerOptions.OPTION_DocCommentSupport, CompilerOptions.ENABLED);
    astParser.setCompilerOptions(compilerOptions);
    String[] classpathEntries = resolve().getDependencies().stream().map(x -> x.getArtifact().getFile().getAbsolutePath()).toArray(i -> new String[i]);
    String[] sourcepathEntries = Stream.concat(
      project.getTestCompileSourceRoots().stream(),
      project.getCompileSourceRoots().stream()
    ).toArray(i -> new String[i]);
    astParser.setEnvironment(classpathEntries, sourcepathEntries, null, true);
    HashMap<String, CompilationUnit> results = new HashMap<>();
    astParser.createASTs(
      FileUtils.listFiles(new File(root), new String[]{"java"}, true).stream().map(x -> x.getAbsolutePath()).toArray(i -> new String[i]),
      null,
      new String[]{},
      new FileASTRequestor() {
        @Override
        public void acceptAST(final String source, final CompilationUnit ast) {
          results.put(source, ast);
        }
      },
      new NullProgressMonitor()
    );
    
    return results;
  }
  
  public DependencyResolutionResult resolve() throws IOException, PlexusContainerException, org.codehaus.plexus.component.repository.exception.ComponentLookupException, ProjectBuildingException, DependencyResolutionException {
    return container.lookup(ProjectDependenciesResolver.class).resolve(new DefaultDependencyResolutionRequest().setRepositorySession(session).setMavenProject(project));
  }
  
  public MavenProject getMavenProject(final DefaultPlexusContainer container, final DefaultRepositorySystemSession session) throws ProjectBuildingException, org.codehaus.plexus.component.repository.exception.ComponentLookupException {
    DefaultProjectBuildingRequest request = new DefaultProjectBuildingRequest();
    request.setRepositorySession(session);
    return container.lookup(ProjectBuilder.class).build(new File(projectRoot, "pom.xml"), request).getProject();
  }
  
  @Nonnull
  public DefaultRepositorySystemSession getSession(final File repositoryLocation, final boolean isOffline, final Map<Object, Object> configProps, final DefaultPlexusContainer container) throws org.codehaus.plexus.component.repository.exception.ComponentLookupException {
    DefaultRepositorySystemSession session = MavenRepositorySystemUtils.newSession();
    session.setConfigProperties(configProps);
    session.setCache(new DefaultRepositoryCache());
    session.setOffline(isOffline);
    session.setUpdatePolicy(RepositoryPolicy.UPDATE_POLICY_ALWAYS);
    session.setResolutionErrorPolicy(new SimpleResolutionErrorPolicy(ResolutionErrorPolicy.CACHE_NOT_FOUND, ResolutionErrorPolicy.CACHE_NOT_FOUND));
    session.setArtifactTypeRegistry(RepositoryUtils.newArtifactTypeRegistry(container.lookup(ArtifactHandlerManager.class)));
    session.setLocalRepositoryManager(container.lookup(DefaultRepositorySystem.class).newLocalRepositoryManager(session, new LocalRepository(repositoryLocation)));
    return session;
  }
  
  @Nonnull
  public DefaultPlexusContainer getPlexusContainer(final File repositoryLocation) throws IOException, PlexusContainerException {
    DefaultRepositoryLayout defaultRepositoryLayout = new DefaultRepositoryLayout();
    ArtifactRepositoryPolicy repositoryPolicy = new ArtifactRepositoryPolicy(true, ArtifactRepositoryPolicy.UPDATE_POLICY_NEVER, ArtifactRepositoryPolicy.CHECKSUM_POLICY_WARN);
    String url = "file://" + repositoryLocation.getCanonicalPath();
    ArtifactRepository repository = new MavenArtifactRepository("central", url, defaultRepositoryLayout, repositoryPolicy, repositoryPolicy);
    ClassWorld classWorld = new ClassWorld("plexus.core", Thread.currentThread().getContextClassLoader());
    ContainerConfiguration configuration = new DefaultContainerConfiguration()
      .setClassWorld(classWorld).setRealm(classWorld.getClassRealm(null))
      .setClassPathScanning("index").setAutoWiring(true).setJSR250Lifecycle(true).setName("maven");
    return new DefaultPlexusContainer(configuration, new SimpleModule(repository));
  }
  
  private static class SimpleModule extends AbstractModule {
    private final ArtifactRepository repository;
    
    public SimpleModule(final ArtifactRepository repository) {this.repository = repository;}
    
    protected void configure() {
      this.bind(ILoggerFactory.class).toInstance(LoggerFactory.getILoggerFactory());
      this.bind(RepositorySystem.class).toInstance(new RepositorySystem() {
        @Override
        public Artifact createArtifact(final String groupId, final String artifactId, final String version, final String packaging) {
          if (0 < 1) throw new RuntimeException("Not Implemented");
          return null;
        }
        
        @Override
        public Artifact createArtifact(final String groupId, final String artifactId, final String version, final String scope, final String type) {
          if (0 < 1) throw new RuntimeException("Not Implemented");
          return null;
        }
        
        @Override
        public Artifact createProjectArtifact(final String groupId, final String artifactId, final String version) {
          if (0 < 1) throw new RuntimeException("Not Implemented");
          return null;
        }
        
        @Override
        public Artifact createArtifactWithClassifier(final String groupId, final String artifactId, final String version, final String type, final String classifier) {
          if (0 < 1) throw new RuntimeException("Not Implemented");
          return null;
        }
        
        @Override
        public Artifact createPluginArtifact(final Plugin plugin) {
          if (0 < 1) throw new RuntimeException("Not Implemented");
          return null;
        }
        
        @Override
        public Artifact createDependencyArtifact(final Dependency dependency) {
          if (0 < 1) throw new RuntimeException("Not Implemented");
          return null;
        }
        
        @Override
        public ArtifactRepository buildArtifactRepository(final Repository r) throws InvalidRepositoryException {
          return repository;
        }
        
        @Override
        public ArtifactRepository createDefaultRemoteRepository() throws InvalidRepositoryException {
          if (0 < 1) throw new RuntimeException("Not Implemented");
          return null;
        }
        
        @Override
        public ArtifactRepository createDefaultLocalRepository() throws InvalidRepositoryException {
          if (0 < 1) throw new RuntimeException("Not Implemented");
          return null;
        }
        
        @Override
        public ArtifactRepository createLocalRepository(final File localRepository) throws InvalidRepositoryException {
          if (0 < 1) throw new RuntimeException("Not Implemented");
          return null;
        }
        
        @Override
        public ArtifactRepository createArtifactRepository(final String id, final String url1, final ArtifactRepositoryLayout repositoryLayout, final ArtifactRepositoryPolicy snapshots, final ArtifactRepositoryPolicy releases) {
          if (0 < 1) throw new RuntimeException("Not Implemented");
          return null;
        }
        
        @Override
        public List<ArtifactRepository> getEffectiveRepositories(final List<ArtifactRepository> repositories) {
          return Arrays.asList(repository);
        }
        
        @Override
        public Mirror getMirror(final ArtifactRepository repository1, final List<Mirror> mirrors) {
          if (0 < 1) throw new RuntimeException("Not Implemented");
          return null;
        }
        
        @Override
        public void injectMirror(final List<ArtifactRepository> repositories, final List<Mirror> mirrors) {
        }
        
        @Override
        public void injectProxy(final List<ArtifactRepository> repositories, final List<Proxy> proxies) {
        
        }
        
        @Override
        public void injectAuthentication(final List<ArtifactRepository> repositories, final List<Server> servers) {
        
        }
        
        @Override
        public void injectMirror(final RepositorySystemSession session, final List<ArtifactRepository> repositories) {
        
        }
        
        @Override
        public void injectProxy(final RepositorySystemSession session, final List<ArtifactRepository> repositories) {
        
        }
        
        @Override
        public void injectAuthentication(final RepositorySystemSession session, final List<ArtifactRepository> repositories) {
        
        }
        
        @Override
        public ArtifactResolutionResult resolve(final ArtifactResolutionRequest request) {
          if (0 < 1) throw new RuntimeException("Not Implemented");
          return null;
        }
        
        @Override
        public void publish(final ArtifactRepository repository1, final File source, final String remotePath, final ArtifactTransferListener transferListener) throws ArtifactTransferFailedException {
        
        }
        
        @Override
        public void retrieve(final ArtifactRepository repository1, final File destination, final String remotePath, final ArtifactTransferListener transferListener) throws ArtifactTransferFailedException, ArtifactDoesNotExistException {
        
        }
      });
    }
  }
}
