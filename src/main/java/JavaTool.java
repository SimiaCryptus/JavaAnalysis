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

import org.eclipse.jdt.core.dom.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.HashMap;

public class JavaTool {
  protected static final Logger logger = LoggerFactory.getLogger(JavaTool.class);
  
  public static void main(String[] args) throws Exception {
    String root = args.length == 0 ? "H:\\SimiaCryptus\\MindsEye" : args[0];
    HashMap<String, CompilationUnit> parsedFiles = new SimpleMavenProject(root).parse();
    
    parsedFiles.forEach((file, ast) -> {
      logger.info("file: " + file);
      Arrays.stream(ast.getProblems()).forEach(problem -> {
        logger.warn("  ERR: " + problem.getMessage());
      });
      Arrays.stream(ast.getMessages()).forEach(problem -> {
        logger.info("  MSG: " + problem.getMessage());
      });
      ast.accept(new ASTVisitor() {
        String indent = "  ";
  
        @Override
        public void preVisit(final ASTNode node) {
          logger.info(String.format("  %s%s%s", node.getStartPosition(), indent, node.getClass().getSimpleName()));
          indent += "  ";
        }
  
        @Override
        public void postVisit(final ASTNode node) {
          indent = indent.substring(indent.length() - 2);
        }
  
        @Override
        public boolean visit(final MethodDeclaration node) {
          IMethodBinding iMethodBinding = node.resolveBinding();
          Javadoc javadoc = node.getJavadoc();
          logger.info(String.format("  %s::%s  %s",
            null == iMethodBinding ? null : iMethodBinding.getDeclaringClass().getQualifiedName(),
            null == iMethodBinding ? null : iMethodBinding.getName(),
            null == javadoc ? null : javadoc.tags()));
          return super.visit(node);
        }
      });
      
    });
  }
  
}
