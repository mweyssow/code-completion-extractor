package com.github.mweyssow.d2v_content_assist;

import java.util.ArrayList;
import java.util.List;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.eclipse.jface.text.contentassist.IContextInformation;
import org.eclipse.jface.text.contentassist.IContextInformationExtension;

// import org.eclipse.jface.text.contentassist.CompletionProposal;
import org.eclipse.jdt.ui.text.java.ContentAssistInvocationContext;
import org.eclipse.jdt.ui.text.java.JavaContentAssistInvocationContext;
import org.eclipse.jdt.ui.text.java.CompletionProposalCollector;

import org.eclipse.jdt.internal.ui.text.java.JavaAllCompletionProposalComputer;
import org.eclipse.jdt.internal.ui.text.java.JavaMethodCompletionProposal;

import org.eclipse.core.runtime.IProgressMonitor;
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
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.CompletionProposal;

//import org.eclipse.jdt.ui.text.java.IJavaCompletionProposalComputer;
// import org.eclipse.ui.texteditor.HippieProposalProcessor;


public class CustomCompletionProposalComputer extends JavaAllCompletionProposalComputer {
	@Override
	public List<ICompletionProposal> computeCompletionProposals(ContentAssistInvocationContext context, IProgressMonitor monitor)  {
		if (context instanceof JavaContentAssistInvocationContext) {
			JavaContentAssistInvocationContext javaContext = (JavaContentAssistInvocationContext) context;
			ICompilationUnit unit = javaContext.getCompilationUnit();
			
			ITextViewer viewer = javaContext.getViewer();
			CompletionProposalCollector collector = createCollector(javaContext);
			collector.setInvocationContext(javaContext);
			
			try {
				unit.codeComplete(296, collector);
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

	@Override
	public List<IContextInformation> computeContextInformation(ContentAssistInvocationContext context, IProgressMonitor monitor) {
		return super.computeContextInformation(context, monitor);
	}

	@Override
	public String getErrorMessage() {
		return super.getErrorMessage();
	}
	
	@Override
	public void sessionStarted() {
	}

	@Override
	public void sessionEnded() {
	}
	
	private int getCurrentPosition(IDocument doc, ContentAssistInvocationContext ctx) 
			throws BadLocationException {
		return doc.getLineOfOffset(ctx.getInvocationOffset());
	}
	
}


