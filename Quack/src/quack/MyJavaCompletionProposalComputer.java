package quack;

import java.io.StringBufferInputStream;
import java.util.List;
import java.util.Vector;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.ui.text.java.ContentAssistInvocationContext;
import org.eclipse.jdt.ui.text.java.IJavaCompletionProposalComputer;
import org.eclipse.jdt.ui.text.java.JavaContentAssistInvocationContext;
import org.eclipse.swt.graphics.Point;

public class MyJavaCompletionProposalComputer implements
        IJavaCompletionProposalComputer {

    public List computeCompletionProposals(
            ContentAssistInvocationContext context, IProgressMonitor monitor) {

        if (context instanceof JavaContentAssistInvocationContext) {
            JavaContentAssistInvocationContext jcontext = (JavaContentAssistInvocationContext) context;

            try {
//                Point sel = jcontext.getViewer().getSelectedRange();
//                List list = Main.getMain().getProposals(
//                        jcontext.getCompilationUnit(), jcontext.getDocument(),
//                        sel.x, sel.y);
//                return list;
            } catch (Exception e) {
                Main.getMain().log("MyJavaCompletionProposalComputer.java", e);
                throw new Error(e);
            }
        }
        return new Vector();
    }

    public List computeContextInformation(
            ContentAssistInvocationContext context, IProgressMonitor monitor) {

        return new Vector();
    }

    public String getErrorMessage() {
        return "Error in Quack completion proposal computer.";
    }

    public void sessionEnded() {
    }

    public void sessionStarted() {
    }
}
