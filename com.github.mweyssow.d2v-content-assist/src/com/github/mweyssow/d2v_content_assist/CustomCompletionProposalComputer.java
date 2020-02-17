package com.github.mweyssow.d2v_content_assist;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;

import java.util.List;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.eclipse.jface.text.contentassist.IContextInformation;
import org.eclipse.jface.text.contentassist.IContextInformationExtension;
import org.eclipse.jface.text.source.ISourceViewer;
import org.eclipse.ui.PartInitException;
// import org.eclipse.jface.text.contentassist.CompletionProposal;
import org.eclipse.jdt.ui.text.java.ContentAssistInvocationContext;
import org.eclipse.jdt.ui.text.java.JavaContentAssistInvocationContext;
import org.eclipse.jdt.ui.wizards.JavaCapabilityConfigurationPage;
import org.eclipse.jdt.ui.JavaUI;
import org.eclipse.jdt.ui.text.java.CompletionProposalCollector;
import org.eclipse.jdt.internal.ui.javaeditor.CompilationUnitEditor;
import org.eclipse.jdt.internal.ui.text.java.JavaAllCompletionProposalComputer;
import org.eclipse.jdt.internal.ui.text.java.JavaMethodCompletionProposal;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.NodeFinder;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.CompletionProposal;
import org.eclipse.jdt.core.IClassFile;
import org.eclipse.jdt.core.IClasspathEntry;



public class CustomCompletionProposalComputer extends JavaAllCompletionProposalComputer {
	@Override
	public List<ICompletionProposal> computeCompletionProposals(ContentAssistInvocationContext context, IProgressMonitor monitor)  {
		if (context instanceof JavaContentAssistInvocationContext) {
			JavaContentAssistInvocationContext javaContext = (JavaContentAssistInvocationContext) context;
			System.out.println(javaContext.getInvocationOffset());
			ICompilationUnit unit = javaContext.getCompilationUnit();
			
			ITextViewer viewer = javaContext.getViewer();
			CompletionProposalCollector collector = createCollector(javaContext);
			collector.setInvocationContext(javaContext);
			
			try {
				unit.codeComplete(23484, collector);
			} catch (JavaModelException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
			ICompletionProposal[] javaProposals = collector.getJavaCompletionProposals();
			System.out.println("Initial code proposals :");
			System.out.println("------------------------");
			for (ICompletionProposal prop : javaProposals) {
				if (prop instanceof JavaMethodCompletionProposal) {
					System.out.println(prop.toString());
				}
			}
			System.out.println("\n");
			
			
			ASTParser parser = ASTParser.newParser(AST.JLS12);
			parser.setSource(unit);
			parser.setKind(ASTParser.K_COMPILATION_UNIT);
			final CompilationUnit cu = (CompilationUnit) parser.createAST(null);
			
			cu.accept(new ASTVisitor() {
				public boolean visit(MethodDeclaration node) {
					System.out.println("Decl: " + node.getName());
					node.accept(new ASTVisitor() {
						public boolean visit(MethodInvocation node) {
							int position = node.getStartPosition();
							System.out.println("Inv: " + node.getParent().toString() + " " + node.getStartPosition());
							System.out.println(node.getName());
							return true;
						}
					});
					return true;
				}
			});
		}
		
		// ArrayList<ICompletionProposal> proposals1 = new ArrayList<ICompletionProposal>();
		return super.computeCompletionProposals(context, monitor);
	}
	
	public IPackageFragment[] getProjectFragments(String projectName) {
		// Get the root of the workspace	
		IWorkspace workspace = ResourcesPlugin.getWorkspace();
		IWorkspaceRoot root = workspace.getRoot();
		// Find project in the workspace
		IProject project = root.getProject(projectName);
		IJavaProject javaProject = JavaCore.create(project);
		
		try {
			// Return all fragments of the project
			return javaProject.getPackageFragments();
		} catch (JavaModelException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;
	}

	public void getProjectCompletion() {
		IPackageFragment[] packages = getProjectFragments("test-project");
		for (IPackageFragment packageFragment : packages) {
			try {
				for (final ICompilationUnit compilationUnit : packageFragment.getCompilationUnits()) {
					ASTParser parser = ASTParser.newParser(AST.JLS12);
					parser.setSource(compilationUnit);
					parser.setKind(ASTParser.K_COMPILATION_UNIT);
					final CompilationUnit cu = (CompilationUnit) parser.createAST(null);

					// System.out.println(compilationUnit.getSource());
				}
			} catch (JavaModelException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}
	
	@Override
	public void sessionStarted() {
	}

	@Override
	public void sessionEnded() {
	}
}
