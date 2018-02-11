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

import org.apache.maven.project.ProjectBuildingException;
import org.codehaus.plexus.PlexusContainerException;
import org.codehaus.plexus.component.repository.exception.ComponentLookupException;
import org.eclipse.jdt.core.dom.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;

/**
 * Analyzes a project based on class member dependencies
 */
public class DependencyScanner extends SimpleMavenProject {
  private static final Logger logger = LoggerFactory.getLogger(DependencyScanner.class);
  
  /**
   * Instantiates a new Simple maven project.
   *
   * @param projectRoot the project root
   * @throws IOException              the io exception
   * @throws PlexusContainerException the plexus container exception
   * @throws ComponentLookupException the component lookup exception
   * @throws ProjectBuildingException the project building exception
   */
  public DependencyScanner(final String projectRoot) throws IOException, PlexusContainerException, ComponentLookupException, ProjectBuildingException {
    super(projectRoot);
  }
  
  /**
   * A sample CLI application which loads a maven java project and prints out the parse tree.
   *
   * @param args the input arguments
   * @throws Exception the exception
   */
  public static void main(String[] args) throws Exception {
    String root = args.length == 0 ? "H:\\SimiaCryptus\\MindsEye" : args[0];
    DependencyScanner mavenProject = new DependencyScanner(root);
    mavenProject.resolve().getDependencies().forEach((org.eclipse.aether.graph.Dependency dependency) -> {
      logger.info(String.format("Dependency: %s (%s)", dependency.getArtifact().getFile().getAbsolutePath(), dependency));
    });
    HashMap<String, CompilationUnit> parsedFiles = mavenProject.parse();
    parsedFiles.forEach((file, ast) -> {
      logger.info("File: " + file);
      Arrays.stream(ast.getProblems()).forEach(problem -> {
        logger.warn("  ERR: " + problem.getMessage());
      });
      Arrays.stream(ast.getMessages()).forEach(problem -> {
        logger.info("  MSG: " + problem.getMessage());
      });
      ast.accept(new ASTVisitor() {
        String indent = "  ";
        MethodDeclaration currentMethod;
        FieldDeclaration currentField;
        
        @Override
        public void preVisit(final ASTNode node) {
          indent += "  ";
          logger.info(String.format("  %s%s%s", node.getStartPosition(), indent, node.getClass().getSimpleName()));
        }
        
        @Override
        public void postVisit(final ASTNode node) {
          if (indent.length() < 2) throw new IllegalStateException();
          indent = indent.substring(indent.length() - 2);
        }
        
        @Override
        public boolean visit(final MethodInvocation node) {
          String currentContext;
          if (null != currentMethod) {
            currentContext = getMethodStr(currentMethod.resolveBinding());
          }
          else if (null != currentField) {
            currentContext = currentField.toString();
          }
          else {
            currentContext = "???";
          }
          logger.info(String.format("  Reference: %s -> %s",
            currentContext,
            null == node ? null : getMethodStr(node.resolveMethodBinding())
          ));
          return super.visit(node);
        }
        
        @Override
        public boolean visit(final FieldAccess node) {
          String currentContext;
          if (null != currentMethod) {
            currentContext = getMethodStr(currentMethod.resolveBinding());
          }
          else if (null != currentField) {
            currentContext = currentField.toString();
          }
          else {
            currentContext = "???";
          }
          logger.info(String.format("  Reference: %s -> %s",
            currentContext,
            null == node ? null : getFieldStr(node.resolveFieldBinding())
          ));
          return super.visit(node);
        }
        
        @Override
        public boolean visit(final FieldDeclaration node) {
          currentField = node;
          currentMethod = null;
          Javadoc javadoc = node.getJavadoc();
          logger.info(String.format("  Field %s",
            node.toString().replaceAll("\n", "\n    ").trim()));
          return super.visit(node);
        }
        
        @Override
        public boolean visit(final MethodDeclaration node) {
          currentMethod = node;
          currentField = null;
          IMethodBinding iMethodBinding = node.resolveBinding();
          Javadoc javadoc = node.getJavadoc();
          logger.info(String.format("  Method %s::%s\n    %s",
            null == iMethodBinding ? null : iMethodBinding.getDeclaringClass().getBinaryName(),
            null == iMethodBinding ? null : iMethodBinding.getName(),
            node.toString().replaceAll("\n", "\n    ").trim()));
          return super.visit(node);
        }
        
      });
      
      
    });
  }
  
  @Nonnull
  private static String getFieldStr(final IVariableBinding iVariableBinding) {
    if (null == iVariableBinding) return null;
    ITypeBinding declaringClass = iVariableBinding.getDeclaringClass();
    return (null == declaringClass ? "?" : declaringClass.getBinaryName()) + "::" + iVariableBinding.getName();
  }
  
  @Nonnull
  private static String getMethodStr(final IMethodBinding iMethodBinding) {
    ITypeBinding declaringClass = iMethodBinding.getDeclaringClass();
    return declaringClass.getBinaryName() + "::" + iMethodBinding.getName();
  }
}
