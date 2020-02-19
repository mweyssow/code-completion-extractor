package com.github.mweyssow.d2v_content_assist;

import java.util.List;
import java.util.ArrayList;
import java.util.LinkedHashMap;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;

import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.eclipse.jface.text.source.ISourceViewer;
import org.eclipse.ui.PartInitException;
import org.eclipse.jdt.ui.text.java.ContentAssistInvocationContext;
import org.eclipse.jdt.ui.text.java.JavaContentAssistInvocationContext;
import org.eclipse.jdt.ui.JavaUI;
import org.eclipse.jdt.ui.text.java.CompletionProposalCollector;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.internal.ui.javaeditor.CompilationUnitEditor;
import org.eclipse.jdt.internal.ui.text.java.JavaAllCompletionProposalComputer;
import org.eclipse.jdt.internal.ui.text.java.JavaMethodCompletionProposal;


public class CustomCompletionProposalComputer extends JavaAllCompletionProposalComputer {
	@Override
	public List<ICompletionProposal> computeCompletionProposals(ContentAssistInvocationContext context, IProgressMonitor monitor)  {
		ArrayList<LinkedHashMap<String, LinkedHashMap<Integer, LinkedHashMap<String, Object>>>> proposals = new ArrayList<LinkedHashMap<String, LinkedHashMap<Integer, LinkedHashMap<String, Object>>>>();
		
		// Get the root of the workspace	
		IWorkspace workspace = ResourcesPlugin.getWorkspace();
		IWorkspaceRoot root = workspace.getRoot();
		
		IProject[] projects = root.getProjects();
		for (IProject project : projects) {
			// System.out.println(project.getName());
		}
		
		IPackageFragment[] packages = getProjectFragments("twitter4j-examples");
		
		int i = 0;
		for (IPackageFragment packageFragment : packages) {
			try {
				for (final ICompilationUnit compilationUnit : packageFragment.getCompilationUnits()) {
					// Limit the number of parsed file
					if (i == 2) break;
					
					// Retrieve valid CompilationUnit for this project
					String packageName = packageFragment.getElementName();
					System.out.println(packageName);
					System.out.println("=".repeat(packageName.length()));
					
					// Get CompilationUnit AST
					ASTParser parser = ASTParser.newParser(AST.JLS13);
					parser.setSource(compilationUnit);
					parser.setKind(ASTParser.K_COMPILATION_UNIT);
					final CompilationUnit cu = (CompilationUnit) parser.createAST(null);
					
					// Load compilation unit in the editor to build a content assist collector
					CompilationUnitEditor cuEditor = (CompilationUnitEditor) JavaUI.openInEditor(compilationUnit);
					ISourceViewer viewer = cuEditor.getViewer();
					IDocument document = viewer.getDocument();
					
					// Build the content assist collector using the current context (compilation unit)
					JavaContentAssistInvocationContext javaContext = new JavaContentAssistInvocationContext(compilationUnit);
					CompletionProposalCollector collector = createCollector(javaContext);
					collector.setInvocationContext(javaContext);
					
					List<Integer> offsets = new ArrayList<Integer>();
					// Retrieve CU completion offsets
					cu.accept(new ASTVisitor() {
						public boolean visit(MethodDeclaration node) {
							String methodDeclName = node.getName().toString();
							System.out.println("Decl: " + methodDeclName);
							LinkedHashMap<String, LinkedHashMap<Integer, LinkedHashMap<String, Object>>> methodProposals = new LinkedHashMap<String, LinkedHashMap<Integer, LinkedHashMap<String, Object>>>();
							methodProposals.put(methodDeclName, new LinkedHashMap<Integer, LinkedHashMap<String, Object>>());
							
							node.accept(new ASTVisitor() {
								public boolean visit(MethodInvocation node) {
									System.out.println("  Inv: " + node.getParent().toString());
									System.out.println("  Method invk: " + node.getName());
									
									int currentNodePosition = node.getStartPosition();
									// Retrieve the content before the invocation method in the current node
									String beforeMethodInvk = node.toString().split(node.getName().toString())[0];
									Integer offset = currentNodePosition + beforeMethodInvk.length();
									System.out.println("  Invocation offset:" + offset + "\n");
									
									methodProposals.get(methodDeclName).put(offset, new LinkedHashMap<String, Object>(){{
										put("name", node.getName());
									}});
									offsets.add(offset);
									return true;
								} 
							});
							
							// Retrieve all completion made for each offset
							for (int offset : offsets) {
								try {
									compilationUnit.codeComplete(offset, collector);
									ICompletionProposal[] javaProposals = collector.getJavaCompletionProposals();
									ArrayList<String> props = new ArrayList<String>();
									System.out.println("Initial code proposals :");
									System.out.println("------------------------");
									for (ICompletionProposal prop : javaProposals) {
										if (prop instanceof JavaMethodCompletionProposal) {
											String proposalName = ((JavaMethodCompletionProposal) prop).getJavaElement().getElementName();
											props.add(proposalName);
											System.out.println(proposalName);
										}
									}
									methodProposals.get(methodDeclName).get(offset).put("proposals", props);
									System.out.println("\n");
								} catch (JavaModelException e) {
									// TODO Auto-generated catch block
									e.printStackTrace();
								}
							}
							
							proposals.add(methodProposals);
							return true;
						}
					});
					
					System.out.println(proposals.toString());
					i++;
				}
				
			} catch (JavaModelException | PartInitException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		System.out.println("Number of CU : " + i);
		
		if (context instanceof JavaContentAssistInvocationContext) {
			// Get offset of plug-in invocation 
			JavaContentAssistInvocationContext javaContext = (JavaContentAssistInvocationContext) context;
			System.out.println(javaContext.getInvocationOffset());
		}
		
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

	@Override
	public void sessionStarted() {}

	@Override
	public void sessionEnded() {}
}
