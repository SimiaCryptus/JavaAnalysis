/*
 * Copyright (c) 2019 by Andrew Charneski.
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

package com.simiacryptus.devutil;

import com.google.inject.AbstractModule;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.repository.ArtifactRepositoryPolicy;
import org.apache.maven.artifact.repository.layout.ArtifactRepositoryLayout;
import org.apache.maven.artifact.resolver.ArtifactResolutionRequest;
import org.apache.maven.artifact.resolver.ArtifactResolutionResult;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.Plugin;
import org.apache.maven.model.Repository;
import org.apache.maven.repository.ArtifactTransferListener;
import org.apache.maven.repository.RepositorySystem;
import org.apache.maven.settings.Mirror;
import org.apache.maven.settings.Proxy;
import org.apache.maven.settings.Server;
import org.eclipse.aether.RepositorySystemSession;
import org.slf4j.ILoggerFactory;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.File;
import java.util.Arrays;
import java.util.List;

class BasicModule extends AbstractModule {
  private final ArtifactRepository repository;

  BasicModule(final ArtifactRepository repository) {
    this.repository = repository;
  }

  protected void configure() {
    this.bind(ILoggerFactory.class).toInstance(LoggerFactory.getILoggerFactory());
    this.bind(RepositorySystem.class).toInstance(new RepositorySystem() {
      @Nullable
      @Override
      public Artifact createArtifact(final String groupId, final String artifactId, final String version, final String packaging) {
        throw new RuntimeException("Not Implemented");
      }

      @Nullable
      @Override
      public Artifact createArtifact(final String groupId, final String artifactId, final String version, final String scope, final String type) {
        throw new RuntimeException("Not Implemented");
      }

      @Nullable
      @Override
      public Artifact createProjectArtifact(final String groupId, final String artifactId, final String version) {
        throw new RuntimeException("Not Implemented");
      }

      @Nullable
      @Override
      public Artifact createArtifactWithClassifier(final String groupId, final String artifactId, final String version, final String type, final String classifier) {
        throw new RuntimeException("Not Implemented");
      }

      @Nullable
      @Override
      public Artifact createPluginArtifact(final Plugin plugin) {
        throw new RuntimeException("Not Implemented");
      }

      @Nullable
      @Override
      public Artifact createDependencyArtifact(final Dependency dependency) {
        throw new RuntimeException("Not Implemented");
      }

      @Override
      public ArtifactRepository buildArtifactRepository(final Repository r) {
        return repository;
      }

      @Nullable
      @Override
      public ArtifactRepository createDefaultRemoteRepository() {
        throw new RuntimeException("Not Implemented");
      }

      @Nullable
      @Override
      public ArtifactRepository createDefaultLocalRepository() {
        throw new RuntimeException("Not Implemented");
      }

      @Nullable
      @Override
      public ArtifactRepository createLocalRepository(final File localRepository) {
        throw new RuntimeException("Not Implemented");
      }

      @Nullable
      @Override
      public ArtifactRepository createArtifactRepository(final String id, final String url1, final ArtifactRepositoryLayout repositoryLayout, final ArtifactRepositoryPolicy snapshots, final ArtifactRepositoryPolicy releases) {
        throw new RuntimeException("Not Implemented");
      }

      @Nonnull
      @Override
      public List<ArtifactRepository> getEffectiveRepositories(final List<ArtifactRepository> repositories) {
        return Arrays.asList(repository);
      }

      @Nullable
      @Override
      public Mirror getMirror(final ArtifactRepository repository1, final List<Mirror> mirrors) {
        throw new RuntimeException("Not Implemented");
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

      @Nullable
      @Override
      public ArtifactResolutionResult resolve(final ArtifactResolutionRequest request) {
        throw new RuntimeException("Not Implemented");
      }

      @Override
      public void publish(final ArtifactRepository repository1, final File source, final String remotePath, final ArtifactTransferListener transferListener) {
      }

      @Override
      public void retrieve(final ArtifactRepository repository1, final File destination, final String remotePath, final ArtifactTransferListener transferListener) {
      }
    });
  }
}
